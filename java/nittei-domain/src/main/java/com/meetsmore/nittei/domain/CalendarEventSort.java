package com.meetsmore.nittei.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Locale;

public enum CalendarEventSort {
    START_TIME_ASC,
    START_TIME_DESC,
    END_TIME_ASC,
    END_TIME_DESC,
    CREATED_ASC,
    CREATED_DESC,
    UPDATED_ASC,
    UPDATED_DESC,
    EVENT_UID_ASC,
    EVENT_UID_DESC;

    @JsonCreator
    public static CalendarEventSort fromJson(String raw) {
        if (raw == null) {
            return null;
        }
        String normalized = raw.trim()
            .replace("-", "_")
            .replace(" ", "_");

        // startTimeAsc -> START_TIME_ASC
        normalized = normalized
            .replaceAll("([a-z0-9])([A-Z])", "$1_$2")
            .toUpperCase(Locale.ROOT);

        return CalendarEventSort.valueOf(normalized);
    }

    @JsonValue
    public String toJson() {
        String[] parts = name().toLowerCase(Locale.ROOT).split("_");
        if (parts.length == 0) {
            return "";
        }
        StringBuilder builder = new StringBuilder(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            if (!parts[i].isEmpty()) {
                builder.append(Character.toUpperCase(parts[i].charAt(0)));
                if (parts[i].length() > 1) {
                    builder.append(parts[i].substring(1));
                }
            }
        }
        return builder.toString();
    }
}
