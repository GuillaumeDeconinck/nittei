package com.meetsmore.nittei.domain;

import java.time.Instant;

public record EventRemindersExpansionJob(ID eventId, Instant timestamp, long version) {}
