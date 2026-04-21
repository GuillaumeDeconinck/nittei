package com.meetsmore.nittei.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class DefaultAccountInitializer implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(DefaultAccountInitializer.class);

  @Override
  public void run(ApplicationArguments args) {
    // Placeholder only: mirrors Rust startup flow where a default account is created if missing.
    log.info("[init_default_account] Placeholder initializer executed");
  }
}
