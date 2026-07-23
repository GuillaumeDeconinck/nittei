package com.meetsmore.nittei.api.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.meetsmore.nittei.api.config.HttpLoggingFilter;
import com.meetsmore.nittei.api.config.JsonAccessDeniedHandler;
import com.meetsmore.nittei.api.config.JsonAuthenticationEntryPoint;
import com.meetsmore.nittei.api.config.RequestAuthenticationFilter;
import com.meetsmore.nittei.api.config.SecurityConfig;
import com.meetsmore.nittei.api.error.ApiExceptionHandler;
import com.meetsmore.nittei.api.error.NitteiApiException;
import com.meetsmore.nittei.api.error.NitteiErrorCode;
import com.meetsmore.nittei.api.status.StatusController;
import com.meetsmore.nittei.utils.config.AppConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest(
    classes = ApiIntegrationTest.TestApplication.class,
    properties = {
      "spring.autoconfigure.exclude="
          + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
          + "org.springframework.boot.actuate.autoconfigure.jdbc.DataSourceHealthContributorAutoConfiguration"
    })
@AutoConfigureMockMvc
class ApiIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void healthcheckIsPublicAndEchoesRequestId() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/healthcheck").header(HttpLoggingFilter.REQUEST_ID_HEADER, "req-health"))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpLoggingFilter.REQUEST_ID_HEADER, "req-health"))
        .andExpect(jsonPath("$.message").value("Ok!"));
  }

  @Test
  void protectedEndpointRejectsRequestsWithoutCredentials() throws Exception {
    mockMvc
        .perform(get("/api/v1/test/protected"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status").value(401))
        .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
        .andExpect(
            jsonPath("$.message").value("Authentication is required to access this resource"))
        .andExpect(jsonPath("$.path").value("/api/v1/test/protected"))
        .andExpect(jsonPath("$.requestId").isNotEmpty())
        .andExpect(jsonPath("$.timestamp").exists());
  }

  @Test
  void protectedEndpointAllowsRequestsWithRecognizedCredentials() throws Exception {
    mockMvc
        .perform(get("/api/v1/test/protected").header("x-api-key", "test-key"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("protected"));
  }

  @Test
  void protectedEndpointAllowsBearerAuthorizationHeaders() throws Exception {
    mockMvc
        .perform(get("/api/v1/test/protected").header(HttpHeaders.AUTHORIZATION, "Bearer token"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("protected"));
  }

  @Test
  void protectedEndpointRejectsMalformedAuthorizationHeaders() throws Exception {
    mockMvc
        .perform(get("/api/v1/test/protected").header(HttpHeaders.AUTHORIZATION, "Basic token"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
  }

  @Test
  void nitteiApiExceptionsReturnStructuredJsonErrorPayload() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/test/nittei-error")
                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                .header(HttpLoggingFilter.REQUEST_ID_HEADER, "req-known"))
        .andExpect(status().isBadRequest())
        .andExpect(header().string(HttpLoggingFilter.REQUEST_ID_HEADER, "req-known"))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.code").value("BAD_CLIENT_DATA"))
        .andExpect(jsonPath("$.message").value("Known test failure"))
        .andExpect(jsonPath("$.path").value("/api/v1/test/nittei-error"))
        .andExpect(jsonPath("$.requestId").value("req-known"))
        .andExpect(jsonPath("$.timestamp").exists());
  }

  @Test
  void unexpectedExceptionsReturnInternalErrorPayload() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/test/unexpected-error")
                .header("x-api-key", "test-key")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isInternalServerError())
        .andExpect(header().exists(HttpLoggingFilter.REQUEST_ID_HEADER))
        .andExpect(jsonPath("$.status").value(500))
        .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
        .andExpect(jsonPath("$.message").value("Internal server error"))
        .andExpect(jsonPath("$.path").value("/api/v1/test/unexpected-error"))
        .andExpect(jsonPath("$.requestId").isNotEmpty())
        .andExpect(jsonPath("$.timestamp").exists());
  }

  @SpringBootApplication
  @Import({
    SecurityConfig.class,
    RequestAuthenticationFilter.class,
    JsonAuthenticationEntryPoint.class,
    JsonAccessDeniedHandler.class,
    HttpLoggingFilter.class,
    ApiExceptionHandler.class,
    StatusController.class,
    TestController.class,
  })
  static class TestApplication {

    @Bean
    AppConfig appConfig() {
      return new AppConfig();
    }
  }

  @RestController
  @RequestMapping("/api/v1/test")
  static class TestController {

    @GetMapping("/protected")
    public java.util.Map<String, String> protectedEndpoint() {
      return java.util.Map.of("message", "protected");
    }

    @GetMapping("/nittei-error")
    public void knownError() {
      throw new NitteiApiException(NitteiErrorCode.BAD_CLIENT_DATA, "Known test failure");
    }

    @GetMapping("/unexpected-error")
    public void unexpectedError() {
      throw new IllegalStateException("Unexpected test failure");
    }
  }
}
