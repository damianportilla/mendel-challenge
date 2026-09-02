package com.mendel.transactions.controller;

import com.mendel.transactions.controller.dto.CreateTransactionRequest;
import com.mendel.transactions.controller.dto.SumResponse;
import com.mendel.transactions.controller.dto.TypeIdsResponse;
import com.mendel.transactions.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/transactions")
public class TransactionController {

  private final TransactionService service;

  public TransactionController(TransactionService service) {
    this.service = service;
  }

  @PutMapping("/{id}")
  public void upsert(@PathVariable long id, @Valid @RequestBody CreateTransactionRequest request) {
    service.upsert(id, request.amount(), request.type(), request.parentId());
  }

  @GetMapping("/types/{type}")
  public TypeIdsResponse findIdsByType(@PathVariable String type) {
    return new TypeIdsResponse(service.findIdsByType(type));
  }

  @GetMapping("/sum/{id}")
  public SumResponse sum(@PathVariable long id) {
    return new SumResponse(service.sumOf(id));
  }
}
