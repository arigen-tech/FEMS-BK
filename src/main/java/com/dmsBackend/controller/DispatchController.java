package com.dmsBackend.controller;

import com.dmsBackend.entity.DispatchListItem;
import com.dmsBackend.response.*;
import com.dmsBackend.response.ApiResponse;
import com.dmsBackend.service.DispatchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/dispatch")
public class DispatchController {

    @Autowired private DispatchService dispatchService;

    @GetMapping("/pending")
    public ResponseEntity<List<DispatchListItem>> getPendingDispatchList() {
        return ResponseEntity.ok(dispatchService.getPendingDispatchList());
    }

    @GetMapping("/report/{reportEntryId}")
    public ResponseEntity<DispatchDetailResponse> getDispatchDetail(@PathVariable Integer reportEntryId) {
        return ResponseEntity.ok(dispatchService.getDispatchDetail(reportEntryId));
    }

    @PostMapping(value = "/save", consumes = "multipart/form-data")
    public ResponseEntity<com.dmsBackend.response.ApiResponse<MessageResponse>> saveDispatch(
            @RequestParam Integer reportEntryId,
            @RequestParam(required = false) String dispatchDate,
            @RequestParam(required = false) String dispatchReferenceNo,
            @RequestParam(required = false) String recipient,
            @RequestParam(required = false) String dispatchMode,
            @RequestParam(required = false) String dispatchRemarks,
            @RequestParam(required = false) Boolean notifyEmail,
            @RequestParam(required = false) Boolean notifySms,
            @RequestParam(required = false) MultipartFile dispatchDocument) {

        ApiResponse<MessageResponse> response = dispatchService.saveDispatch(
                reportEntryId, dispatchDate, dispatchReferenceNo, recipient,
                dispatchMode, dispatchRemarks, notifyEmail, notifySms, dispatchDocument);

        return ResponseEntity.ok(response);
    }
}