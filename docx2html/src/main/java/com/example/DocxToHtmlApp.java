package com.example;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFPictureData;
import fr.opensagres.poi.xwpf.converter.xhtml.XHTMLConverter;
import fr.opensagres.poi.xwpf.converter.xhtml.XHTMLOptions;
import fr.opensagres.poi.xwpf.converter.core.BasicURIResolver;
import fr.opensagres.poi.xwpf.converter.core.FileImageExtractor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class DocxToHtmlApp {

	public static void main(String[] args) {
		if (args.length < 2) {
			System.out.println("Usage: java -jar docx2html.jar <input.docx> <output.html>");
			System.out.println("Or with Gradle: ./gradlew run --args=\"/abs/path/input.docx /abs/path/output.html\"");
			return;
		}

		Path inputPath = Paths.get(args[0]).toAbsolutePath().normalize();
		Path outputPath = Paths.get(args[1]).toAbsolutePath().normalize();

		try {
			convertDocxToHtml(inputPath, outputPath);
			System.out.println("Converted: " + inputPath + " -> " + outputPath);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	public static void convertDocxToHtml(Path inputDocxPath, Path outputHtmlPath) throws Exception {
		Objects.requireNonNull(inputDocxPath, "inputDocxPath");
		Objects.requireNonNull(outputHtmlPath, "outputHtmlPath");

		Files.createDirectories(outputHtmlPath.getParent());

		// Temporary folder to let the converter write images. We'll inline them after conversion and then delete this folder.
		Path tempImagesDir = outputHtmlPath.getParent().resolve("images-" + UUID.randomUUID());
		Files.createDirectories(tempImagesDir);

		try (InputStream inputStream = new FileInputStream(inputDocxPath.toFile());
			 XWPFDocument document = new XWPFDocument(inputStream)) {

			XHTMLOptions options = XHTMLOptions.create();
			options.setIgnoreStylesIfUnused(false);
			options.setFragment(false);
			options.setExtractor(new FileImageExtractor(tempImagesDir.toFile()));
			options.URIResolver(new BasicURIResolver(tempImagesDir.getFileName().toString()));

			ByteArrayOutputStream htmlOut = new ByteArrayOutputStream();
			XHTMLConverter.getInstance().convert(document, htmlOut, options);

			String html = new String(htmlOut.toByteArray(), StandardCharsets.UTF_8);
			html = ensureMetaUtf8(html);
			html = embedImagesAsBase64(html, document.getAllPictures());

			try (OutputStream os = new FileOutputStream(outputHtmlPath.toFile());
				 BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8))) {
				writer.write(html);
			}
		} finally {
			// Clean up temp images dir
			deleteDirectoryQuietly(tempImagesDir);
		}
	}

	private static String ensureMetaUtf8(String html) {
		Document doc = Jsoup.parse(html);
		Element head = doc.head();
		boolean hasMeta = head.select("meta[charset]").stream().anyMatch(e -> "utf-8".equalsIgnoreCase(e.attr("charset")));
		if (!hasMeta) {
			head.prepend("<meta charset=\"UTF-8\">");
		}
		return doc.outerHtml();
	}

	private static String embedImagesAsBase64(String html, List<XWPFPictureData> pictures) throws IOException {
		if (pictures == null || pictures.isEmpty()) {
			return html;
		}

		Document doc = Jsoup.parse(html);
		Elements imgElements = doc.getElementsByTag("img");

		for (Element img : imgElements) {
			String src = img.attr("src");
			if (src == null || src.isEmpty()) {
				continue;
			}
			for (XWPFPictureData pic : pictures) {
				String fileName = pic.getFileName();
				if (src.endsWith(fileName)) {
					String extension = pic.suggestFileExtension();
					String base64 = Base64.getEncoder().encodeToString(pic.getData());
					String dataUri = "data:image/" + extension + ";base64," + base64;
					img.attr("src", dataUri);
					break;
				}
			}
		}
		return doc.outerHtml();
	}

	private static void deleteDirectoryQuietly(Path dir) {
		if (dir == null) return;
		try {
			if (Files.notExists(dir)) return;
			Files.walk(dir)
				.sorted((a, b) -> b.getNameCount() - a.getNameCount())
				.forEach(path -> {
					try { Files.deleteIfExists(path); } catch (IOException ignored) {}
				});
		} catch (IOException ignored) {}
	}
}