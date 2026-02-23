package com.meetsmore.nittei.domain;

public record User(ID id, ID accountId, Object metadata, String externalId) {
}
