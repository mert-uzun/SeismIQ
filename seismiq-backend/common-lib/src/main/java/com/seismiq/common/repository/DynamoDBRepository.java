package com.seismiq.common.repository;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;
import software.amazon.awssdk.services.dynamodb.model.*;
import java.net.URI;
import java.util.Map;

/**
 * Base repository class for DynamoDB operations.
 * Provides common functionality for interacting with DynamoDB tables
 * in the SeismIQ system.
 *
 * @author Sıla Bozkurt
 */
public abstract class DynamoDBRepository {
    protected final DynamoDbClient dynamoDbClient;
    protected final String tableName;

    protected DynamoDBRepository(String tableName) {
        this.tableName = tableName;
        this.dynamoDbClient = createDynamoDbClient();
    }

    private DynamoDbClient createDynamoDbClient() {
        DynamoDbClientBuilder builder = DynamoDbClient.builder();
        
        // Set region - use environment variable or default to us-east-1
        String region = System.getenv("AWS_REGION");
        if (region == null || region.isEmpty()) {
            region = "us-east-1";
        }
        builder.region(Region.of(region));
        
        // If running locally (e.g., with SAM local), use local DynamoDB
        String isLocal = System.getenv("AWS_SAM_LOCAL");
        if ("true".equals(isLocal)) {
            builder.endpointOverride(URI.create("http://localhost:8000"));
        }
        
        return builder.build();
    }

    protected PutItemResponse putItem(Map<String, AttributeValue> item) {
        PutItemRequest request = PutItemRequest.builder()
                .tableName(tableName)
                .item(item)
                .build();
        return dynamoDbClient.putItem(request);
    }

    protected GetItemResponse getItem(Map<String, AttributeValue> key) {
        GetItemRequest request = GetItemRequest.builder()
                .tableName(tableName)
                .key(key)
                .build();
        return dynamoDbClient.getItem(request);
    }

    protected DeleteItemResponse deleteItem(Map<String, AttributeValue> key) {
        DeleteItemRequest request = DeleteItemRequest.builder()
                .tableName(tableName)
                .key(key)
                .build();
        return dynamoDbClient.deleteItem(request);
    }
}