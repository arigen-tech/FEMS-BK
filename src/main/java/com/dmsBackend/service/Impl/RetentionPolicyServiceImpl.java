package com.dmsBackend.service.Impl;

import com.dmsBackend.ArchiveCodes.ArchiveJobRepository;
import com.dmsBackend.ArchiveWithLTO9.LtoRetentionJob;
import com.dmsBackend.ArchiveWithLTO9.LtoRetentionJobRepository;
import com.dmsBackend.P5Archive.P5DashboardRes1;
import com.dmsBackend.P5Archive.P5RequestResponce;
import com.dmsBackend.P5Archive.P5RequestResponceRepository;
import com.dmsBackend.entity.*;
import com.dmsBackend.repository.*;
import com.dmsBackend.response.NewRetentionPolicyRequest;
import com.dmsBackend.service.RetentionPolicyService;
import com.dmsBackend.utils.CurrentUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Slf4j
public class RetentionPolicyServiceImpl implements RetentionPolicyService {

    private final RetentionPolicyRepository retentionPolicyRepository;
    private final ArchiveJobRepository archiveJobRepository;

    @Autowired
    private CurrentUser currentUser;
    @Autowired
    private  LtoRetentionJobRepository jobRepository;

    @Autowired
    private P5RequestResponceRepository p5RequestResponceRepository;

    @Autowired
    private DocumentDetailsRepository documentDetailsRepository;

    @Autowired
    private BranchMasterRepository branchMasterRepository;

    @Autowired
    private DepartmentMasterRepository departmentMasterRepository;

    @Autowired
    private CategoryMasterRepository categoryMasterRepository; // ✅ added

    public RetentionPolicyServiceImpl(
            RetentionPolicyRepository retentionPolicyRepository,
            ArchiveJobRepository archiveJobRepository) {
        this.retentionPolicyRepository = retentionPolicyRepository;
        this.archiveJobRepository = archiveJobRepository;

        log.info("RetentionPolicyServiceImpl initialized - Automatic scheduling should be active");
    }

    // ---------------- CREATE POLICY ----------------

    //surbhi media
//    @Override
//    @Transactional
//    public RetentionPolicy NewCreatePolicy(NewRetentionPolicyRequest newRequest) {
//        // validate before saving
//        validatePolicyPeriod(newRequest, null);
//
//        RetentionPolicy policy = mapToNewEntity(new RetentionPolicy(), newRequest);
//        RetentionPolicy savedPolicy = retentionPolicyRepository.save(policy);
//
//        ArchiveJob job = ArchiveJob.builder()
//                .retentionPolicy(savedPolicy)
//                .policyType(savedPolicy.getPolicyType().name())
//                .fromDate(savedPolicy.getFromDate())
//                .toDate(savedPolicy.getToDate())
//                .createdOn(LocalDateTime.now())
//                .description(savedPolicy.getDescription())
//                .branch(savedPolicy.getBranch())
//                .department(savedPolicy.getDepartment())
//                .archiveDateTime(savedPolicy.getRetentionDateTime())
//                .archiveName(savedPolicy.getArchiveName())
//                .status(ArchiveJob.Status.WAITING)
//                .archivedDocuments(0)   // 👈 set explicitly
//                .failedDocuments(0)
//                .totalDocuments(0)
//                .archivedFiles(0)
//                .failedFiles(0)
//                .totalFiles(0)
//                .build();
//
//
//        archiveJobRepository.save(job);
//
//        return savedPolicy;
//    }

    @Override
    @Transactional
    public RetentionPolicy NewCreatePolicy(NewRetentionPolicyRequest newRequest) {

        log.info("🆕 Creating retention policy...");

        validatePolicyPeriod(newRequest, null);

        RetentionPolicy policy =
                mapToNewEntity(new RetentionPolicy(), newRequest);

        RetentionPolicy savedPolicy =
                retentionPolicyRepository.save(policy);

        log.info("✅ Retention policy saved with id {}", savedPolicy.getId());

        // 🔥 CREATE JOB AT POLICY CREATION
        LtoRetentionJob job = new LtoRetentionJob();
        job.setRetentionPolicy(savedPolicy);
        job.setStatus(LtoRetentionJob.JobStatus.PENDING);
        job.setStartedOn(LocalDateTime.now());

        job.setTotalFiles(0);
        job.setArchivedFiles(0);
        job.setFailedFiles(0);
        job.setTotalHeaders(0);
        job.setArchivedHeaders(0);
        job.setFailedHeaders(0);

        jobRepository.save(job);

        log.info("📌 LTO job {} created with status PENDING for policy {}",
                job.getId(), savedPolicy.getId());

        return savedPolicy;
    }

