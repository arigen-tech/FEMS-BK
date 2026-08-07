package com.dmsBackend.response;

import com.dmsBackend.entity.BranchMaster;
import com.dmsBackend.entity.CategoryMaster;
import com.dmsBackend.entity.DepartmentMaster;
import com.dmsBackend.repository.*;
import com.dmsBackend.service.BranchMasterService;
import com.dmsBackend.service.DocumentDetailsService;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.events.PdfDocumentEvent;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.itextpdf.kernel.events.IEventHandler;
import com.itextpdf.kernel.events.Event;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;

import com.itextpdf.layout.element.Image;
import com.itextpdf.io.image.ImageDataFactory;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import java.io.OutputStream;

import java.util.List;

@Component
public class PDFGenerator {

    @Value("${report.rights.reserved1}")
    private String rightsReservedText;

    @Value("${company.name}")
    private String companyName;
    private final YearMasterRepository yearMasterRepository;
    private final EmployeeRepository employeeRepository;
    private final DocumentDetailsRepository documentDetailsRepository;
    private final DepartmentMasterRepository departmentMasterRepository;
    private final BranchMasterRepository branchMasterRepository;
    private final CategoryMasterRepository categoryMasterRepository;

    @Autowired
    public PDFGenerator(DocumentHeaderRepository documentHeaderRepository,
                                     DepartmentMasterRepository departmentMasterRepository,
                                     BranchMasterRepository branchMasterRepository,
                                     DocumentDetailsService documentDetailsService,
                                     CategoryMasterRepository categoryMasterRepository,
                                     YearMasterRepository yearMasterRepository,
                                     EmployeeRepository employeeRepository,
                                     DocumentDetailsRepository documentDetailsRepository) {
        this.departmentMasterRepository = departmentMasterRepository;
        this.branchMasterRepository =branchMasterRepository;
        this.categoryMasterRepository = categoryMasterRepository;
        this.yearMasterRepository = yearMasterRepository;
        this.employeeRepository = employeeRepository;
        this.documentDetailsRepository = documentDetailsRepository;
    }




    public void generate(OutputStream outputStream, List<DocumentResponse> documents, DocFilterRequest filterRequest, String fileName) throws Exception {
        try (PdfWriter writer = new PdfWriter(outputStream);
             PdfDocument pdfDocument = new PdfDocument(writer);
             Document document = new Document(pdfDocument, PageSize.A4.rotate())) {

            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

            String status = filterRequest.getApprovalStatus() != null ? filterRequest.getApprovalStatus().toString() : "All Status";
            String categoryName = "All Categories";
            if (filterRequest.getCategoryId() != null) {
                categoryName = categoryMasterRepository.findById(filterRequest.getCategoryId())
                        .map(CategoryMaster::getName)
                        .orElse("All Category");
            }

            String branch = "All Branches";
            if (filterRequest.getBranchId() != null) {
                branch = branchMasterRepository.findById(filterRequest.getBranchId())
                        .map(BranchMaster::getName)
                        .orElse("All Branch");
            }

            String department = "All Departments";
            if (filterRequest.getDepartmentId() != null) {
                department = departmentMasterRepository.findById(filterRequest.getDepartmentId())
                        .map(DepartmentMaster::getName)
                        .orElse("All Department");
            }
            String printDateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm:ss a"));

            int year = LocalDateTime.now().getYear();

            String fromDate = filterRequest.getStartDate() != null
                    ? filterRequest.getStartDate().toLocalDateTime().format(dateFormatter)
                    : "N/A";

            String toDate = filterRequest.getEndDate() != null
                    ? filterRequest.getEndDate().toLocalDateTime().format(dateFormatter)
                    : "N/A";
            addLogo(document);
            addTitle(document, categoryName);
            addPrintDate(document, printDateTime);
            addMetadata(document, status, categoryName, department, branch, fromDate, toDate);
            addTable(document, documents, dateFormatter);
            addPageNumbersAndRights(pdfDocument, year, rightsReservedText, companyName);
        }
    }

    private static void addTitle(Document document, String categoryName) throws Exception {
        PdfFont boldFont = PdfFontFactory.createFont(StandardFonts.TIMES_BOLD);
        Paragraph title = new Paragraph(categoryName + " Documents Reports")
                .setFont(boldFont)
                .setFontSize(16)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(5);
        document.add(title);

    }

    private static void addMetadata(Document document, String status, String categoryName, String department, String branch, String fromDate, String toDate) throws Exception {
        float[] columnWidths = {3, 3, 3};
        Table table = new Table(UnitValue.createPercentArray(columnWidths)).setWidth(UnitValue.createPercentValue(100));
        PdfFont cellFont = PdfFontFactory.createFont(StandardFonts.HELVETICA);

        table.addCell(createCell("Branch: ", branch, cellFont));
        table.addCell(createCell("Department: ", department, cellFont));
        table.addCell(createCell("Status: ", status, cellFont));
        table.addCell(createCell("Category: ", categoryName, cellFont));
        table.addCell(createCell("From Date: ", fromDate, cellFont));
        table.addCell(createCell("To Date: ", toDate, cellFont));

        document.add(table);
        document.add(new Paragraph("\n"));
    }

