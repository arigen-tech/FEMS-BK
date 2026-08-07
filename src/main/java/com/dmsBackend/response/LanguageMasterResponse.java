package com.dmsBackend.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LanguageMasterResponse {
    private Long id;
    private String code;
    private String name;

    private Boolean isActive;
}

