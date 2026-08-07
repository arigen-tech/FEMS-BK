//package com.dmsBackend.controller;
//
//import com.dmsBackend.ArchiveCodes.*;
//import com.dmsBackend.entity.DocumentDetails;
//import com.dmsBackend.repository.RetentionPolicyRepository;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.core.io.InputStreamResource;
//import org.springframework.core.io.Resource;
//import org.springframework.http.HttpHeaders;
//import org.springframework.http.MediaType;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.io.File;
//import java.io.FileInputStream;
//import java.io.IOException;
//import java.util.List;
//
//
//@RestController
//@RequestMapping("/archiveJob")
//@Slf4j
//public class ArchiveJobController {
//    private final ArchiveService archiveService;
//
//    private ArchiveJobRepository archiveJobRepository;
//
//    public ArchiveJobController(ArchiveJobRepository archiveJobRepository, ArchiveService archiveService) {
//        this.archiveJobRepository = archiveJobRepository;
//        this.archiveService = archiveService;
//    }
//
////    @GetMapping("/download/{jobId}")
////    public ResponseEntity<Resource> downloadArchive(@PathVariable("jobId") Long jobId) throws IOException {
////        ArchiveJob arobj = archiveJobRepository.findByRetentionPolicyId(jobId);
////        File zipFile = archiveService.generateArchiveZip(arobj.getId());
////
////        InputStreamResource resource = new InputStreamResource(new FileInputStream(zipFile));
////
////        return ResponseEntity.ok()
////                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + zipFile.getName())
////                .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "Content-Disposition")
////                .contentLength(zipFile.length())
////                .contentType(MediaType.APPLICATION_OCTET_STREAM)
////                .body(resource);
////    }
//
//
//    @GetMapping("/getALL/DhashboardData")
//    public ResponseEntity<List<ArchiveJobDTO>> getArchiveJobs(
//            @RequestParam(required = false) Integer branchId,
//            @RequestParam(required = false) Integer deptId,
//            @RequestParam(required = false) ArchiveJob.Status status
//    ) {
//        List<ArchiveJobDTO> jobs = archiveService.getArchiveJobs(branchId, deptId, status);
//        return ResponseEntity.ok(jobs);
//    }
//
//
//    @GetMapping("/grouped/{archiveJobId}")
//    public ResponseEntity<List<DocumentHeaderArchivedDTO>> getGroupedArchives(
//            @PathVariable Long archiveJobId) {
//        return ResponseEntity.ok(archiveService.getGroupedArchives(archiveJobId));
//    }
//
//    @GetMapping("/archived/files")
//    public ResponseEntity<List<DocumentDetails>> getArchivedDocs(
//            @RequestParam Integer documentHeaderId,
//            @RequestParam Long archiveJobId,
//            @RequestParam String version) {
//        return ResponseEntity.ok(
//                archiveService.getArchivedDocs(documentHeaderId, archiveJobId, version)
//        );
//    }
//
//
//
//    @PostMapping("/retry/{jobId}")
//    public ResponseEntity<String> retryArchives(@PathVariable Long jobId) {
//        archiveService.retryFailedArchives(jobId);
//        return ResponseEntity.ok("Retry triggered for jobId=" + jobId);
//    }
//
//    @PostMapping("/delete/archivedFile")
//    public ArchivedDelete delete(@RequestBody ArchivedDeleteRequest request) {
//        return archiveService.deleteObject(request);
//    }
//
//}
