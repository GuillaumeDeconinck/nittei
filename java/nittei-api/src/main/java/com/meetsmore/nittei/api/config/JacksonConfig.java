package com.meetsmore.nittei.api.config;

import com.fasterxml.jackson.databind.MapperFeature;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer caseInsensitiveEnumsCustomizer() {
        return builder -> builder.featuresToEnable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS);
    }
}
