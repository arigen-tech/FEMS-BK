package com.dmsBackend.ArchiveCodes;

import com.dmsBackend.entity.*;
import com.dmsBackend.repository.*;
import com.dmsBackend.utils.CurrentUser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Slf4j
public class ArchiveServicesImpl implements ArchiveService {

    private final RetentionPolicyRepository retentionPolicyRepository;
    private final ArchiveJobRepository archiveJobRepository;
    private final DocumentDetailsRepository documentDetailsRepository;
    private final DocumentArchiveRepository documentArchiveRepository;
    private final RestTemplate restTemplate;

    private final CurrentUser currentUser;
    private final ArchivedDeleteRepository archivedDeleteRepository;

    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${archive.api.login-url}") private String loginUrl;
    @Value("${archive.api.archive-url}") private String archiveUrl;

    @Value("${archive.api.delete-url}") private String deleteUrl;
    @Value("${archive.api.restore-url}") private String restoreUrl;
    @Value("${archive.api.base-url}") private String baseUrl;
    @Value("${archive.api.username}") private String apiUsername;
    @Value("${archive.api.password}") private String apiPassword;
    @Value("${archive.api.media}") private String media;
    @Value("${archive.api.priority}") private Integer priority;

    @Value("${archive.api.instance}") private Integer instance;

    @Value("${archive.api.qos}") private Integer qos;
    @Value("${archive.api.source-server}") private String sourceServer;
    @Value("${archive.api.options}") private String options;

    @Value("${document.storage.base.path}") private String baseFilePath;

    @Value("${document.storage.path}")
    private String documentStoragePath;


    private String authToken;
    private LocalDateTime tokenExpiry;

    //================================force try============================================

    @Transactional
    public void retryFailedArchives(Long jobId) {
        ArchiveJob job = archiveJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("ArchiveJob not found: " + jobId));

        // mark job as in-progress
        job.setStatus(ArchiveJob.Status.IN_PROGRESS);
        archiveJobRepository.save(job);

        ensureValidToken(); // ensures auth token is present

        List<DocumentArchive> archives = documentArchiveRepository.findByArchiveJob(job);
        if (archives == null || archives.isEmpty()) {
            log.warn("No DocumentArchive rows found for jobId={}", jobId);
        }

        for (DocumentArchive archive : archives) {
            // only retry FAILED archives
            if (!"FAILED".equalsIgnoreCase(archive.getStatus())) {
                continue;
            }

            try {
                DocumentArchiveRequest request = mapper.treeToValue(archive.getRequestJson(), DocumentArchiveRequest.class);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("Authorization", "Bearer " + getValidToken());
                HttpEntity<DocumentArchiveRequest> entity = new HttpEntity<>(request, headers);

                // this will throw HttpStatusCodeException for non-2xx responses
                ResponseEntity<DocumentArchiveResponse> resp = restTemplate.exchange(
                        archiveUrl, HttpMethod.POST, entity, DocumentArchiveResponse.class);

                // 2xx responses here
                if (resp != null && resp.getBody() != null) {
                    DocumentArchiveResponse body = resp.getBody();

                    if ("ok".equalsIgnoreCase(body.getStatusName())) {
                        // Success
                        String requestId = body.getRequestId();
                        archive.setStatus("ARCHIVED");
                        archive.setFailedReason(null);
                        archive.setRequestId(requestId);
                        documentArchiveRepository.save(archive);

                        updateLinkedDetails(archive, requestId);

                        log.info("✅ Retry success for archiveId={} requestId={}", archive.getArchiveId(), requestId);

                    } else if ("ERR_OBJECT_ALREADY_EXISTS".equalsIgnoreCase(body.getStatusName())) {
                        // API returned 2xx but with ERR_OBJECT_ALREADY_EXISTS — treat as archived
                        log.info("ℹ️ Archive already exists (2xx+body) for archiveId={} objectName={}",
                                archive.getArchiveId(), archive.getObjectName());

                        archive.setStatus("ARCHIVED");
                        archive.setFailedReason(null);
                        // keep existing requestId
                        documentArchiveRepository.save(archive);

                        updateLinkedDetails(archive, archive.getRequestId());

                    } else {
                        // unexpected 2xx body (treat as failure)
                        archive.setStatus("FAILED");
                        archive.setFailedReason("Retry API returned unexpected statusName: " + body.getStatusName());
                        documentArchiveRepository.save(archive);

                        log.error("❌ Retry returned unexpected body for archiveId={} statusName={}", archive.getArchiveId(),
                                body.getStatusName());
                    }
                } else {
                    archive.setStatus("FAILED");
                    archive.setFailedReason("Retry API returned empty response");
                    documentArchiveRepository.save(archive);
                    log.error("❌ Retry returned empty response for archiveId={}", archive.getArchiveId());
                }

            } catch (HttpStatusCodeException hsce) {
                // Non-2xx response: try to parse body and specially handle ERR_OBJECT_ALREADY_EXISTS
                String responseBody = hsce.getResponseBodyAsString();
                try {
                    DocumentArchiveResponse errorResp = mapper.readValue(responseBody, DocumentArchiveResponse.class);
                    if ("ERR_OBJECT_ALREADY_EXISTS".equalsIgnoreCase(errorResp.getStatusName())) {
                        // treat as archived (already exists)
                        log.info("ℹ️ Archive already exists (HTTP {}) for archiveId={} objectName={}",
                                hsce.getStatusCode(), archive.getArchiveId(), archive.getObjectName());

                        archive.setStatus("ARCHIVED");
                        archive.setFailedReason(null);
                        // keep existing requestId
                        documentArchiveRepository.save(archive);

                        updateLinkedDetails(archive, archive.getRequestId());

                    } else {
                        archive.setStatus("FAILED");
                        archive.setFailedReason("HTTP " + hsce.getStatusCode() + " : " + responseBody);
                        documentArchiveRepository.save(archive);

                        log.error("❌ Retry failed for archiveId={} HTTP {} body={}",
                                archive.getArchiveId(), hsce.getStatusCode(), responseBody);
                    }
                } catch (Exception parseEx) {
                    // couldn't parse error body — mark failed with raw info
                    archive.setStatus("FAILED");
                    archive.setFailedReason("HTTP " + hsce.getStatusCode() + " body: " + responseBody);
                    documentArchiveRepository.save(archive);

                    log.error("❌ Retry failed for archiveId={} HTTP {} (unparseable body)", archive.getArchiveId(), hsce.getStatusCode());
                }

                // continue to next archive (do NOT return)
            } catch (Exception ex) {
                // general exception
                archive.setStatus("FAILED");
                archive.setFailedReason("Exception: " + ex.getMessage());
                documentArchiveRepository.save(archive);

                log.error("❌ Exception retrying archiveId={}: {}", archive.getArchiveId(), ex.getMessage(), ex);
                // continue to next archive
            }
        } // end for archives

