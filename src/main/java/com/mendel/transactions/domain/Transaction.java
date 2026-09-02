package com.mendel.transactions.domain;

public record Transaction(long id, double amount, String type, Long parentId) {}
