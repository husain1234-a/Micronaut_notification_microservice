package com.yash.notification.config;

import io.micronaut.context.annotation.Context;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

@Context
public class DynamoDbTableInitializer {
    private static final Logger LOG = LoggerFactory.getLogger(DynamoDbTableInitializer.class);

    @Inject
    private DynamoDbClient dynamoDbClient;

    @PostConstruct
    public void initializeTables() {
        createNotificationsTable();
    }

    private void createNotificationsTable() {
        try {
            CreateTableRequest request = CreateTableRequest.builder()
                    .tableName("notifications")
                    .keySchema(
                            KeySchemaElement.builder()
                                    .attributeName("id")
                                    .keyType(KeyType.HASH)
                                    .build()
                    )
                    .attributeDefinitions(
                            AttributeDefinition.builder()
                                    .attributeName("id")
                                    .attributeType(ScalarAttributeType.S)
                                    .build()
                    )
                    .provisionedThroughput(
                            ProvisionedThroughput.builder()
                                    .readCapacityUnits(5L)
                                    .writeCapacityUnits(5L)
                                    .build()
                    )
                    .build();

            dynamoDbClient.createTable(request);
            LOG.info("Notifications table created successfully");
        } catch (ResourceInUseException e) {
            LOG.info("Notifications table already exists");
        } catch (Exception e) {
            LOG.error("Error creating notifications table", e);
        }
    }
} 