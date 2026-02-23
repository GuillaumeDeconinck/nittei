package com.meetsmore.nittei.api.structs.schedule;

import com.meetsmore.nittei.domain.ID;
import com.meetsmore.nittei.domain.Schedule;
import com.meetsmore.nittei.domain.ScheduleRule;
import java.util.List;

public final class ScheduleDtos {

    private ScheduleDtos() {
    }

    public record ScheduleDTO(ID id, ID userId, List<ScheduleRule> rules, String timezone, Object metadata) {
        public static ScheduleDTO from(Schedule schedule) {
            return new ScheduleDTO(
                schedule.id(),
                schedule.userId(),
                schedule.rules(),
                schedule.timezone(),
                schedule.metadata()
            );
        }
    }
}
