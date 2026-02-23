package com.meetsmore.nittei.api.structs.service;

import com.meetsmore.nittei.api.structs.user.UserDtos;
import com.meetsmore.nittei.domain.BusyCalendarProvider;
import com.meetsmore.nittei.domain.ID;
import com.meetsmore.nittei.domain.Service;
import com.meetsmore.nittei.domain.ServiceMultiPersonOptions;
import com.meetsmore.nittei.domain.ServiceResource;
import com.meetsmore.nittei.domain.ServiceWithUsers;
import com.meetsmore.nittei.domain.TimePlan;
import com.meetsmore.nittei.domain.User;
import com.meetsmore.nittei.domain.booking.ServiceBookingSlot;
import com.meetsmore.nittei.domain.booking.ServiceBookingSlots;
import com.meetsmore.nittei.domain.booking.ServiceBookingSlotsDate;
import java.time.Instant;
import java.util.List;

public final class ServiceApi {

    private ServiceApi() {
    }

    public record ServiceResponse(ServiceDtos.ServiceDTO service) {
        public static ServiceResponse from(Service value) {
            return new ServiceResponse(ServiceDtos.ServiceDTO.from(value));
        }
    }

    public record ServiceWithUsersResponse(ServiceDtos.ServiceWithUsersDTO service) {
        public static ServiceWithUsersResponse from(ServiceWithUsers value) {
            return new ServiceWithUsersResponse(ServiceDtos.ServiceWithUsersDTO.from(value));
        }
    }

    public record ServiceResourceResponse(ServiceDtos.ServiceResourceDTO user) {
        public static ServiceResourceResponse from(ServiceResource value) {
            return new ServiceResourceResponse(ServiceDtos.ServiceResourceDTO.from(value));
        }
    }

    public record ServicePathParams(ID serviceId) {
    }

    public record ServiceUserPathParams(ID serviceId, ID userId) {
    }

    public record AddUserToServiceRequestBody(
        ID userId,
        TimePlan availability,
        Long bufferAfter,
        Long bufferBefore,
        Long closestBookingTime,
        Long furthestBookingTime
    ) {
    }

    public record AddBusyCalendarRequestBody(BusyCalendarProvider busy) {
    }

    public record RemoveBusyCalendarRequestBody(BusyCalendarProvider busy) {
    }

    public record RemoveServiceEventIntendQueryParams(Instant timestamp) {
    }

    public record RemoveServiceEventIntendAPIResponse(String message) {
        public static RemoveServiceEventIntendAPIResponse defaultValue() {
            return new RemoveServiceEventIntendAPIResponse("Deleted Booking Intend");
        }
    }

    public record CreateServiceEventIntendRequestBody(List<ID> hostUserIds, Instant timestamp, long duration, long interval) {
    }

    public record CreateServiceEventIntendAPIResponse(List<UserDtos.UserDTO> selectedHosts, boolean createEventForHosts) {
        public static CreateServiceEventIntendAPIResponse from(List<User> selectedHosts, boolean createEventForHosts) {
            return new CreateServiceEventIntendAPIResponse(
                selectedHosts.stream().map(UserDtos.UserDTO::from).toList(),
                createEventForHosts
            );
        }
    }

    public record CreateServiceRequestBody(Object metadata, ServiceMultiPersonOptions multiPerson) {
    }

    public record UpdateServiceRequestBody(Object metadata, ServiceMultiPersonOptions multiPerson) {
    }

    public record GetServiceBookingSlotsQueryParams(
        String timezone,
        long duration,
        long interval,
        String startDate,
        String endDate,
        String hostUserIds
    ) {
    }

    public record ServiceBookingSlotDTO(Instant start, long duration, List<ID> userIds) {
        public static ServiceBookingSlotDTO from(ServiceBookingSlot slot) {
            return new ServiceBookingSlotDTO(slot.start(), slot.duration(), slot.userIds());
        }
    }

    public record ServiceBookingSlotsDateDTO(String date, List<ServiceBookingSlotDTO> slots) {
        public static ServiceBookingSlotsDateDTO from(ServiceBookingSlotsDate value) {
            return new ServiceBookingSlotsDateDTO(
                value.date(),
                value.slots().stream().map(ServiceBookingSlotDTO::from).toList()
            );
        }
    }

    public record GetServiceBookingSlotsAPIResponse(List<ServiceBookingSlotsDateDTO> dates) {
        public static GetServiceBookingSlotsAPIResponse from(ServiceBookingSlots slots) {
            return new GetServiceBookingSlotsAPIResponse(
                slots.dates().stream().map(ServiceBookingSlotsDateDTO::from).toList()
            );
        }
    }

    public record GetServicesByMetaQueryParams(String key, String value, Integer skip, Integer limit) {
    }

    public record GetServicesByMetaAPIResponse(List<ServiceDtos.ServiceDTO> services) {
    }

    public record UpdateServiceUserRequestBody(
        TimePlan availability,
        Long bufferAfter,
        Long bufferBefore,
        Long closestBookingTime,
        Long furthestBookingTime
    ) {
    }
}
