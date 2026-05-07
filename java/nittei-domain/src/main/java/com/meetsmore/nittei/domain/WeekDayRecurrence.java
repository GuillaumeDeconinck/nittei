package com.meetsmore.nittei.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.time.DayOfWeek;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record WeekDayRecurrence(Integer n, DayOfWeek weekday) {

  private static final Pattern WITH_N = Pattern.compile("^([+-]?\\d+)([A-Za-z]{3})$");

  @JsonCreator
  public static WeekDayRecurrence fromJson(String raw) {
    if (raw == null || raw.isBlank()) {
      return null;
    }
    String text = raw.trim();
    Matcher m = WITH_N.matcher(text);
    if (m.matches()) {
      return new WeekDayRecurrence(Integer.parseInt(m.group(1)), parseDay3(m.group(2)));
    }
    return new WeekDayRecurrence(null, parseDay3(text));
  }

  @JsonValue
  public String toJson() {
    String day =
        switch (weekday) {
          case MONDAY -> "Mon";
          case TUESDAY -> "Tue";
          case WEDNESDAY -> "Wed";
          case THURSDAY -> "Thu";
          case FRIDAY -> "Fri";
          case SATURDAY -> "Sat";
          case SUNDAY -> "Sun";
        };
    return n == null ? day : (n + day);
  }

  private static DayOfWeek parseDay3(String token) {
    return switch (token.toLowerCase(Locale.ROOT)) {
      case "mon" -> DayOfWeek.MONDAY;
      case "tue" -> DayOfWeek.TUESDAY;
      case "wed" -> DayOfWeek.WEDNESDAY;
      case "thu" -> DayOfWeek.THURSDAY;
      case "fri" -> DayOfWeek.FRIDAY;
      case "sat" -> DayOfWeek.SATURDAY;
      case "sun" -> DayOfWeek.SUNDAY;
      default -> throw new IllegalArgumentException("Unsupported weekday: " + token);
    };
  }
}
