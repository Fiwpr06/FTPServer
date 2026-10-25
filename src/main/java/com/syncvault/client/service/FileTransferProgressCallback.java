package com.syncvault.client.service;

@FunctionalInterface
public interface FileTransferProgressCallback {
    void onProgress(long transferredBytes, long totalBytes, double percentage);
}
