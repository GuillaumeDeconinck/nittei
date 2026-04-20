package com.meetsmore.nittei.api.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.meetsmore.nittei.domain.ScheduleRule;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class ServiceBookingServiceTest {

    @Test
    void scheduleDateRuleMatchesPaddedDates() {
        ScheduleRule rule = new ScheduleRule("date", "2026-04-02", List.of());

        assertTrue(ServiceBookingService.scheduleRuleMatchesDate(rule, LocalDate.of(2026, 4, 2)));
    }

    @Test
    void scheduleDateRuleMatchesUnpaddedDates() {
        ScheduleRule rule = new ScheduleRule("date", "2026-4-2", List.of());

        assertTrue(ServiceBookingService.scheduleRuleMatchesDate(rule, LocalDate.of(2026, 4, 2)));
    }

    @Test
    void scheduleDateRuleRejectsDifferentDates() {
        ScheduleRule rule = new ScheduleRule("date", "2026-4-2", List.of());

        assertFalse(ServiceBookingService.scheduleRuleMatchesDate(rule, LocalDate.of(2026, 4, 3)));
    }
}
