package com.meetsmore.nittei.infra;

import com.meetsmore.nittei.infra.context.InfraSetupService;
import com.meetsmore.nittei.infra.context.NitteiContext;
import com.meetsmore.nittei.utils.config.AppConfig;
import org.springframework.stereotype.Component;

@Component
public class InfraFacade {

  private final InfraSetupService setupService;
  private final AppConfig appConfig;

  public InfraFacade(InfraSetupService setupService, AppConfig appConfig) {
    this.setupService = setupService;
    this.appConfig = appConfig;
  }

  public NitteiContext setupContext() {
    return setupService.setupContext();
  }

  public void runMigration() {
    setupService.runMigration(appConfig);
  }
}
