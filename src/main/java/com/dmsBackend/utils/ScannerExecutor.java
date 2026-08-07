package com.dmsBackend.utils;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class ScannerExecutor {
    public static String executeCommand(String[] command) throws Exception{
        ProcessBuilder pb =new ProcessBuilder(command);
        Process process = pb.start();

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream())
        );

        StringBuilder output = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null){
            output.append(line).append("\n");
        }

        int exitCode = process.waitFor();
        if(exitCode != 0){
            throw new RuntimeException("Scan failed with exit code" + exitCode);
        }

        return output.toString();
    }
}
