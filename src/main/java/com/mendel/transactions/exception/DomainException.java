package com.mendel.transactions.exception;

public abstract class DomainException extends RuntimeException {

  protected DomainException(String message) {
    super(message);
  }

  public abstract String code();
}
