package com.meetsmore.nittei.domain;

import java.util.List;

public record ServiceWithUsers(
    ID id,
    ID accountId,
    java.util.List<ServiceResource> users,
    ServiceMultiPersonOptions multiPerson,
    Object metadata
) {
}
