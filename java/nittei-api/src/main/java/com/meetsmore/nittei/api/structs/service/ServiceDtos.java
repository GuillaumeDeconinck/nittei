package com.meetsmore.nittei.api.structs.service;

import com.meetsmore.nittei.domain.ID;
import com.meetsmore.nittei.domain.Service;
import com.meetsmore.nittei.domain.ServiceResource;
import com.meetsmore.nittei.domain.ServiceWithUsers;
import com.meetsmore.nittei.domain.TimePlan;
import java.util.List;

public final class ServiceDtos {

  private ServiceDtos() {}

  public record ServiceResourceDTO(
      ID userId,
      ID serviceId,
      TimePlan availability,
      long bufferAfter,
      long bufferBefore,
      long closestBookingTime,
      Long furthestBookingTime) {
    public static ServiceResourceDTO from(ServiceResource value) {
      return new ServiceResourceDTO(
          value.userId(),
          value.serviceId(),
          value.availability(),
          value.bufferAfter(),
          value.bufferBefore(),
          value.closestBookingTime(),
          value.furthestBookingTime());
    }
  }

  public record ServiceDTO(ID id, Object metadata) {
    public static ServiceDTO from(Service service) {
      return new ServiceDTO(service.id(), service.metadata());
    }
  }

  public record ServiceWithUsersDTO(ID id, List<ServiceResourceDTO> users, Object metadata) {
    public static ServiceWithUsersDTO from(ServiceWithUsers service) {
      return new ServiceWithUsersDTO(
          service.id(),
          service.users().stream().map(ServiceResourceDTO::from).toList(),
          service.metadata());
    }
  }
}