    // ---------------- FIND ALL ----------------
    @Override
    public List<RetentionPolicy> findAll() {
        List<RetentionPolicy> policies = retentionPolicyRepository.findAll();

        LocalDateTime now = LocalDateTime.now();

        return policies.stream()
                .sorted(Comparator
                        .comparing((RetentionPolicy r) -> !r.getIsActive())
                        .thenComparing(r -> {
                            LocalDateTime dt = r.getRetentionDateTime();
                            return dt == null ? Long.MAX_VALUE : Math.abs(Duration.between(now, dt).toMillis());
                        })
                )
                .toList();
    }

    @Transactional(readOnly = true)
    public List<P5DashboardRes1> findAll(Long branchId, Long departmentId) {

        List<RetentionPolicy> policies =
                retentionPolicyRepository.findByBranchAndDepartment(branchId, departmentId);

        LocalDateTime now = LocalDateTime.now();

        List<RetentionPolicy> sortedPolicies = policies.stream()
                .sorted(Comparator
                        .comparing((RetentionPolicy r) -> !r.getIsActive())
                        .thenComparing(r -> {
                            LocalDateTime dt = r.getRetentionDateTime();
                            return dt == null
                                    ? Long.MAX_VALUE
                                    : Math.abs(Duration.between(now, dt).toMillis());
                        }))
                .toList();

        return mapToDashboardRes(sortedPolicies);
    }
    // ---------------- UPDATE POLICY ----------------

//    surbhi media
//    @Override
//    @Transactional
//    public RetentionPolicy updateNewPolicy(Long id, NewRetentionPolicyRequest req) {
//        validatePolicyPeriod(req, id);
//
//        RetentionPolicy existing = retentionPolicyRepository.findById(id)
//                .orElseThrow(() -> new RuntimeException("Policy not found with id: " + id));
//
//        existing.setDescription(req.getDescription());
//
//        if (req.getFromdate() != null) {
//            existing.setFromDate(req.getFromdate().withHour(0).withMinute(0).withSecond(0).withNano(0));
//        }
//        if (req.getTodate() != null) {
//            existing.setToDate(req.getTodate().withHour(23).withMinute(59).withSecond(59).withNano(0));
//        }
//
//        existing.setRetentionDate(req.getRetentionDate());
//        existing.setRetentionTime(req.getRetentionTime());
//        existing.setIsActive(req.getIsActive());
//        existing.setPolicyType(req.getPolicyType());
//
//        existing.setBranch(req.getBranchId() == null ? null :
//                branchMasterRepository.findById(Math.toIntExact(req.getBranchId())).orElse(null));
//        existing.setDepartment(req.getDepartmentId() == null ? null :
//                departmentMasterRepository.findById(Math.toIntExact(req.getDepartmentId())).orElse(null));
//        existing.setCategory(req.getCategoryId() == null ? null :
//                categoryMasterRepository.findById(Math.toIntExact(req.getCategoryId())).orElse(null));
//
//        existing.setUpdatedOn(LocalDateTime.now());
//
//        RetentionPolicy updatedPolicy = retentionPolicyRepository.save(existing);
//
//        // Update ArchiveJob as well
//        ArchiveJob job = archiveJobRepository.findByRetentionPolicyId(updatedPolicy.getId());
//        if (job != null) {
//            job.setPolicyType(updatedPolicy.getPolicyType().name());
//            job.setFromDate(updatedPolicy.getFromDate());
//            job.setToDate(updatedPolicy.getToDate());
//            job.setDescription(updatedPolicy.getDescription());
//            job.setBranch(updatedPolicy.getBranch());
//            job.setDepartment(updatedPolicy.getDepartment());
//            job.setArchiveDateTime(updatedPolicy.getRetentionDateTime());
//            job.setStatus(ArchiveJob.Status.WAITING);
//            archiveJobRepository.save(job);
//        }
//
//        return updatedPolicy;
//    }

