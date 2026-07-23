package com.meetsmore.nittei.app;

import java.lang.Thread.UncaughtExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.meetsmore.nittei")
public class NitteiJavaApplication {

  private static final Logger log = LoggerFactory.getLogger(NitteiJavaApplication.class);

  public static void main(String[] args) {
    installCustomPanicHook();
    SpringApplication.run(NitteiJavaApplication.class, args);
  }

  private static void installCustomPanicHook() {
    UncaughtExceptionHandler currentHandler = Thread.getDefaultUncaughtExceptionHandler();
    Thread.setDefaultUncaughtExceptionHandler(
        (thread, throwable) -> {
          log.error("Application panic occurred on thread {}", thread.getName(), throwable);
          if (currentHandler != null) {
            currentHandler.uncaughtException(thread, throwable);
          }
        });
  }
}
