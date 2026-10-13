package com.syncvault.shared.exception;

public class FtpProtocolException extends Exception {
    private final int replyCode;

    public FtpProtocolException(int replyCode, String message) {
        super(message);
        this.replyCode = replyCode;
    }

    public int getReplyCode() {
        return replyCode;
    }
}
