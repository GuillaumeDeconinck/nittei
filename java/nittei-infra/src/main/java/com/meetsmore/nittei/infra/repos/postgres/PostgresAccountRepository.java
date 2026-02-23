package com.meetsmore.nittei.infra.repos.postgres;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.meetsmore.nittei.domain.Account;
import com.meetsmore.nittei.domain.AccountSettings;
import com.meetsmore.nittei.domain.ID;
import com.meetsmore.nittei.domain.PEMKey;
import com.meetsmore.nittei.infra.repos.AccountRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

public class PostgresAccountRepository implements AccountRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final Cache<String, Account> cache;

    public PostgresAccountRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.cache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(5))
            .maximumSize(10_000)
            .build();
    }

    @Override
    public void insert(Account account) {
        jdbcTemplate.update(
            "INSERT INTO accounts(account_uid, secret_api_key, public_jwt_key, settings) VALUES (?, ?, ?, CAST(? AS jsonb))",
            toUuid(account.id()),
            account.secretApiKey(),
            account.publicJwtKey() == null ? null : account.publicJwtKey().value(),
            toJson(account.settings())
        );
    }

    @Override
    public void save(Account account) {
        jdbcTemplate.update(
            "UPDATE accounts SET secret_api_key = ?, public_jwt_key = ?, settings = CAST(? AS jsonb) WHERE account_uid = ?",
            account.secretApiKey(),
            account.publicJwtKey() == null ? null : account.publicJwtKey().value(),
            toJson(account.settings()),
            toUuid(account.id())
        );
        cache.invalidate(account.secretApiKey());
    }

    @Override
    public Optional<Account> find(ID accountId) {
        List<Account> rows = jdbcTemplate.query(
            "SELECT account_uid, secret_api_key, public_jwt_key, settings::text AS settings FROM accounts WHERE account_uid = ?",
            (rs, rowNum) -> mapAccount(rs),
            toUuid(accountId)
        );
        return rows.stream().findFirst();
    }

    @Override
    public List<Account> findMany(List<ID> accountIds) {
        if (accountIds.isEmpty()) {
            return List.of();
        }
        String sql = "SELECT account_uid, secret_api_key, public_jwt_key, settings::text AS settings FROM accounts WHERE account_uid IN ("
            + placeholders(accountIds.size()) + ")";
        Object[] args = accountIds.stream().map(PostgresAccountRepository::toUuid).toArray();
        return jdbcTemplate.query(sql, (rs, rowNum) -> mapAccount(rs), args);
    }

    @Override
    public Optional<Account> delete(ID accountId) {
        List<Account> rows = jdbcTemplate.query(
            "DELETE FROM accounts WHERE account_uid = ? RETURNING account_uid, secret_api_key, public_jwt_key, settings::text AS settings",
            (rs, rowNum) -> mapAccount(rs),
            toUuid(accountId)
        );
        if (!rows.isEmpty()) {
            cache.invalidate(rows.get(0).secretApiKey());
        }
        return rows.stream().findFirst();
    }

    @Override
    public Optional<Account> findByApiKey(String apiKey) {
        Account cached = cache.getIfPresent(apiKey);
        if (cached != null) {
            return Optional.of(cached);
        }

        List<Account> rows = jdbcTemplate.query(
            "SELECT account_uid, secret_api_key, public_jwt_key, settings::text AS settings FROM accounts WHERE secret_api_key = ?",
            (rs, rowNum) -> mapAccount(rs),
            apiKey
        );

        Optional<Account> found = rows.stream().findFirst();
        found.ifPresent(value -> cache.put(apiKey, value));
        return found;
    }

    private Account mapAccount(ResultSet rs) throws SQLException {
        String settings = rs.getString("settings");
        AccountSettings mappedSettings;
        try {
            mappedSettings = objectMapper.readValue(settings, AccountSettings.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Invalid account settings JSON", e);
        }

        String publicKey = rs.getString("public_jwt_key");
        return new Account(
            toId(rs.getObject("account_uid", UUID.class)),
            rs.getString("secret_api_key"),
            publicKey == null ? null : new PEMKey(publicKey),
            mappedSettings
        );
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("JSON serialization failed", e);
        }
    }

    private static String placeholders(int count) {
        List<String> values = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            values.add("?");
        }
        return String.join(",", values);
    }

    private static UUID toUuid(ID id) {
        return UUID.fromString(id.value());
    }

    private static ID toId(UUID id) {
        return new ID(id.toString());
    }
}
