package com.dmsBackend.service;

import com.dmsBackend.entity.WaitingRoom;
import com.dmsBackend.entity.WaitingRoomStatus;
import com.dmsBackend.repository.WaitingRoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WaitingRoomScheduler {

    private final WaitingRoomRepository waitingRoomRepository;

    @Value("${waitingroom.storage.path}")
    private String storagePath;


    @Scheduled(fixedRateString = "${scheduled.waitingroom.time}")// every 1 minute
    @Transactional
    public void scanAndSaveDocuments() {
        File folder = new File(storagePath);

        if (!folder.exists() || !folder.isDirectory()) {
            System.out.println("⚠️ Storage path is invalid: " + storagePath);
            return;
        }

        File[] files = folder.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isFile()) {
                try {
                    // Expected format: documentName_sourceName_year_version.ext
                    String fileName = file.getName();
                    String[] parts = fileName.split("_");

                    if (parts.length < 4) {
                        System.out.println("Skipping invalid file name: " + fileName);
                        continue;
                    }

                    String documentName = parts[0];
                    String sourceName = parts[1];
                    String year = parts[2];
                    String version = parts[3];
                    int dotIndexInVersion = version.lastIndexOf('.');
                    if (dotIndexInVersion != -1) {
                        version = version.substring(0, dotIndexInVersion);
                    }

                    // Extract file extension (file type)
                    String fileType = "";
                    int dotIndex = fileName.lastIndexOf('.');
                    if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
                        fileType = fileName.substring(dotIndex + 1).toLowerCase();
                        // handle double extension like .pdf.pdf
                        if (fileType.equals("pdf") && fileName.toLowerCase().endsWith(".pdf.pdf")) {
                            fileType = "pdf";
                        }
                    }

                    // Duplicate check: documentName + sourceName + year + version + fileType
                    boolean exists = waitingRoomRepository
                            .existsByDocumentNameAndSourceNameAndYearAndVersionAndFileType(
                                    documentName, sourceName, year, version, fileType
                            );

                    if (exists) {
                        System.out.println("File already saved, skipping: " + fileName);
                        continue;
                    }

                    // Save into DB
                    WaitingRoom wr = new WaitingRoom();
                    wr.setDocumentName(documentName);
                    wr.setSourceName(sourceName);
                    wr.setYear(year);
                    wr.setVersion(version);
                    wr.setFileType(fileType); // ✅ save extension
                    wr.setFilepath(file.getAbsolutePath());
                    wr.setCreatedOn(Timestamp.from(Instant.now()));
                    wr.setStatus(WaitingRoomStatus.PENDING);

                    waitingRoomRepository.save(wr);
                    System.out.println("✅ Saved file details: " + fileName);

                } catch (Exception e) {
                    System.err.println("❌ Error processing file: " + file.getName());
                    e.printStackTrace();
                }
            }
        }
    }



    @Transactional
    public void updateStatusToMoved(List<Integer> waitingRoomIds) {
        try {
            List<WaitingRoom> rooms = waitingRoomRepository.findAllById(waitingRoomIds);
            for (WaitingRoom room : rooms) {
                room.setStatus(WaitingRoomStatus.MOVED);
                waitingRoomRepository.save(room);
            }
            log.info("Updated {} waiting room entries to MOVED status", waitingRoomIds.size());
        } catch (Exception e) {
            log.error("Failed to update waiting room status to MOVED", e);
        }
    }

    @Transactional
    public void updateStatusToFailed(List<Integer> waitingRoomIds) {
        try {
            List<WaitingRoom> rooms = waitingRoomRepository.findAllById(waitingRoomIds);
            for (WaitingRoom room : rooms) {
                room.setStatus(WaitingRoomStatus.FAILED);
                waitingRoomRepository.save(room);
            }
            log.info("Updated {} waiting room entries to FAILED status", waitingRoomIds.size());
        } catch (Exception e) {
            log.error("Failed to update waiting room status to FAILED", e);
        }
    }


}
