package com.dmsBackend.response;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SimpleLoginRequest {
    @NotBlank
    private String identifier;

    @NotBlank
    private String password;

}
