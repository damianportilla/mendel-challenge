package com.mendel.transactions.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mendel.transactions.domain.Transaction;
import com.mendel.transactions.domain.TransactionRepository;
import com.mendel.transactions.exception.TransactionNotFoundException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TransactionServiceImplTest {

  @Mock private TransactionRepository repository;

  private TransactionServiceImpl underTest;

  @BeforeEach
  void setUp() {
    underTest = new TransactionServiceImpl(repository);
  }

  @Test
  void upsertSavesTransactionBuiltFromArguments() {
    underTest.upsert(10L, 5000.0, "cars", null);

    ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
    verify(repository).save(captor.capture());
    assertThat(captor.getValue()).isEqualTo(new Transaction(10L, 5000.0, "cars", null));
  }

  @Test
  void findIdsByTypeDelegatesToRepository() {
    when(repository.findIdsByType("cars")).thenReturn(List.of(10L));

    assertThat(underTest.findIdsByType("cars")).containsExactly(10L);
  }

  @Test
  void sumOfThrowsNotFoundWhenTransactionDoesNotExist() {
    when(repository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> underTest.sumOf(99L)).isInstanceOf(TransactionNotFoundException.class);
  }

  @Test
  void sumOfMatchesPdfExample() {
    Transaction t10 = new Transaction(10L, 5000, "cars", null);
    Transaction t11 = new Transaction(11L, 10000, "shopping", 10L);
    Transaction t12 = new Transaction(12L, 5000, "shopping", 11L);

    when(repository.findById(10L)).thenReturn(Optional.of(t10));
    when(repository.findById(11L)).thenReturn(Optional.of(t11));
    when(repository.findChildrenOf(10L)).thenReturn(List.of(t11));
    when(repository.findChildrenOf(11L)).thenReturn(List.of(t12));
    when(repository.findChildrenOf(12L)).thenReturn(List.of());

    assertThat(underTest.sumOf(10L)).isEqualTo(20000.0);
    assertThat(underTest.sumOf(11L)).isEqualTo(15000.0);
  }

  @Test
  void sumOfDoesNotLoopForeverOnCyclicReference() {
    Transaction t1 = new Transaction(1L, 100, "a", null);
    Transaction t2 = new Transaction(2L, 50, "b", 1L);

    when(repository.findById(1L)).thenReturn(Optional.of(t1));
    when(repository.findChildrenOf(1L)).thenReturn(List.of(t2));
    when(repository.findChildrenOf(2L)).thenReturn(List.of(t1));

    assertThat(underTest.sumOf(1L)).isEqualTo(150.0);
  }
}
