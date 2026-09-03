package com.mendel.transactions.repository.dynamo;

import static org.assertj.core.api.Assertions.assertThat;

import com.mendel.transactions.domain.Transaction;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.localstack.LocalStackContainer.Service;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.BillingMode;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.GlobalSecondaryIndex;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.Projection;
import software.amazon.awssdk.services.dynamodb.model.ProjectionType;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;

@Testcontainers
class DynamoTransactionRepositoryTest {

  @Container
  static final LocalStackContainer localstack =
      new LocalStackContainer(DockerImageName.parse("localstack/localstack:3.8"))
          .withServices(Service.DYNAMODB);

  private static DynamoDbClient client;
  private DynamoTransactionRepository repository;

  @BeforeAll
  static void setUpClient() {
    client =
        DynamoDbClient.builder()
            .endpointOverride(localstack.getEndpointOverride(Service.DYNAMODB))
            .region(Region.of(localstack.getRegion()))
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(
                        localstack.getAccessKey(), localstack.getSecretKey())))
            .build();
    createTable();
  }

  private static void createTable() {
    client.createTable(
        CreateTableRequest.builder()
            .tableName(DynamoConfig.TABLE_NAME)
            .billingMode(BillingMode.PAY_PER_REQUEST)
            .attributeDefinitions(
                AttributeDefinition.builder()
                    .attributeName(TransactionItem.ATTR_TRANSACTION_ID)
                    .attributeType(ScalarAttributeType.N)
                    .build(),
                AttributeDefinition.builder()
                    .attributeName(TransactionItem.ATTR_TYPE)
                    .attributeType(ScalarAttributeType.S)
                    .build(),
                AttributeDefinition.builder()
                    .attributeName(TransactionItem.ATTR_PARENT_ID)
                    .attributeType(ScalarAttributeType.N)
                    .build())
            .keySchema(
                KeySchemaElement.builder()
                    .attributeName(TransactionItem.ATTR_TRANSACTION_ID)
                    .keyType(KeyType.HASH)
                    .build())
            .globalSecondaryIndexes(
                GlobalSecondaryIndex.builder()
                    .indexName(DynamoConfig.TYPE_INDEX)
                    .keySchema(
                        KeySchemaElement.builder()
                            .attributeName(TransactionItem.ATTR_TYPE)
                            .keyType(KeyType.HASH)
                            .build())
                    .projection(
                        Projection.builder().projectionType(ProjectionType.KEYS_ONLY).build())
                    .build(),
                GlobalSecondaryIndex.builder()
                    .indexName(DynamoConfig.PARENT_INDEX)
                    .keySchema(
                        KeySchemaElement.builder()
                            .attributeName(TransactionItem.ATTR_PARENT_ID)
                            .keyType(KeyType.HASH)
                            .build())
                    .projection(Projection.builder().projectionType(ProjectionType.ALL).build())
                    .build())
            .build());
  }

  @BeforeEach
  void setUp() {
    repository = new DynamoTransactionRepository(client);
  }

  @Test
  void shouldSaveAndFindATransactionById() {
    repository.save(new Transaction(10L, 5000.0, "cars", null));

    Optional<Transaction> found = repository.findById(10L);

    assertThat(found).contains(new Transaction(10L, 5000.0, "cars", null));
  }

  @Test
  void shouldReturnEmptyForUnknownId() {
    assertThat(repository.findById(123456789L)).isEmpty();
  }

  @Test
  void shouldReplaceAnExistingTransactionOnUpsert() {
    repository.save(new Transaction(20L, 100.0, "food", null));
    repository.save(new Transaction(20L, 200.0, "shopping", 99L));

    assertThat(repository.findById(20L)).contains(new Transaction(20L, 200.0, "shopping", 99L));
  }

  @Test
  void shouldReturnOnlyMatchingIdsByType() {
    repository.save(new Transaction(30L, 5000.0, "cars", null));
    repository.save(new Transaction(31L, 100.0, "food", null));

    assertThat(repository.findIdsByType("cars")).contains(30L).doesNotContain(31L);
  }

  @Test
  void shouldReturnDirectChildrenWithAmounts() {
    repository.save(new Transaction(40L, 5000.0, "cars", null));
    repository.save(new Transaction(41L, 10000.0, "shopping", 40L));
    repository.save(new Transaction(42L, 5000.0, "shopping", 41L));

    List<Transaction> children = repository.findChildrenOf(40L);

    assertThat(children).containsExactly(new Transaction(41L, 10000.0, "shopping", 40L));
  }

  @Test
  void shouldSupportOrphanParentReferences() {
    repository.save(new Transaction(50L, 250.0, "misc", 999999L));

    assertThat(repository.findChildrenOf(999999L))
        .containsExactly(new Transaction(50L, 250.0, "misc", 999999L));
  }
}
