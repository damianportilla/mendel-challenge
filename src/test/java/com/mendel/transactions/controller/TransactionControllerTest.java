package com.mendel.transactions.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mendel.transactions.exception.TransactionNotFoundException;
import com.mendel.transactions.service.TransactionService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import software.amazon.awssdk.core.exception.SdkClientException;

@WebMvcTest(TransactionController.class)
class TransactionControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @MockBean private TransactionService service;

  @Test
  void shouldUpsertTransactionAndReturnOk() throws Exception {
    mockMvc
        .perform(
            put("/transactions/10")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("amount", 5000, "type", "cars"))))
        .andExpect(status().isOk());

    verify(service).upsert(eq(10L), eq(5000.0), eq("cars"), eq(null));
  }

  @Test
  void shouldReadSnakeCaseParentId() throws Exception {
    mockMvc
        .perform(
            put("/transactions/11")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of("amount", 10000, "type", "shopping", "parent_id", 10))))
        .andExpect(status().isOk());

    verify(service).upsert(eq(11L), eq(10000.0), eq("shopping"), eq(10L));
  }

  @Test
  void shouldReturn400WhenTypeIsMissing() throws Exception {
    mockMvc
        .perform(
            put("/transactions/10")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("amount", 5000))))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldReturnBareJsonArrayOfIds() throws Exception {
    when(service.findIdsByType("cars")).thenReturn(List.of(10L));

    mockMvc
        .perform(get("/transactions/types/cars"))
        .andExpect(status().isOk())
        .andExpect(content().json("[10]"));
  }

  @Test
  void shouldReturnSumObject() throws Exception {
    when(service.sumOf(10L)).thenReturn(20000.0);

    mockMvc
        .perform(get("/transactions/sum/10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.sum").value(20000.0));
  }

  @Test
  void shouldReturn404ForUnknownId() throws Exception {
    when(service.sumOf(99L)).thenThrow(new TransactionNotFoundException(99L));

    mockMvc.perform(get("/transactions/sum/99")).andExpect(status().isNotFound());
  }

  @Test
  void shouldReturn503WhenDynamoDbFailsInsteadOfLeakingAStackTrace() throws Exception {
    when(service.sumOf(10L)).thenThrow(SdkClientException.create("connection reset"));

    mockMvc
        .perform(get("/transactions/sum/10"))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.code").value("STORAGE_UNAVAILABLE"))
        .andExpect(jsonPath("$.message").value("The storage backend is unavailable"));
  }
}
