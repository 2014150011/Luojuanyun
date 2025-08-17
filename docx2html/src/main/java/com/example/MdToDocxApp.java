package com.example;

import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.html.HtmlRenderer;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.UnderlinePatterns;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFNumbering;
import org.apache.poi.xwpf.usermodel.LineSpacingRule;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTAbstractNum;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTLvl;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STNumberFormat;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTFonts;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPr;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.ArrayList;
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
			ListNumberingIds listIds = ensureListNumberings(xdoc);
			RenderState state = new RenderState();
			List<Element> children = new ArrayList<>(doc.body().children());
			for (int i = 0; i < children.size(); i++) {
				Element el = children.get(i);
				if ((isSmallHeading(el) || isShortHeading(el)) && i + 1 < children.size() && isParagraphish(children.get(i + 1))) {
					appendInlineHeadingWithParagraph(xdoc, el, children.get(i + 1));
					state.seenAnyHeading = true;
					i++; // skip next since merged
				} else {
					appendBlockElementToDoc(xdoc, el, listIds, 0, state);
				}
			}
			xdoc.write(os);
		}
	}

	private static boolean isSmallHeading(Element el) {
		String tag = el.tagName().toLowerCase();
		return tag.equals("h5") || tag.equals("h6") || tag.equals("h4");
	}

	private static boolean isShortHeading(Element el) {
		String tag = el.tagName().toLowerCase();
		if (!(tag.equals("h2") || tag.equals("h3"))) return false;
		String text = el.text() == null ? "" : el.text().trim();
		return text.length() > 0 && text.length() <= 10; // short subtitle merge threshold
	}

	private static boolean isParagraphish(Element el) {
		String tag = el.tagName().toLowerCase();
		return tag.equals("p") || tag.equals("div") || tag.equals("center");
	}

	private static void appendInlineHeadingWithParagraph(XWPFDocument xdoc, Element heading, Element paragraphEl) {
		XWPFParagraph p = xdoc.createParagraph();
		p.setSpacingBetween(1.0, LineSpacingRule.AUTO);
		p.setSpacingBefore(0);
		p.setSpacingAfter(0);
		// heading part (bold, slightly larger)
		int hSize = headingFontSize(heading.tagName());
		appendInlineContentWithOverrides(p, heading, hSize, true, false, false);
		XWPFRun sep = p.createRun();
		sep.setText(" ");
		applyRunFontDefaults(sep, true);
		// content part
		appendInlineContent(p, paragraphEl, 12, false);
	}

	private static int headingFontSize(String tagName) {
		String t = tagName.toLowerCase();
		if (t.equals("h2")) return 22;
		if (t.equals("h3")) return 18;
		if (t.equals("h4")) return 16;
		if (t.equals("h5")) return 14;
		return 12;
	}

	private static boolean isHeadingTag(String tag) {
		return tag.equals("h1") || tag.equals("h2") || tag.equals("h3") || tag.equals("h4") || tag.equals("h5") || tag.equals("h6");
	}

	private static void appendBlockElementToDoc(XWPFDocument xdoc, Element el, ListNumberingIds listIds, int listLevel, RenderState state) {
		String tag = el.tagName().toLowerCase();
		switch (tag) {
			case "h1": {
				boolean center = !state.mainTitleDone;
				state.mainTitleDone = true;
				state.seenAnyHeading = true;
				createParagraphFromInline(xdoc, el, 26, true, center, "Heading1");
				break;
			}
			case "h2": {
				boolean center = !state.mainTitleDone && !state.seenAnyHeading;
				if (!state.mainTitleDone && center) state.mainTitleDone = true;
				state.seenAnyHeading = true;
				createParagraphFromInline(xdoc, el, 22, true, center, "Heading2");
				break;
			}
			case "h3": {
				boolean center = !state.mainTitleDone && !state.seenAnyHeading;
				if (!state.mainTitleDone && center) state.mainTitleDone = true;
				state.seenAnyHeading = true;
				createParagraphFromInline(xdoc, el, 18, true, center, "Heading3");
				break;
			}
			case "h4":
				state.seenAnyHeading = true;
				createParagraphFromInline(xdoc, el, 16, true, false, "Heading4");
				break;
			case "h5":
				state.seenAnyHeading = true;
				createParagraphFromInline(xdoc, el, 14, true, false, "Heading5");
				break;
			case "h6":
				state.seenAnyHeading = true;
				createParagraphFromInline(xdoc, el, 12, true, false, "Heading6");
				break;
			case "p":
			case "div":
			case "center":
				createParagraphFromInline(xdoc, el, 12, false, isCentered(el) || tag.equals("center"), null);
				break;
			case "blockquote": {
				XWPFParagraph p = xdoc.createParagraph();
				p.setSpacingBetween(1.0, LineSpacingRule.AUTO);
				p.setSpacingBefore(0);
				p.setSpacingAfter(0);
				p.setIndentationLeft(720);
				if (isCentered(el)) p.setAlignment(ParagraphAlignment.CENTER);
				appendInlineContent(p, el, 12, false);
				break;
			}
			case "pre":
				createCodeBlock(xdoc, el);
				break;
			case "ul":
				appendList(xdoc, el, listIds, false, listLevel, state);
				break;
			case "ol":
				appendList(xdoc, el, listIds, true, listLevel, state);
				break;
			case "hr": {
				XWPFParagraph p = xdoc.createParagraph();
				p.setSpacingBetween(1.0, LineSpacingRule.AUTO);
				p.setSpacingBefore(0);
				p.setSpacingAfter(0);
				XWPFRun r = p.createRun();
				r.setText("────────");
				applyRunFontDefaults(r, false);
				break;
			}
			default:
				createParagraphFromInline(xdoc, el, 12, false, isCentered(el), null);
		}
	}

	private static void createParagraphFromInline(XWPFDocument xdoc, Element el, int fontSize, boolean bold, boolean centered, String styleId) {
		XWPFParagraph p = xdoc.createParagraph();
		p.setSpacingBetween(1.0, LineSpacingRule.AUTO);
		p.setSpacingBefore(0);
		p.setSpacingAfter(0);
		if (centered) p.setAlignment(ParagraphAlignment.CENTER);
		if (styleId != null) p.setStyle(styleId);
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
						r.setFontFamily("Consolas");
						r.setFontSize(baseFontSize);
						applyRunFontDefaults(r, false);
						break;
					}
					case "br": {
						XWPFRun r = p.createRun();
						r.addBreak();
						applyRunFontDefaults(r, false);
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
			applyRunFontDefaults(r, false);
			if (i < parts.length - 1) r.addBreak();
		}
	}

	private static void createCodeBlock(XWPFDocument xdoc, Element pre) {
		XWPFParagraph p = xdoc.createParagraph();
		p.setSpacingBetween(1.0, LineSpacingRule.AUTO);
		p.setSpacingBefore(0);
		p.setSpacingAfter(0);
		for (org.jsoup.nodes.Node node : pre.childNodes()) {
			if (node instanceof TextNode) {
				String text = ((TextNode) node).text();
				XWPFRun r = p.createRun();
				r.setFontFamily("Consolas");
				r.setText(text);
				r.addBreak();
				applyRunFontDefaults(r, false);
			} else if (node instanceof Element) {
				Element child = (Element) node;
				if (child.tagName().equalsIgnoreCase("code")) {
					XWPFRun r = p.createRun();
					r.setFontFamily("Consolas");
					r.setText(child.text());
					r.addBreak();
					applyRunFontDefaults(r, false);
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

	private static void appendList(XWPFDocument xdoc, Element listEl, ListNumberingIds ids, boolean ordered, int level, RenderState state) {
		BigInteger numId;
		if (state.seenAnyHeading) {
			numId = ordered ? ids.insideDecimalNumId : ids.insideBulletNumId;
		} else {
			numId = ordered ? ids.outsideDecimalNumId : ids.outsideBulletNumId;
		}
		for (Element li : listEl.children()) {
			if (!li.tagName().equalsIgnoreCase("li")) continue;
			Element liLine = li.clone();
			liLine.select("ul,ol").remove();
			XWPFParagraph p = xdoc.createParagraph();
			p.setSpacingBetween(1.0, LineSpacingRule.AUTO);
			p.setSpacingBefore(0);
			p.setSpacingAfter(0);
			p.setIndentationRight(720);
			p.setNumID(numId);
			p.setNumILvl(BigInteger.valueOf(level));
			appendInlineContent(p, liLine, 12, false);
			for (Element nested : li.children()) {
				String t = nested.tagName().toLowerCase();
				if (t.equals("ul")) {
					appendList(xdoc, nested, ids, false, level + 1, state);
				} else if (t.equals("ol")) {
					appendList(xdoc, nested, ids, true, level + 1, state);
				}
			}
		}
	}

	private static ListNumberingIds ensureListNumberings(XWPFDocument xdoc) {
		XWPFNumbering numbering = xdoc.createNumbering();

		// Outside bullets: use the same symbol for all levels (•)
		CTAbstractNum outBulAbs = CTAbstractNum.Factory.newInstance();
		for (int lvl = 0; lvl < 3; lvl++) {
			outBulAbs.addNewLvl();
			CTLvl l = outBulAbs.getLvlArray(lvl);
			l.setIlvl(BigInteger.valueOf(lvl));
			l.addNewNumFmt().setVal(STNumberFormat.BULLET);
			l.addNewLvlText().setVal("•");
			l.addNewStart().setVal(BigInteger.ONE);
			l.addNewPPr().addNewInd().setLeft(BigInteger.valueOf(720 + lvl * 360));
		}
		BigInteger outBulAbsId = numbering.addAbstractNum(new org.apache.poi.xwpf.usermodel.XWPFAbstractNum(outBulAbs));
		BigInteger outBulNumId = numbering.addNum(outBulAbsId);

		// Outside ordered: level 0 numeric, nested levels use bullet to avoid inner '1、'
		CTAbstractNum outDecAbs = CTAbstractNum.Factory.newInstance();
		for (int lvl = 0; lvl < 3; lvl++) {
			outDecAbs.addNewLvl();
			CTLvl l = outDecAbs.getLvlArray(lvl);
			l.setIlvl(BigInteger.valueOf(lvl));
			if (lvl == 0) {
				l.addNewNumFmt().setVal(STNumberFormat.DECIMAL);
				l.addNewLvlText().setVal("%1.");
			} else {
				l.addNewNumFmt().setVal(STNumberFormat.BULLET);
				l.addNewLvlText().setVal("·");
			}
			l.addNewStart().setVal(BigInteger.ONE);
			l.addNewPPr().addNewInd().setLeft(BigInteger.valueOf(720 + lvl * 360));
		}
		BigInteger outDecAbsId = numbering.addAbstractNum(new org.apache.poi.xwpf.usermodel.XWPFAbstractNum(outDecAbs));
		BigInteger outDecNumId = numbering.addNum(outDecAbsId);

		// Inside bullets: use the same symbol for all levels (▪)
		CTAbstractNum inBulAbs = CTAbstractNum.Factory.newInstance();
		for (int lvl = 0; lvl < 3; lvl++) {
			inBulAbs.addNewLvl();
			CTLvl l = inBulAbs.getLvlArray(lvl);
			l.setIlvl(BigInteger.valueOf(lvl));
			l.addNewNumFmt().setVal(STNumberFormat.BULLET);
			l.addNewLvlText().setVal("▪");
			l.addNewStart().setVal(BigInteger.ONE);
			l.addNewPPr().addNewInd().setLeft(BigInteger.valueOf(720 + lvl * 360));
		}
		BigInteger inBulAbsId = numbering.addAbstractNum(new org.apache.poi.xwpf.usermodel.XWPFAbstractNum(inBulAbs));
		BigInteger inBulNumId = numbering.addNum(inBulAbsId);

		// Inside ordered: level 0 numeric, nested levels use bullet to avoid inner '1、'
		CTAbstractNum inDecAbs = CTAbstractNum.Factory.newInstance();
		for (int lvl = 0; lvl < 3; lvl++) {
			inDecAbs.addNewLvl();
			CTLvl l = inDecAbs.getLvlArray(lvl);
			l.setIlvl(BigInteger.valueOf(lvl));
			if (lvl == 0) {
				l.addNewNumFmt().setVal(STNumberFormat.DECIMAL);
				l.addNewLvlText().setVal("%1.");
			} else {
				l.addNewNumFmt().setVal(STNumberFormat.BULLET);
				l.addNewLvlText().setVal("·");
			}
			l.addNewStart().setVal(BigInteger.ONE);
			l.addNewPPr().addNewInd().setLeft(BigInteger.valueOf(720 + lvl * 360));
		}
		BigInteger inDecAbsId = numbering.addAbstractNum(new org.apache.poi.xwpf.usermodel.XWPFAbstractNum(inDecAbs));
		BigInteger inDecNumId = numbering.addNum(inDecAbsId);

		ListNumberingIds ids = new ListNumberingIds();
		ids.outsideBulletNumId = outBulNumId;
		ids.outsideDecimalNumId = outDecNumId;
		ids.insideBulletNumId = inBulNumId;
		ids.insideDecimalNumId = inDecNumId;
		return ids;
	}

	private static void applyRunFontDefaults(XWPFRun run, boolean isHeading) {
		// Standardize fonts to avoid odd spacing between CJK and Latin chars
		run.setFontFamily("Calibri");
		CTRPr rpr = run.getCTR().isSetRPr() ? run.getCTR().getRPr() : run.getCTR().addNewRPr();
		CTFonts fonts = rpr.addNewRFonts();
		fonts.setAscii("Calibri");
		fonts.setHAnsi("Calibri");
		fonts.setCs("Calibri");
		fonts.setEastAsia("Microsoft YaHei");
		run.setKerning(0);
	}

	private static class ListNumberingIds {
		BigInteger outsideBulletNumId;
		BigInteger outsideDecimalNumId;
		BigInteger insideBulletNumId;
		BigInteger insideDecimalNumId;
	}

	private static class RenderState {
		boolean mainTitleDone = false;
		boolean seenAnyHeading = false;
	}
}