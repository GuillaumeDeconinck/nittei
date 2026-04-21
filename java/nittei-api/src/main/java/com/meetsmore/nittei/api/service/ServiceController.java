package com.meetsmore.nittei.api.service;

import com.meetsmore.nittei.api.shared.BaseApiController;
import com.meetsmore.nittei.api.structs.service.ServiceApi;
import com.meetsmore.nittei.api.structs.service.ServiceDtos;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class ServiceController extends BaseApiController {

  private final ServiceManagementService managementService;
  private final ServiceBookingService bookingService;

  public ServiceController(
      ServiceManagementService managementService, ServiceBookingService bookingService) {
    this.managementService = managementService;
    this.bookingService = bookingService;
  }

  @PostMapping("/service")
  public ResponseEntity<ServiceApi.ServiceResponse> createService(
      @RequestHeader HttpHeaders headers,
      @Valid @RequestBody ServiceApi.CreateServiceRequestBody body) {
    return managementService.createService(headers, body);
  }

  @GetMapping("/service/meta")
  public ResponseEntity<ServiceApi.GetServicesByMetaAPIResponse> getServicesByMeta(
      @RequestHeader HttpHeaders headers,
      @Valid @ModelAttribute ServiceApi.GetServicesByMetaQueryParams query) {
    return managementService.getServicesByMeta(headers, query);
  }

  @GetMapping("/service/{serviceId}")
  public ResponseEntity<ServiceDtos.ServiceWithUsersDTO> getService(
      @RequestHeader HttpHeaders headers, @PathVariable String serviceId) {
    return managementService.getService(headers, serviceId);
  }

  @PutMapping("/service/{serviceId}")
  public ResponseEntity<ServiceApi.ServiceResponse> updateService(
      @RequestHeader HttpHeaders headers,
      @PathVariable String serviceId,
      @Valid @RequestBody ServiceApi.UpdateServiceRequestBody body) {
    return managementService.updateService(headers, serviceId, body);
  }

  @DeleteMapping("/service/{serviceId}")
  public ResponseEntity<ServiceApi.ServiceResponse> deleteService(
      @RequestHeader HttpHeaders headers, @PathVariable String serviceId) {
    return managementService.deleteService(headers, serviceId);
  }

  @PostMapping("/service/{serviceId}/users")
  public ResponseEntity<ServiceApi.ServiceResourceResponse> addUserToService(
      @RequestHeader HttpHeaders headers,
      @PathVariable String serviceId,
      @Valid @RequestBody ServiceApi.AddUserToServiceRequestBody body) {
    return managementService.addUserToService(headers, serviceId, body);
  }

  @DeleteMapping("/service/{serviceId}/users/{userId}")
  public ResponseEntity<ServiceApi.ServiceResourceResponse> removeUserFromService(
      @RequestHeader HttpHeaders headers,
      @PathVariable String serviceId,
      @PathVariable String userId) {
    return managementService.removeUserFromService(headers, serviceId, userId);
  }

  @PutMapping("/service/{serviceId}/users/{userId}")
  public ResponseEntity<ServiceApi.ServiceResourceResponse> updateServiceUser(
      @RequestHeader HttpHeaders headers,
      @PathVariable String serviceId,
      @PathVariable String userId,
      @Valid @RequestBody ServiceApi.UpdateServiceUserRequestBody body) {
    return managementService.updateServiceUser(headers, serviceId, userId, body);
  }

  @PutMapping("/service/{serviceId}/users/{userId}/busy")
  public ResponseEntity<ServiceApi.ServiceResourceResponse> addBusyCalendar(
      @RequestHeader HttpHeaders headers,
      @PathVariable String serviceId,
      @PathVariable String userId,
      @Valid @RequestBody ServiceApi.AddBusyCalendarRequestBody body) {
    return managementService.addBusyCalendar(headers, serviceId, userId, body);
  }

  @DeleteMapping("/service/{serviceId}/users/{userId}/busy")
  public ResponseEntity<ServiceApi.ServiceResourceResponse> removeBusyCalendar(
      @RequestHeader HttpHeaders headers,
      @PathVariable String serviceId,
      @PathVariable String userId,
      @Valid @RequestBody ServiceApi.RemoveBusyCalendarRequestBody body) {
    return managementService.removeBusyCalendar(headers, serviceId, userId, body);
  }

  @GetMapping("/service/{serviceId}/booking")
  public ResponseEntity<ServiceApi.GetServiceBookingSlotsAPIResponse> getServiceBookingSlots(
      @RequestHeader HttpHeaders headers,
      @PathVariable String serviceId,
      @Valid @ModelAttribute ServiceApi.GetServiceBookingSlotsQueryParams query) {
    return bookingService.getServiceBookingSlots(headers, serviceId, query);
  }

  @PostMapping("/service/{serviceId}/booking-intend")
  public ResponseEntity<ServiceApi.CreateServiceEventIntendAPIResponse> createServiceEventIntend(
      @RequestHeader HttpHeaders headers,
      @PathVariable String serviceId,
      @Valid @RequestBody ServiceApi.CreateServiceEventIntendRequestBody body) {
    return bookingService.createServiceEventIntend(headers, serviceId, body);
  }

  @DeleteMapping("/service/{serviceId}/booking-intend")
  public ResponseEntity<ServiceApi.RemoveServiceEventIntendAPIResponse> removeServiceEventIntend(
      @RequestHeader HttpHeaders headers,
      @PathVariable String serviceId,
      @Valid @ModelAttribute ServiceApi.RemoveServiceEventIntendQueryParams query) {
    return bookingService.removeServiceEventIntend(headers, serviceId, query);
  }
}
