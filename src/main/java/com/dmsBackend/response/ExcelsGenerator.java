package com.dmsBackend.response;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.OutputStream;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

@Component
public class ExcelsGenerator {

    public static void generateExcel(
            OutputStream outputStream,
            List<DocumentResponse> documents,
            DocFilterRequest filterRequest,
            String fileName
    ) throws Exception {

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Documents Report");

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        // Sort documents by status order (same logic as PDFGenerator)
        documents.sort(Comparator.comparingInt(doc -> getStatusOrder(doc.getApprovalStatus())));

        // Header Row
        Row headerRow = sheet.createRow(0);
        String[] headers = {
                "S.N.", "File No", "Title", "Subject",
                "Category", "Branch", "Department",
                "File Count", "Uploaded Date", "Status"
        };

        // Style for headers
        CellStyle headerStyle = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        headerStyle.setFont(font);

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Data Rows
        int rowIndex = 1;
        for (int i = 0; i < documents.size(); i++) {
            DocumentResponse doc = documents.get(i);
            Row row = sheet.createRow(rowIndex++);

            row.createCell(0).setCellValue(i + 1);
            row.createCell(1).setCellValue(doc.getFileNo());
            row.createCell(2).setCellValue(doc.getTitle());
            row.createCell(3).setCellValue(doc.getSubject());
            row.createCell(4).setCellValue(doc.getCategoryName());
            row.createCell(5).setCellValue(doc.getBranchName());
            row.createCell(6).setCellValue(doc.getDepartmentName());
            row.createCell(7).setCellValue(doc.getDocumentDetailsLength());

            String formattedDate = doc.getCreatedOn() != null
                    ? doc.getCreatedOn().toLocalDateTime().format(dateFormatter)
                    : "N/A";
            row.createCell(8).setCellValue(formattedDate);

            row.createCell(9).setCellValue(doc.getApprovalStatus());
        }

        // Auto-size all columns
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        workbook.write(outputStream);
        workbook.close();
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
}
