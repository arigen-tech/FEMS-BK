package com.dmsBackend.service;

import com.dmsBackend.entity.DispatchListItem;
import com.dmsBackend.response.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface DispatchService {
    List<DispatchListItem> getPendingDispatchList();
    DispatchDetailResponse getDispatchDetail(Integer reportEntryId);
    ApiResponse<MessageResponse> saveDispatch(
            Integer reportEntryId,
            String dispatchDate,
            String dispatchReferenceNo,
            String recipient,
            String dispatchMode,
            String dispatchRemarks,
            Boolean notifyEmail,
            Boolean notifySms,
            MultipartFile dispatchDocument);
}