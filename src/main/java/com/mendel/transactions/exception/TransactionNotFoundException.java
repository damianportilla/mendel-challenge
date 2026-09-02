package com.mendel.transactions.exception;

public class TransactionNotFoundException extends DomainException {

  public TransactionNotFoundException(long id) {
    super("Transaction not found: " + id);
  }

  @Override
  public String code() {
    return "TRANSACTION_NOT_FOUND";
  }
}
