package com.mendel.transactions.service;

import com.mendel.transactions.domain.Transaction;
import com.mendel.transactions.domain.TransactionRepository;
import com.mendel.transactions.exception.TransactionNotFoundException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class TransactionServiceImpl implements TransactionService {

  private final TransactionRepository repository;

  public TransactionServiceImpl(TransactionRepository repository) {
    this.repository = repository;
  }

  @Override
  public void upsert(long id, double amount, String type, Long parentId) {
    repository.save(new Transaction(id, amount, type, parentId));
  }

  @Override
  public List<Long> findIdsByType(String type) {
    return repository.findIdsByType(type);
  }

  @Override
  public double sumOf(long id) {
    Transaction root =
        repository.findById(id).orElseThrow(() -> new TransactionNotFoundException(id));

    double sum = 0;
    Set<Long> visited = new HashSet<>();
    Deque<Transaction> pending = new ArrayDeque<>();
    visited.add(root.id());
    pending.add(root);

    while (!pending.isEmpty()) {
      Transaction current = pending.poll();
      sum += current.amount();
      for (Transaction child : repository.findChildrenOf(current.id())) {
        if (visited.add(child.id())) {
          pending.add(child);
        }
      }
    }

    return sum;
  }
}
