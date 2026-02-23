package com.meetsmore.nittei.api.structs.schedule;

import com.meetsmore.nittei.domain.ID;
import com.meetsmore.nittei.domain.Schedule;
import com.meetsmore.nittei.domain.ScheduleRule;
import java.util.List;

public final class ScheduleApi {

    private ScheduleApi() {
    }

    public record ScheduleResponse(ScheduleDtos.ScheduleDTO schedule) {
        public static ScheduleResponse from(Schedule schedule) {
            return new ScheduleResponse(ScheduleDtos.ScheduleDTO.from(schedule));
        }
    }

    public record UserPathParams(ID userId) {
    }

    public record SchedulePathParams(ID scheduleId) {
    }

    public record CreateScheduleRequestBody(String timezone, List<ScheduleRule> rules, Object metadata) {
    }

    public record UpdateScheduleRequestBody(String timezone, List<ScheduleRule> rules, Object metadata) {
    }

    public record GetSchedulesByMetaQueryParams(String key, String value, Integer skip, Integer limit) {
    }

    public record GetSchedulesByMetaAPIResponse(List<ScheduleDtos.ScheduleDTO> schedules) {
    }
}
