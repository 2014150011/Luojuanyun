package com.example.docx2html;

import fr.opensagres.poi.xwpf.converter.xhtml.XHTMLConverter;
import fr.opensagres.poi.xwpf.converter.xhtml.XHTMLOptions;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFPictureData;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class DocxToHtml {

    private DocxToHtml() {}

    public static void convert(Path inputDocxPath, Path outputHtmlPath) throws IOException {
        Files.createDirectories(outputHtmlPath.toAbsolutePath().getParent());

        try (InputStream inputStream = Files.newInputStream(inputDocxPath);
             XWPFDocument document = new XWPFDocument(inputStream)) {

            XHTMLOptions options = XHTMLOptions.create();
            options.setIgnoreStylesIfUnused(false);
            options.setFragment(false);

            ByteArrayOutputStream htmlBuffer = new ByteArrayOutputStream(64 * 1024);
            XHTMLConverter.getInstance().convert(document, htmlBuffer, options);

            String html = htmlBuffer.toString(StandardCharsets.UTF_8);
            if (!html.toLowerCase(Locale.ROOT).contains("<meta charset")) {
                html = html.replaceFirst("(?i)<head>", "<head>\n<meta charset=\"UTF-8\"/>");
            }

            String htmlWithEmbeddedImages = embedImagesAsBase64(html, document);

            try (BufferedWriter writer = Files.newBufferedWriter(outputHtmlPath, StandardCharsets.UTF_8)) {
                writer.write(htmlWithEmbeddedImages);
            }
        }
    }

    private static String embedImagesAsBase64(String html, XWPFDocument document) {
        Document doc = Jsoup.parse(html);
        Elements imgElements = doc.select("img[src]");

        List<XWPFPictureData> pictures = document.getAllPictures();
        Map<String, XWPFPictureData> fileNameToPic = new HashMap<>();
        for (XWPFPictureData picture : pictures) {
            fileNameToPic.put(picture.getFileName(), picture);
        }

        for (Element img : imgElements) {
            String src = img.attr("src");
            String fileName = src;
            int slash = src.lastIndexOf('/');
            if (slash >= 0 && slash < src.length() - 1) {
                fileName = src.substring(slash + 1);
            }

            XWPFPictureData pic = fileNameToPic.get(fileName);
            if (pic != null) {
                String ext = pic.suggestFileExtension();
                String mime = toMimeType(ext);
                String base64 = Base64.getEncoder().encodeToString(pic.getData());
                img.attr("src", "data:" + mime + ";base64," + base64);
            }
        }

        return doc.outerHtml();
    }

    private static String toMimeType(String ext) {
        if (ext == null) {
            return "application/octet-stream";
        }
        String e = ext.toLowerCase(Locale.ROOT);
        switch (e) {
            case "png":
                return "image/png";
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "gif":
                return "image/gif";
            case "bmp":
                return "image/bmp";
            case "svg":
                return "image/svg+xml";
            case "tif":
            case "tiff":
                return "image/tiff";
            default:
                return "application/octet-stream";
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("用法: java -jar app.jar <input.docx> <output.html>");
            System.exit(1);
        }
        Path input = Paths.get(args[0]);
        Path output = Paths.get(args[1]);
        convert(input, output);
        System.out.println("转换完成: " + output.toAbsolutePath());
    }
}