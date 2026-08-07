package com.dmsBackend.controller;

import com.dmsBackend.entity.DocumentHeader;
import com.dmsBackend.repository.DocumentHeaderRepository;
import com.dmsBackend.response.PdfReportUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
public class ReportController {
    @Autowired
    private DocumentHeaderRepository headerRepository;

    @Autowired
    private PdfReportUtil pdfReportUtil;

    @GetMapping("/document/{id}")
    public ResponseEntity<byte[]> generatePdf(@PathVariable Integer id) throws Exception {
        DocumentHeader header = headerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("DocumentHeader not found with id " + id));

        byte[] pdfBytes = pdfReportUtil.generatePdfReport(header);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=document_" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}
