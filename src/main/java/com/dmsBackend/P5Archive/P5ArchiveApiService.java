package com.dmsBackend.P5Archive;

import com.dmsBackend.entity.ActionTypeForReport;
import com.dmsBackend.entity.DocumentDetails;
import com.dmsBackend.entity.DocumentHeader;
import com.dmsBackend.entity.RetentionPolicy;
import com.dmsBackend.service.DocumentActivityReportService;
import com.dmsBackend.utils.CurrentUser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class P5ArchiveApiService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final P5RequestResponceRepository requestRepo;
    private final P5ApiTransactionLogger txLogger;

    @Autowired
    private CurrentUser currentUser;

    @Autowired
    private DocumentActivityReportService documentActivityReportService;
    @Value("${p5.username}") private String username;
    @Value("${p5.password}") private String password;
    @Value("${p5.client}") private String client;
    @Value("${p5.database}") private String database;
    @Value("${p5.pool}") private String pool;
    @Value("${p5.server.host}") private String serverHost;
    @Value("${p5.create.plan}") private String createPlanUrl;
    @Value("${p5.attach.files}") private String attachFilesUrl;
    @Value("${document.storage.path}") private String documentStoragePath;

    /* ================= UTIL ================= */

    private String buildAbsolutePath(String relativePath) {
        return Paths.get(documentStoragePath, relativePath)
                .toString()
                .replace("\\", "/");
    }

    /* ================= MAIN ================= */

    @Transactional
    public P5AttachResult archiveViaApi(
            List<DocumentDetails> documents,
            P5RequestResponce request,
            RetentionPolicy policy
    ) {

        if (request.getId() == null) {
            requestRepo.saveAndFlush(request);
        }

        /* ===== 1️⃣ CREATE PLAN ===== */
        Map<String, Object> planReq = Map.of(
                "description", "DMS_ARCHIVE_JOB_" + request.getLtoRetentionJob().getId(),
                "enabled", true,
                "database", database,
                "pool", pool,
                "deletefiles", false,
                "deleteall", false
        );

        request.setP5PlanrequestJson(objectMapper.valueToTree(planReq));
        requestRepo.saveAndFlush(request);

        P5ApiTransactions planTx =
                txLogger.create("POST", serverHost + createPlanUrl, policy, "PLAN");

        JsonNode planResp = executePost(planTx, planReq);

        String planId = planResp.path("ID").asText();
        request.setP5PlanId(planId);
        request.setP5PlanResponceJson(planResp);
        requestRepo.saveAndFlush(request);

        /* ===== 2️⃣ ADD FILES (ONCE) ===== */
        List<Map<String, String>> paths = documents.stream()
                .map(d -> Map.of("path", buildAbsolutePath(d.getPath())))
                .toList();

        Map<String, Object> jobReq = Map.of(
                "paths", paths,
                "level", "full",
                "description", "Archive via Java API"
        );

        request.setP5JobrequestJson(objectMapper.valueToTree(jobReq));
        requestRepo.saveAndFlush(request);

        String attachUrl =
                serverHost + attachFilesUrl.replace("{planId}", planId);

        P5ApiTransactions jobTx =
                txLogger.create("POST", attachUrl, policy, "ADDFILES");

        jobTx.setExpectedSizeKb(calculateSizeKb(documents));
        txLogger.update(jobTx);

        JsonNode jobResp = executePost(jobTx, jobReq);

        String jobId = jobResp.path("job").path("ID").asText();

        Map<String, String> handleMap = new HashMap<>();
        for (JsonNode e : jobResp.path("entries")) {
            handleMap.put(
                    e.path("path").asText(),
                    e.path("handle").asText()
            );
        }


        for (DocumentDetails detail : documents) {
            String archiveRoot = "lto:";
            DocumentHeader header = detail.getDocumentHeader();
            String archivalLocation = archiveRoot + "/" + detail.getPath();

            documentActivityReportService.logAction(
                    header,
                    detail,
                    ActionTypeForReport.ARCHIVE,
                    "REQUESTED",
                    currentUser.getCurrentEmployeeOrThrow(),
                    null,
                    Map.of(
                            "jobId", jobId,
                            "location", archivalLocation
                    )
            );
        }

        request.setP5JobId(jobId);
        request.setP5JobResponceJson(jobResp);
        requestRepo.saveAndFlush(request);

        log.info("✅ P5 Attach accepted. JobId={}", jobId);

        return new P5AttachResult(jobId, handleMap);
    }

    /* ================= HTTP ================= */

    private JsonNode executePost(P5ApiTransactions tx, Object body) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBasicAuth(username, password);
        headers.add("client", client);

        long startTime = System.currentTimeMillis();

        try {
            String json = objectMapper.writeValueAsString(body);

            // ✅ SAVE REQUEST DATA
            tx.setRequestHeaders(headers.toString());
            tx.setRequestBody(json);

            ResponseEntity<String> response =
                    restTemplate.exchange(
                            tx.getApiUrl(),
                            HttpMethod.POST,
                            new HttpEntity<>(json, headers),
                            String.class
                    );

            // ✅ SAVE RESPONSE DATA
            tx.setHttpStatus(response.getStatusCodeValue());
            tx.setResponseHeaders(response.getHeaders().toString());
            tx.setResponseBody(response.getBody());
            tx.setExecutionTimeMs(System.currentTimeMillis() - startTime);

            txLogger.update(tx);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException(
                        "P5 API failed with HTTP " + response.getStatusCodeValue()
                );
            }

            return objectMapper.readTree(response.getBody());

        } catch (Exception e) {

            tx.setHttpStatus(500);
            tx.setResponseBody(e.getMessage());
            tx.setExecutionTimeMs(System.currentTimeMillis() - startTime);
            txLogger.update(tx);

            throw new RuntimeException("P5 API Error", e);
        }
    }

    private long calculateSizeKb(List<DocumentDetails> docs) {
        return docs.stream()
                .map(DocumentDetails::getFileSizeBytes)
                .filter(Objects::nonNull)
                .mapToLong(Long::parseLong)
                .sum() / 1024;
    }



}
