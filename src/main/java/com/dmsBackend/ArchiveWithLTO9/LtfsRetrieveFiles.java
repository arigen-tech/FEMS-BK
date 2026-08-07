package com.dmsBackend.ArchiveWithLTO9;


import com.dmsBackend.entity.ActionTypeForReport;
import com.dmsBackend.entity.DocumentDetails;
import com.dmsBackend.repository.DocumentDetailsRepository;
import com.dmsBackend.repository.RetentionPolicyRepository;
import com.dmsBackend.response.CartridgeInfo;
import com.dmsBackend.service.DocumentActivityReportService;
import com.dmsBackend.utils.CurrentUser;
import com.dmsBackend.utils.DetectCurrCartridge;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;


@Service
@RequiredArgsConstructor
@Slf4j
public class LtfsRetrieveFiles {

    private final DocumentDetailsRepository documentRepo;
    @Value("${current.ltfs.drive}")
    private String currentLtfsDrive;
    private final CurrentUser currentUser;

    @Autowired
    private DocumentActivityReportService documentActivityReportService;



    @Value("${document.storage.path}")
    private String documentStoragePath;



    @Transactional
    public String restoreFile(Integer documentDetailsId) {

        DocumentDetails doc = documentRepo.findById(documentDetailsId)
                .orElseThrow(() -> new RuntimeException("DocumentDetails not found"));

        if (doc.getApprovedOn() == null) {
            throw new RuntimeException("Document not approved yet");
        }

        String complateFilePath = documentStoragePath +"/" + doc.getPath();

        if (Files.exists(Path.of(complateFilePath))) {
            log.debug("File available in local storage | path={}", complateFilePath);
            return ("");
        }

        CartridgeInfo cartridgeInfo = DetectCurrCartridge.detect(currentLtfsDrive);

        if (!doc.getCartridgeId().equalsIgnoreCase(cartridgeInfo.getCartridge())) {
            return ("File Available On: " + doc.getCartridgeId() + " Please Insert Correct Cartridge!");
        }

        try {

            if (doc.getCartridgeId().equalsIgnoreCase(cartridgeInfo.getCartridge())) {

                Path sourcePath = Path.of(doc.getArchivedPath());
                Path destinationPath = Path.of(complateFilePath);

                if (Files.exists(sourcePath)) {
                    // Copy the file from archived location to the local storage
                    Files.copy(sourcePath, destinationPath);

                    log.debug("File successfully restored from cartridge | source={}, destination={}", sourcePath, destinationPath);

                    // Mark the document as restored
                    doc.setRestored(true);
                    doc.setRestoredCount(
                            doc.getRestoredCount() == null ? 1 : doc.getRestoredCount() + 1
                    );
                    doc.setRestoredStatus(LtoRetentionJob.JobStatus.COMPLETED.name());

                    String hlpLog = "Server:";
                    String retrieveLocation = hlpLog + "/" + doc.getPath();
                    documentActivityReportService.logAction(
                            doc.getDocumentHeader(),
                            doc,
                            ActionTypeForReport.RETRIEVE,
                            "REQUESTED",
                            currentUser.getCurrentEmployeeOrThrow(),
                            null,
                            Map.of(
                                    "location", retrieveLocation
                            )
                    );

                } else {
                    log.error("Source file not found at archived path: {}", doc.getArchivedPath());
                    throw new RuntimeException("Archived file not found");
                }

            } else {
                doc.setRestored(false);
                doc.setRestoredStatus(LtoRetentionJob.JobStatus.FAILED.name());
            }

        } catch (Exception ex) {
            log.error("Error restoring file: {}", ex.getMessage(), ex);
            doc.setRestored(false);
            doc.setRestoredStatus(LtoRetentionJob.JobStatus.FAILED.name());
            return ex.getMessage();
        }

        documentRepo.save(doc);
        return ("");
    }

    public String restoreBulk(Integer policy){
        List<DocumentDetails> dtList = documentRepo.findByLtoJobId(String.valueOf(policy));

        if(dtList.isEmpty()){throw new RuntimeException("Archived files not found");}
        List<String> response=new ArrayList<>();
        for (DocumentDetails doc :dtList) {
            response.add(restoreFile(doc.getId()));
        }
        Set<String> distinctStrings = new HashSet<>(response);

        String paragraph = String.join(" .", distinctStrings);
        log.info(paragraph);
        return paragraph;


    }

}
