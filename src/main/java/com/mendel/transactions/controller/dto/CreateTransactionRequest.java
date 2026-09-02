package com.mendel.transactions.controller.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateTransactionRequest(
    @NotNull Double amount, @NotBlank String type, @JsonProperty("parent_id") Long parentId) {}
