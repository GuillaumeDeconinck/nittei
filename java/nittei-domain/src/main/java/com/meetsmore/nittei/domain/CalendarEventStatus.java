package com.meetsmore.nittei.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Locale;

public enum CalendarEventStatus {
    TENTATIVE,
    CONFIRMED,
    CANCELLED;

    @JsonCreator
    public static CalendarEventStatus fromJson(String raw) {
        if (raw == null) {
            return null;
        }
        return CalendarEventStatus.valueOf(raw.trim().toUpperCase(Locale.ROOT));
    }

    @JsonValue
    public String toJson() {
        return name().toLowerCase(Locale.ROOT);
    }
}
