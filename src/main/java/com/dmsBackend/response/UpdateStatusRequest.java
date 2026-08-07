package com.dmsBackend.response;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class UpdateStatusRequest {
    private List<ApplicationStatusUpdate> applications;

    @Getter
    @Setter
    public static class ApplicationStatusUpdate {
        private String appId;
        private String status;
    }
}
