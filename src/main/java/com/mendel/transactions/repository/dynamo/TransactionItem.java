package com.mendel.transactions.repository.dynamo;

import com.mendel.transactions.domain.Transaction;
import java.util.HashMap;
import java.util.Map;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

record TransactionItem(long transactionId, double amount, String type, Long parentId) {

  static final String ATTR_TRANSACTION_ID = "transaction_id";
  static final String ATTR_AMOUNT = "amount";
  static final String ATTR_TYPE = "type";
  static final String ATTR_PARENT_ID = "parent_id";

  static TransactionItem fromDomain(Transaction transaction) {
    return new TransactionItem(
        transaction.id(), transaction.amount(), transaction.type(), transaction.parentId());
  }

  static TransactionItem fromAttributeMap(Map<String, AttributeValue> item) {
    long id = Long.parseLong(item.get(ATTR_TRANSACTION_ID).n());
    double amount = Double.parseDouble(item.get(ATTR_AMOUNT).n());
    String type = item.get(ATTR_TYPE).s();
    AttributeValue parentAttribute = item.get(ATTR_PARENT_ID);
    Long parentId = parentAttribute != null ? Long.parseLong(parentAttribute.n()) : null;
    return new TransactionItem(id, amount, type, parentId);
  }

  Transaction toDomain() {
    return new Transaction(transactionId, amount, type, parentId);
  }

  Map<String, AttributeValue> toAttributeMap() {
    Map<String, AttributeValue> item = new HashMap<>();
    item.put(ATTR_TRANSACTION_ID, AttributeValue.fromN(String.valueOf(transactionId)));
    item.put(ATTR_AMOUNT, AttributeValue.fromN(String.valueOf(amount)));
    item.put(ATTR_TYPE, AttributeValue.fromS(type));
    if (parentId != null) {
      item.put(ATTR_PARENT_ID, AttributeValue.fromN(String.valueOf(parentId)));
    }
    return item;
  }
}
