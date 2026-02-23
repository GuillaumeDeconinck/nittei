package com.meetsmore.nittei.infra.metrics;

import com.zaxxer.hikari.HikariDataSource;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class InfraMetrics {

    public InfraMetrics(MeterRegistry registry, HikariDataSource dataSource) {
        Gauge.builder("db_connection_pool_total", dataSource, ds -> ds.getHikariPoolMXBean() == null ? 0 : ds.getHikariPoolMXBean().getTotalConnections())
            .description("Total number of connections in the pool")
            .register(registry);

        Gauge.builder("db_connection_pool_idle", dataSource, ds -> ds.getHikariPoolMXBean() == null ? 0 : ds.getHikariPoolMXBean().getIdleConnections())
            .description("Number of idle connections in the pool")
            .register(registry);

        Gauge.builder("db_connection_pool_busy", dataSource, ds -> {
                if (ds.getHikariPoolMXBean() == null) {
                    return 0;
                }
                return ds.getHikariPoolMXBean().getActiveConnections();
            })
            .description("Number of busy connections in the pool")
            .register(registry);
    }
}
