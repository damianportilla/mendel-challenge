package com.mendel.transactions.service;

import java.util.List;

public interface TransactionService {

  void upsert(long id, double amount, String type, Long parentId);

  List<Long> findIdsByType(String type);

  double sumOf(long id);
}
