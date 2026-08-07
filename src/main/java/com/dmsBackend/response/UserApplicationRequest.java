package com.dmsBackend.response;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class UserApplicationRequest {

    private String userAppName;
    private String url;

}
