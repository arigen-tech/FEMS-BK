package com.dmsBackend.response;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;

@Component
public class EmpExcelGenerator {

    public byte[] generateExcel(List<EmployeeResponse> employees) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Users Reports");

            // Create Header Row
            Row headerRow = sheet.createRow(0);
            String[] headers = {
                    "S.N.", "Name", "Branch Name", "Department Name",
                    "Role", "Status", "Created Date", "Mobile No.", "Email"
            };
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                CellStyle style = workbook.createCellStyle();
                Font font = workbook.createFont();
                font.setBold(true);
                style.setFont(font);
                cell.setCellStyle(style);
            }
            employees.sort((e1, e2) -> e1.getStatus().compareToIgnoreCase(e2.getStatus()));


            // Populate Data Rows
            int rowIdx = 1;
            int serialNumber = 1;
            for (EmployeeResponse employee : employees) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(serialNumber++);
                row.createCell(1).setCellValue(employee.getName() != null ? employee.getName() : "No Data");
                row.createCell(2).setCellValue(employee.getBranchName() != null ? employee.getBranchName() : "No Data");
                row.createCell(3).setCellValue(employee.getDepartmentName() != null ? employee.getDepartmentName() : "No Data");
                row.createCell(4).setCellValue(employee.getRoleName() != null ? employee.getRoleName() : "No Data");
                row.createCell(5).setCellValue(employee.getStatus() != null ? employee.getStatus() : "No Data");
                row.createCell(6).setCellValue(
                        employee.getCreateDate() != null
                                ? EmpExcelGenerator.formatTimestamp(employee.getCreateDate(), "dd/MM/yyyy")
                                : "No Data"
                );
                row.createCell(7).setCellValue(employee.getMobile() != null ? employee.getMobile() : "No Data");
                row.createCell(8).setCellValue(employee.getEmail() != null ? employee.getEmail() : "No Data");
            }

            // Auto-size Columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generating Excel", e);
        }
    }
    public static String formatTimestamp(Timestamp timestamp, String format) {
        if (timestamp == null) return "";
        return new SimpleDateFormat(format).format(timestamp);
    }

    public String getDynamicFileName(String branchName, String departmentName, String fromDate, String toDate) {
        String sanitizedBranchName = branchName != null ? branchName.replaceAll("\\s+", "_") : "";
        String sanitizedDepartmentName = departmentName != null ? departmentName.replaceAll("\\s+", "_") : "";
        String sanitizedFromDate = fromDate !=null ? fromDate.replaceAll("\\s+", "_") : "";
        String sanitizedToDate = fromDate !=null ? toDate.replaceAll("\\s+", "_") : "";
        String name = sanitizedBranchName + "_" + sanitizedDepartmentName +"_"+sanitizedFromDate+"_To_"+sanitizedToDate+ "_Users_Reports.csv";
        System.out.println("pdf generater "+ name);
        return name;
    }
//    public String getDynamicFileName(String branchName, String departmentName, String fromDate, String toDate) {
//        // Ensure branchName and departmentName are valid
//        String branch = branchName != null ? branchName : "All_Branches";
//        String department = departmentName != null ? departmentName : "All_Departments";
//
//        // Format the file name with branch, department, and date range
//        return String.format(
//                "Employee_Report_%s_%s_%s_to_%s.xlsx",
//                branch.replace(" ", "_"),
//                department.replace(" ", "_"),
//                fromDate != null ? fromDate.replace("/", "-") : "Start",
//                toDate != null ? toDate.replace("/", "-") : "End"
//        );
//    }


    public static String formatDate(String date, String inputFormat, String outputFormat) {
        try {
            SimpleDateFormat inputFormatter = new SimpleDateFormat(inputFormat);
            SimpleDateFormat outputFormatter = new SimpleDateFormat(outputFormat);
            return outputFormatter.format(inputFormatter.parse(date));
        } catch (ParseException e) {
            e.printStackTrace();
            return ""; // Return empty string on error
        }
    }
}
