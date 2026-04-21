package com.meetsmore.nittei.infra.repos;

import com.meetsmore.nittei.domain.ID;
import com.meetsmore.nittei.domain.IntegrationProvider;
import com.meetsmore.nittei.domain.UserIntegration;
import java.util.List;

public interface UserIntegrationRepository {
  void insert(UserIntegration integration);

  void save(UserIntegration integration);

  List<UserIntegration> find(ID userId);

  void delete(ID userId, IntegrationProvider provider);
}
