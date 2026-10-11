package com.syncvault.shared.model;

// Vai trò của máy chủ trong cụm phân tán
public enum ServerMode {
    PRIMARY,   // Server A - Tiếp nhận ghi chính, điều phối đồng bộ tức thời
    SECONDARY  // Server B - Bản sao dự phòng, ủy quyền ghi về Server A
}
