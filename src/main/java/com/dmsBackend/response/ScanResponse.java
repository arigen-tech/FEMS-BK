package com.dmsBackend.response;

public class ScanResponse {
    private boolean success;
    private String message;
    private String pdfBase64;

    public ScanResponse() {}

    public ScanResponse(boolean success, String message, String pdfBase64) {
        this.success = success;
        this.message = message;
        this.pdfBase64 = pdfBase64;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getPdfBase64() { return pdfBase64; }
    public void setPdfBase64(String pdfBase64) { this.pdfBase64 = pdfBase64; }
}