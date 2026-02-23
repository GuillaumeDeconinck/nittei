package com.meetsmore.nittei.domain;

import java.util.Map;

public record Metadata(Map<String, Object> inner) {
    public static Metadata of(String key, String value) {
        return new Metadata(Map.of(key, value));
    }
}
