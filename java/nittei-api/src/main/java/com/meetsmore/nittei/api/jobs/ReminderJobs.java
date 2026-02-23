package com.meetsmore.nittei.api.jobs;

import com.meetsmore.nittei.utils.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ReminderJobs {

    private static final Logger log = LoggerFactory.getLogger(ReminderJobs.class);

    private final AppConfig appConfig;

    public ReminderJobs(AppConfig appConfig) {
        this.appConfig = appConfig;
    }

    @Scheduled(fixedRate = 30 * 60 * 1000L)
    public void generateReminders() {
        if (appConfig.isDisableReminders()) {
            return;
        }
        log.debug("[reminder_generation_job] Triggered placeholder reminder generation");
    }

    @Scheduled(cron = "0 * * * * *")
    public void sendReminders() {
        if (appConfig.isDisableReminders()) {
            return;
        }
        log.debug("[send_reminders_job] Triggered placeholder reminder dispatch");
    }
}
