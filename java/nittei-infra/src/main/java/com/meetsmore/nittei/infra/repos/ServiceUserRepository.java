package com.meetsmore.nittei.infra.repos;

import com.meetsmore.nittei.domain.ID;
import com.meetsmore.nittei.domain.ServiceResource;
import java.util.List;
import java.util.Optional;

public interface ServiceUserRepository {
  void insert(ServiceResource user);

  void save(ServiceResource user);

  Optional<ServiceResource> find(ID serviceId, ID userId);

  List<ServiceResource> findByUser(ID userId);

  void delete(ID serviceId, ID userId);
}
