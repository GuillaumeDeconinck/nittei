package com.meetsmore.nittei.api.schedule;

import com.meetsmore.nittei.api.shared.BaseApiController;
import com.meetsmore.nittei.api.structs.schedule.ScheduleApi;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class ScheduleController extends BaseApiController {

    private final ScheduleApplicationService scheduleService;

    public ScheduleController(ScheduleApplicationService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @PostMapping("/user/{userId}/schedule")
    public ResponseEntity<ScheduleApi.ScheduleResponse> createScheduleAdmin(
        @RequestHeader HttpHeaders headers,
        @PathVariable String userId,
        @Valid @RequestBody ScheduleApi.CreateScheduleRequestBody body
    ) {
        return scheduleService.createScheduleAdmin(headers, userId, body);
    }

    @GetMapping("/user/schedule/{scheduleId}")
    public ResponseEntity<ScheduleApi.ScheduleResponse> getScheduleAdmin(
        @RequestHeader HttpHeaders headers,
        @PathVariable String scheduleId
    ) {
        return scheduleService.getScheduleAdmin(headers, scheduleId);
    }

    @GetMapping("/schedule/meta")
    public ResponseEntity<ScheduleApi.GetSchedulesByMetaAPIResponse> getSchedulesByMetaAdmin(
        @RequestHeader HttpHeaders headers,
        @Valid @ModelAttribute ScheduleApi.GetSchedulesByMetaQueryParams query
    ) {
        return scheduleService.getSchedulesByMetaAdmin(headers, query);
    }

    @DeleteMapping("/user/schedule/{scheduleId}")
    public ResponseEntity<ScheduleApi.ScheduleResponse> deleteScheduleAdmin(
        @RequestHeader HttpHeaders headers,
        @PathVariable String scheduleId
    ) {
        return scheduleService.deleteScheduleAdmin(headers, scheduleId);
    }

    @PutMapping("/user/schedule/{scheduleId}")
    public ResponseEntity<ScheduleApi.ScheduleResponse> updateScheduleAdmin(
        @RequestHeader HttpHeaders headers,
        @PathVariable String scheduleId,
        @Valid @RequestBody ScheduleApi.UpdateScheduleRequestBody body
    ) {
        return scheduleService.updateScheduleAdmin(headers, scheduleId, body);
    }

    @PostMapping("/schedule")
    public ResponseEntity<ScheduleApi.ScheduleResponse> createSchedule(
        @RequestHeader HttpHeaders headers,
        @Valid @RequestBody ScheduleApi.CreateScheduleRequestBody body
    ) {
        return scheduleService.createSchedule(headers, body);
    }

    @GetMapping("/schedule/{scheduleId}")
    public ResponseEntity<ScheduleApi.ScheduleResponse> getSchedule(
        @RequestHeader HttpHeaders headers,
        @PathVariable String scheduleId
    ) {
        return scheduleService.getSchedule(headers, scheduleId);
    }

    @DeleteMapping("/schedule/{scheduleId}")
    public ResponseEntity<ScheduleApi.ScheduleResponse> deleteSchedule(
        @RequestHeader HttpHeaders headers,
        @PathVariable String scheduleId
    ) {
        return scheduleService.deleteSchedule(headers, scheduleId);
    }

    @PutMapping("/schedule/{scheduleId}")
    public ResponseEntity<ScheduleApi.ScheduleResponse> updateSchedule(
        @RequestHeader HttpHeaders headers,
        @PathVariable String scheduleId,
        @Valid @RequestBody ScheduleApi.UpdateScheduleRequestBody body
    ) {
        return scheduleService.updateSchedule(headers, scheduleId, body);
    }
}