        // ---------- recompute job counters (robust) ----------
        List<DocumentArchive> allArchives = documentArchiveRepository.findByArchiveJob(job);

        int totalFiles = 0;
        int finalArchivedFiles = 0;
        int finalFailedFiles = 0;

        // group archives by header id for document level calculation
        Map<Long, List<DocumentArchive>> byHeader = new HashMap<>();
        for (DocumentArchive a : allArchives) {
            int comps = componentsCount(a);
            totalFiles += comps;
            if ("ARCHIVED".equalsIgnoreCase(a.getStatus())) finalArchivedFiles += comps;
            if ("FAILED".equalsIgnoreCase(a.getStatus())) finalFailedFiles += comps;

            Long headerId = Long.valueOf(a.getDocumentHeader() != null ? a.getDocumentHeader().getId() : null);
            if (headerId != null) {
                byHeader.computeIfAbsent(headerId, k -> new ArrayList<>()).add(a);
            }
        }

        int finalArchivedDocs = 0;
        int finalFailedDocs = 0;
        for (Map.Entry<Long, List<DocumentArchive>> e : byHeader.entrySet()) {
            boolean anyFailed = e.getValue().stream().anyMatch(ar -> "FAILED".equalsIgnoreCase(ar.getStatus()));
            if (anyFailed) finalFailedDocs++; else finalArchivedDocs++;
        }

        // update job with recomputed values
        job.setTotalFiles(totalFiles);
        job.setArchivedFiles(finalArchivedFiles);
        job.setFailedFiles(finalFailedFiles);
        job.setArchivedDocuments(finalArchivedDocs);
        job.setFailedDocuments(finalFailedDocs);

        // final job status: FAILED if any document still failed, otherwise ARCHIVED (or your preferred final state)
        if (finalFailedDocs > 0) {
            job.setStatus(ArchiveJob.Status.FAILED);
        } else {
            job.setStatus(ArchiveJob.Status.ARCHIVED);
        }

        job.setArchivedOn(LocalDateTime.now());
        archiveJobRepository.save(job);

