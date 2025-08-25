package com.seismiq.earthquake;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.seismiq.common.model.Earthquake;

/**
 * Repository class for managing earthquake data in DynamoDB.
 * Handles CRUD operations and specialized queries for earthquake records.
 *
 * @author Sıla Bozkurt
 */
import com.seismiq.common.repository.DynamoDBRepository;

import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

public class EarthquakeRepository extends DynamoDBRepository {
    private static final String EARTHQUAKES_TABLE = "Earthquakes";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;
    private static final long SIX_MONTHS_IN_DAYS = 180;

    public EarthquakeRepository() {
        super(EARTHQUAKES_TABLE);
    }

    public void saveEarthquake(Earthquake earthquake) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("earthquakeId", AttributeValue.builder().s(earthquake.getEarthquakeId()).build());
        item.put("magnitude", AttributeValue.builder().n(String.valueOf(earthquake.getMagnitude())).build());
        item.put("latitude", AttributeValue.builder().n(String.valueOf(earthquake.getLatitude())).build());
        item.put("longitude", AttributeValue.builder().n(String.valueOf(earthquake.getLongitude())).build());
        item.put("depth", AttributeValue.builder().n(String.valueOf(earthquake.getDepth())).build());
        item.put("location", AttributeValue.builder().s(earthquake.getLocation()).build());
        item.put("timestamp", AttributeValue.builder().s(earthquake.getTimestamp().format(DATE_FORMATTER)).build());
        item.put("source", AttributeValue.builder().s(earthquake.getSource()).build());
        item.put("isActive", AttributeValue.builder().bool(earthquake.isActive()).build());

        putItem(item);
    }

    public Earthquake getEarthquake(String earthquakeId) {
        Map<String, AttributeValue> key = new HashMap<>();
        key.put("earthquakeId", AttributeValue.builder().s(earthquakeId).build());
        
        GetItemResponse response = getItem(key);
        if (!response.hasItem()) {
            return null;
        }

        return mapToEarthquake(response.item());
    }

    public List<Earthquake> getActiveEarthquakes() {
        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(":isActive", AttributeValue.builder().bool(true).build());

        ScanRequest request = ScanRequest.builder()
            .tableName(EARTHQUAKES_TABLE)
            .filterExpression("isActive = :isActive")
            .expressionAttributeValues(expressionValues)
            .build();

        ScanResponse response = dynamoDbClient.scan(request);
        List<Earthquake> earthquakes = new ArrayList<>();

        for (Map<String, AttributeValue> item : response.items()) {
            earthquakes.add(mapToEarthquake(item));
        }

        return earthquakes;
    }

    public List<Earthquake> getEarthquakesByLocation(double latitude, double longitude, double radiusKm) {
        // Using a simple bounding box for demonstration
        // In production, you'd want to use more sophisticated geospatial queries
        double latDiff = radiusKm / 111.0; // approximate degrees for km at equator
        double lonDiff = radiusKm / (111.0 * Math.cos(Math.toRadians(latitude)));

        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(":minLat", AttributeValue.builder().n(String.valueOf(latitude - latDiff)).build());
        expressionValues.put(":maxLat", AttributeValue.builder().n(String.valueOf(latitude + latDiff)).build());
        expressionValues.put(":minLon", AttributeValue.builder().n(String.valueOf(longitude - lonDiff)).build());
        expressionValues.put(":maxLon", AttributeValue.builder().n(String.valueOf(longitude + lonDiff)).build());
        expressionValues.put(":isActive", AttributeValue.builder().bool(true).build());

        ScanRequest request = ScanRequest.builder()
            .tableName(EARTHQUAKES_TABLE)
            .filterExpression("latitude BETWEEN :minLat AND :maxLat AND longitude BETWEEN :minLon AND :maxLon AND isActive = :isActive")
            .expressionAttributeValues(expressionValues)
            .build();

        ScanResponse response = dynamoDbClient.scan(request);
        List<Earthquake> earthquakes = new ArrayList<>();

        for (Map<String, AttributeValue> item : response.items()) {
            earthquakes.add(mapToEarthquake(item));
        }

        return earthquakes;
    }

    public void deactivateOldEarthquakes() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(SIX_MONTHS_IN_DAYS);
        String cutoffDateStr = cutoffDate.format(DATE_FORMATTER);

        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(":cutoffDate", AttributeValue.builder().s(cutoffDateStr).build());
        expressionValues.put(":isActive", AttributeValue.builder().bool(true).build());

        ScanRequest scanRequest = ScanRequest.builder()
            .tableName(EARTHQUAKES_TABLE)
            .filterExpression("timestamp < :cutoffDate AND isActive = :isActive")
            .expressionAttributeValues(expressionValues)
            .build();

        ScanResponse scanResponse = dynamoDbClient.scan(scanRequest);

        for (Map<String, AttributeValue> item : scanResponse.items()) {
            String earthquakeId = item.get("earthquakeId").s();
            Map<String, AttributeValue> key = new HashMap<>();
            key.put("earthquakeId", AttributeValue.builder().s(earthquakeId).build());

            Map<String, AttributeValue> updateValues = new HashMap<>();
            updateValues.put(":isActive", AttributeValue.builder().bool(false).build());

            UpdateItemRequest updateRequest = UpdateItemRequest.builder()
                .tableName(EARTHQUAKES_TABLE)
                .key(key)
                .updateExpression("SET isActive = :isActive")
                .expressionAttributeValues(updateValues)
                .build();

            dynamoDbClient.updateItem(updateRequest);
        }
    }

    private Earthquake mapToEarthquake(Map<String, AttributeValue> item) {
        Earthquake earthquake = new Earthquake(
            item.get("earthquakeId").s(),
            Double.parseDouble(item.get("magnitude").n()),
            Double.parseDouble(item.get("latitude").n()),
            Double.parseDouble(item.get("longitude").n()),
            Double.parseDouble(item.get("depth").n()),
            item.get("location").s(),
            LocalDateTime.parse(item.get("timestamp").s(), DATE_FORMATTER),
            item.get("source").s()
        );
        earthquake.setActive(item.get("isActive").bool());
        return earthquake;
    }
}
