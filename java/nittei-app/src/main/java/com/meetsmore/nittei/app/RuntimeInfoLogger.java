package com.meetsmore.nittei.app;

import com.meetsmore.nittei.utils.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class RuntimeInfoLogger implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(RuntimeInfoLogger.class);

    private final AppConfig appConfig;

    public RuntimeInfoLogger(AppConfig appConfig) {
        this.appConfig = appConfig;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!appConfig.isPrintRuntimeInfo()) {
            return;
        }

        int cpuCount = Runtime.getRuntime().availableProcessors();
        long maxMemory = Runtime.getRuntime().maxMemory();

        log.info("[print_runtime_info] Java runtime: {}", Runtime.version());
        log.info("[print_runtime_info] Number of CPUs detected: {}", cpuCount);
        log.info("[print_runtime_info] Max heap memory bytes: {}", maxMemory);
    }
}