        log.info("🔄 Retry finished for jobId={} → totalFiles={} archivedFiles={} failedFiles={} archivedDocs={} failedDocs={}",
                jobId, totalFiles, finalArchivedFiles, finalFailedFiles, finalArchivedDocs, finalFailedDocs);
    }

    /* helper */
    private int componentsCount(DocumentArchive archive) {
        if (archive == null) return 0;
        JsonNode comps = archive.getComponents();
        if (comps == null) return 0;
        return comps.isArray() ? comps.size() : 1;
    }

    /* helper (unchanged) */
    private void updateLinkedDetails(DocumentArchive archive, String requestId) {
        List<DocumentDetails> linkedDetails = documentDetailsRepository.findByDocumentArchive(archive);
        for (DocumentDetails d : linkedDetails) {
            d.setArchive(true);
            d.setDocumentArchive(archive);
            d.setFailedReason(null);
            d.setArchivalStatus("ARCHIVED");
            if (requestId != null) {
                try {
                    d.setRequestId(Long.parseLong(requestId));
                } catch (NumberFormatException nfe) {
                    log.warn("Non-numeric requestId '{}' for detailId={}", requestId, d.getId());
                }
            }
        }
        documentDetailsRepository.saveAll(linkedDetails);
    }



    //===================================scheduler try======================================
    @Override
    @Transactional
    public void executeDueArchives() {
        List<RetentionPolicy> policies = retentionPolicyRepository.findAllActive();
        log.info("Found {} active policies", policies.size());

        for (RetentionPolicy policy : policies) {
            try {
                if (policy.getRetentionDateTime() != null &&
                        policy.getRetentionDateTime().isBefore(LocalDateTime.now())) {
                    log.info("✅ Policy {} is due for archiving", policy.getId());
                    archiveJobRepository.findByRetentionPolicy(policy)
                            .ifPresentOrElse(job -> processArchive(policy, job),
                                    () -> log.warn("⚠️ No ArchiveJob found for Policy {}", policy.getId()));
                }
            } catch (Exception e) {
                log.error("❌ Error processing policy {}: {}", policy.getId(), e.getMessage(), e);
            }
        }
    }

    private void processArchive(RetentionPolicy policy, ArchiveJob job) {
        try {
            // Always move to IN_PROGRESS if still waiting
            if (job.getStatus() == ArchiveJob.Status.WAITING) {
                job.setStatus(ArchiveJob.Status.IN_PROGRESS);
                archiveJobRepository.save(job);
            }

            Integer branchId   = policy.getBranch() != null ? policy.getBranch().getId() : null;
            Integer deptId     = policy.getDepartment() != null ? policy.getDepartment().getId() : null;
            Integer categoryId = policy.getCategory() != null ? policy.getCategory().getId() : null;

            List<DocumentDetails> details = documentDetailsRepository.findApprovedDetailsWithinPeriod(
                    policy.getFromDate(), policy.getToDate(), branchId, deptId, categoryId);

            if (details.isEmpty()) {
                log.info("No details to archive for policy {}", policy.getId());
                job.setArchivedOn(LocalDateTime.now());
                archiveJobRepository.save(job);

                // ✅ Mark policy inactive since processing is done
                policy.setIsActive(false);
                retentionPolicyRepository.save(policy);

                return; // stays IN_PROGRESS
            }

            job.setTotalFiles(details.size());
            job.setArchivedFiles(0);
            job.setFailedFiles(0);
            job.setTotalDocuments((int) details.stream()
                    .map(DocumentDetails::getDocumentHeader)
                    .distinct()
                    .count());
            job.setArchivedDocuments(0);
            job.setFailedDocuments(0);
            archiveJobRepository.save(job);

            ensureValidToken();

            int archivedDocs = 0;
            int failedDocs = 0;
            int archivedFiles = 0;
            int failedFiles = 0;

            Map<DocumentHeader, List<DocumentDetails>> groupedByHeader =
                    details.stream().collect(Collectors.groupingBy(DocumentDetails::getDocumentHeader));


            for (Map.Entry<DocumentHeader, List<DocumentDetails>> headerEntry : groupedByHeader.entrySet()) {
                DocumentHeader header = headerEntry.getKey();
                List<DocumentDetails> headerDetails = headerEntry.getValue();

                try {
                    ArchiveResult ar = processHeaderDetails(header, headerDetails, job);

                    // Files = DocumentDetails
                    archivedFiles += ar.archivedDocs;  // was archivedDocs → actually file count
                    failedFiles   += ar.failedDocs;

                    // Documents = DocumentHeader
                    if (ar.success) {
                        archivedDocs++;  // one header (document) processed fully
                    } else {
                        failedDocs++;    // one header (document) failed
                    }
                } catch (Exception e) {
                    failedFiles += headerDetails.size(); // all details failed
                    failedDocs++; // the document failed as a whole
                    log.error("❌ Failed processing header {}: {}", header.getId(), e.getMessage(), e);
                }

                // update counters live
                job.setArchivedDocuments(archivedDocs);
                job.setFailedDocuments(failedDocs);
                job.setArchivedFiles(archivedFiles);
                job.setFailedFiles(failedFiles);
                archiveJobRepository.save(job);
            }

            // ---------- Finalize ----------
            if (failedDocs > 0) {
                job.setStatus(ArchiveJob.Status.FAILED);
                log.error("❌ Archiving FAILED for policy {} ({} docs failed)", policy.getId(), failedDocs);
            } else {
                log.info("✅ Archiving completed for policy {} ({} docs archived) → stays IN_PROGRESS",
                        policy.getId(), archivedDocs);
            }

            job.setArchivedOn(LocalDateTime.now());
            archiveJobRepository.save(job);

            // ✅ After job is finalized → deactivate policy
            policy.setIsActive(false);
            retentionPolicyRepository.save(policy);

        } catch (Exception e) {
            log.error("❌ Archiving crashed for Policy {}: {}", policy.getId(), e.getMessage(), e);
            job.setStatus(ArchiveJob.Status.FAILED);
            archiveJobRepository.save(job);

            // ✅ Even if exception, mark policy inactive
            policy.setIsActive(false);
            retentionPolicyRepository.save(policy);
        }
    }

    // small helper
    private static class ArchiveResult { boolean success; int archivedDocs; int failedDocs; }


    private ArchiveResult processHeaderDetails(DocumentHeader header,
                                               List<DocumentDetails> details,
                                               ArchiveJob job) {

        ArchiveResult result = new ArchiveResult();
        int archived = 0;
        int failed = 0;

        // group by version (defensive: treat null/blank version as skip)
        Map<String, List<DocumentDetails>> byVersion = details.stream()
                .filter(d -> d.getVersion() != null && !d.getVersion().trim().isEmpty())
                .collect(Collectors.groupingBy(DocumentDetails::getVersion));

        for (Map.Entry<String, List<DocumentDetails>> verEntry : byVersion.entrySet()) {
            String version = verEntry.getKey();
            List<DocumentDetails> versionDetails = verEntry.getValue();

            if (versionDetails == null || versionDetails.isEmpty()) {
                log.warn("Skipping empty version group for header {} version {}", header.getId(), version);
                continue;
            }

            // components (filenames only)
            List<String> docNames = versionDetails.stream()
                    .map(DocumentDetails::getDocName)
                    .filter(Objects::nonNull)
                    .toList();

            if (docNames.isEmpty()) {
                log.warn("Skipping version {} for header {} because no docNames", version, header.getId());
                continue;
            }


//            String commonRoot = computeCommonPathRoot(paths);
            List<String> paths = versionDetails.stream()
                    .map(DocumentDetails::getPath)
                    .filter(Objects::nonNull)
                    .toList();

            String commonRoot = computeCommonPathRoot(paths, version);

// build filePathRoot
            String filePathRoot = (commonRoot == null || commonRoot.isBlank())
                    ? "/DMS_Document/"
                    : "/DMS_Document/" + commonRoot.replaceAll("^/+", "").replaceAll("/+$", "");

// object name
            Random random = new Random();
            int ranNum = 100 + random.nextInt(900);
            String objectName = header.getFileNo() + "_" + version + "_" + ranNum;

            // build request
            DocumentArchiveRequest request = DocumentArchiveRequest.builder()
                    .collectionName(header.getCategoryMaster().getName())
                    .comments(header.getTitle())
                    .components(docNames)
                    .filePathRoot(filePathRoot)
                    .media(media)
                    .objectName(objectName)
                    .options(options)
                    .priority(priority)
                    .qos(qos)
                    .sourceServer(sourceServer)
                    .build();

            try {
                // Prevent duplicate archive rows: find existing by job+header+objectName
                Optional<DocumentArchive> existingOpt =
                        documentArchiveRepository.findFirstByArchiveJobAndDocumentHeaderAndObjectName(job, header, objectName);

                DocumentArchive archive = existingOpt.orElseGet(DocumentArchive::new);

                // populate archive
                archive.setDocumentHeader(header);
                archive.setCollectionName(header.getCategoryMaster().getName());
                archive.setObjectName(objectName);
                archive.setVersion(version);
                archive.setFilePathRoot(filePathRoot);
                archive.setMedia(media);
                archive.setSourceServer(sourceServer);
                archive.setPriority(priority);
                archive.setComments(header.getTitle());
                archive.setStatus("PENDING");
                archive.setArchiveJob(job);
                archive.setComponents(mapper.valueToTree(docNames));
                archive.setRequestJson(mapper.valueToTree(request));

                // save (create or update)
                archive = documentArchiveRepository.save(archive);
                log.info("Saved archive row id={} objectName={} (status=PENDING)", archive.getArchiveId(), objectName);

                // Call API
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("Authorization", getValidToken());

                HttpEntity<DocumentArchiveRequest> entity = new HttpEntity<>(request, headers);

                ResponseEntity<DocumentArchiveResponse> resp = restTemplate.exchange(
                        archiveUrl, HttpMethod.POST, entity, DocumentArchiveResponse.class);

                String returnedRequestId = null;
                if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null
                        && "ok".equalsIgnoreCase(resp.getBody().getStatusName())) {

                    returnedRequestId = resp.getBody().getRequestId();
                    archive.setStatus("ARCHIVED");
                    archive.setRequestId(returnedRequestId);
                    documentArchiveRepository.save(archive);

                    Long parsedId = null;
                    if (returnedRequestId != null) {
                        try {
                            parsedId = Long.parseLong(returnedRequestId);
                        } catch (NumberFormatException nfe) {
                            log.warn("Returned requestId is not numeric: '{}' for archive id={}", returnedRequestId, archive.getArchiveId());
                        }
                    }

                    // update details
                    for (DocumentDetails d : versionDetails) {
                        d.setArchiveJob(job);
                        d.setArchive(true);
                        if (parsedId != null) {
                            d.setRequestId(parsedId);
                            d.setDocumentArchive(archive);
                        }
                    }
                    documentDetailsRepository.saveAll(versionDetails);

                    archived += versionDetails.size();
                    log.info("✅ Archived headerId={} version={} files={} requestId={}", header.getId(), version, versionDetails.size(), returnedRequestId);

                } else {
                    // API returned failure
                    archive.setStatus("FAILED");
                    String reason = (resp == null) ? "Null response" : ("HTTP " + resp.getStatusCode());
                    archive.setFailedReason(reason);
                    documentArchiveRepository.save(archive);
                    failed += versionDetails.size();
                    log.error("❌ Archive API failed for header {} version {}: {}", header.getId(), version, reason);
                }

            } catch (Exception ex) {
                log.error("❌ Exception during archiving header {} version {}: {}", header.getId(), version, ex.getMessage(), ex);
                failed += versionDetails.size();
            }
        } // end version loop

        result.archivedDocs = archived;
        result.failedDocs = failed;
        result.success = (failed == 0);
        return result;
    }

    private String computeCommonPathRoot(List<String> paths, String version) {
        if (paths == null || paths.isEmpty()) return "";

        // Normalize separators and strip filenames first
        List<String[]> parts = paths.stream()
                .map(p -> {
                    String norm = p.replace("\\", "/");
                    int lastSlash = norm.lastIndexOf('/');
                    if (lastSlash > 0) {
                        norm = norm.substring(0, lastSlash); // drop filename
                    }
                    return norm.split("/");
                })
                .toList();

        // Find common prefix length
        int minLen = parts.stream().mapToInt(a -> a.length).min().orElse(0);
        int sameCount = 0;
        for (int i = 0; i < minLen; i++) {
            String seg = parts.get(0)[i];
            boolean allMatch = true;
            for (int j = 1; j < parts.size(); j++) {
                if (!seg.equals(parts.get(j)[i])) {
                    allMatch = false;
                    break;
                }
            }
            if (!allMatch) break;
            sameCount++;
        }

        if (sameCount == 0) return "";

        // Build common prefix
        StringBuilder sb = new StringBuilder();
        for (int k = 0; k < sameCount; k++) {
            if (sb.length() > 0) sb.append('/');
            sb.append(parts.get(0)[k]);
        }
        String common = sb.toString();

        // ✅ Ensure version folder is included only once
        if (version != null && !version.isBlank()) {
            if (!common.endsWith("/" + version)) {
                common = common + "/" + version;
            }
        }

        return common;
    }

    // ==================== Nightly Reconciliation ==========================================


    /**
     * Run every night at midnight also need to change according to ltfs
     */
    @Scheduled(cron = "${scheduled.clean.time}")
    @Transactional
    public void reconcileInProgressJobs() {
        log.info("🔔 Starting nightly reconciliation...");



        List<ArchiveJob> jobs = archiveJobRepository.findByStatus(ArchiveJob.Status.IN_PROGRESS);
        log.info("📊 Found {} IN_PROGRESS jobs", jobs.size());

        if (jobs.isEmpty()) {
            log.info("No jobs to reconcile today.");
        }

        for (ArchiveJob job : jobs) {
            log.info("➡ Processing job id={} status={}", job.getId(), job.getStatus());
            try {
                processJobStatus(job);
            } catch (Exception e) {
                log.error("❌ Reconciliation failed for job {}: {}", job.getId(), e.getMessage(), e);
            }
        }

        log.info("✅ Nightly reconciliation finished.");
    }

    private void processJobStatus(ArchiveJob job) {
        log.info("   🔹 Reconciling job {}...", job.getId());

        int archivedDocs = 0;
        int failedDocs = 0;

        List<DocumentArchive> archives = documentArchiveRepository.findByArchiveJob(job);
        log.info("   Found {} archives for job {}", archives.size(), job.getId());

        for (DocumentArchive archive : archives) {
            log.info("      🔸 Processing archive id={} docHeaderId={}", archive.getArchiveId(),
                    archive.getDocumentHeader() != null ? archive.getDocumentHeader().getId() : null);

            try {
                String reqUrl = baseUrl + "/requests/" + archive.getRequestId();
                String objUrl = baseUrl + "/objects/info?objectName=" + archive.getObjectName()
                        + "&collectionName=" + archive.getCollectionName();

                HttpHeaders headers = new HttpHeaders();
                headers.set("Authorization", getValidToken());
                HttpEntity<Void> entity = new HttpEntity<>(headers);

                ResponseEntity<RequestResponse> reqResp =
                        restTemplate.exchange(reqUrl, HttpMethod.GET, entity, RequestResponse.class);
                ResponseEntity<ObjectResponse> objResp =
                        restTemplate.exchange(objUrl, HttpMethod.GET, entity, ObjectResponse.class);

                RequestResponse reqBody = reqResp.getBody();
                ObjectResponse objBody = objResp.getBody();

                log.info("         API responses → reqStatus={}, objStatus={}, stateName={}",
                        reqBody != null ? reqBody.getStatusName() : "null",
                        objBody != null ? objBody.getStatusName() : "null",
                        reqBody != null ? reqBody.getStateName() : "null");

                // 🆕 Skip if waiting for resources
                if (reqBody != null && "WAITING_FOR_RESOURCES".equalsIgnoreCase(reqBody.getStateName())) {
                    log.info("         Archive id={} is WAITING_FOR_RESOURCES → skipping reconciliation.", archive.getArchiveId());
                    continue;
                }

                boolean success = isArchiveSuccess(reqBody, objBody);
                log.info("         Archive success? {}", success);

                if (success) {
                    archive.setStatus("ARCHIVED");
                    updateDetails(archive, "ARCHIVED", null);
                    archivedDocs += archive.getComponents().size();

                    // 🆕 Delete local files
                    deleteArchivedFiles(archive);

                    log.info("         Archive id={} marked as ARCHIVED, components={}", archive.getArchiveId(),
                            archive.getComponents().size());
                } else {
                    String reason = extractFailureReason(reqBody, objBody);
                    archive.setStatus("FAILED");
                    archive.setFailedReason(reason);
                    updateDetails(archive, "FAILED", reason);
                    failedDocs += archive.getComponents().size();
                    log.info("         Archive id={} FAILED: {}, components={}", archive.getArchiveId(), reason,
                            archive.getComponents().size());
                }

                documentArchiveRepository.save(archive);
                log.info("         Archive id={} saved to DB", archive.getArchiveId());

            } catch (Exception e) {
                log.error("         ❌ API error for archive {}: {}", archive.getArchiveId(), e.getMessage(), e);
                archive.setStatus("FAILED");
                archive.setFailedReason("API call error: " + e.getMessage());
                documentArchiveRepository.save(archive);
                updateDetails(archive, "FAILED", "API call error");
                failedDocs += archive.getComponents().size();
            }
        }

        // update job counters
        job.setArchivedFiles(archivedDocs);
        job.setFailedFiles(failedDocs);
        job.setTotalFiles(archivedDocs + failedDocs);

        if (failedDocs == 0 && archivedDocs > 0) {
            job.setStatus(ArchiveJob.Status.ARCHIVED);
        } else if (archivedDocs > 0 && failedDocs > 0) {
            job.setStatus(ArchiveJob.Status.PARTIAL_SUCCESS);
        } else {
            job.setStatus(ArchiveJob.Status.FAILED);
        }
        job.setArchivedOn(LocalDateTime.now());
        archiveJobRepository.save(job);

        log.info("   🔹 Job {} reconciled → status={} (archived={}, failed={})",
                job.getId(), job.getStatus(), archivedDocs, failedDocs);
    }

    private boolean isArchiveSuccess(RequestResponse req, ObjectResponse obj) {
        return req != null
                && "ok".equalsIgnoreCase(req.getStatusName())
                && obj != null
                && "ok".equalsIgnoreCase(obj.getStatusName())
                && obj.isInserted();
    }

    private String extractFailureReason(RequestResponse req, ObjectResponse obj) {
        if (req != null && !"ok".equalsIgnoreCase(req.getStatusName())) {
            return "Request failed: " + req.getStatusDescription();
        }
        if (obj != null && !"ok".equalsIgnoreCase(obj.getStatusName())) {
            return "Object check failed: " + obj.getStatusDescription();
        }
        return "Unknown error";
    }

    private void deleteArchivedFiles(DocumentArchive archive) {
        try {
            List<DocumentDetails> details =
                    documentDetailsRepository.findByDocumentArchiveAndArchivalStatus(archive, "ARCHIVED");

            if (details.isEmpty()) {
                log.warn("         ⚠ No DocumentDetails with ARCHIVED status found for archive id={}", archive.getArchiveId());
                return;
            }

            if (archive.getComponents() == null || !archive.getComponents().isArray()) {
                log.warn("         ⚠ No components found for archive id={}", archive.getArchiveId());
                return;
            }

            for (JsonNode component : archive.getComponents()) {
                String fileName = component.asText();   // ✅ direct string
                Path filePath = Paths.get(baseFilePath, archive.getFilePathRoot(), fileName);

                if (Files.exists(filePath)) {
                    log.info("         Deleting file: {}", filePath);
                    Files.delete(filePath);
                    log.info("         ✅ Deleted file: {}", filePath);
                } else {
                    log.warn("         ⚠ File not found for deletion: {}", filePath);
                }
            }
        } catch (Exception e) {
            log.error("         ❌ Error deleting files for archive {}: {}", archive.getArchiveId(), e.getMessage(), e);
        }
    }


    private void updateDetails(DocumentArchive archive, String status, String reason) {
        List<DocumentDetails> details =
                documentDetailsRepository.findByArchiveJobAndDocumentHeader(archive.getArchiveJob(),
                        archive.getDocumentHeader());

        log.info("         Updating {} document details for archive id={}", details.size(), archive.getArchiveId());

        for (DocumentDetails d : details) {
            d.setArchivalStatus(status);
            d.setFailedReason(reason);
        }

        documentDetailsRepository.saveAll(details);
        log.info("         Document details saved for archive id={}", archive.getArchiveId());
    }

    //===========================Authentication=====================================

    private synchronized String getValidToken() {
        if (authToken == null || tokenExpiry == null || tokenExpiry.isBefore(LocalDateTime.now().minusMinutes(5))) {
            loginAndFetchToken();
        }
        return authToken;
    }

    private void ensureValidToken() {
        getValidToken();
    }

    private void loginAndFetchToken() {
        try {
            LoginRequest loginRequest = new LoginRequest(apiUsername, apiPassword);
            ResponseEntity<LoginResponse> response = restTemplate.postForEntity(loginUrl, loginRequest, LoginResponse.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                this.authToken = response.getBody().getToken();
                this.tokenExpiry = LocalDateTime.now().plusHours(1);
                log.info("✅ Obtained API token");
            } else {
                throw new RuntimeException("Login failed: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("❌ API login failed: {}", e.getMessage(), e);
            throw new RuntimeException("API authentication failed", e);
        }
    }


    //===================================Restore==========================================

    public boolean restoreFile(Map<String, Object> restoreRequest) {
        ensureValidToken();
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(authToken);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(restoreRequest, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(restoreUrl, entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() &&
                    "Success".equalsIgnoreCase((String) response.getBody().get("statusDescription"))) {
                log.info("✅ Restore request successful: {}", response.getBody());
                return true;
            }
        } catch (Exception e) {
            log.error("❌ Restore API call failed: {}", e.getMessage(), e);
        }
        return false;
    }

    @Scheduled(cron = "${scheduled.clean.time}")
    @Transactional
    public void cleanRestoredFiles() {
        List<DocumentDetails> restoredFiles = documentDetailsRepository.findByRestoredTrue();

        for (DocumentDetails doc : restoredFiles) {
            try {
                String filePath = documentStoragePath + "/" + doc.getPath();
                File file = new File(filePath);

                log.info("🛠 Trying to delete: {}", file.getAbsolutePath());

                if (file.exists()) {
                    boolean deleted = file.delete();
                    if (deleted) {
                        log.info("✅ Deleted file: {}", filePath);
                    } else {
                        log.error("❌ Failed to delete file (maybe locked or permission issue): {}", filePath);
                    }
                } else {
                    log.warn("⚠️ File not found: {}", filePath);
                }

                // reset restored flag
                doc.setRestored(false);
                documentDetailsRepository.save(doc);

            } catch (Exception e) {
                log.error("❌ Exception while deleting file {}: {}", doc.getPath(), e.getMessage(), e);
            }
        }

        log.info("Restored files cleanup completed. Total files processed: {}", restoredFiles.size());
    }


    //=============================Deleting=====================================================

    @Override
    public ArchivedDelete deleteObject(ArchivedDeleteRequest request) {
        Employee employee = currentUser.getCurrentEmployeeOrThrow();

        // build external request
        DeleteApiRequest apiRequest = DeleteApiRequest.builder()
                .objectName(request.getObjectName())
                .collectionName(request.getCollectionName())
                .priority(priority)
                .instance(instance)
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + getValidToken());
        HttpEntity<DeleteApiRequest> entity = new HttpEntity<>(apiRequest, headers);

        ResponseEntity<DeleteApiResponse> response = restTemplate.exchange(
                deleteUrl,
                HttpMethod.POST,
                entity,
                DeleteApiResponse.class
        );

        DeleteApiResponse body = response.getBody();

        // persist to DB
        ArchivedDelete archivedDelete = ArchivedDelete.builder()
                .objectName(request.getObjectName())
                .collectionName(request.getCollectionName())
                .priority(priority)
                .instance(instance)
                .statusCode(body != null ? body.getStatusCode() : null)
                .statusName(body != null ? body.getStatusName() : null)
                .statusDescription(body != null ? body.getStatusDescription() : null)
                .requestId(body != null ? body.getRequestId() : null)
                .deletedReason(request.getDeletedReason())
                .deletedBy(employee)
                .deletedAt(LocalDateTime.now())
                .build();

        return archivedDeleteRepository.save(archivedDelete);
    }


    //=================================dashboard==============================================

    @Override
    public List<ArchiveJobDTO> getArchiveJobs(Integer branch, Integer department, ArchiveJob.Status status) {
        List<ArchiveJob> jobs = archiveJobRepository.findByFilters(branch, department, status);
        return jobs.stream().map(job -> ArchiveJobDTO.builder()
                .id(job.getId())
                .policyType(job.getPolicyType())
                .fromDate(job.getFromDate())
                .toDate(job.getToDate())
                .archiveDateTime(job.getArchiveDateTime())
                .archivedDateTime(job.getArchivedOn())
                .archiveName(job.getArchiveName())
                .branchName(job.getBranch() != null ? job.getBranch().getName() : null)
                .branchId(job.getBranch() != null ? job.getBranch().getId() : null)
                .departmentName(job.getDepartment() != null ? job.getDepartment().getName() : null)
                .departmentId(job.getDepartment() != null ? job.getDepartment().getId() : null)
                .description(job.getDescription())
                .status(job.getStatus())
                .totalFiles(job.getTotalFiles())
                .archivedDocuments(job.getArchivedDocuments())
                .failedDocuments(job.getFailedDocuments())
                .totalDocuments(job.getTotalDocuments())
                .archivedFiles(job.getArchivedFiles())
                .failedFiles(job.getFailedFiles())
                .totalVersion(documentArchiveRepository.findByArchiveJob(job).toArray().length)
                .build()
        ).toList();
    }


    @Override
    public List<DocumentHeaderArchivedDTO> getGroupedArchives(Long archiveJobId) {
        List<DocumentArchive> archives = documentArchiveRepository.findByArchivedJobId(archiveJobId);

        // Group by DocumentHeader
        Map<DocumentHeader, List<DocumentArchive>> grouped = archives.stream()
                .collect(Collectors.groupingBy(DocumentArchive::getDocumentHeader));

        // Convert to DTO
        return grouped.entrySet().stream()
                .map(entry -> {
                    DocumentHeader dh = entry.getKey();
                    List<String> versions = entry.getValue().stream()
                            .map(DocumentArchive::getVersion)
                            .filter(Objects::nonNull)
                            .toList();

                    String status = entry.getValue().stream()
                            .map(DocumentArchive::getStatus)
                            .filter(Objects::nonNull)
                            .distinct()
                            .collect(Collectors.joining(", "));

                    return new DocumentHeaderArchivedDTO(
                            UUID.randomUUID().toString(), // uniqueId
                            dh.getFileNo(),
                            dh.getTitle(),
                            dh.getCategoryMaster(),
                            dh.getEmployee().getBranch(),
                            dh.getEmployee().getDepartment(),
                            dh.getId(),
                            archiveJobId,
                            versions,
                            documentDetailsRepository.countByArchiveJob_IdAndDocumentHeader_Id(archiveJobId,dh.getId()),
                            status
                    );
                })
                .toList();
    }

    @Override
    public List<DocumentDetails> getArchivedDocs(Integer documentHeaderId, Long archiveJobId, String version) {
        return documentDetailsRepository.findArchivedDocs(documentHeaderId, archiveJobId, version);
    }

}

