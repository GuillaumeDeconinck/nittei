package com.meetsmore.nittei.infra.repos;

import com.meetsmore.nittei.domain.ID;
import com.meetsmore.nittei.domain.Service;
import com.meetsmore.nittei.domain.ServiceWithUsers;
import com.meetsmore.nittei.infra.repos.query.MetadataFindQuery;
import java.util.List;
import java.util.Optional;

public interface ServiceRepository {
  void insert(Service service);

  void save(Service service);

  Optional<Service> find(ID serviceId);

  Optional<ServiceWithUsers> findWithUsers(ID serviceId);

  void delete(ID serviceId);

  List<Service> findByMetadata(MetadataFindQuery query);
}
