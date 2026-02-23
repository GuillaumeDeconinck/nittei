package com.meetsmore.nittei.domain.booking;

import java.util.List;

public record ServiceBookingSlotsDate(String date, List<ServiceBookingSlot> slots) {
}
