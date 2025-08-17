package com.example;

import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.html.HtmlRenderer;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MdToDocxApp {

	public static void main(String[] args) {
		if (args.length < 2) {
			System.out.println("Usage: java -cp build/libs/* com.example.MdToDocxApp <input.md> <output.docx>");
			System.out.println("Or with Gradle: gradle run -PmainClass=com.example.MdToDocxApp -PappArgs=\"[/abs/input.md,/abs/output.docx]\"");
			return;
		}

		Path inputPath = Paths.get(args[0]).toAbsolutePath().normalize();
		Path outputPath = Paths.get(args[1]).toAbsolutePath().normalize();

		try {
			convertMarkdownToDocx(inputPath, outputPath);
			System.out.println("Converted: " + inputPath + " -> " + outputPath);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	public static void convertMarkdownToDocx(Path inputMdPath, Path outputDocxPath) throws Exception {
		Objects.requireNonNull(inputMdPath, "inputMdPath");
		Objects.requireNonNull(outputDocxPath, "outputDocxPath");

		Files.createDirectories(outputDocxPath.getParent());

		String markdown = new String(Files.readAllBytes(inputMdPath), StandardCharsets.UTF_8);

		Parser parser = Parser.builder().build();
		Node mdDocument = parser.parse(markdown);
		HtmlRenderer renderer = HtmlRenderer.builder().build();
		String html = renderer.render(mdDocument);

		// Strip tables and images, keep only text content structure
		List<String> paragraphs = extractTextParagraphs(html);

		try (XWPFDocument doc = new XWPFDocument(); OutputStream os = new FileOutputStream(outputDocxPath.toFile())) {
			for (String para : paragraphs) {
				if (para.trim().isEmpty()) continue;
				XWPFParagraph p = doc.createParagraph();
				XWPFRun run = p.createRun();
				writeTextWithLineBreaks(run, para);
			}
			doc.write(os);
		}
	}

	private static List<String> extractTextParagraphs(String html) {
		Document doc = Jsoup.parse(html);
		// Remove tables and images entirely
		Elements remove = doc.select("table, thead, tbody, tfoot, tr, td, th, img, figure");
		remove.remove();

		List<String> paragraphs = new ArrayList<>();

		// Headings and paragraphs
		for (Element el : doc.select("h1,h2,h3,h4,h5,h6,p,blockquote,pre,li")) {
			String text = collectOwnAndDescendantText(el).trim();
			if (!text.isEmpty()) {
				paragraphs.add(text);
			}
		}

		// Fallback: capture stray text nodes under body not wrapped by blocks
		for (TextNode tn : doc.body().textNodes()) {
			String text = tn.text().trim();
			if (!text.isEmpty()) paragraphs.add(text);
		}

		return paragraphs;
	}

	private static String collectOwnAndDescendantText(Element el) {
		// Jsoup's text() collapses whitespace; acceptable for plain-text output
		return el.text();
	}

	private static void writeTextWithLineBreaks(XWPFRun run, String text) {
		String[] lines = text.split("\r?\n");
		for (int i = 0; i < lines.length; i++) {
			if (i > 0) run.addBreak();
			run.setText(lines[i]);
		}
	}
}