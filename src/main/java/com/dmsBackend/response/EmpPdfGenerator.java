package com.dmsBackend.response;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.events.PdfDocumentEvent;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class EmpPdfGenerator {

    @Value("${report.rights.reserved1}")
    private String rightsReservedText;

    @Value("${company.name}")
    private String companyName;

    public byte[] generatePdf(List<EmployeeResponse> employees, String branchName, String departmentName,
                              String statusString, String formattedFromDate, String formattedToDate) throws Exception {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(out);
            PdfDocument pdfDocument = new PdfDocument(writer);
            Document document = new Document(pdfDocument, PageSize.A4.rotate());
            System.out.println("rightsReservedText: " + rightsReservedText);
            System.out.println("companyName: " + companyName);

            int year = LocalDateTime.now().getYear();
            addPageNumbersAndRights(pdfDocument, year, rightsReservedText, companyName);
            addLogo(document);

            // Add Title
            Paragraph title = new Paragraph(statusString + " Users Report")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(16)
                    .setBold();
            document.add(title);

            String printDateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm:ss a"));
            addPrintDate(document, printDateTime);

            Table filterTable = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                    .useAllAvailableWidth()
                    .setMarginBottom(10);

            filterTable.addCell(createFilterCell("Branch Name: ", branchName));
            filterTable.addCell(createFilterCell("Department Name: ", departmentName));
            filterTable.addCell(createFilterCell("Status: ", statusString));
            filterTable.addCell(createFilterCell("Date Range: ", formattedFromDate + " to " + formattedToDate));

            document.add(filterTable);




            Table table = new Table(new float[]{1, 2, 2, 2, 2, 2, 2, 2, 2})
                    .setWidth(UnitValue.createPercentValue(100))
                    .setMarginTop(5);

            String[] headers = {"S.N.", "Name", "Branch", "Department", "Role", "Status", "Created Date", "Mobile", "Email"};
            for (String header : headers) {
                table.addHeaderCell(new Cell().add(new Paragraph(header).setBold()));
            }

            employees.sort((e1, e2) -> e1.getStatus().compareToIgnoreCase(e2.getStatus()));

            int count = 1;
            for (EmployeeResponse emp : employees) {
                table.addCell(String.valueOf(count++));
                table.addCell(emp.getName());
                table.addCell(emp.getBranchName());
                table.addCell(emp.getDepartmentName());
                table.addCell(emp.getRoleName());
                table.addCell(emp.getStatus());
                table.addCell(formatTimestamp(emp.getCreateDate(), "dd/MM/yyyy"));
                table.addCell(emp.getMobile());
                table.addCell(emp.getEmail());
            }

            document.add(table);
            document.close();
            return out.toByteArray();
        }
    }

    private Cell createFilterCell(String label, String value) {
        Paragraph paragraph = new Paragraph()
                .add(new Text(label).setBold())
                .add(new Text(value));

        return new Cell()
                .add(paragraph)
                .setTextAlignment(TextAlignment.LEFT)
                .setPadding(6);
    }



    private void addLogo(Document document) throws Exception {
        InputStream logoStream = getClass().getClassLoader().getResourceAsStream("static/logo.png");
        if (logoStream != null) {
            byte[] logoBytes = logoStream.readAllBytes();
            Image logo = new Image(ImageDataFactory.create(logoBytes));
            logo.setWidth(100);
            logo.setHeight(40);
            logo.setHorizontalAlignment(HorizontalAlignment.RIGHT);
            document.add(logo);
        }

    }


    private static void addPrintDate(Document document, String printDateTime) throws Exception {
        PdfFont font = PdfFontFactory.createFont(StandardFonts.TIMES_BOLD);
        Paragraph date = new Paragraph("Printed on: " + printDateTime)
                .setFont(font)
                .setFontSize(14)
                .setHorizontalAlignment(HorizontalAlignment.LEFT);
        document.add(date);
    }

    private void addPageNumbersAndRights(PdfDocument pdfDocument, int year, String rightsReservedText, String companyName) {
        pdfDocument.addEventHandler(PdfDocumentEvent.END_PAGE, event -> {
            PdfDocumentEvent docEvent = (PdfDocumentEvent) event;
            PdfPage page = docEvent.getPage();
            PdfCanvas canvas = new PdfCanvas(page);
            int pageNumber = docEvent.getDocument().getPageNumber(page);
            int totalPages = docEvent.getDocument().getNumberOfPages();

            String rightsText = rightsReservedText + " © " + year + " " + companyName;
            String pageNumberText = "Page " + pageNumber + " of " + totalPages;

            try {
                PdfFont font = PdfFontFactory.createFont(StandardFonts.HELVETICA);
                PdfFont boldFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);

                float pageWidth = page.getPageSize().getWidth();

                canvas.beginText();
                canvas.setFontAndSize(boldFont, 9);
                float rightsWidth = boldFont.getWidth(rightsText, 9);
                float rightsX = (pageWidth - rightsWidth) / 2;
                canvas.moveText(rightsX, 20);
                canvas.showText(rightsText);
                canvas.endText();

                canvas.beginText();
                canvas.setFontAndSize(font, 9);
                float pageNumberWidth = font.getWidth(pageNumberText, 9);
                float pageNumberX = pageWidth - pageNumberWidth - 40; // 40px right margin
                canvas.moveText(pageNumberX, 20);
                canvas.showText(pageNumberText);
                canvas.endText();

                canvas.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public static String formatTimestamp(Timestamp timestamp, String format) {
        if (timestamp == null) return "";
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        return sdf.format(timestamp);
    }

    public String getDynamicFileName(String branchName, String departmentName) {
        String sanitizedBranchName = sanitizeName(branchName != null ? branchName : "Branch");
        String sanitizedDepartmentName = sanitizeName(departmentName != null ? departmentName : "Department");
        return sanitizedBranchName + "_" + sanitizedDepartmentName + "_Users_Report.pdf";
    }

    private String sanitizeName(String input) {
        Pattern pattern = Pattern.compile("\\(([^)]*)\\)");
        Matcher matcher = pattern.matcher(input);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String content = matcher.group(1).replace(" ", "_");
            matcher.appendReplacement(result, "(" + content + ")");
        }
        matcher.appendTail(result);

        return result.toString().replace(" ", "_");
    }

    public static String formatDate(String date, String inputFormat, String outputFormat) {
        try {
            SimpleDateFormat inputFormatter = new SimpleDateFormat(inputFormat);
            SimpleDateFormat outputFormatter = new SimpleDateFormat(outputFormat);
            return outputFormatter.format(inputFormatter.parse(date));
        } catch (ParseException e) {
            e.printStackTrace();
            return "";
        }
    }
}
