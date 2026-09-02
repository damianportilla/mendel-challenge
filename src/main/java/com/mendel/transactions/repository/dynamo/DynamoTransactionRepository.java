package com.mendel.transactions.repository.dynamo;

import com.mendel.transactions.domain.Transaction;
import com.mendel.transactions.domain.TransactionRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;

@Repository
public class DynamoTransactionRepository implements TransactionRepository {

  private final DynamoDbClient client;

  public DynamoTransactionRepository(DynamoDbClient client) {
    this.client = client;
  }

  @Override
  public void save(Transaction transaction) {
    client.putItem(
        PutItemRequest.builder()
            .tableName(DynamoConfig.TABLE_NAME)
            .item(TransactionItem.fromDomain(transaction).toAttributeMap())
            .build());
  }

  @Override
  public Optional<Transaction> findById(long id) {
    GetItemResponse response =
        client.getItem(
            GetItemRequest.builder()
                .tableName(DynamoConfig.TABLE_NAME)
                .key(
                    Map.of(
                        TransactionItem.ATTR_TRANSACTION_ID,
                        AttributeValue.fromN(String.valueOf(id))))
                .build());
    if (!response.hasItem()) {
      return Optional.empty();
    }
    return Optional.of(TransactionItem.fromAttributeMap(response.item()).toDomain());
  }

  @Override
  public List<Long> findIdsByType(String type) {
    QueryRequest request =
        QueryRequest.builder()
            .tableName(DynamoConfig.TABLE_NAME)
            .indexName(DynamoConfig.TYPE_INDEX)
            .keyConditionExpression("#type = :type")
            .expressionAttributeNames(Map.of("#type", TransactionItem.ATTR_TYPE))
            .expressionAttributeValues(Map.of(":type", AttributeValue.fromS(type)))
            .build();
    return client.query(request).items().stream()
        .map(item -> Long.parseLong(item.get(TransactionItem.ATTR_TRANSACTION_ID).n()))
        .toList();
  }

  @Override
  public List<Transaction> findChildrenOf(long parentId) {
    QueryRequest request =
        QueryRequest.builder()
            .tableName(DynamoConfig.TABLE_NAME)
            .indexName(DynamoConfig.PARENT_INDEX)
            .keyConditionExpression("#parentId = :parentId")
            .expressionAttributeNames(Map.of("#parentId", TransactionItem.ATTR_PARENT_ID))
            .expressionAttributeValues(
                Map.of(":parentId", AttributeValue.fromN(String.valueOf(parentId))))
            .build();
    return client.query(request).items().stream()
        .map(item -> TransactionItem.fromAttributeMap(item).toDomain())
        .toList();
  }
}
