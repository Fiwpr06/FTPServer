package com.syncvault.server.rmi.dto;

import java.io.Serializable;

// Phản hồi kết quả ủy quyền thao tác ghi từ Server A về Server B
public class WriteOperationResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean success;
    private String message;
    private String verifiedSha256;

    public WriteOperationResponse() {}

    public WriteOperationResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public static WriteOperationResponse ok(String sha256) {
        WriteOperationResponse resp = new WriteOperationResponse(true, "Operation successful");
        resp.setVerifiedSha256(sha256);
        return resp;
    }

    public static WriteOperationResponse fail(String message) {
        return new WriteOperationResponse(false, message);
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getVerifiedSha256() { return verifiedSha256; }
    public void setVerifiedSha256(String verifiedSha256) { this.verifiedSha256 = verifiedSha256; }
}
