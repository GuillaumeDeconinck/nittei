package com.meetsmore.nittei.domain;

import com.fasterxml.jackson.annotation.JsonValue;
import java.util.UUID;

public record ID(@JsonValue String value) {

    public static ID random() {
        return new ID(UUID.randomUUID().toString());
    }
}
