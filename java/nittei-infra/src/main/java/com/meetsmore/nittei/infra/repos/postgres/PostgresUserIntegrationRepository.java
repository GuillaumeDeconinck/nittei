package com.meetsmore.nittei.infra.repos.postgres;

import com.meetsmore.nittei.domain.ID;
import com.meetsmore.nittei.domain.IntegrationProvider;
import com.meetsmore.nittei.domain.UserIntegration;
import com.meetsmore.nittei.infra.repos.UserIntegrationRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

public class PostgresUserIntegrationRepository implements UserIntegrationRepository {

    private final JdbcTemplate jdbcTemplate;

    public PostgresUserIntegrationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void insert(UserIntegration integration) {
        jdbcTemplate.update(
            "INSERT INTO user_integrations(account_uid, user_uid, provider, refresh_token, access_token, access_token_expires_ts) VALUES (?, ?, ?, ?, ?, ?)",
            toUuid(integration.accountId()),
            toUuid(integration.userId()),
            providerToDb(integration.provider()),
            integration.refreshToken(),
            integration.accessToken(),
            integration.accessTokenExpiresTs()
        );
    }

    @Override
    public void save(UserIntegration integration) {
        jdbcTemplate.update(
            "UPDATE user_integrations SET access_token = ?, access_token_expires_ts = ?, refresh_token = ? WHERE user_uid = ? AND provider = ?",
            integration.accessToken(),
            integration.accessTokenExpiresTs(),
            integration.refreshToken(),
            toUuid(integration.userId()),
            providerToDb(integration.provider())
        );
    }

    @Override
    public List<UserIntegration> find(ID userId) {
        return jdbcTemplate.query(
            "SELECT user_uid, account_uid, refresh_token, access_token, access_token_expires_ts, provider FROM user_integrations WHERE user_uid = ?",
            (rs, rowNum) -> new UserIntegration(
                toId(rs.getObject("user_uid", UUID.class)),
                toId(rs.getObject("account_uid", UUID.class)),
                providerFromDb(rs.getString("provider")),
                rs.getString("refresh_token"),
                rs.getString("access_token"),
                rs.getLong("access_token_expires_ts")
            ),
            toUuid(userId)
        );
    }

    @Override
    public void delete(ID userId, IntegrationProvider provider) {
        int affected = jdbcTemplate.update(
            "DELETE FROM user_integrations WHERE user_uid = ? AND provider = ?",
            toUuid(userId),
            providerToDb(provider)
        );
        if (affected != 1) {
            throw new IllegalStateException("Unable to delete user integration");
        }
    }

    private static String providerToDb(IntegrationProvider provider) {
        return switch (provider) {
            case GOOGLE -> "google";
            case OUTLOOK -> "outlook";
        };
    }

    private static IntegrationProvider providerFromDb(String provider) {
        return "outlook".equalsIgnoreCase(provider) ? IntegrationProvider.OUTLOOK : IntegrationProvider.GOOGLE;
    }

    private static UUID toUuid(ID id) {
        return UUID.fromString(id.value());
    }

    private static ID toId(UUID id) {
        return new ID(id.toString());
    }
}
