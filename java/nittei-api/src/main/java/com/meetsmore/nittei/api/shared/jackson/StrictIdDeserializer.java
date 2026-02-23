package com.meetsmore.nittei.api.shared.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.meetsmore.nittei.domain.ID;
import java.io.IOException;
import java.util.UUID;

public class StrictIdDeserializer extends StdDeserializer<ID> {

    public StrictIdDeserializer() {
        super(ID.class);
    }

    @Override
    public ID deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getValueAsString();
        if (value == null || value.isBlank()) {
            throw JsonMappingException.from(p, "Malformed id: " + value);
        }
        try {
            UUID.fromString(value);
        } catch (Exception e) {
            throw JsonMappingException.from(p, "Malformed id: " + value);
        }
        return new ID(value);
    }
}
