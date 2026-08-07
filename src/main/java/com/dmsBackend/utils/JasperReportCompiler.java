package com.dmsBackend.utils;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;

import java.io.File;

public class JasperReportCompiler {
    public static void compileAllReports(String sourceDirPath) {
        File folder = new File(sourceDirPath);
        File[] jrxmlFiles = folder.listFiles((dir, name) -> name.endsWith(".jrxml"));

        if (jrxmlFiles == null || jrxmlFiles.length == 0) {
            System.out.println("No .jrxml files found in: " + sourceDirPath);
            return;
        }

        for (File jrxml : jrxmlFiles) {
            try {
                String jasperPath = jrxml.getAbsolutePath().replace(".jrxml", ".jasper");
                JasperCompileManager.compileReportToFile(jrxml.getAbsolutePath(), jasperPath);
                System.out.println("Compiled: " + jrxml.getName());
            } catch (JRException e) {
                System.err.println("Failed to compile: " + jrxml.getName());
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        compileAllReports("D:\\Arigen\\Backend\\Dms\\dms\\src\\main\\resources\\jasperReport");
    }
}
