package com.meetsmore.nittei.infra.repos;

public record Repositories(
    AccountRepository accounts,
    AccountIntegrationRepository accountIntegrations,
    CalendarRepository calendars,
    CalendarSyncedRepository calendarSynced,
    EventRepository events,
    EventReminderGenerationJobsRepository eventRemindersGenerationJobs,
    EventSyncedRepository eventSynced,
    ScheduleRepository schedules,
    ReminderRepository reminders,
    ReservationRepository reservations,
    ServiceRepository services,
    ServiceUserRepository serviceUsers,
    ServiceUserBusyCalendarRepository serviceUserBusyCalendars,
    StatusRepository status,
    UserRepository users,
    UserIntegrationRepository userIntegrations) {}
