package com.syncvault.shared.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// Tiện ích định dạng thời gian
public final class DateTimeUtil {
    private DateTimeUtil() {}

    private static final DateTimeFormatter DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static String format(LocalDateTime time) {
        if (time == null) return "N/A";
        return time.format(DISPLAY_FORMATTER);
    }

    public static String now() {
        return LocalDateTime.now().format(DISPLAY_FORMATTER);
    }
}
