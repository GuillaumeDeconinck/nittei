package com.meetsmore.nittei.domain;

public record ServiceResource(
    ID userId,
    ID serviceId,
    TimePlan availability,
    long bufferAfter,
    long bufferBefore,
    long closestBookingTime,
    Long furthestBookingTime
) {
}