    private static void addTable(Document document, List<DocumentResponse> documents, DateTimeFormatter dateFormatter) {

        documents.sort((d1, d2) -> {
            int order1 = getStatusOrder(d1.getApprovalStatus());
            int order2 = getStatusOrder(d2.getApprovalStatus());
            return Integer.compare(order1, order2);
        });
        documents.forEach(doc -> System.out.println("Status: " + doc.getApprovalStatus()));


        float[] columnWidths = {3f, 6f, 12f, 11f, 9f, 9f, 10f, 5f, 10f, 10f};
        Table table = new Table(UnitValue.createPercentArray(columnWidths)).setWidth(UnitValue.createPercentValue(100));

        String[] headers = {"S.N.", "File No", "Title", "Subject", "Category", "Branch", "Department", "File Count", "Uploaded Date", "Status"};
        for (String header : headers) {
            table.addCell(new Cell().add(new Paragraph(header).setBold()).setTextAlignment(TextAlignment.CENTER));
        }

        for (int i = 0; i < documents.size(); i++) {
            DocumentResponse doc = documents.get(i);
            table.addCell(new Cell().add(new Paragraph(String.valueOf(i + 1))));
            table.addCell(new Cell().add(new Paragraph(doc.getFileNo())));
            table.addCell(new Cell().add(new Paragraph(doc.getTitle())));
            table.addCell(new Cell().add(new Paragraph(doc.getSubject())));
            table.addCell(new Cell().add(new Paragraph(doc.getCategoryName())));
            table.addCell(new Cell().add(new Paragraph(doc.getBranchName())));
            table.addCell(new Cell().add(new Paragraph(doc.getDepartmentName())));
            table.addCell(new Cell().add(new Paragraph(String.valueOf(doc.getDocumentDetailsLength()))));
            String formattedDate = doc.getCreatedOn() != null
                    ? doc.getCreatedOn().toLocalDateTime().format(dateFormatter)
                    : "N/A";
            table.addCell(new Cell().add(new Paragraph(formattedDate)));
            table.addCell(new Cell().add(new Paragraph(doc.getApprovalStatus())));
        }

        document.add(table);
    }

    private void addPageNumbersAndRights(PdfDocument pdfDocument, int year, String rightsReservedText, String companyName) {
        pdfDocument.addEventHandler(PdfDocumentEvent.END_PAGE, new IEventHandler() {
            @Override
            public void handleEvent(Event event) {
                PdfDocumentEvent docEvent = (PdfDocumentEvent) event;
                PdfPage page = docEvent.getPage();
                PdfCanvas canvas = new PdfCanvas(page);
                int pageNumber = docEvent.getDocument().getPageNumber(page);
                int totalPages = docEvent.getDocument().getNumberOfPages();

                String rightsText = rightsReservedText + " © " + year + " " + companyName;
                String pageNumberText = "Page " + pageNumber + " of " + totalPages;

                try {
                    PdfFont regularFont = PdfFontFactory.createFont(StandardFonts.HELVETICA);
                    PdfFont boldFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
                    float pageWidth = page.getPageSize().getWidth();

                    canvas.beginText();
                    canvas.setFontAndSize(boldFont, 9);
                    float rightsTextWidth = boldFont.getWidth(rightsText, 9);
                    canvas.moveText((pageWidth - rightsTextWidth) / 2, 20);
                    canvas.showText(rightsText);

                    canvas.setFontAndSize(regularFont, 9);
                    float pageNumberTextWidth = regularFont.getWidth(pageNumberText, 9);
                    float xRightAligned = pageWidth - pageNumberTextWidth - 40;
                    canvas.moveText(xRightAligned - ((pageWidth - rightsTextWidth) / 2), 0);
                    canvas.showText(pageNumberText);

                    canvas.endText();
                    canvas.release();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void addLogo(Document document) throws Exception {
        InputStream logoStream = getClass().getClassLoader().getResourceAsStream("static/logo.png");
        if (logoStream != null) {
            byte[] logoBytes = logoStream.readAllBytes();
            Image logo = new Image(ImageDataFactory.create(logoBytes));
            logo.setWidth(80);
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

    private static int getStatusOrder(String status) {
        if (status == null) return 3;
        return switch (status.trim().toUpperCase()) {
            case "PENDING" -> 0;
            case "REJECTED" -> 1;
            case "APPROVED" -> 2;
            default -> 3;
        };
    }




    private static Cell createCell(String label, String value, PdfFont font) {
        return new Cell().add(new Paragraph()
                .add(new Text(label).setFont(font).setBold())
                .add(new Text(value).setFont(font))
        ).setPadding(5);
    }


}
