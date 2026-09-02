package com.mendel.transactions.controller.dto;

import com.fasterxml.jackson.annotation.JsonValue;
import java.util.List;

public record TypeIdsResponse(List<Long> ids) {

  @JsonValue
  public List<Long> ids() {
    return ids;
  }
}