    @Override
    @Transactional
    public RetentionPolicy updateNewPolicy(Long id, NewRetentionPolicyRequest req) {

        validatePolicyPeriod(req, id);

        RetentionPolicy existing =
                retentionPolicyRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException("Policy not found"));

        existing.setDescription(req.getDescription());
        existing.setFromDate(req.getFromdate().withHour(0).withMinute(0).withSecond(0));
        existing.setToDate(req.getTodate().withHour(23).withMinute(59).withSecond(59));
        existing.setRetentionDate(req.getRetentionDate());
        existing.setRetentionTime(req.getRetentionTime());
        existing.setIsActive(req.getIsActive());
        existing.setPolicyType(req.getPolicyType());
        existing.setUpdatedOn(LocalDateTime.now());
        existing.setCreatedBy(currentUser.getCurrentEmployeeOrThrow().getId());

        return retentionPolicyRepository.save(existing);
    }

    // ---------------- NAME GENERATION ----------------
    private String generateRetentionName(NewRetentionPolicyRequest req) {
        String period = "";
        if (req.getFromdate() != null && req.getTodate() != null) {
            DateTimeFormatter startFmt = DateTimeFormatter.ofPattern("ddMMMyyyy");
            DateTimeFormatter endFmt   = DateTimeFormatter.ofPattern("ddMMMyyyy");
            period = req.getFromdate().format(startFmt) + "-" + req.getTodate().format(endFmt);
        } else if (req.getFromdate() != null) {
            period = req.getFromdate().format(DateTimeFormatter.ofPattern("ddMMMyyyy"));
        } else if (req.getTodate() != null) {
            period = req.getTodate().format(DateTimeFormatter.ofPattern("ddMMMyyyy"));
        }

        String branchName = "AllBr";
        if (req.getBranchId() != null) {
            BranchMaster branch = branchMasterRepository.findById(Math.toIntExact(req.getBranchId()))
                    .orElse(null);
            if (branch != null && branch.getName() != null) {
                branchName = branch.getName().replaceAll("\\s+", "") + "Br";
            }
        }

        String deptName = "AllDept";
        if (req.getDepartmentId() != null) {
            DepartmentMaster dept = departmentMasterRepository.findById(Math.toIntExact(req.getDepartmentId()))
                    .orElse(null);
            if (dept != null && dept.getName() != null) {
                deptName = dept.getName().replaceAll("\\s+", "");
            }
        }

        String categoryName = "AllCat";
        if (req.getCategoryId() != null) {
            CategoryMaster cat = categoryMasterRepository.findById(Math.toIntExact(req.getCategoryId()))
                    .orElse(null);
            if (cat != null && cat.getName() != null) {
                categoryName = cat.getName().replaceAll("\\s+", "");
            }
        }

        return "ARCH_" + period + "_" + branchName + "_" + deptName + "_" + categoryName;
    }

    // ---------------- ENTITY MAPPING ----------------
    private RetentionPolicy mapToNewEntity(RetentionPolicy policy, NewRetentionPolicyRequest req) {
        policy.setDescription(req.getDescription());

        if (req.getFromdate() != null) {
            LocalDateTime from = req.getFromdate()
                    .withHour(0).withMinute(0).withSecond(0).withNano(0);
            policy.setFromDate(from);
        }

        if (req.getTodate() != null) {
            LocalDateTime to = req.getTodate()
                    .withHour(23).withMinute(59).withSecond(59).withNano(0);
            policy.setToDate(to);
        }

        policy.setRetentionDate(req.getRetentionDate());
        policy.setRetentionTime(req.getRetentionTime());
        policy.setIsActive(req.getIsActive());
        policy.setPolicyType(req.getPolicyType());
        policy.setArchiveStatus("WAITING");
        policy.setCreatedBy(currentUser.getCurrentEmployeeOrThrow().getId());

        if (req.getBranchId() != null) {
            BranchMaster branchObj = branchMasterRepository.findById(Math.toIntExact(req.getBranchId()))
                    .orElse(null);
            policy.setBranch(branchObj);
        } else {
            policy.setBranch(null);
        }

        if (req.getDepartmentId() != null) {
            DepartmentMaster department = departmentMasterRepository.findById(Math.toIntExact(req.getDepartmentId()))
                    .orElse(null);
            policy.setDepartment(department);
        } else {
            policy.setDepartment(null);
        }

        if (req.getCategoryId() != null) {
            CategoryMaster category = categoryMasterRepository.findById(Math.toIntExact(req.getCategoryId()))
                    .orElse(null);
            policy.setCategory(category);
        } else {
            policy.setCategory(null);
        }

        String generatedName = generateRetentionName(req);
        policy.setArchiveName(generatedName);
        return policy;
    }

    // ---------------- VALIDATION ----------------
    private static final DateTimeFormatter PERIOD_FMT =
            DateTimeFormatter.ofPattern("dd MMMM yyyy 'at' HH:mm");

    private void validatePolicyPeriod(NewRetentionPolicyRequest req, Long excludeId) {
        // Normalize to day start/end
        LocalDateTime from = req.getFromdate()
                .withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime to = req.getTodate()
                .withHour(23).withMinute(59).withSecond(59).withNano(0);

        List<RetentionPolicy> conflicts = retentionPolicyRepository.findOverlappingPolicies(
                req.getPolicyType(),
                from, to,
                req.getBranchId(),
                req.getDepartmentId(),
                req.getCategoryId(),
                excludeId
        );

        if (!conflicts.isEmpty()) {
            RetentionPolicy existing = conflicts.get(0); // show first conflict
            String newPeriod = from.format(PERIOD_FMT) + " TO " + to.format(PERIOD_FMT);
            String existingPeriod = existing.getFromDate().format(PERIOD_FMT)
                    + " TO " + existing.getToDate().format(PERIOD_FMT);

            String statusMsg = Boolean.TRUE.equals(existing.getIsActive())
                    ? "when already archive is on waiting stage"
                    : "when already archived completed";

            throw new IllegalArgumentException(
                    "Something went wrong: In \"" + newPeriod + "\" overlaps with existing \"" +
                            existingPeriod + "\" period " + statusMsg
            );
        }
    }

    public List<P5DashboardRes1> mapToDashboardRes(List<RetentionPolicy> policies) {
        return policies.stream()
                .map(r -> {
                    P5DashboardRes1 dto = new P5DashboardRes1();

                    dto.setId(r.getId());
                    dto.setPolicyType(r.getPolicyType() != null ? r.getPolicyType().name() : null);
                    dto.setArchivalDateTime(r.getRetentionDateTime());
                    dto.setArchiveName(r.getArchiveName());
                    dto.setBranchId(r.getBranch() != null ? r.getBranch().getId().intValue() : null);
                    dto.setBranchName(r.getBranch() != null ? r.getBranch().getName() : null);
                    dto.setDepartmentId(r.getDepartment() != null ? r.getDepartment().getId().intValue() : null);
                    dto.setDepartmentName(r.getDepartment() != null ? r.getDepartment().getName() : null);
                    dto.setDescription(r.getDescription());
                    dto.setArchiveStatus(r.getArchiveStatus());

                    Optional<LtoRetentionJob> ltoObj = jobRepository.findByRetentionPolicyId(r.getId());

                    ltoObj.ifPresent(job -> {
                        if (job.getCompletedOn() != null) {
                            dto.setArchivedDate(job.getCompletedOn());
                        }

                        List<DocumentDetails> docList =
                                documentDetailsRepository.findByLtoJobId(String.valueOf(r.getId()));

                        dto.setTotalFiles(docList.size());

                        long totalDocuments = docList.stream()
                                .map(DocumentDetails::getDocumentHeader)
                                .filter(Objects::nonNull)
                                .distinct()
                                .count();

                        dto.setTotalDocuments(totalDocuments);
                    });

                    if (ltoObj.isEmpty()) {
                        dto.setTotalFiles(0);
                        dto.setTotalDocuments(0L);
                    }

                    return dto;
                })
                .toList();
    }
}
