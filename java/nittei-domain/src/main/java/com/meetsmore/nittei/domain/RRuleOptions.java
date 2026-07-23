package com.meetsmore.nittei.domain;

import java.time.Instant;
import java.time.Month;
import java.util.List;

public record RRuleOptions(
    RRuleFrequency freq,
    Integer interval,
    Integer count,
    Instant until,
    List<Integer> bysetpos,
    List<WeekDayRecurrence> byweekday,
    List<Integer> bymonthday,
    List<Month> bymonth,
    List<Integer> byyearday,
    List<Integer> byweekno,
    String weekstart) {}
