package com.meetsmore.nittei.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Locale;

public enum RRuleFrequency {
  YEARLY,
  MONTHLY,
  WEEKLY,
  DAILY;

  @JsonCreator
  public static RRuleFrequency fromJson(String raw) {
    if (raw == null) {
      return null;
    }
    return RRuleFrequency.valueOf(raw.trim().toUpperCase(Locale.ROOT));
  }

  @JsonValue
  public String toJson() {
    return name().toLowerCase(Locale.ROOT);
  }
}
