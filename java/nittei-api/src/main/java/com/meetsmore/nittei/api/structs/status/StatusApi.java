package com.meetsmore.nittei.api.structs.status;

public final class StatusApi {

  private StatusApi() {}

  public record ServiceHealthResponse(String message) {}
}
