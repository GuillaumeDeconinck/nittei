package com.meetsmore.nittei.infra.repos.query;

import com.meetsmore.nittei.domain.ID;
import com.meetsmore.nittei.domain.Metadata;

public record MetadataFindQuery(Metadata metadata, int skip, int limit, ID accountId) {}
