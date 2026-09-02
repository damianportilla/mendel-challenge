package com.mendel.transactions.domain;

import java.util.List;
import java.util.Optional;

public interface TransactionRepository {

  void save(Transaction transaction);

  Optional<Transaction> findById(long id);

  List<Long> findIdsByType(String type);

  List<Transaction> findChildrenOf(long parentId);
}
