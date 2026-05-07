package com.meetsmore.nittei.api.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.meetsmore.nittei.api.config.RequestAuthenticationPrincipal.AuthenticationScheme;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class RequestAuthenticationFilterTest {

  private final RequestAuthenticationFilter filter = new RequestAuthenticationFilter();

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void authenticatesAdminRequestsFromApiKeyHeader() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("x-api-key", "secret-key");

    filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

    var authentication = SecurityContextHolder.getContext().getAuthentication();
    assertNotNull(authentication);
    assertTrue(authentication.getPrincipal() instanceof RequestAuthenticationPrincipal);
    RequestAuthenticationPrincipal principal =
        (RequestAuthenticationPrincipal) authentication.getPrincipal();
    assertEquals(AuthenticationScheme.API_KEY, principal.scheme());
    assertEquals("secret-key", principal.credential());
    assertNull(principal.accountIdHeader());
    assertTrue(
        authentication.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
  }

  @Test
  void authenticatesUserRequestsFromBearerAuthorizationHeader() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer signed-token");

    filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

    var authentication = SecurityContextHolder.getContext().getAuthentication();
    assertNotNull(authentication);
    assertTrue(authentication.getPrincipal() instanceof RequestAuthenticationPrincipal);
    RequestAuthenticationPrincipal principal =
        (RequestAuthenticationPrincipal) authentication.getPrincipal();
    assertEquals(AuthenticationScheme.BEARER_TOKEN, principal.scheme());
    assertEquals("signed-token", principal.credential());
    assertNull(principal.accountIdHeader());
    assertTrue(
        authentication.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
  }

  @Test
  void authenticatesPublicAccountRequestsFromUuidAccountHeader() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("nittei-account", "75fc2370-8e0a-4d97-98ff-b895ee2f1dca");

    filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

    var authentication = SecurityContextHolder.getContext().getAuthentication();
    assertNotNull(authentication);
    assertTrue(authentication.getPrincipal() instanceof RequestAuthenticationPrincipal);
    RequestAuthenticationPrincipal principal =
        (RequestAuthenticationPrincipal) authentication.getPrincipal();
    assertEquals(AuthenticationScheme.PUBLIC_ACCOUNT, principal.scheme());
    assertEquals("75fc2370-8e0a-4d97-98ff-b895ee2f1dca", principal.credential());
    assertEquals("75fc2370-8e0a-4d97-98ff-b895ee2f1dca", principal.accountIdHeader());
  }

  @Test
  void doesNotAuthenticateMalformedAccountHeader() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("nittei-account", "not-a-uuid");

    filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

    assertNull(SecurityContextHolder.getContext().getAuthentication());
  }

  @Test
  void doesNotAuthenticateNonBearerAuthorizationHeaders() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Basic abc123");

    filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

    assertNull(SecurityContextHolder.getContext().getAuthentication());
  }

  @Test
  void doesNotAuthenticateBlankBearerToken() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer   ");

    filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

    assertNull(SecurityContextHolder.getContext().getAuthentication());
  }

  @Test
  void existingAuthenticationIsNotOverridden() throws Exception {
    UsernamePasswordAuthenticationToken existingAuthentication =
        new UsernamePasswordAuthenticationToken("existing-user", "N/A");
    SecurityContextHolder.getContext().setAuthentication(existingAuthentication);

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("x-api-key", "secret-key");

    filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

    assertEquals(existingAuthentication, SecurityContextHolder.getContext().getAuthentication());
  }

  @Test
  void bearerAuthorizationHelperOnlyAcceptsNonBlankBearerTokens() {
    assertTrue(RequestAuthenticationFilter.isBearerAuthorization("Bearer token"));
    assertTrue(RequestAuthenticationFilter.isBearerAuthorization("bearer token"));
    assertFalse(RequestAuthenticationFilter.isBearerAuthorization("Basic token"));
    assertFalse(RequestAuthenticationFilter.isBearerAuthorization("Bearer   "));
    assertFalse(RequestAuthenticationFilter.isBearerAuthorization("token"));
    assertFalse(RequestAuthenticationFilter.isBearerAuthorization(null));
  }

  @Test
  void accountHeaderHelperOnlyAcceptsUuidValues() {
    assertTrue(RequestAuthenticationFilter.isAccountHeader("75fc2370-8e0a-4d97-98ff-b895ee2f1dca"));
    assertTrue(
        RequestAuthenticationFilter.isAccountHeader(" 75fc2370-8e0a-4d97-98ff-b895ee2f1dca "));
    assertFalse(RequestAuthenticationFilter.isAccountHeader("not-a-uuid"));
    assertFalse(RequestAuthenticationFilter.isAccountHeader(" "));
    assertFalse(RequestAuthenticationFilter.isAccountHeader(null));
  }
}
