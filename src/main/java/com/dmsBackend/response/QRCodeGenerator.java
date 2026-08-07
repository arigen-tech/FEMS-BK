package com.dmsBackend.response;

import com.dmsBackend.entity.DocumentHeader;
import com.dmsBackend.entity.Employee;
import com.dmsBackend.repository.EmployeeRepository;
import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@Component
public class QRCodeGenerator {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Value("${document.storage.path}")
    private String documentStoragePath;

    @Value("${server.host.name}")
    private String redirectPath;

    public String generateQRCode(Integer documentId, String title, String qrContent) throws WriterException, IOException {
        // Create the directory for storing QR codes if it doesn't exist
        Path qrCodeDir = Paths.get(documentStoragePath, "QRCodesStorage");
        if (!Files.exists(qrCodeDir)) {
            Files.createDirectories(qrCodeDir);
        }

        // Generate a sanitized file name
        String sanitizedTitle = title != null ? title.replaceAll("\\s+", "_") : "Untitled";
        String fileName = "Document_" + documentId + "_" + sanitizedTitle + "_" + System.currentTimeMillis() + ".png";

        // Resolve the full QR code file path
        Path qrCodePath = qrCodeDir.resolve(fileName);

        // Generate the QR code
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(qrContent, BarcodeFormat.QR_CODE, 300, 300, Map.of(EncodeHintType.MARGIN, 1));
        MatrixToImageWriter.writeToPath(bitMatrix, "PNG", qrCodePath);

        // Return the relative path (remove documentStoragePath prefix)
        return Paths.get(documentStoragePath).relativize(qrCodePath).toString();
    }

    public String generateQRCodeForDocument(DocumentHeader documentHeader) {
        try {
            Integer empId = (documentHeader.getEmployee() != null) ? documentHeader.getEmployee().getId() : null;
            Employee employee = empId != null ? employeeRepository.findById(empId).orElse(null) : null;

            Integer branchId = (employee != null && employee.getBranch() != null) ? employee.getBranch().getId() : null;
            Integer departmentId = (employee != null && employee.getDepartment() != null) ? employee.getDepartment().getId() : null;

            String redirectUrl = redirectPath + "/dms-ui#/searchByScan?id=" + documentHeader.getId()
                    + "&e=" + (empId != null ? empId : "")
                    + "&d=" + (departmentId != null ? departmentId : "")
                    + "&b=" + (branchId != null ? branchId : "");

            return generateQRCode(documentHeader.getId(), documentHeader.getTitle(), redirectUrl);
        } catch (WriterException | IOException e) {
            throw new RuntimeException("Failed to generate QR Code", e);
        }
    }


    public String readQRCode(String qrCodeFilePath) {
        try {
            BufferedImage bufferedImage = ImageIO.read(new File(qrCodeFilePath));

            if (bufferedImage == null) {
                throw new IOException("Failed to read the image. Ensure the file is a valid image.");
            }

            LuminanceSource source = new BufferedImageLuminanceSource(bufferedImage);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
            Result result = new MultiFormatReader().decode(bitmap);

            return result.getText();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read the QR code image file.", e);
        } catch (NotFoundException e) {
            throw new RuntimeException("No QR code found in the provided image.", e);
        }
    }


    private String encodeUrlParam(String param) {
        try {
            return param != null ? java.net.URLEncoder.encode(param, StandardCharsets.UTF_8.toString()) : "";
        } catch (Exception e) {
            throw new IllegalArgumentException("Error encoding URL parameter", e);
        }
    }
}

