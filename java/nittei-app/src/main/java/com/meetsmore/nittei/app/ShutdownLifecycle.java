package com.meetsmore.nittei.app;

import com.meetsmore.nittei.utils.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class ShutdownLifecycle {

  private static final Logger log = LoggerFactory.getLogger(ShutdownLifecycle.class);

  private final AppConfig appConfig;

  public ShutdownLifecycle(AppConfig appConfig) {
    this.appConfig = appConfig;
  }

  @EventListener
  public void onShutdown(ContextClosedEvent event) {
    long sleepSeconds = appConfig.getServerShutdownSleep();
    if (sleepSeconds <= 0) {
      return;
    }

    log.info("[shutdown_handler] Waiting {}s before stopping", sleepSeconds);
    try {
      Thread.sleep(sleepSeconds * 1000L);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      log.warn("[shutdown_handler] Interrupted while sleeping", ex);
    }
    log.info("[shutdown_handler] api crate - shutdown complete");
  }
}
