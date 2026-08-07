//package com.dmsBackend.service.Impl;
//
//import com.dmsBackend.service.ScannerService;
//import com.jacob.activeX.ActiveXComponent;
//import com.jacob.com.ComFailException;
//import com.jacob.com.Dispatch;
//import com.jacob.com.Variant;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.pdfbox.pdmodel.PDDocument;
//import org.apache.pdfbox.pdmodel.PDPage;
//import org.apache.pdfbox.pdmodel.PDPageContentStream;
//import org.apache.pdfbox.pdmodel.common.PDRectangle;
//import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
//import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
//import org.springframework.stereotype.Service;
//
//import javax.imageio.ImageIO;
//import java.awt.image.BufferedImage;
//import java.io.File;
//import java.nio.file.Files;
//import java.util.ArrayList;
//import java.util.List;
//
//@Service
//@Slf4j
//public class TwainScannerService implements ScannerService {
//
//    // WIA constants we need
//    // DeviceType: 1 = Scanner
//    private static final int WIA_DEVICE_TYPE_SCANNER = 1;
//
//    // Some common image format GUIDs (WIA FormatIDs)
//    // JPEG  {B96B3CAE-0728-11D3-9D7B-0000F81EF32E}
//    private static final String WIA_FORMAT_JPEG = "{B96B3CAE-0728-11D3-9D7B-0000F81EF32E}";
//    // BMP   {B96B3CAB-0728-11D3-9D7B-0000F81EF32E}
//    private static final String WIA_FORMAT_BMP = "{B96B3CAB-0728-11D3-9D7B-0000F81EF32E}";
//
//    /**
//     * Public API – scan and return a single merged PDF file in the temp folder.
//     *
//     * @param totalPages how many pages to attempt
//     * @param scanType   oneByOne | multiple
//     * @param fileName   desired pdf base name (without extension)
//     */
//
//    @Override
//    public File scanToPdf(int totalPages, String scanType, String fileName) throws Exception {
//        long t0 = System.nanoTime();
//        log.info("🖨️ scanToPdf() called | totalPages={}, scanType='{}', fileName='{}'", totalPages, scanType, fileName);
//
//        if (totalPages < 1) {
//            log.error("❌ Invalid totalPages: {} (must be >= 1)", totalPages);
//            throw new IllegalArgumentException("totalPages must be >= 1");
//        }
//
//        String safeBaseName = sanitizeFilename(fileName);
//        log.debug("🔐 Using safeBaseName='{}' for outputs", safeBaseName);
//
//        List<File> scannedImages;
//
//        try {
//            if ("multiple".equalsIgnoreCase(scanType)) {
//                log.info("📄 Mode: 'multiple' (ADF/feeder, no UI). Starting feeder scan for {} page(s) as JPEG…", totalPages);
//                scannedImages = scanFeederNoUI(totalPages, safeBaseName + "_p", WIA_FORMAT_JPEG);
//                log.info("📥 Feeder scan completed. Images received: {}", scannedImages.size());
//            } else if ("oneByOne".equalsIgnoreCase(scanType)) {
//                log.info("🧑‍💻 Mode: 'oneByOne' (UI per page). Scanning {} page(s)…", totalPages);
//                scannedImages = new ArrayList<>();
//                for (int i = 1; i <= totalPages; i++) {
//                    String outName = safeBaseName + "_p" + i + ".jpg";
//                    log.info("  ▶️ Page {}/{}: opening scanner UI… (output: {})", i, totalPages, outName);
//                    File f = scanSingleWithUI(outName, WIA_FORMAT_JPEG);
//                    long size = Files.size(f.toPath());
//                    log.info("  ✅ Page {}/{} scanned → {} ({} bytes)", i, totalPages, f.getAbsolutePath(), size);
//                    scannedImages.add(f);
//                }
//                log.info("📥 One-by-one scan completed. Images received: {}", scannedImages.size());
//            } else {
//                log.error("❌ Invalid scanType: '{}'", scanType);
//                throw new IllegalArgumentException("Invalid scanType: " + scanType);
//            }
//
//            if (scannedImages.isEmpty()) {
//                log.warn("⚠️ No images were produced by the scanner.");
//            } else if (scannedImages.size() != totalPages) {
//                log.warn("⚠️ Image count ({}) differs from requested totalPages ({})", scannedImages.size(), totalPages);
//            }
//
//            // Build PDF
//            File pdfFile = new File(System.getProperty("java.io.tmpdir"), safeBaseName + ".pdf");
//            log.info("🧩 Creating PDF at {}", pdfFile.getAbsolutePath());
//            createPdfFromImages(scannedImages, pdfFile);
//            long pdfSize = Files.size(pdfFile.toPath());
//            log.info("📄 PDF created: {} ({} bytes)", pdfFile.getAbsolutePath(), pdfSize);
//
//            // (optional) clean up temp images
//            log.info("🧹 Cleaning up {} temporary image(s)…", scannedImages.size());
//            for (File f : scannedImages) {
//                try {
//                    boolean deleted = Files.deleteIfExists(f.toPath());
//                    log.debug("   🗑️ {} {}", deleted ? "Deleted" : "Not found", f.getAbsolutePath());
//                } catch (Exception ex) {
//                    log.warn("   ⚠️ Failed to delete temp file: {} ({})", f.getAbsolutePath(), ex.toString());
//                }
//            }
//
//            long elapsedMs = (System.nanoTime() - t0) / 1_000_000;
//            log.info("✅ scanToPdf() finished | totalPages={}, scanType='{}' → {} ({} bytes) in {} ms",
//                    totalPages, scanType, pdfFile.getName(), pdfSize, elapsedMs);
//
//            return pdfFile;
//
//        } catch (Exception e) {
//            long elapsedMs = (System.nanoTime() - t0) / 1_000_000;
//            log.error("💥 scanToPdf() failed after {} ms | totalPages={}, scanType='{}', fileName='{}'",
//                    elapsedMs, totalPages, scanType, fileName, e);
//            throw e;
//        }
//    }
//
//    /**
//     * Scan a single page while showing the Windows/WIA scan UI.
//     */
//    private File scanSingleWithUI(String targetFileName, String formatGuid) throws Exception {
//        ActiveXComponent wia = new ActiveXComponent("WIA.CommonDialog");
//
//        // ShowAcquireImage params (WIA 2.0):
//        // DeviceType, Intent, Bias, FormatID, AlwaysSelectDevice, UseCommonUI, CancelError
//        Dispatch image = Dispatch.call(
//                wia, "ShowAcquireImage",
//                new Variant(WIA_DEVICE_TYPE_SCANNER), // DeviceType = Scanner
//                new Variant(0),                        // Intent = Unspecified
//                new Variant(0),                        // Bias
//                new Variant(formatGuid),               // FormatID
//                new Variant(false),                    // AlwaysSelectDevice
//                new Variant(true),                     // UseCommonUI (show UI)
//                new Variant(true)                      // CancelError (throw on cancel)
//        ).toDispatch();
//
//        File out = new File(System.getProperty("java.io.tmpdir"), targetFileName);
//        if (out.exists()) out.delete();
//        Dispatch.call(image, "SaveFile", out.getAbsolutePath());
//        return out;
//    }
//
//    /**
//     * Scan multiple pages from ADF without showing popup per page.
//     * Uses ShowSelectDevice once, then transfers pages until feeder empty.
//     */
//    private List<File> scanFeederNoUI(int totalPages, String basePrefix, String formatGuid) throws Exception {
//        List<File> files = new ArrayList<>();
//
//        // Create CommonDialog
//        ActiveXComponent wia = new ActiveXComponent("WIA.CommonDialog");
//
//        // Show device selection dialog ONCE
//        Dispatch device = wia.invoke("ShowSelectDevice").toDispatch();
//        if (device == null) {
//            throw new IllegalStateException("No scanner device selected.");
//        }
//
//        // Get Items collection from scanner
//        Dispatch items = Dispatch.get(device, "Items").toDispatch();
//
//        for (int i = 1; i <= totalPages; i++) {
//            try {
//                // Transfer one page from feeder
//                Dispatch item = Dispatch.call(items, "Item", new Variant(1)).toDispatch();
//                Dispatch imageFile = Dispatch.call(item, "Transfer", formatGuid).toDispatch();
//
//                File out = new File(System.getProperty("java.io.tmpdir"), basePrefix + i + ".jpg");
//                if (out.exists()) out.delete();
//                Dispatch.call(imageFile, "SaveFile", out.getAbsolutePath());
//
//                files.add(out);
//            } catch (ComFailException e) {
//                // Feeder is empty or no more pages
//                break;
//            }
//        }
//
//        if (files.isEmpty()) {
//            throw new IllegalStateException("No pages scanned (feeder empty?).");
//        }
//
//        return files;
//    }
//
//    /**
//     * Build a single PDF from a list of image files.
//     */
//    private void createPdfFromImages(List<File> images, File outputPdf) throws Exception {
//        try (PDDocument doc = new PDDocument()) {
//            for (File imgFile : images) {
//                BufferedImage bimg = ImageIO.read(imgFile);
//
//                // Page size same as image pixels (points). For 1:1, we use raw width/height.
//                PDPage page = new PDPage(new PDRectangle(bimg.getWidth(), bimg.getHeight()));
//                doc.addPage(page);
//
//                PDImageXObject pdImage = LosslessFactory.createFromImage(doc, bimg);
//                try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
//                    cs.drawImage(pdImage, 0, 0, bimg.getWidth(), bimg.getHeight());
//                }
//            }
//            if (outputPdf.exists()) outputPdf.delete();
//            doc.save(outputPdf);
//        }
//    }
//
//    private String sanitizeFilename(String name) {
//        String n = (name == null || name.isBlank()) ? "scanned_output" : name.trim();
//        n = n.replaceAll("[\\\\/:*?\"<>|]", "_");
//        if (n.length() > 120) n = n.substring(0, 120);
//        return n;
//    }
//}
