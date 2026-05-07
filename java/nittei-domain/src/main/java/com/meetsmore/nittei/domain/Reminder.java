package com.meetsmore.nittei.domain;

import java.time.Instant;

public record Reminder(
    ID accountId, ID eventId, long version, Instant remindAt, String identifier) {}
