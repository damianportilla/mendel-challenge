package com.mendel.transactions.repository.dynamo;

import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.BillingMode;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest;
import software.amazon.awssdk.services.dynamodb.model.GlobalSecondaryIndex;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.Projection;
import software.amazon.awssdk.services.dynamodb.model.ProjectionType;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;

@Configuration
public class DynamoConfig {

  static final String TABLE_NAME = "transactions";
  static final String TYPE_INDEX = "type-index";
  static final String PARENT_INDEX = "parent-index";

  @Value("${AWS_ENDPOINT_URL:}")
  private String endpointUrl;

  @Value("${AWS_REGION:us-east-1}")
  private String region;

  @Value("${AWS_ACCESS_KEY_ID:test}")
  private String accessKeyId;

  @Value("${AWS_SECRET_ACCESS_KEY:test}")
  private String secretAccessKey;

  @Bean
  public DynamoDbClient dynamoDbClient() {
    var builder =
        DynamoDbClient.builder()
            .region(Region.of(region))
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(accessKeyId, secretAccessKey)));
    if (!endpointUrl.isBlank()) {
      builder.endpointOverride(URI.create(endpointUrl));
    }
    return builder.build();
  }

  @Bean
  public CommandLineRunner createTransactionsTable(DynamoDbClient client) {
    return args -> ensureTableExists(client);
  }

  private void ensureTableExists(DynamoDbClient client) {
    try {
      client.describeTable(DescribeTableRequest.builder().tableName(TABLE_NAME).build());
    } catch (ResourceNotFoundException e) {
      client.createTable(buildCreateTableRequest());
    }
  }

  private CreateTableRequest buildCreateTableRequest() {
    return CreateTableRequest.builder()
        .tableName(TABLE_NAME)
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
                .indexName(TYPE_INDEX)
                .keySchema(
                    KeySchemaElement.builder()
                        .attributeName(TransactionItem.ATTR_TYPE)
                        .keyType(KeyType.HASH)
                        .build())
                .projection(Projection.builder().projectionType(ProjectionType.KEYS_ONLY).build())
                .build(),
            GlobalSecondaryIndex.builder()
                .indexName(PARENT_INDEX)
                .keySchema(
                    KeySchemaElement.builder()
                        .attributeName(TransactionItem.ATTR_PARENT_ID)
                        .keyType(KeyType.HASH)
                        .build())
                .projection(Projection.builder().projectionType(ProjectionType.ALL).build())
                .build())
        .build();
  }
}
