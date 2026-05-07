package com.meetsmore.nittei.infra.repos;

import com.meetsmore.nittei.domain.ID;
import com.meetsmore.nittei.domain.IntegrationProvider;

public record ExternalBusyCalendarIdentifier(
    ID serviceId, ID userId, String extCalendarId, IntegrationProvider provider) {}
