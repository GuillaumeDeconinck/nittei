package com.meetsmore.nittei.infra.repos.postgres;

import com.meetsmore.nittei.domain.ID;
import com.meetsmore.nittei.domain.ServiceResource;
import com.meetsmore.nittei.domain.TimePlan;
import com.meetsmore.nittei.infra.repos.ServiceUserRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

public class PostgresServiceUserRepository implements ServiceUserRepository {

    private final JdbcTemplate jdbcTemplate;

    public PostgresServiceUserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void insert(ServiceResource user) {
        UUID availableCalendarId = null;
        UUID availableScheduleId = null;
        String variant = user.availability() == null ? "Empty" : user.availability().variant();
        ID planId = user.availability() == null ? null : user.availability().id();

        if ("calendar".equalsIgnoreCase(variant) && planId != null) {
            availableCalendarId = toUuid(planId);
        } else if ("schedule".equalsIgnoreCase(variant) && planId != null) {
            availableScheduleId = toUuid(planId);
        }

        jdbcTemplate.update(
            "INSERT INTO service_users(service_uid, user_uid, available_calendar_uid, available_schedule_uid, buffer_after, buffer_before, closest_booking_time, furthest_booking_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
            toUuid(user.serviceId()),
            toUuid(user.userId()),
            availableCalendarId,
            availableScheduleId,
            user.bufferAfter(),
            user.bufferBefore(),
            user.closestBookingTime(),
            user.furthestBookingTime()
        );
    }

    @Override
    public void save(ServiceResource user) {
        UUID availableCalendarId = null;
        UUID availableScheduleId = null;
        String variant = user.availability() == null ? "Empty" : user.availability().variant();
        ID planId = user.availability() == null ? null : user.availability().id();

        if ("calendar".equalsIgnoreCase(variant) && planId != null) {
            availableCalendarId = toUuid(planId);
        } else if ("schedule".equalsIgnoreCase(variant) && planId != null) {
            availableScheduleId = toUuid(planId);
        }

        jdbcTemplate.update(
            "UPDATE service_users SET available_calendar_uid = ?, available_schedule_uid = ?, buffer_after = ?, buffer_before = ?, closest_booking_time = ?, furthest_booking_time = ? WHERE service_uid = ? AND user_uid = ?",
            availableCalendarId,
            availableScheduleId,
            user.bufferAfter(),
            user.bufferBefore(),
            user.closestBookingTime(),
            user.furthestBookingTime(),
            toUuid(user.serviceId()),
            toUuid(user.userId())
        );
    }

    @Override
    public Optional<ServiceResource> find(ID serviceId, ID userId) {
        List<ServiceResource> rows = jdbcTemplate.query(
            "SELECT service_uid, user_uid, available_calendar_uid, available_schedule_uid, buffer_after, buffer_before, closest_booking_time, furthest_booking_time FROM service_users WHERE service_uid = ? AND user_uid = ?",
            (rs, rowNum) -> mapServiceUser(rs),
            toUuid(serviceId),
            toUuid(userId)
        );
        return rows.stream().findFirst();
    }

    @Override
    public List<ServiceResource> findByUser(ID userId) {
        return jdbcTemplate.query(
            "SELECT service_uid, user_uid, available_calendar_uid, available_schedule_uid, buffer_after, buffer_before, closest_booking_time, furthest_booking_time FROM service_users WHERE user_uid = ?",
            (rs, rowNum) -> mapServiceUser(rs),
            toUuid(userId)
        );
    }

    @Override
    public void delete(ID serviceId, ID userId) {
        jdbcTemplate.update(
            "DELETE FROM service_users WHERE service_uid = ? AND user_uid = ?",
            toUuid(serviceId),
            toUuid(userId)
        );
    }

    private ServiceResource mapServiceUser(ResultSet rs) throws SQLException {
        UUID calendarId = rs.getObject("available_calendar_uid", UUID.class);
        UUID scheduleId = rs.getObject("available_schedule_uid", UUID.class);
        TimePlan plan;
        if (calendarId != null) {
            plan = new TimePlan("Calendar", toId(calendarId));
        } else if (scheduleId != null) {
            plan = new TimePlan("Schedule", toId(scheduleId));
        } else {
            plan = new TimePlan("Empty", null);
        }

        return new ServiceResource(
            toId(rs.getObject("user_uid", UUID.class)),
            toId(rs.getObject("service_uid", UUID.class)),
            plan,
            rs.getLong("buffer_after"),
            rs.getLong("buffer_before"),
            rs.getLong("closest_booking_time"),
            rs.getObject("furthest_booking_time") == null ? null : rs.getLong("furthest_booking_time")
        );
    }

    private static UUID toUuid(ID id) {
        return UUID.fromString(id.value());
    }

    private static ID toId(UUID id) {
        return new ID(id.toString());
    }
}
