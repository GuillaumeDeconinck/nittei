package com.meetsmore.nittei.api.shared;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public abstract class BaseApiController {

  protected <T> T notImplemented() {
    throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "Not implemented yet");
  }
}
