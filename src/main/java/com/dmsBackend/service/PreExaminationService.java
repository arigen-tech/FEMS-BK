package com.dmsBackend.service;

import com.dmsBackend.response.*;
import java.util.List;

public interface PreExaminationService {
    List<PreExaminationCaseResponse> getPendingPreExamCases();
    PreExaminationCaseResponse getCaseForPreExam(Integer documentHeaderId);
    ApiResponse<MessageResponse> savePreExamination(PreExaminationRequest request);

     List<EmployeeResponse> getScientificOfficersByDivision(Integer divisionId);
}
