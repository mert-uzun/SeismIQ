package com.seismiq.report;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

/**
 * Repository class for Landmark-related operations.
 * This is a simplified version for use in the Report service.
 * 
 * @author Sıla Bozkurt
 */
public class LandmarkRepository {
    private final DynamoDbClient dynamoDbClient;
    private final String tableName;

    /**
     * Creates a new LandmarkRepository with default DynamoDB client.
     */
    public LandmarkRepository() {
        this(DynamoDbClient.create(), System.getenv("LANDMARK_TABLE_NAME") != null 
            ? System.getenv("LANDMARK_TABLE_NAME") : "Landmarks");
    }

    /**
     * Creates a new LandmarkRepository with the provided DynamoDB client and table name.
     * 
     * @param dynamoDbClient The DynamoDB client to use
     * @param tableName The name of the table to interact with
     */
    public LandmarkRepository(DynamoDbClient dynamoDbClient, String tableName) {
        this.dynamoDbClient = dynamoDbClient;
        this.tableName = tableName;
    }
}
