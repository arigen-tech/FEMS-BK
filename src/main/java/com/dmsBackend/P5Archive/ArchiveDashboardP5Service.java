package com.dmsBackend.P5Archive;

import com.dmsBackend.entity.DocumentDetails;
import com.dmsBackend.entity.DocumentHeader;
import com.dmsBackend.entity.RetentionPolicy;
import com.dmsBackend.repository.DocumentDetailsRepository;
import com.dmsBackend.repository.RetentionPolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ArchiveDashboardP5Service {

    private final RetentionPolicyRepository retentionPolicyRepository;
    private final DocumentDetailsRepository documentDetailsRepository;

    private final P5RequestResponceRepository p5RequestResponceRepository;
    public DashboardCountResponse getDashboardCounts() {

        DashboardCountResponse response = new DashboardCountResponse();

        // -------- Policy Counts --------
        Long totalPolicy = retentionPolicyRepository.countBy();
        Long archivedPolicy = retentionPolicyRepository.countByArchiveStatus("COMPLETED");
        Long failedPolicy = retentionPolicyRepository.countByArchiveStatus("FAILED");
        Long inProgressPolicy = retentionPolicyRepository.countByArchiveStatus("IN_PROGRESS");
        Long waitingPolicy = retentionPolicyRepository.countByArchiveStatus("WAITING");

        response.setTotalPolicy(totalPolicy);
        response.setArchivedPolicy(archivedPolicy);
        response.setFailedPolicy(failedPolicy);
        response.setInProgressPolicy(inProgressPolicy);
        response.setWaitingPolicy(waitingPolicy);

        // Percentage calculation (safe)
        double archivedPercent = (totalPolicy != null && totalPolicy > 0)
                ? (archivedPolicy * 100.0) / totalPolicy
                : 0.0;

        response.setArchivedPercentByPolicy(archivedPercent);

        // -------- File Counts --------
        Long archivedFiles = documentDetailsRepository.countByLtoStatus("ARCHIVED");
        Long failedFiles = documentDetailsRepository.countByLtoStatus("FAILED");
        Long inProgressFiles = documentDetailsRepository.countByLtoStatus("IN_PROGRESS");

        Long totalFiles = archivedFiles + failedFiles + inProgressFiles;

        response.setArchivedFiles(archivedFiles);
        response.setFailedFiles(failedFiles);
        response.setInProgressFiles(inProgressFiles);
        response.setTotalFiles(totalFiles);

        return response;
    }

    public List<P5DashboardRes2> getDashboardByPolicy(Integer policyId) {

        retentionPolicyRepository.findById(Long.valueOf(policyId))
                .orElseThrow(() -> new RuntimeException("Policy not found with id: " + policyId));


        // 3️⃣ Fetch all DocumentDetails
        List<DocumentDetails> docList =
                documentDetailsRepository.findByLtoJobId(String.valueOf(policyId));

        if (docList.isEmpty()) {
            return Collections.emptyList();
        }

        // 4️⃣ Group by DocumentHeader
        Map<DocumentHeader, List<DocumentDetails>> headerMap = docList.stream()
                .filter(d -> d.getDocumentHeader() != null)
                .collect(Collectors.groupingBy(DocumentDetails::getDocumentHeader));

        // 5️⃣ Build response list
        List<P5DashboardRes2> responseList = new ArrayList<>();

        for (Map.Entry<DocumentHeader, List<DocumentDetails>> entry : headerMap.entrySet()) {

            DocumentHeader header = entry.getKey();
            List<DocumentDetails> details = entry.getValue();

            P5DashboardRes2 dto = new P5DashboardRes2();

            dto.setId(header.getId().longValue());
            dto.setDocNumber(header.getFileNo());
            dto.setTitle(header.getTitle());

            // Branch & Department from employee
            if (header.getEmployee() != null) {
                dto.setBranchId(header.getEmployee().getBranch() != null
                        ? header.getEmployee().getBranch().getId()
                        : null);
                dto.setBranchName(header.getEmployee().getBranch() != null
                        ? header.getEmployee().getBranch().getName()
                        : null);

                dto.setDepartmentId(header.getEmployee().getDepartment() != null
                        ? header.getEmployee().getDepartment().getId()
                        : null);
                dto.setDepartmentName(header.getEmployee().getDepartment() != null
                        ? header.getEmployee().getDepartment().getName()
                        : null);
            }

            // ✅ Total files = all DocumentDetails for this header
            dto.setTotalfiles(details.size());

            // ✅ Total versions = distinct version per header
            int totalVersions = (int) details.stream()
                    .map(DocumentDetails::getVersion)
                    .filter(Objects::nonNull)
                    .distinct()
                    .count();
            dto.setTotalVersion(totalVersions);

            responseList.add(dto);
        }

        return responseList;
    }
    public List<P5DashboardRes3> getDashboardByDocumentHeader(
            Integer policyId,
            Integer headerId) {


        Optional<RetentionPolicy> polocuObj = retentionPolicyRepository.findById(Long.valueOf(policyId));

        List<DocumentDetails> detailsList =
                documentDetailsRepository.findByLtoJobIdAndDocumentHeaderId(String.valueOf(policyId),headerId);

        if (detailsList.isEmpty()) {
            return Collections.emptyList();
        }

        // 🔥 GROUP BY VERSION
        Map<String, List<DocumentDetails>> versionMap = detailsList.stream()
                .filter(d -> d.getVersion() != null)
                .collect(Collectors.groupingBy(DocumentDetails::getVersion));

        List<P5DashboardRes3> response = new ArrayList<>();

        for (Map.Entry<String, List<DocumentDetails>> entry : versionMap.entrySet()) {

            String version = entry.getKey();
            List<DocumentDetails> versionDocs = entry.getValue();

            P5DashboardRes3 dto = new P5DashboardRes3();

            // ✅ comma-separated IDs
            String ids = versionDocs.stream()
                    .map(d -> d.getId().toString())
                    .collect(Collectors.joining(","));
            dto.setId(ids);

            dto.setVersion(version);
            dto.setArchivalDateTime(
                    LocalDateTime.of(
                            polocuObj.get().getRetentionDate(),
                            polocuObj.get().getRetentionTime()
                    )
            );
            // status & dates → take from first record (same for same version)
            DocumentDetails first = versionDocs.get(0);
            dto.setStatus(first.getLtoStatus());
            dto.setArchivedDate(first.getLtoArchivedOn());


            // ✅ total files per version
            dto.setTotalfiles(versionDocs.size());

            response.add(dto);
        }

        return response;
    }



    public List<P5DashboardRes4> getDocumentDetailsByIds(String idsCsv) {
        if (idsCsv == null || idsCsv.isEmpty()) {
            return Collections.emptyList();
        }

        List<Integer> ids = Arrays.stream(idsCsv.split(","))
                .map(String::trim)
                .map(Integer::valueOf)
                .collect(Collectors.toList());

        List<DocumentDetails> docs = documentDetailsRepository.findByIdIn(ids);

        return docs.stream().map(doc -> {
            P5DashboardRes4 dto = new P5DashboardRes4();
            dto.setId(doc.getId().longValue());
            dto.setFileName(doc.getDocName());
            dto.setMimeType(doc.getMimeType());
            dto.setPageCounts(doc.getPageCounts());
            dto.setFileSize(doc.getFileSizeHuman());
            dto.setStatus(doc.getLtoStatus() != null ? doc.getLtoStatus() : "PENDING");
            return dto;
        }).toList();
    }
}
