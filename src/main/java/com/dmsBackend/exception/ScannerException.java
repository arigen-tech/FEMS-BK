package com.dmsBackend.exception;

public class ScannerException extends Exception {
    public ScannerException(String message) {
        super(message);
    }

    public ScannerException(String message, Throwable cause) {
        super(message, cause);
    }

}