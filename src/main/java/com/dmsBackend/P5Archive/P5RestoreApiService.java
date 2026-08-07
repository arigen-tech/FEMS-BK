package com.dmsBackend.P5Archive;

import com.dmsBackend.ArchiveWithLTO9.LtoRetentionJob;
import com.dmsBackend.entity.ActionTypeForReport;
import com.dmsBackend.entity.DocumentDetails;
import com.dmsBackend.entity.DocumentHeader;
import com.dmsBackend.entity.RetentionPolicy;
import com.dmsBackend.repository.DocumentDetailsRepository;
import com.dmsBackend.repository.RetentionPolicyRepository;
import com.dmsBackend.service.DocumentActivityReportService;
import com.dmsBackend.utils.CurrentUser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class P5RestoreApiService {

    private final DocumentDetailsRepository documentRepo;
    private final RetentionPolicyRepository retentionRepo;
    private final P5RequestResponceRepository p5RequestResponceRepository;
    private final P5ApiTransactionsRepository txRepo;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private final P5ApiTransactionLogger txLogger;

    private final CurrentUser currentUser;

    @Autowired
    private DocumentActivityReportService documentActivityReportService;

    @Value("${p5.username}") private String username;
    @Value("${p5.password}") private String password;
    @Value("${p5.client}") private String client;
    @Value("${p5.server.host}") private String serverHost;
    @Value("${p5.restore.files}") private String restoreUrl;

    @Value("${p5.job.info}") private String jobInfoUrl;

    @Value("${document.storage.path}")
    private String documentStoragePath;

    /* ======================================================
       MAIN RESTORE METHOD
       ====================================================== */

    @Transactional
    public void restoreFile(Integer documentDetailsId, String relocateBasePath) {

        DocumentDetails doc = documentRepo.findById(documentDetailsId)
                .orElseThrow(() -> new RuntimeException("DocumentDetails not found"));

        if (doc.getApprovedOn() == null) {
            throw new RuntimeException("Document not approved yet");
        }

        RetentionPolicy policy =
                retentionRepo.findByFromDateLessThanEqualAndToDateGreaterThanEqual(
                        doc.getApprovedOn().toLocalDateTime(),
                        doc.getApprovedOn().toLocalDateTime()
                ).orElseThrow(() ->
                        new RuntimeException("RetentionPolicy not found"));

        P5ApiTransactions addFilesTx =
                txRepo.findTopByRetentionPolicyAndApiTypeOrderByCreatedAtDesc(
                        policy, "ADDFILES"
                ).orElseThrow(() ->
                        new RuntimeException("Archive transaction not found"));

        String handle = extractHandle(addFilesTx.getResponseBody(), doc.getPath());
        if (handle == null) {
            throw new RuntimeException("Archive handle not found for file");
        }

        Map<String, Object> restoreRequest = Map.of(
                "entries", List.of(
                        Map.of(
                                "ID", handle,
                                "targetPath", doc.getPath()
                        )
                ),
                "description", "Restore via Java API"
        );

        P5ApiTransactions restoreTx =
                txLogger.create(
                        "POST",
                        serverHost + restoreUrl,
                        policy,
                        "RESTORE"
                );

        try {
            JsonNode response =
                    executePost(restoreTx, restoreRequest, relocateBasePath);

            // ✅ HTTP 202 → IN PROGRESS
            if (restoreTx.getHttpStatus() == 202) {

                String jobId = response.path("ID").asText(null);
                if (jobId == null) {
                    throw new RuntimeException("Restore job ID not found in response");
                }

                doc.setRestored(true);
                doc.setRestoredCount(
                        doc.getRestoredCount() == null ? 1 : doc.getRestoredCount() + 1
                );
                doc.setRestoredStatus(LtoRetentionJob.JobStatus.IN_PROGRESS.name());
                doc.setRestoreJobId(jobId);


                String hlpLog = "Server:";
                String retriveLocation = hlpLog + "/" + doc.getPath();
                documentActivityReportService.logAction(
                        doc.getDocumentHeader(),
                        doc,
                        ActionTypeForReport.RETRIEVE,
                        "REQUESTED",
                        currentUser.getCurrentEmployeeOrThrow(),
                        null,
                        Map.of(
                                "jobId", jobId,
                                "location", retriveLocation
                        )
                );

            }
            else {
                // Safety (should not normally happen)
                doc.setRestored(false);
                doc.setRestoredStatus(LtoRetentionJob.JobStatus.FAILED.name());
            }

        } catch (Exception ex) {

            // ❌ Restore API failed
            doc.setRestored(false);
            doc.setRestoredStatus(LtoRetentionJob.JobStatus.FAILED.name());
        }

        documentRepo.save(doc);
    }

    /* ======================================================
       HANDLE EXTRACTION (CRITICAL FIX)
       ====================================================== */

    private String extractHandle(String responseBody, String documentRelativePath) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode entries = root.path("entries");

            // Build absolute document path
            String expectedPath = normalizePath(
                    Paths.get(documentStoragePath, documentRelativePath).toString()
            );

            for (JsonNode entry : entries) {
                String entryPath = normalizePath(entry.path("path").asText());

                if (entryPath.equals(expectedPath)) {
                    return entry.path("handle").asText(null); // ✅ CORRECT FIELD
                }
            }

            return null;

        } catch (Exception e) {
            throw new RuntimeException("Failed to parse archive response", e);
        }
    }

    /* ======================================================
       HTTP EXECUTION
       ====================================================== */

    private JsonNode executePost(
            P5ApiTransactions tx,
            Object body,
            String relocatePath
    ) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBasicAuth(username, password);
        headers.add("client", client);
        headers.add("relocate", relocatePath);

        try {
            String json = objectMapper.writeValueAsString(body);

            tx.setRequestHeaders(headers.toString());
            tx.setRequestBody(json);

            ResponseEntity<String> response =
                    restTemplate.exchange(
                            tx.getApiUrl(),
                            HttpMethod.POST,
                            new HttpEntity<>(json, headers),
                            String.class
                    );

            tx.setHttpStatus(response.getStatusCodeValue());
            tx.setResponseBody(response.getBody());
            txLogger.update(tx);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Restore API failed");
            }

            return objectMapper.readTree(response.getBody());

        } catch (Exception e) {
            tx.setHttpStatus(500);
            tx.setResponseBody(e.getMessage());
            txLogger.update(tx);
            throw new RuntimeException("Restore API error", e);
        }
    }

    /* ======================================================
       PATH HELPERS
       ====================================================== */

    private String normalizePath(String path) {
        return path.replace("\\", "/");
    }

    private String buildAbsolutePath(String relativePath) {
        return normalizePath(
                Paths.get(documentStoragePath, relativePath).toString()
        );
    }

    private void evaluateByFileSystem(DocumentDetails doc) {

        Path restoredFile = Paths.get(documentStoragePath)
                .resolve(doc.getPath())
                .normalize();

        if (Files.exists(restoredFile) && Files.isRegularFile(restoredFile)) {

            doc.setRestored(true);
            doc.setRestoredStatus(
                    LtoRetentionJob.JobStatus.COMPLETED.name()
            );
            doc.setLtoRestoredOn(LocalDateTime.now());

            String hlpLog = "Server:";
            String retriveLocation = hlpLog + "/" + doc.getPath();
            String jobId = doc.getRestoreJobId();
            documentActivityReportService.logAction(
                    doc.getDocumentHeader(),
                    doc,
                    ActionTypeForReport.RETRIEVE,
                    "SUCCESS",
                    currentUser.getCurrentEmployeeOrThrow(),
                    null,
                    Map.of(
                            "jobId", jobId,
                            "location", retriveLocation
                    )
            );

            log.info("✅ File is ready to view for docname=\"{}\" path={}",
                    doc.getDocName(), restoredFile);

        } else {

            doc.setRestored(false);
            doc.setRestoredStatus(
                    LtoRetentionJob.JobStatus.FAILED.name()
            );

            log.warn("❌ File failed to view for docname=\"{}\" path={}",
                    doc.getDocName(), restoredFile);
        }

        documentRepo.save(doc);
    }

    @Transactional
    public void resolveFinalRestoreStatus(
            Integer documentDetailsId,
            String jobId,
            RetentionPolicy policy
    ) {

        DocumentDetails doc = documentRepo.findById(documentDetailsId)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        String jobUrl = serverHost + jobInfoUrl + "/" + jobId;

        P5ApiTransactions tx =
                txLogger.create("GET", jobUrl, policy, "JOB_INFO");

        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(username, password);
        headers.add("client", client);

        try {
            tx.setRequestHeaders(headers.toString());

            ResponseEntity<String> response =
                    restTemplate.exchange(
                            jobUrl,
                            HttpMethod.GET,
                            new HttpEntity<>(headers),
                            String.class
                    );

            int status = response.getStatusCodeValue();

            tx.setHttpStatus(status);
            tx.setResponseBody(response.getBody());
            txLogger.update(tx);

            // 🟡 JOB STILL RUNNING
            if (status == 200) {
                log.info("Restore job {} still running", jobId);
                return;
            }

            // 🔥 YOUR BUSINESS RULE
            if (status == 404) {

                log.info("Restore job {} finished (404)", jobId);

                // FINAL FILE CHECK
                evaluateByFileSystem(doc);

                // CLOSE JOB
//                doc.setRestoreJobId(null);
                documentRepo.save(doc);

                return;
            }

            // ❌ Any other status → FAILED
            log.error("Unexpected job status {} for job {}", status, jobId);

            doc.setRestored(false);
            doc.setRestoredStatus(
                    LtoRetentionJob.JobStatus.FAILED.name()
            );
//            doc.setRestoreJobId(null);
            documentRepo.save(doc);

        } catch (Exception ex) {

            tx.setHttpStatus(500);
            tx.setResponseBody(ex.getMessage());
            txLogger.update(tx);

            log.error("Restore status check failed for job {}", jobId, ex);
        }
    }




    private void failBulk(List<DocumentDetails> documents) {
        for (DocumentDetails doc : documents) {
            doc.setRestored(false);
            doc.setRestoredStatus(
                    LtoRetentionJob.JobStatus.FAILED.name()
            );
        }
        documentRepo.saveAll(documents);
    }



    @Async
    @Transactional
    public void restoreFilesBulk(
            Long policyId,
            String relocateBasePath
    ) {

        P5RequestResponce p5rrObj = p5RequestResponceRepository.findByRetentionPolicyId(policyId);



        List<DocumentDetails> documents =
                documentRepo.findByLtoJobId(p5rrObj.getP5JobId());

        if (documents.isEmpty()) {
            throw new RuntimeException("No documents found for bulk restore");
        }

        // Assume all docs belong to same retention window
        DocumentDetails firstDoc = documents.get(0);

        RetentionPolicy policy =
                retentionRepo.findByFromDateLessThanEqualAndToDateGreaterThanEqual(
                        firstDoc.getApprovedOn().toLocalDateTime(),
                        firstDoc.getApprovedOn().toLocalDateTime()
                ).orElseThrow(() ->
                        new RuntimeException("RetentionPolicy not found"));

        P5ApiTransactions addFilesTx =
                txRepo.findTopByRetentionPolicyAndApiTypeOrderByCreatedAtDesc(
                        policy, "ADDFILES"
                ).orElseThrow(() ->
                        new RuntimeException("Archive transaction not found"));

        List<Map<String, Object>> entries = new java.util.ArrayList<>();

        for (DocumentDetails doc : documents) {

            if (doc.getApprovedOn() == null) {
                log.warn("Skipping unapproved document id={}", doc.getId());
                continue;
            }

            String handle =
                    extractHandle(addFilesTx.getResponseBody(), doc.getPath());

            if (handle == null) {
                log.warn("Handle not found for {}", doc.getPath());
                continue;
            }

            entries.add(
                    Map.of(
                            "ID", handle,
                            "targetPath", doc.getPath()
                    )
            );
        }

        if (entries.isEmpty()) {
            throw new RuntimeException("No valid entries for bulk restore");
        }

        Map<String, Object> restoreRequest = Map.of(
                "entries", entries,
                "description", "Bulk Restore via Java API"
        );

        P5ApiTransactions restoreTx =
                txLogger.create(
                        "POST",
                        serverHost + restoreUrl,
                        policy,
                        "RESTORE_BULK"
                );

        try {
            JsonNode response =
                    executePost(restoreTx, restoreRequest, relocateBasePath);

            if (restoreTx.getHttpStatus() == 202) {

                String jobId = response.path("ID").asText(null);
                if (jobId == null) {
                    throw new RuntimeException("Restore job ID not returned");
                }

                // Update all documents
                for (DocumentDetails doc : documents) {
                    doc.setRestored(true);
                    doc.setRestoredCount(
                            doc.getRestoredCount() == null
                                    ? 1
                                    : doc.getRestoredCount() + 1
                    );
                    doc.setRestoredStatus(
                            LtoRetentionJob.JobStatus.IN_PROGRESS.name()
                    );
                    doc.setRestoreJobId(jobId);

                    String hlpLog = "Server:";
                    String retriveLocation = hlpLog + "/" + doc.getPath();
                    documentActivityReportService.logAction(
                            doc.getDocumentHeader(),
                            doc,
                            ActionTypeForReport.RETRIEVE,
                            "REQUESTED",
                            currentUser.getCurrentEmployeeOrThrow(),
                            null,
                            Map.of(
                                    "jobId", jobId,
                                    "location", retriveLocation
                            )
                    );
                }

                documentRepo.saveAll(documents);

            } else {
                failBulk(documents);
            }

        } catch (Exception ex) {
            failBulk(documents);
            throw ex;
        }
    }

    @Transactional
    public void cleanRestoredFilesOlderThan(LocalDateTime cutoffDate) {
        List<DocumentDetails> filesToDelete = documentRepo.findRestoredBefore(cutoffDate);

        for (DocumentDetails doc : filesToDelete) {
            try {
                String filePath = documentStoragePath + "/" + doc.getPath();
                Path path = Path.of(filePath);

                if (Files.exists(path)) {
                    Files.delete(path);
                    System.out.println("Deleted file: " + filePath);
                }

                // Update DB flags
                doc.setRestored(false);
                doc.setRestoredStatus(null);
                doc.setLtoRestoredOn(null);
                doc.setRestoreJobId(null);
                documentRepo.save(doc);

            } catch (Exception e) {
                System.err.println("Failed to delete file: " + doc.getDocName() + " - " + e.getMessage());
            }
        }
    }




