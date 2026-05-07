package com.meetsmore.nittei.infra.repos;

import com.meetsmore.nittei.domain.ID;
import com.meetsmore.nittei.domain.User;
import com.meetsmore.nittei.infra.repos.query.MetadataFindQuery;
import java.util.List;
import java.util.Optional;

public interface UserRepository {
  void insert(User user);

  void save(User user);

  Optional<User> delete(ID userId);

  Optional<User> find(ID userId);

  List<User> findMany(List<ID> userIds);

  Optional<User> findByAccountId(ID userId, ID accountId);

  Optional<User> getByExternalId(String externalId);

  List<User> findByMetadata(MetadataFindQuery query);
}
