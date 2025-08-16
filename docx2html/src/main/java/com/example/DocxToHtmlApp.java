package com.example;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFPictureData;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import fr.opensagres.poi.xwpf.converter.xhtml.XHTMLConverter;
import fr.opensagres.poi.xwpf.converter.xhtml.XHTMLOptions;
import fr.opensagres.poi.xwpf.converter.core.BasicURIResolver;
import fr.opensagres.poi.xwpf.converter.core.FileImageExtractor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblGridCol;
import com.vladsch.flexmark.html2md.converter.FlexmarkHtmlConverter;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class DocxToHtmlApp {

	private static final int A4_WIDTH_PX = 794; // ~8.27in * 96dpi

	public static void main(String[] args) {
		if (args.length < 2) {
			System.out.println("Usage: java -jar docx2html.jar <input.docx> <output.md>");
			System.out.println("Or with Gradle: gradle run -PappArgs=\"[/abs/input.docx,/abs/output.md]\"");
			return;
		}

		Path inputPath = Paths.get(args[0]).toAbsolutePath().normalize();
		Path outputPath = Paths.get(args[1]).toAbsolutePath().normalize();

		try {
			convertDocxToMarkdown(inputPath, outputPath);
			System.out.println("Converted: " + inputPath + " -> " + outputPath);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	public static void convertDocxToMarkdown(Path inputDocxPath, Path outputMdPath) throws Exception {
		Objects.requireNonNull(inputDocxPath, "inputDocxPath");
		Objects.requireNonNull(outputMdPath, "outputMdPath");

		Files.createDirectories(outputMdPath.getParent());

		Path tempImagesDir = outputMdPath.getParent().resolve("images-" + UUID.randomUUID());
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
			html = enhanceTables(html, document);
			html = enhanceImages(html);

			// Replace tables and images with tokens to preserve as raw HTML in Markdown
			PlaceholderStore store = replaceTablesAndImagesWithTokens(html);

			String markdown = FlexmarkHtmlConverter.builder().build().convert(store.tokenizedHtml);

			// Replace tokens with raw HTML blocks to keep centering and width styles
			for (Map.Entry<String, String> entry : store.tokenToHtml.entrySet()) {
				markdown = markdown.replace(entry.getKey(), entry.getValue());
			}

			try (OutputStream os = new FileOutputStream(outputMdPath.toFile());
				 BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8))) {
				writer.write(markdown);
			}
		} finally {
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

	private static String enhanceImages(String html) {
		Document doc = Jsoup.parse(html);
		Elements imgs = doc.getElementsByTag("img");
		for (Element img : imgs) {
			appendInlineStyle(img, "display:block;margin:0 auto;max-width:" + A4_WIDTH_PX + "px;width:100%;height:auto;");
		}
		return doc.outerHtml();
	}

	private static String enhanceTables(String html, XWPFDocument xwpfDocument) {
		Document doc = Jsoup.parse(html);
		Elements htmlTables = doc.getElementsByTag("table");
		List<XWPFTable> xwpfTables = xwpfDocument.getTables();

		int pairCount = Math.min(htmlTables.size(), xwpfTables.size());
		for (int i = 0; i < pairCount; i++) {
			Element htmlTable = htmlTables.get(i);
			XWPFTable xTable = xwpfTables.get(i);

			// Center table and fit A4 width
			appendInlineStyle(htmlTable, "border-collapse:collapse;table-layout:fixed;" +
				"margin-left:auto;margin-right:auto;" +
				"max-width:" + A4_WIDTH_PX + "px;width:100%;box-sizing:border-box;");

			List<Integer> colWidthsPx = extractColumnWidthsPx(xTable);
			if (!colWidthsPx.isEmpty()) {
				Element colgroup = doc.createElement("colgroup");
				for (Integer w : colWidthsPx) {
					Element col = doc.createElement("col");
					if (w != null && w > 0) {
						col.attr("style", "width: " + w + "px");
					}
					colgroup.appendChild(col);
				}
				htmlTable.prependChild(colgroup);
			}

			// Add inline styles to cells to ensure visible borders/padding
			Elements cells = htmlTable.select("td, th");
			for (Element cell : cells) {
				appendInlineStyle(cell, "border:1px solid #ccc;padding:4px;word-break:break-word;white-space:normal;overflow-wrap:anywhere;");
			}
		}

		return doc.outerHtml();
	}

	private static void appendInlineStyle(Element el, String styleToAppend) {
		String style = el.hasAttr("style") ? el.attr("style") : "";
		if (!style.endsWith(";") && !style.isEmpty()) {
			style += ";";
		}
		style += styleToAppend;
		el.attr("style", style);
	}

	private static List<Integer> extractColumnWidthsPx(XWPFTable table) {
		List<Integer> widthsPx = new ArrayList<>();
		if (table == null || table.getCTTbl() == null || table.getCTTbl().getTblGrid() == null) {
			return widthsPx;
		}
		List<CTTblGridCol> gridCols = table.getCTTbl().getTblGrid().getGridColList();
		for (CTTblGridCol col : gridCols) {
			Object wObj = col.getW();
			if (wObj != null) {
				long twips;
				if (wObj instanceof BigInteger) {
					twips = ((BigInteger) wObj).longValue();
				} else if (wObj instanceof Long) {
					twips = (Long) wObj;
				} else if (wObj instanceof Integer) {
					twips = ((Integer) wObj).longValue();
				} else {
					try {
						twips = Long.parseLong(wObj.toString());
					} catch (NumberFormatException e) {
						widthsPx.add(null);
						continue;
					}
				}
				int px = (int) Math.round(twips / 15.0); // 1 px ≈ 15 twips (96 dpi)
				widthsPx.add(Math.max(px, 1));
			} else {
				widthsPx.add(null);
			}
		}
		return widthsPx;
	}

	private static PlaceholderStore replaceTablesAndImagesWithTokens(String html) {
		Document doc = Jsoup.parse(html);
		Map<String, String> tokenToHtml = new LinkedHashMap<>();
		int tableIdx = 0;
		int imgIdx = 0;

		// Tables -> token
		for (Element table : new ArrayList<>(doc.getElementsByTag("table"))) {
			String token = "MDPH_TABLE_" + (tableIdx++);
			String htmlBlock = table.outerHtml();
			Element p = doc.createElement("p");
			p.appendChild(new TextNode(token));
			table.replaceWith(p);
			tokenToHtml.put(token, htmlBlock);
		}

		// Images -> token (wrap in centered paragraph)
		for (Element img : new ArrayList<>(doc.getElementsByTag("img"))) {
			String token = "MDPH_IMG_" + (imgIdx++);
			Element wrapper = doc.createElement("p");
			wrapper.attr("style", "text-align:center;margin:8px 0;");
			wrapper.appendChild(img.clone());
			String htmlBlock = wrapper.outerHtml();
			img.replaceWith(new TextNode(token));
			tokenToHtml.put(token, htmlBlock);
		}

		PlaceholderStore store = new PlaceholderStore();
		store.tokenizedHtml = doc.outerHtml();
		store.tokenToHtml = tokenToHtml;
		return store;
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

	private static class PlaceholderStore {
		String tokenizedHtml;
		Map<String, String> tokenToHtml;
	}
}