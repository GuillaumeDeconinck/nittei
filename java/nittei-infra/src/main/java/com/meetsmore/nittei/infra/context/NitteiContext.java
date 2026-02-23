package com.meetsmore.nittei.infra.context;

import com.meetsmore.nittei.infra.config.InfraConfig;
import com.meetsmore.nittei.infra.repos.Repositories;
import com.meetsmore.nittei.infra.system.SystemClock;

public record NitteiContext(Repositories repos, InfraConfig config, SystemClock sys) {
}
