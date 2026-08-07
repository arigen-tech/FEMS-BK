package com.dmsBackend.response;

import com.dmsBackend.entity.DocumentDetails;
import com.dmsBackend.entity.DocumentHeader;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;

@Component
public class PdfReportUtil {

    public byte[] generatePdfReport(DocumentHeader header) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc);

        PdfFont font = PdfFontFactory.createFont(StandardFonts.HELVETICA);

        // Title
        Paragraph title = new Paragraph(header.getTitle())
                .setFont(font)
                .setFontSize(18)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER);
        document.add(title);

        document.add(new Paragraph(" "));
        document.add(new Paragraph("File No: " + header.getFileNo()).setFont(font));
        document.add(new Paragraph("Subject: " + header.getSubject()).setFont(font));
        document.add(new Paragraph("Category: " + header.getCategoryMaster().getName()).setFont(font));
        document.add(new Paragraph("Approval Status: " + header.getApprovalStatus()).setFont(font));
        document.add(new Paragraph("Created By: " + header.getCreatedBy()).setFont(font));
        document.add(new Paragraph("Created On: " + header.getCreatedOn()).setFont(font));
        document.add(new Paragraph(" "));

        // Employee Info
        var emp = header.getEmployee();
        document.add(new Paragraph("Employee Info:").setFont(font).setBold());
        document.add(new Paragraph("Name: " + emp.getName()).setFont(font));
        document.add(new Paragraph("Email: " + emp.getEmail()).setFont(font));
        document.add(new Paragraph("Mobile: " + emp.getMobile()).setFont(font));
        document.add(new Paragraph("Department: " + emp.getDepartment().getName()).setFont(font));
        document.add(new Paragraph("Branch: " + emp.getBranch().getName()).setFont(font));

        document.add(new Paragraph(" "));

        // Document Details Table
        if (header.getDocumentDetails() != null && !header.getDocumentDetails().isEmpty()) {
            document.add(new Paragraph("Document Details:").setFont(font).setBold());

            float[] columnWidths = {4, 2, 2, 2, 2, 2};
            Table table = new Table(columnWidths);

            table.addHeaderCell("Document Name");
            table.addHeaderCell("Version");
            table.addHeaderCell("Status");
            table.addHeaderCell("Year");
            table.addHeaderCell("Created By");
            table.addHeaderCell("Approved By");

            for (DocumentDetails detail : header.getDocumentDetails()) {
                table.addCell(detail.getDocName());
                table.addCell(detail.getVersion() != null ? detail.getVersion() : "-");
                table.addCell(detail.getStatus().name());
                table.addCell(detail.getYearMaster().getName());
                table.addCell(detail.getCreatedBy());
                table.addCell(detail.getApprovedBy() != null ? detail.getApprovedBy() : "-");
            }

            document.add(table);
        }

        document.close();
        return baos.toByteArray();
    }
}
