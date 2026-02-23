package com.meetsmore.nittei.api.structs.user;

import com.meetsmore.nittei.domain.ID;
import com.meetsmore.nittei.domain.IntegrationProvider;
import com.meetsmore.nittei.domain.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public final class UserApi {

    private UserApi() {
    }

    public record UserResponse(UserDtos.UserDTO user) {
        public static UserResponse from(User user) {
            return new UserResponse(UserDtos.UserDTO.from(user));
        }
    }

    public record CreateUserRequestBody(Object metadata, String externalId, ID userId) {
    }

    public record OAuthIntegrationRequestBody(@NotBlank String code, @NotNull IntegrationProvider provider) {
    }

    public record RemoveIntegrationPathParams(IntegrationProvider provider, ID userId) {
    }

    public record OAuthOutlookRequestBody(@NotBlank String code) {
    }

    public record UpdateUserRequestBody(String externalId, Object metadata) {
    }

    public record GetUserByExternalIdPathParams(String externalId) {
    }

    public record UserPathParams(ID userId) {
    }

    public record OAuthPathParams(ID userId) {
    }

    public record GetUsersByMetaQueryParams(String key, String value, Integer skip, Integer limit) {
    }

    public record GetUsersByMetaAPIResponse(List<UserDtos.UserDTO> users) {
    }
}
