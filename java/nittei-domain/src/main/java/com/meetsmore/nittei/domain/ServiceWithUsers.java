package com.meetsmore.nittei.domain;

public record ServiceWithUsers(
    ID id,
    ID accountId,
    java.util.List<ServiceResource> users,
    ServiceMultiPersonOptions multiPerson,
    Object metadata) {}
