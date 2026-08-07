package com.dmsBackend.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
public class UserApplicationResponse {

    private Long id;
    private String userAppName;
    private String url;
    private String status;
    private Integer lastChgBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastChgDate;
}
