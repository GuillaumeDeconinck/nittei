package com.meetsmore.nittei.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public record PEMKey(String value) {

  @JsonCreator
  public PEMKey {}

  @JsonValue
  public String jsonValue() {
    return value;
  }
}
