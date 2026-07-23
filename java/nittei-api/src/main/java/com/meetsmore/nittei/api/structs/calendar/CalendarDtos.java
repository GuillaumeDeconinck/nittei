package com.meetsmore.nittei.api.structs.calendar;

import com.meetsmore.nittei.domain.Calendar;
import com.meetsmore.nittei.domain.CalendarSettings;
import com.meetsmore.nittei.domain.ID;
import java.time.DayOfWeek;

public final class CalendarDtos {

  private CalendarDtos() {}

  public record CalendarDTO(
      ID id, ID userId, String name, String key, CalendarSettingsDTO settings, Object metadata) {
    public static CalendarDTO from(Calendar calendar) {
      return new CalendarDTO(
          calendar.id(),
          calendar.userId(),
          calendar.name(),
          calendar.key(),
          CalendarSettingsDTO.from(calendar.settings()),
          calendar.metadata());
    }
  }

  public record CalendarSettingsDTO(DayOfWeek weekStart, String timezone) {
    public static CalendarSettingsDTO from(CalendarSettings settings) {
      return new CalendarSettingsDTO(settings.weekStart(), settings.timezone());
    }
  }
}
