package com.dmsBackend.utils;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.poi.hslf.usermodel.HSLFSlide;
import org.apache.poi.hslf.usermodel.HSLFSlideShow;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.nio.file.Path;
import java.util.List;

@Component
public class DocHelper {
    public int getPageCount(Path filePath, String ext) {
        try {
            if (ext.equals(".pdf")) {
                try (PDDocument doc = PDDocument.load(filePath.toFile())) {
                    return doc.getNumberOfPages();
                }
            } else if (ext.equals(".docx")) {
                try (FileInputStream fis = new FileInputStream(filePath.toFile());
                     XWPFDocument doc = new XWPFDocument(fis)) {
                    return doc.getProperties().getExtendedProperties()
                            .getUnderlyingProperties().getPages();
                }
            } else if (ext.equals(".doc")) {
                try (FileInputStream fis = new FileInputStream(filePath.toFile());
                     HWPFDocument doc = new HWPFDocument(fis)) {
                    return doc.getSummaryInformation().getPageCount();
                }
            } else if (ext.equals(".xlsx")) {
                try (FileInputStream fis = new FileInputStream(filePath.toFile());
                     XSSFWorkbook workbook = new XSSFWorkbook(fis)) {
                    return workbook.getNumberOfSheets();
                }
            } else if (ext.equals(".xls")) {
                try (FileInputStream fis = new FileInputStream(filePath.toFile());
                     HSSFWorkbook workbook = new HSSFWorkbook(fis)) {
                    return workbook.getNumberOfSheets();
                }
            } else if (ext.equals(".pptx")) {
                try (FileInputStream fis = new FileInputStream(filePath.toFile());
                     XMLSlideShow ppt = new XMLSlideShow(fis)) {
                    return ppt.getSlides().size();
                }
            } else if (ext.equals(".ppt")) {
                try (FileInputStream fis = new FileInputStream(filePath.toFile());
                     HSLFSlideShow ppt = new HSLFSlideShow(fis)) {
                    List<HSLFSlide> slides = ppt.getSlides();
                    return slides.size();
                }
            }
        } catch (Exception e) {
            return -1; // parsing failed
        }
        return -1; // not supported
    }

    public String humanReadableSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        char unit = "KMGTPE".charAt(exp - 1);
        return String.format("%.2f %sB", bytes / Math.pow(1024, exp), unit);
    }
}
