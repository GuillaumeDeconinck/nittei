package com.meetsmore.nittei.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

  private final RequestAuthenticationFilter requestAuthenticationFilter;
  private final JsonAuthenticationEntryPoint authenticationEntryPoint;
  private final JsonAccessDeniedHandler accessDeniedHandler;

  public SecurityConfig(
      RequestAuthenticationFilter requestAuthenticationFilter,
      JsonAuthenticationEntryPoint authenticationEntryPoint,
      JsonAccessDeniedHandler accessDeniedHandler) {
    this.requestAuthenticationFilter = requestAuthenticationFilter;
    this.authenticationEntryPoint = authenticationEntryPoint;
    this.accessDeniedHandler = accessDeniedHandler;
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable)
        .cors(Customizer.withDefaults())
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .formLogin(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable)
        .logout(AbstractHttpConfigurer::disable)
        .exceptionHandling(
            exceptions ->
                exceptions
                    .authenticationEntryPoint(authenticationEntryPoint)
                    .accessDeniedHandler(accessDeniedHandler))
        .addFilterBefore(requestAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(HttpMethod.POST, "/api/v1/account")
                    .permitAll()
                    .requestMatchers("/api/v1/healthcheck", "/api/v1/metrics")
                    .permitAll()
                    .requestMatchers("/actuator/**", "/v3/api-docs/**", "/swagger-ui/**")
                    .permitAll()
                    .requestMatchers("/api/v1/**")
                    .authenticated()
                    .anyRequest()
                    .denyAll());

    return http.build();
  }
}
