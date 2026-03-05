package com.planB.myexpressionfriend.common.service;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfWriter;
import com.planB.myexpressionfriend.common.domain.report.GeneratedReport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReportExportService {

    private final GeneratedReportService generatedReportService;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public byte[] exportCsv(UUID userId, UUID reportId) {
        GeneratedReport report = generatedReportService.getUserReport(userId, reportId);

        StringBuilder sb = new StringBuilder();
        sb.append("\uFEFF");
        sb.append("Field,Value\n");
        addCsvRow(sb, "Report ID", report.getReportId() != null ? report.getReportId().toString() : "");
        addCsvRow(sb, "Title", nullToEmpty(report.getTitle()));
        addCsvRow(sb, "Status", report.getStatus() != null ? report.getStatus().name() : "");
        addCsvRow(sb, "Summary", toPlainText(report.getSummary()));
        addCsvRow(sb, "Created At", report.getCreatedAt() != null ? report.getCreatedAt().format(DATE_TIME_FORMATTER) : "");
        addCsvRow(sb, "Issued At", report.getIssuedAt() != null ? report.getIssuedAt().format(DATE_TIME_FORMATTER) : "");
        addCsvRow(sb, "Model", nullToEmpty(report.getModelName()));
        addCsvRow(sb, "Body", toPlainText(report.getReportBody()));

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    public byte[] exportPdf(UUID userId, UUID reportId) {
        GeneratedReport report = generatedReportService.getUserReport(userId, reportId);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document();
        try {
            PdfWriter.getInstance(document, baos);
            document.open();

            Font titleFont = createUnicodeFont(16, Font.BOLD);
            Font bodyFont = createUnicodeFont(11, Font.NORMAL);

            document.add(new Paragraph(nullToEmpty(report.getTitle()), titleFont));
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Report ID: " + (report.getReportId() != null ? report.getReportId() : ""), bodyFont));
            document.add(new Paragraph("Status: " + (report.getStatus() != null ? report.getStatus().name() : ""), bodyFont));
            document.add(new Paragraph("Issued At: " + (report.getIssuedAt() != null ? report.getIssuedAt().format(DATE_TIME_FORMATTER) : ""), bodyFont));
            document.add(new Paragraph("Model: " + nullToEmpty(report.getModelName()), bodyFont));
            document.add(new Paragraph(" "));

            document.add(new Paragraph("Summary", titleFont));
            document.add(new Paragraph(toPlainText(report.getSummary()), bodyFont));
            document.add(new Paragraph(" "));

            document.add(new Paragraph("Body", titleFont));
            document.add(new Paragraph(toPlainText(report.getReportBody()), bodyFont));
        } catch (DocumentException e) {
            throw new IllegalStateException("PDF generation failed.", e);
        } finally {
            document.close();
        }

        return baos.toByteArray();
    }

    private void addCsvRow(StringBuilder sb, String key, String value) {
        sb.append(escapeCsv(key)).append(',').append(escapeCsv(value)).append('\n');
    }

    private String escapeCsv(String value) {
        String safe = nullToEmpty(value).replace("\"", "\"\"");
        return "\"" + safe + "\"";
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private Font createUnicodeFont(float size, int style) {
        List<String> candidates = List.of(
                "C:/Windows/Fonts/malgun.ttf",
                "C:/Windows/Fonts/NanumGothic.ttf",
                "/usr/share/fonts/truetype/nanum/NanumGothic.ttf",
                "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"
        );

        for (String path : candidates) {
            try {
                if (!Files.exists(Path.of(path))) {
                    continue;
                }
                BaseFont baseFont = BaseFont.createFont(path, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                return new Font(baseFont, size, style);
            } catch (Exception ignored) {
                // Try next candidate
            }
        }

        return FontFactory.getFont(FontFactory.HELVETICA, size, style);
    }

    private String toPlainText(String markdown) {
        String text = nullToEmpty(markdown).replace("\r\n", "\n");

        text = text.replaceAll("(?m)^\\s{0,3}#{1,6}\\s*", "");
        text = text.replaceAll("\\*\\*(.*?)\\*\\*", "$1");
        text = text.replaceAll("__(.*?)__", "$1");
        text = text.replaceAll("`([^`]*)`", "$1");
        text = text.replaceAll("(?m)^\\s*>\\s?", "");
        text = text.replaceAll("\\[(.*?)]\\((.*?)\\)", "$1 ($2)");
        text = text.replaceAll("(?m)^\\s*[-*+]\\s+", "- ");

        return text.trim();
    }
}