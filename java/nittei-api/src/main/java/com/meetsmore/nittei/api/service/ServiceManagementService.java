package com.meetsmore.nittei.api.service;

import com.meetsmore.nittei.api.error.NitteiApiException;
import com.meetsmore.nittei.api.error.NitteiErrorCode;
import com.meetsmore.nittei.api.shared.auth.RequestContextResolver;
import com.meetsmore.nittei.api.structs.service.ServiceApi;
import com.meetsmore.nittei.api.structs.service.ServiceDtos;
import com.meetsmore.nittei.domain.ID;
import com.meetsmore.nittei.domain.IntegrationProvider;
import com.meetsmore.nittei.domain.Metadata;
import com.meetsmore.nittei.domain.Service;
import com.meetsmore.nittei.domain.ServiceResource;
import com.meetsmore.nittei.domain.ServiceWithUsers;
import com.meetsmore.nittei.domain.TimePlan;
import com.meetsmore.nittei.infra.context.NitteiContext;
import com.meetsmore.nittei.infra.repos.BusyCalendarIdentifier;
import com.meetsmore.nittei.infra.repos.ExternalBusyCalendarIdentifier;
import com.meetsmore.nittei.infra.repos.query.MetadataFindQuery;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

@org.springframework.stereotype.Service
public class ServiceManagementService {

    private final NitteiContext ctx;
    private final RequestContextResolver auth;

    public ServiceManagementService(NitteiContext ctx, RequestContextResolver auth) {
        this.ctx = ctx;
        this.auth = auth;
    }

    public ResponseEntity<ServiceApi.ServiceResponse> createService(
        HttpHeaders headers,
        ServiceApi.CreateServiceRequestBody body
    ) {
        var account = auth.requireAdminAccount(headers, ctx);
        Service service = new Service(
            ID.random(),
            account.id(),
            body == null ? null : body.multiPerson(),
            body == null ? null : body.metadata()
        );
        ctx.repos().services().insert(service);
        return ResponseEntity.status(201).body(ServiceApi.ServiceResponse.from(service));
    }

    public ResponseEntity<ServiceApi.GetServicesByMetaAPIResponse> getServicesByMeta(
        HttpHeaders headers,
        ServiceApi.GetServicesByMetaQueryParams query
    ) {
        var account = auth.requireAdminAccount(headers, ctx);
        var services = ctx.repos().services().findByMetadata(
            new MetadataFindQuery(
                Metadata.of(query.key(), query.value()),
                query.skip() == null ? 0 : query.skip(),
                query.limit() == null ? 20 : query.limit(),
                account.id()
            )
        ).stream().map(ServiceDtos.ServiceDTO::from).toList();
        return ResponseEntity.ok(new ServiceApi.GetServicesByMetaAPIResponse(services));
    }

    public ResponseEntity<ServiceDtos.ServiceWithUsersDTO> getService(
        HttpHeaders headers,
        String serviceId
    ) {
        var account = auth.requireAdminAccount(headers, ctx);
        ID sid = auth.parseId(serviceId, "Malformed serviceId");
        auth.requireAccountService(account, sid, ctx);
        ServiceWithUsers service = ctx.repos().services().findWithUsers(sid)
            .orElseThrow(() -> new NitteiApiException(NitteiErrorCode.NOT_FOUND, "Service not found"));
        return ResponseEntity.ok(ServiceDtos.ServiceWithUsersDTO.from(service));
    }

    public ResponseEntity<ServiceApi.ServiceResponse> updateService(
        HttpHeaders headers,
        String serviceId,
        ServiceApi.UpdateServiceRequestBody body
    ) {
        var account = auth.requireAdminAccount(headers, ctx);
        Service current = auth.requireAccountService(account, auth.parseId(serviceId, "Malformed serviceId"), ctx);
        Service updated = new Service(
            current.id(),
            current.accountId(),
            body != null && body.multiPerson() != null ? body.multiPerson() : current.multiPerson(),
            body != null && body.metadata() != null ? body.metadata() : current.metadata()
        );
        ctx.repos().services().save(updated);
        return ResponseEntity.ok(ServiceApi.ServiceResponse.from(updated));
    }

    public ResponseEntity<ServiceApi.ServiceResponse> deleteService(
        HttpHeaders headers,
        String serviceId
    ) {
        var account = auth.requireAdminAccount(headers, ctx);
        ID sid = auth.parseId(serviceId, "Malformed serviceId");
        Service current = auth.requireAccountService(account, sid, ctx);
        ctx.repos().events().deleteByService(sid);
        ctx.repos().services().delete(sid);
        return ResponseEntity.ok(ServiceApi.ServiceResponse.from(current));
    }

    public ResponseEntity<ServiceApi.ServiceResourceResponse> addUserToService(
        HttpHeaders headers,
        String serviceId,
        ServiceApi.AddUserToServiceRequestBody body
    ) {
        var account = auth.requireAdminAccount(headers, ctx);
        ID sid = auth.parseId(serviceId, "Malformed serviceId");
        auth.requireAccountService(account, sid, ctx);
        var user = auth.requireAccountUser(account, body.userId(), ctx);
        ServiceResource resource = new ServiceResource(
            user.id(),
            sid,
            body.availability() == null ? new TimePlan("Empty", null) : body.availability(),
            body.bufferAfter() == null ? 0 : body.bufferAfter(),
            body.bufferBefore() == null ? 0 : body.bufferBefore(),
            body.closestBookingTime() == null ? 0 : body.closestBookingTime(),
            body.furthestBookingTime()
        );
        ctx.repos().serviceUsers().insert(resource);
        return ResponseEntity.ok(ServiceApi.ServiceResourceResponse.from(resource));
    }

