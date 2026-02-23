package com.meetsmore.nittei.infra.repos;

import com.meetsmore.nittei.domain.AccountIntegration;
import com.meetsmore.nittei.domain.ID;
import com.meetsmore.nittei.domain.IntegrationProvider;
import java.util.List;

public interface AccountIntegrationRepository {
    void insert(AccountIntegration integration);
    List<AccountIntegration> find(ID accountId);
    void delete(ID accountId, IntegrationProvider provider);
}
