package com.dmsBackend.response;


import lombok.Data;

@Data
public class ChangePasswordDTO {
    private String email;
    private String currentPassword;
    private String newPassword;

}
