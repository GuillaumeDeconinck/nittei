package com.meetsmore.nittei.domain;

import java.time.Instant;

public record EventInstance(Instant startTime, Instant endTime, boolean busy) {}
