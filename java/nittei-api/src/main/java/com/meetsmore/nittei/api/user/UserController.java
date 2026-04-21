package com.meetsmore.nittei.api.user;

import com.meetsmore.nittei.api.shared.BaseApiController;
import com.meetsmore.nittei.api.structs.calendar.CalendarApi;
import com.meetsmore.nittei.api.structs.user.UserApi;
import com.meetsmore.nittei.domain.EventInstance;
import com.meetsmore.nittei.domain.ID;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class UserController extends BaseApiController {

  private final UserApplicationService userService;

  public UserController(UserApplicationService userService) {
    this.userService = userService;
  }

  @PostMapping("/user")
  public ResponseEntity<UserApi.UserResponse> createUser(
      @RequestHeader HttpHeaders headers, @Valid @RequestBody UserApi.CreateUserRequestBody body) {
    return userService.createUser(headers, body);
  }

  @GetMapping("/user/meta")
  public ResponseEntity<UserApi.GetUsersByMetaAPIResponse> getUsersByMeta(
      @RequestHeader HttpHeaders headers,
      @Valid @ModelAttribute UserApi.GetUsersByMetaQueryParams query) {
    return userService.getUsersByMeta(headers, query);
  }

  @GetMapping("/user/{userId}")
  public ResponseEntity<UserApi.UserResponse> getUser(
      @RequestHeader HttpHeaders headers, @PathVariable String userId) {
    return userService.getUser(headers, userId);
  }

  @GetMapping("/user/external_id/{externalId}")
  public ResponseEntity<UserApi.UserResponse> getUserByExternalId(
      @RequestHeader HttpHeaders headers, @PathVariable String externalId) {
    return userService.getUserByExternalId(headers, externalId);
  }

  @PutMapping("/user/{userId}")
  public ResponseEntity<UserApi.UserResponse> updateUser(
      @RequestHeader HttpHeaders headers,
      @PathVariable String userId,
      @Valid @RequestBody UserApi.UpdateUserRequestBody body) {
    return userService.updateUser(headers, userId, body);
  }

  @DeleteMapping("/user/{userId}")
  public ResponseEntity<UserApi.UserResponse> deleteUser(
      @RequestHeader HttpHeaders headers, @PathVariable String userId) {
    return userService.deleteUser(headers, userId);
  }

  @PostMapping("/user/{userId}/oauth")
  public ResponseEntity<UserApi.UserResponse> oauthIntegrationAdmin(
      @RequestHeader HttpHeaders headers,
      @PathVariable String userId,
      @Valid @RequestBody UserApi.OAuthIntegrationRequestBody body) {
    return userService.oauthIntegrationAdmin(headers, userId, body);
  }

  @DeleteMapping("/user/{userId}/oauth/{provider}")
  public ResponseEntity<UserApi.UserResponse> removeIntegrationAdmin(
      @RequestHeader HttpHeaders headers,
      @PathVariable String userId,
      @PathVariable String provider) {
    return userService.removeIntegrationAdmin(headers, userId, provider);
  }

  @GetMapping("/me")
  public ResponseEntity<UserApi.UserResponse> getMe(@RequestHeader HttpHeaders headers) {
    return userService.getMe(headers);
  }

  @PostMapping("/me/oauth")
  public ResponseEntity<UserApi.UserResponse> oauthIntegration(
      @RequestHeader HttpHeaders headers,
      @Valid @RequestBody UserApi.OAuthIntegrationRequestBody body) {
    return userService.oauthIntegration(headers, body);
  }

  @DeleteMapping("/me/oauth/{provider}")
  public ResponseEntity<UserApi.UserResponse> removeIntegration(
      @RequestHeader HttpHeaders headers, @PathVariable String provider) {
    return userService.removeIntegration(headers, provider);
  }

  @GetMapping("/user/{userId}/freebusy")
  public ResponseEntity<CalendarApi.GetUserFreeBusyAPIResponse> getUserFreebusy(
      @RequestHeader HttpHeaders headers,
      @PathVariable String userId,
      @Valid @ModelAttribute CalendarApi.GetUserFreeBusyQueryParams query,
      @RequestParam(name = "calendarIds", required = false) String calendarIdsRaw) {
    return userService.getUserFreebusy(headers, userId, query, calendarIdsRaw);
  }

  @PostMapping("/user/freebusy")
  public ResponseEntity<Map<ID, List<EventInstance>>> getMultipleUsersFreebusy(
      @RequestHeader HttpHeaders headers,
      @Valid @RequestBody CalendarApi.MultipleFreeBusyRequestBody body) {
    return userService.getMultipleUsersFreebusy(headers, body);
  }
}
