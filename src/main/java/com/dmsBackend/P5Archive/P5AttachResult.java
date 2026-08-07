package com.dmsBackend.P5Archive;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

@Data
@AllArgsConstructor
public class P5AttachResult {
    private String jobId;
    private Map<String, String> pathToHandle;
}
