package com.dmsBackend.controller;

import com.dmsBackend.entity.Employee;
import com.dmsBackend.entity.WaitingRoom;
import com.dmsBackend.entity.WaitingRoomStatus;
import com.dmsBackend.repository.WaitingRoomRepository;
import com.dmsBackend.response.ApiResponse;
import com.dmsBackend.utils.ResponseUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/home")
//@CrossOrigin("https://happytyagi.github.io/DmsFrontend/")
public class HomeController {

    private final WaitingRoomRepository waitingRoomRepository;

    @Value("${waitingroom.storage.path}")
    private String waitingRoomPath;

    public HomeController(WaitingRoomRepository waitingRoomRepository) {
        this.waitingRoomRepository = waitingRoomRepository;
    }

    @GetMapping("/welcome")
    public ResponseEntity<?> home(HttpSession session) {
        Employee employee = (Employee) session.getAttribute("employee");
        if (employee != null) {
            return ResponseEntity.ok("Welcome " + employee.getEmail());
        } else {
            return ResponseEntity.status(401).body("Unauthorized");
        }
    }

    @GetMapping("/getallwaitingroom")
    public ResponseEntity<ApiResponse<List<WaitingRoom>>> getAllDocuments() {
        List<WaitingRoom> docs = waitingRoomRepository.findByStatusIn(
                List.of(WaitingRoomStatus.PENDING, WaitingRoomStatus.FAILED)
        );
        ApiResponse<List<WaitingRoom>> response =
                ResponseUtils.createSuccessResponse(docs, new TypeReference<List<WaitingRoom>>() {});

        return ResponseEntity.ok(response);
    }


    @GetMapping("/download/waitingroom/{fileName}")
    public ResponseEntity<Resource> downloadWaitingRoomFile(@PathVariable String fileName) {
        try {
            // Decode the filename
            fileName = URLDecoder.decode(fileName, StandardCharsets.UTF_8.name());



            // Construct full file path
            Path filePath = Paths.get(waitingRoomPath, fileName);

            // Check if file exists
            if (!Files.exists(filePath)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }

            // Create resource
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }

            // Determine content type
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                    .body(resource);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }



}
