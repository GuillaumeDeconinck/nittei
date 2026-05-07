package com.meetsmore.nittei.domain;

public record Service(
    ID id, ID accountId, ServiceMultiPersonOptions multiPerson, Object metadata) {}
