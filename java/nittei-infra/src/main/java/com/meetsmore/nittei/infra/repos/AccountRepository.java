package com.meetsmore.nittei.infra.repos;

import com.meetsmore.nittei.domain.Account;
import com.meetsmore.nittei.domain.ID;
import java.util.List;
import java.util.Optional;

public interface AccountRepository {
    void insert(Account account);
    void save(Account account);
    Optional<Account> find(ID accountId);
    List<Account> findMany(List<ID> accountIds);
    Optional<Account> delete(ID accountId);
    Optional<Account> findByApiKey(String apiKey);
}
