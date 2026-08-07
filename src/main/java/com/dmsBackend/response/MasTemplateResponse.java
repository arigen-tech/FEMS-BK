package com.dmsBackend.response;


import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;


import java.time.Instant;

@Getter
@Setter
public class MasTemplateResponse {
    private Long id;
    private String templateCode;
    private String templateName;
    private String status;
    private Integer lastChgBy;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "UTC")
    private Instant lastChgDate;
    private Integer branchId;
}