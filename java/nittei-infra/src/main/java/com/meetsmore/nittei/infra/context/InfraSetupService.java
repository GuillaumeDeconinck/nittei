package com.meetsmore.nittei.infra.context;

import com.meetsmore.nittei.utils.config.AppConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.springframework.stereotype.Service;

@Service
public class InfraSetupService {

    private final NitteiContext context;

    public InfraSetupService(NitteiContext context) {
        this.context = context;
    }

    public NitteiContext setupContext() {
        return context;
    }

    public void runMigration(AppConfig appConfig) {
        HikariConfig hikari = new HikariConfig();
        String url = appConfig.getPg().getDatabaseUrl();
        if (url.startsWith("postgresql://")) {
            url = "jdbc:" + url;
        }
        hikari.setJdbcUrl(url);
        hikari.setMaximumPoolSize(5);

        try (HikariDataSource dataSource = new HikariDataSource(hikari)) {
            Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load()
                .migrate();
        }
    }
}