    public ResponseEntity<ServiceApi.ServiceResourceResponse> removeUserFromService(
        HttpHeaders headers,
        String serviceId,
        String userId
    ) {
        var account = auth.requireAdminAccount(headers, ctx);
        ID sid = auth.parseId(serviceId, "Malformed serviceId");
        auth.requireAccountService(account, sid, ctx);
        ID uid = auth.parseId(userId, "Malformed userId");
        ServiceResource current = ctx.repos().serviceUsers().find(sid, uid)
            .orElseThrow(() -> new NitteiApiException(NitteiErrorCode.NOT_FOUND, "Service user not found"));
        ctx.repos().serviceUsers().delete(sid, uid);
        return ResponseEntity.ok(ServiceApi.ServiceResourceResponse.from(current));
    }

    public ResponseEntity<ServiceApi.ServiceResourceResponse> updateServiceUser(
        HttpHeaders headers,
        String serviceId,
        String userId,
        ServiceApi.UpdateServiceUserRequestBody body
    ) {
        var account = auth.requireAdminAccount(headers, ctx);
        ID sid = auth.parseId(serviceId, "Malformed serviceId");
        auth.requireAccountService(account, sid, ctx);
        ID uid = auth.parseId(userId, "Malformed userId");
        ServiceResource current = ctx.repos().serviceUsers().find(sid, uid)
            .orElseThrow(() -> new NitteiApiException(NitteiErrorCode.NOT_FOUND, "Service user not found"));
        ServiceResource updated = new ServiceResource(
            current.userId(),
            current.serviceId(),
            body.availability() == null ? current.availability() : body.availability(),
            body.bufferAfter() == null ? current.bufferAfter() : body.bufferAfter(),
            body.bufferBefore() == null ? current.bufferBefore() : body.bufferBefore(),
            body.closestBookingTime() == null ? current.closestBookingTime() : body.closestBookingTime(),
            body.furthestBookingTime() == null ? current.furthestBookingTime() : body.furthestBookingTime()
        );
        ctx.repos().serviceUsers().save(updated);
        return ResponseEntity.ok(ServiceApi.ServiceResourceResponse.from(updated));
    }

    public ResponseEntity<ServiceApi.ServiceResourceResponse> addBusyCalendar(
        HttpHeaders headers,
        String serviceId,
        String userId,
        ServiceApi.AddBusyCalendarRequestBody body
    ) {
        var account = auth.requireAdminAccount(headers, ctx);
        ID sid = auth.parseId(serviceId, "Malformed serviceId");
        auth.requireAccountService(account, sid, ctx);
        ID uid = auth.parseId(userId, "Malformed userId");
        ctx.repos().serviceUsers().find(sid, uid)
            .orElseThrow(() -> new NitteiApiException(NitteiErrorCode.NOT_FOUND, "Service user not found"));
        if ("google".equalsIgnoreCase(body.busy().provider()) || "outlook".equalsIgnoreCase(body.busy().provider())) {
            ctx.repos().serviceUserBusyCalendars().insertExternal(
                new ExternalBusyCalendarIdentifier(sid, uid, body.busy().id(), parseProvider(body.busy().provider()))
            );
        } else {
            ctx.repos().serviceUserBusyCalendars().insert(
                new BusyCalendarIdentifier(sid, uid, auth.parseId(body.busy().id(), "Malformed busy calendar id"))
            );
        }
        ServiceResource current = ctx.repos().serviceUsers().find(sid, uid).orElseThrow();
        return ResponseEntity.ok(ServiceApi.ServiceResourceResponse.from(current));
    }

    public ResponseEntity<ServiceApi.ServiceResourceResponse> removeBusyCalendar(
        HttpHeaders headers,
        String serviceId,
        String userId,
        ServiceApi.RemoveBusyCalendarRequestBody body
    ) {
        var account = auth.requireAdminAccount(headers, ctx);
        ID sid = auth.parseId(serviceId, "Malformed serviceId");
        auth.requireAccountService(account, sid, ctx);
        ID uid = auth.parseId(userId, "Malformed userId");
        ctx.repos().serviceUsers().find(sid, uid)
            .orElseThrow(() -> new NitteiApiException(NitteiErrorCode.NOT_FOUND, "Service user not found"));
        if ("google".equalsIgnoreCase(body.busy().provider()) || "outlook".equalsIgnoreCase(body.busy().provider())) {
            ctx.repos().serviceUserBusyCalendars().deleteExternal(
                new ExternalBusyCalendarIdentifier(sid, uid, body.busy().id(), parseProvider(body.busy().provider()))
            );
        } else {
            ctx.repos().serviceUserBusyCalendars().delete(
                new BusyCalendarIdentifier(sid, uid, auth.parseId(body.busy().id(), "Malformed busy calendar id"))
            );
        }
        ServiceResource current = ctx.repos().serviceUsers().find(sid, uid).orElseThrow();
        return ResponseEntity.ok(ServiceApi.ServiceResourceResponse.from(current));
    }

    private IntegrationProvider parseProvider(String provider) {
        if (provider == null) {
            throw new NitteiApiException(NitteiErrorCode.BAD_CLIENT_DATA, "Missing provider");
        }
        return switch (provider.toLowerCase()) {
            case "google" -> IntegrationProvider.GOOGLE;
            case "outlook" -> IntegrationProvider.OUTLOOK;
            default -> throw new NitteiApiException(NitteiErrorCode.BAD_CLIENT_DATA, "Unsupported provider");
        };
    }
}