//    for external api requirement restore by doc details ids

    @Async
    @Transactional
    public void restoreByDocumentDetailsIds(
            List<Integer> documentDetailsIds,
            String relocateBasePath
    ) {

        List<DocumentDetails> documents =
                documentRepo.findAllById(documentDetailsIds);

        if (documents.isEmpty()) {
            throw new RuntimeException("No documents found for restore");
        }

        // Group documents by retention policy
        Map<RetentionPolicy, List<DocumentDetails>> policyGroupedDocs =
                groupDocumentsByRetentionPolicy(documents);

        for (Map.Entry<RetentionPolicy, List<DocumentDetails>> entry
                : policyGroupedDocs.entrySet()) {

            RetentionPolicy policy = entry.getKey();
            List<DocumentDetails> policyDocs = entry.getValue();

            restoreForSinglePolicy(policy, policyDocs, relocateBasePath);
        }
    }



    private Map<RetentionPolicy, List<DocumentDetails>> groupDocumentsByRetentionPolicy(
            List<DocumentDetails> documents
    ) {

        Map<RetentionPolicy, List<DocumentDetails>> grouped = new HashMap<>();

        for (DocumentDetails doc : documents) {

            if (doc.getApprovedOn() == null) {
                log.warn("Skipping unapproved document id={}", doc.getId());
                continue;
            }

            RetentionPolicy policy =
                    retentionRepo.findByFromDateLessThanEqualAndToDateGreaterThanEqual(
                            doc.getApprovedOn().toLocalDateTime(),
                            doc.getApprovedOn().toLocalDateTime()
                    ).orElseThrow(() ->
                            new RuntimeException(
                                    "RetentionPolicy not found for document id=" + doc.getId()
                            )
                    );

            grouped
                    .computeIfAbsent(policy, k -> new ArrayList<>())
                    .add(doc);
        }

        return grouped;
    }



    @Transactional
    public void restoreForSinglePolicy(
            RetentionPolicy policy,
            List<DocumentDetails> documents,
            String relocateBasePath
    ) {

        P5ApiTransactions addFilesTx =
                txRepo.findTopByRetentionPolicyAndApiTypeOrderByCreatedAtDesc(
                        policy, "ADDFILES"
                ).orElseThrow(() ->
                        new RuntimeException("ADDFILES transaction not found for policy " + policy.getId())
                );

        List<Map<String, Object>> entries = new ArrayList<>();

        for (DocumentDetails doc : documents) {

            String handle =
                    extractHandle(addFilesTx.getResponseBody(), doc.getPath());

            if (handle == null) {
                log.warn("Handle not found for document id={}", doc.getId());
                continue;
            }

            entries.add(
                    Map.of(
                            "ID", handle,
                            "targetPath", doc.getPath()
                    )
            );
        }

        if (entries.isEmpty()) {
            failBulk(documents);
            return;
        }

        Map<String, Object> restoreRequest = Map.of(
                "entries", entries,
                "description", "Restore by documentDetailsId list"
        );

        P5ApiTransactions restoreTx =
                txLogger.create(
                        "POST",
                        serverHost + restoreUrl,
                        policy,
                        "RESTORE_BULK_AUTO"
                );

        try {
            JsonNode response =
                    executePost(restoreTx, restoreRequest, relocateBasePath);

            if (restoreTx.getHttpStatus() == 202) {

                String jobId = response.path("ID").asText(null);

                if (jobId == null) {
                    throw new RuntimeException("Restore job ID not returned");
                }

                for (DocumentDetails doc : documents) {
                    doc.setRestored(true);
                    doc.setRestoredStatus(
                            LtoRetentionJob.JobStatus.IN_PROGRESS.name()
                    );
                    doc.setRestoreJobId(jobId);
                    doc.setRestoredCount(
                            doc.getRestoredCount() == null
                                    ? 1
                                    : doc.getRestoredCount() + 1
                    );

                    String hlpLog = "Server:";
                    String retriveLocation = hlpLog + "/" + doc.getPath();
                    documentActivityReportService.logAction(
                            doc.getDocumentHeader(),
                            doc,
                            ActionTypeForReport.RETRIEVE,
                            "REQUESTED",
                            currentUser.getCurrentEmployeeOrThrow(),
                            null,
                            Map.of(
                                    "jobId", jobId,
                                    "location", retriveLocation
                            )
                    );
                }

                documentRepo.saveAll(documents);

            } else {
                failBulk(documents);
            }

        } catch (Exception ex) {
            failBulk(documents);
            throw ex;
        }
    }






}
