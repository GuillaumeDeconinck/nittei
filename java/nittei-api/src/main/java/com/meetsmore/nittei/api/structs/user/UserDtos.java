package com.meetsmore.nittei.api.structs.user;

import com.meetsmore.nittei.domain.ID;
import com.meetsmore.nittei.domain.User;

public final class UserDtos {

  private UserDtos() {}

  public record UserDTO(ID id, String externalId, Object metadata) {
    public static UserDTO from(User user) {
      return new UserDTO(user.id(), user.externalId(), user.metadata());
    }
  }
}
