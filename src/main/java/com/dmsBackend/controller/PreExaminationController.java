package com.dmsBackend.controller;


import com.dmsBackend.response.*;
import com.dmsBackend.service.PreExaminationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/pre-examination")
public class PreExaminationController {

    @Autowired private PreExaminationService preExaminationService;

    @GetMapping("/pending")
    public ResponseEntity<List<PreExaminationCaseResponse>> getPendingCases() {
        return ResponseEntity.ok(preExaminationService.getPendingPreExamCases());
    }

    @GetMapping("/case/{documentHeaderId}")
    public ResponseEntity<PreExaminationCaseResponse> getCase(@PathVariable Integer documentHeaderId) {
        return ResponseEntity.ok(preExaminationService.getCaseForPreExam(documentHeaderId));
    }

    @PostMapping("/save")
    public ResponseEntity<com.dmsBackend.response.ApiResponse<MessageResponse>> save(
            @RequestBody PreExaminationRequest request) {

        return ResponseEntity.ok(
                preExaminationService.savePreExamination(request)
        );
    }


    @GetMapping("/employees/scientific-officers/{divisionId}")
    public ResponseEntity<List<EmployeeResponse>> getScientificOfficersByDivision(@PathVariable Integer divisionId) {
        return ResponseEntity.ok(preExaminationService.getScientificOfficersByDivision(divisionId));
    }

}
