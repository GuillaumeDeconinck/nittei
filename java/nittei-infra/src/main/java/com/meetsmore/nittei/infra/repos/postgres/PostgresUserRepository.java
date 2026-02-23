package com.meetsmore.nittei.infra.repos.postgres;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meetsmore.nittei.domain.ID;
import com.meetsmore.nittei.domain.User;
import com.meetsmore.nittei.infra.repos.UserRepository;
import com.meetsmore.nittei.infra.repos.query.MetadataFindQuery;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

public class PostgresUserRepository implements UserRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public PostgresUserRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void insert(User user) {
        jdbcTemplate.update(
            "INSERT INTO users(user_uid, account_uid, external_id, metadata) VALUES (?, ?, ?, CAST(? AS jsonb))",
            toUuid(user.id()),
            toUuid(user.accountId()),
            user.externalId(),
            toJson(user.metadata())
        );
    }

    @Override
    public void save(User user) {
        jdbcTemplate.update(
            "UPDATE users SET account_uid = ?, external_id = ?, metadata = CAST(? AS jsonb) WHERE user_uid = ?",
            toUuid(user.accountId()),
            user.externalId(),
            toJson(user.metadata()),
            toUuid(user.id())
        );
    }

    @Override
    public Optional<User> delete(ID userId) {
        List<User> rows = jdbcTemplate.query(
            "DELETE FROM users WHERE user_uid = ? RETURNING user_uid, account_uid, external_id, metadata::text AS metadata",
            (rs, rowNum) -> mapUser(rs),
            toUuid(userId)
        );
        return rows.stream().findFirst();
    }

    @Override
    public Optional<User> find(ID userId) {
        List<User> rows = jdbcTemplate.query(
            "SELECT user_uid, account_uid, external_id, metadata::text AS metadata FROM users WHERE user_uid = ?",
            (rs, rowNum) -> mapUser(rs),
            toUuid(userId)
        );
        return rows.stream().findFirst();
    }

    @Override
    public List<User> findMany(List<ID> userIds) {
        if (userIds.isEmpty()) {
            return List.of();
        }
        String sql = "SELECT user_uid, account_uid, external_id, metadata::text AS metadata FROM users WHERE user_uid IN ("
            + placeholders(userIds.size()) + ")";
        Object[] args = userIds.stream().map(PostgresUserRepository::toUuid).toArray();
        return jdbcTemplate.query(sql, (rs, rowNum) -> mapUser(rs), args);
    }

    @Override
    public Optional<User> findByAccountId(ID userId, ID accountId) {
        List<User> rows = jdbcTemplate.query(
            "SELECT user_uid, account_uid, external_id, metadata::text AS metadata FROM users WHERE user_uid = ? AND account_uid = ?",
            (rs, rowNum) -> mapUser(rs),
            toUuid(userId),
            toUuid(accountId)
        );
        return rows.stream().findFirst();
    }

    @Override
    public Optional<User> getByExternalId(String externalId) {
        List<User> rows = jdbcTemplate.query(
            "SELECT user_uid, account_uid, external_id, metadata::text AS metadata FROM users WHERE external_id = ?",
            (rs, rowNum) -> mapUser(rs),
            externalId
        );
        return rows.stream().findFirst();
    }

    @Override
    public List<User> findByMetadata(MetadataFindQuery query) {
        String metadataJson = toJson(query.metadata() == null ? null : query.metadata().inner());
        return jdbcTemplate.query(
            "SELECT user_uid, account_uid, external_id, metadata::text AS metadata FROM users WHERE account_uid = ? AND metadata @> CAST(? AS jsonb) LIMIT ? OFFSET ?",
            (rs, rowNum) -> mapUser(rs),
            toUuid(query.accountId()),
            metadataJson,
            query.limit(),
            query.skip()
        );
    }

    private User mapUser(ResultSet rs) throws SQLException {
        Object metadata;
        try {
            metadata = objectMapper.readValue(rs.getString("metadata"), Object.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Invalid user metadata JSON", e);
        }

        return new User(
            toId(rs.getObject("user_uid", UUID.class)),
            toId(rs.getObject("account_uid", UUID.class)),
            metadata,
            rs.getString("external_id")
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
