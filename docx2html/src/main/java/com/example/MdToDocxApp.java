package com.example;

import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.html.HtmlRenderer;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.UnderlinePatterns;
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

		Document doc = Jsoup.parse(html);
		// Remove tables and images entirely
		doc.select("table, thead, tbody, tfoot, tr, td, th, img, figure").remove();

		try (XWPFDocument xdoc = new XWPFDocument(); OutputStream os = new FileOutputStream(outputDocxPath.toFile())) {
			for (Element el : doc.body().children()) {
				appendBlockElementToDoc(xdoc, el);
			}
			xdoc.write(os);
		}
	}

	private static void appendBlockElementToDoc(XWPFDocument xdoc, Element el) {
		String tag = el.tagName().toLowerCase();
		switch (tag) {
			case "h1":
				createParagraphFromInline(xdoc, el, 26, true, isCentered(el));
				break;
			case "h2":
				createParagraphFromInline(xdoc, el, 22, true, isCentered(el));
				break;
			case "h3":
				createParagraphFromInline(xdoc, el, 18, true, isCentered(el));
				break;
			case "h4":
				createParagraphFromInline(xdoc, el, 16, true, isCentered(el));
				break;
			case "h5":
				createParagraphFromInline(xdoc, el, 14, true, isCentered(el));
				break;
			case "h6":
				createParagraphFromInline(xdoc, el, 12, true, isCentered(el));
				break;
			case "p":
			case "div":
			case "center":
				createParagraphFromInline(xdoc, el, 12, false, isCentered(el) || tag.equals("center"));
				break;
			case "blockquote": {
				XWPFParagraph p = xdoc.createParagraph();
				p.setIndentationLeft(720); // 720 twips = 0.5 inch
				if (isCentered(el)) p.setAlignment(ParagraphAlignment.CENTER);
				appendInlineContent(p, el, 12, false);
				break;
			}
			case "pre":
				createCodeBlock(xdoc, el);
				break;
			case "ul":
			case "ol":
				for (Element li : el.children()) {
					if (!li.tagName().equalsIgnoreCase("li")) continue;
					createParagraphFromInline(xdoc, li, 12, false, isCentered(el));
				}
				break;
			case "hr": {
				XWPFParagraph p = xdoc.createParagraph();
				XWPFRun r = p.createRun();
				r.setText("────────");
				break;
			}
			default:
				// Fallback: treat as paragraph
				createParagraphFromInline(xdoc, el, 12, false, isCentered(el));
		}
	}

	private static void createParagraphFromInline(XWPFDocument xdoc, Element el, int fontSize, boolean bold, boolean centered) {
		XWPFParagraph p = xdoc.createParagraph();
		if (centered) p.setAlignment(ParagraphAlignment.CENTER);
		appendInlineContent(p, el, fontSize, bold);
	}

	private static void appendInlineContent(XWPFParagraph p, Element el, int baseFontSize, boolean baseBold) {
		for (org.jsoup.nodes.Node node : el.childNodes()) {
			if (node instanceof TextNode) {
				String text = ((TextNode) node).text();
				appendTextRun(p, text, baseFontSize, baseBold, false, false);
			} else if (node instanceof Element) {
				Element child = (Element) node;
				String tag = child.tagName().toLowerCase();
				switch (tag) {
					case "strong":
					case "b":
						appendInlineContentWithOverrides(p, child, baseFontSize, true, false, false);
						break;
					case "em":
					case "i":
						appendInlineContentWithOverrides(p, child, baseFontSize, baseBold, true, false);
						break;
					case "u":
						appendInlineContentWithOverrides(p, child, baseFontSize, baseBold, false, true);
						break;
					case "code": {
						String codeText = child.text();
						XWPFRun r = p.createRun();
						r.setText(codeText);
						r.setFontFamily("Courier New");
						r.setFontSize(baseFontSize);
						break;
					}
					case "br": {
						XWPFRun r = p.createRun();
						r.addBreak();
						break;
					}
					case "span":
					case "small":
						appendInlineContent(p, child, baseFontSize, baseBold);
						break;
					default:
						appendInlineContent(p, child, baseFontSize, baseBold);
				}
			}
		}
	}

	private static void appendInlineContentWithOverrides(XWPFParagraph p, Element el, int fontSize, boolean bold, boolean italic, boolean underline) {
		for (org.jsoup.nodes.Node node : el.childNodes()) {
			if (node instanceof TextNode) {
				appendTextRun(p, ((TextNode) node).text(), fontSize, bold, italic, underline);
			} else if (node instanceof Element) {
				appendInlineContentWithOverrides(p, (Element) node, fontSize, bold, italic, underline);
			}
		}
	}

	private static void appendTextRun(XWPFParagraph p, String text, int fontSize, boolean bold, boolean italic, boolean underline) {
		if (text == null || text.isEmpty()) return;
		String[] parts = text.split("\r?\n", -1);
		for (int i = 0; i < parts.length; i++) {
			XWPFRun r = p.createRun();
			r.setText(parts[i]);
			r.setFontSize(fontSize);
			r.setBold(bold);
			r.setItalic(italic);
			if (underline) r.setUnderline(UnderlinePatterns.SINGLE);
			if (i < parts.length - 1) r.addBreak();
		}
	}

	private static void createCodeBlock(XWPFDocument xdoc, Element pre) {
		XWPFParagraph p = xdoc.createParagraph();
		for (org.jsoup.nodes.Node node : pre.childNodes()) {
			if (node instanceof TextNode) {
				String text = ((TextNode) node).text();
				XWPFRun r = p.createRun();
				r.setFontFamily("Courier New");
				r.setText(text);
				r.addBreak();
			} else if (node instanceof Element) {
				Element child = (Element) node;
				if (child.tagName().equalsIgnoreCase("code")) {
					XWPFRun r = p.createRun();
					r.setFontFamily("Courier New");
					r.setText(child.text());
					r.addBreak();
				}
			}
		}
	}

	private static boolean isCentered(Element el) {
		String align = el.hasAttr("align") ? el.attr("align").toLowerCase() : "";
		if ("center".equals(align)) return true;
		String style = el.hasAttr("style") ? el.attr("style").toLowerCase() : "";
		if (style.contains("text-align:center") || style.contains("text-align: center")) return true;
		String classAttr = el.className().toLowerCase();
		return classAttr.contains("text-center") || classAttr.contains("center");
	}
}