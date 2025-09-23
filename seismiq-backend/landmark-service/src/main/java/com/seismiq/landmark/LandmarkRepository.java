package com.seismiq.landmark;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.seismiq.common.model.Landmark;
import com.seismiq.common.model.LandmarkCategory;
import ch.hsr.geohash.GeoHash;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;

/**
 * Repository class for managing Landmark data in DynamoDB.
 *
 * @author Sıla Bozkurt
 */
public class LandmarkRepository {
    private final DynamoDbClient dynamoDbClient;
    private final String tableName;

    public LandmarkRepository() {
        this.dynamoDbClient = DynamoDbClient.builder().build();
        this.tableName = System.getenv("LANDMARKS_TABLE") != null ? 
            System.getenv("LANDMARKS_TABLE") : "Landmarks";
    }

    public void saveLandmark(Landmark landmark) {
        Map<String, AttributeValue> item = new HashMap<>();
        // Convert Landmark to DynamoDB item
        item.put("landmarkId", AttributeValue.builder().s(landmark.getLandmarkId()).build());
        item.put("name", AttributeValue.builder().s(landmark.getName()).build());
        item.put("location", AttributeValue.builder().s(landmark.getLocation()).build());
        item.put("category", AttributeValue.builder().s(landmark.getCategory().name()).build());
        
        // Handle null description safely
        if (landmark.getDescription() != null) {
            item.put("description", AttributeValue.builder().s(landmark.getDescription()).build());
        } else {
            item.put("description", AttributeValue.builder().s("").build());
        }
        
        item.put("createdAt", AttributeValue.builder().s(landmark.getCreatedAt().toString()).build());
        item.put("lastUpdated", AttributeValue.builder().s(landmark.getLastUpdated().toString()).build());
        item.put("isActive", AttributeValue.builder().bool(landmark.isActive()).build());
        item.put("createdBy", AttributeValue.builder().s(landmark.getCreatedBy()).build());
        
        // Add associated report if exists
        if (landmark.getAssociatedReport() != null) {
            item.put("associatedReportId", AttributeValue.builder()
                .s(landmark.getAssociatedReport().getReportId())
                .build());
        }

        // Add latitude and longitude
        item.put("latitude", AttributeValue.builder().n(String.valueOf(landmark.getLatitude())).build());
        item.put("longitude", AttributeValue.builder().n(String.valueOf(landmark.getLongitude())).build());
        
        // Add geohash for location-based queries
        String geohash = GeoHash.withCharacterPrecision(
            landmark.getLatitude(), 
            landmark.getLongitude(), 
            12
        ).toBase32();
        item.put("geohash", AttributeValue.builder().s(geohash).build());
        
        PutItemRequest request = PutItemRequest.builder()
            .tableName(tableName)
            .item(item)
            .build();

        dynamoDbClient.putItem(request);
    }

    public Landmark getLandmark(String landmarkId) {
        GetItemRequest request = GetItemRequest.builder()
            .tableName(tableName)
            .key(Map.of("landmarkId", AttributeValue.builder().s(landmarkId).build()))
            .build();

        GetItemResponse response = dynamoDbClient.getItem(request);
        if (!response.hasItem()) {
            throw new RuntimeException("Landmark not found");
        }

        return mapToLandmark(response.item());
    }

    public List<Landmark> listLandmarks(Map<String, String> queryParams) {
        ScanRequest.Builder scanBuilder = ScanRequest.builder().tableName(tableName);
        
        if (queryParams != null && queryParams.containsKey("category")) {
            scanBuilder.filterExpression("category = :category")
                .expressionAttributeValues(Map.of(
                    ":category", AttributeValue.builder().s(queryParams.get("category")).build()
                ));
        }

        ScanResponse response = dynamoDbClient.scan(scanBuilder.build());
        List<Landmark> landmarks = new ArrayList<>();
        
        for (Map<String, AttributeValue> item : response.items()) {
            landmarks.add(mapToLandmark(item));
        }

        return landmarks;
    }

    public void updateLandmark(Landmark landmark) {
        // Verify landmark exists
        getLandmark(landmark.getLandmarkId());
        saveLandmark(landmark);
    }

    public void deleteLandmark(String landmarkId) {
        DeleteItemRequest request = DeleteItemRequest.builder()
            .tableName(tableName)
            .key(Map.of("landmarkId", AttributeValue.builder().s(landmarkId).build()))
            .build();

        dynamoDbClient.deleteItem(request);
    }

    public List<Landmark> findLandmarksByCategory(String category) {
        QueryRequest request = QueryRequest.builder()
            .tableName(tableName)
            .indexName("CategoryIndex")
            .keyConditionExpression("category = :category")
            .expressionAttributeValues(Map.of(
                ":category", AttributeValue.builder().s(category).build()
            ))
            .build();

        QueryResponse response = dynamoDbClient.query(request);
        return response.items().stream()
            .map(this::mapToLandmark)
            .collect(Collectors.toList());
    }

    public List<Landmark> findLandmarksNearLocation(double latitude, double longitude, double radiusKm) {
        // Convert location to geohash with precision based on radius
        int precision = getGeohashPrecision(radiusKm);
        String geohash = GeoHash.withCharacterPrecision(latitude, longitude, precision).toBase32();
        String geohashPrefix = geohash.substring(0, precision - 2); // Use a slightly wider area

        QueryRequest request = QueryRequest.builder()
            .tableName(tableName)
            .indexName("LocationIndex")
            .keyConditionExpression("begins_with(geohash, :prefix)")
            .expressionAttributeValues(Map.of(
                ":prefix", AttributeValue.builder().s(geohashPrefix).build()
            ))
            .build();

        QueryResponse response = dynamoDbClient.query(request);
        return response.items().stream()
            .map(this::mapToLandmark)
            .filter(landmark -> isWithinRadius(
                latitude, longitude,
                landmark.getLatitude(), landmark.getLongitude(),
                radiusKm
            ))
            .collect(Collectors.toList());
    }

    public List<Landmark> findLandmarksByReport(String reportId) {
        QueryRequest request = QueryRequest.builder()
            .tableName(tableName)
            .indexName("ReportIndex")
            .keyConditionExpression("reportId = :reportId")
            .expressionAttributeValues(Map.of(
                ":reportId", AttributeValue.builder().s(reportId).build()
            ))
            .build();

        QueryResponse response = dynamoDbClient.query(request);
        return response.items().stream()
            .map(this::mapToLandmark)
            .collect(Collectors.toList());
    }

    private Landmark mapToLandmark(Map<String, AttributeValue> item) {
        double latitude = Double.parseDouble(item.get("latitude").n());
        double longitude = Double.parseDouble(item.get("longitude").n());
        
        Landmark landmark = new Landmark(
            item.get("landmarkId").s(),
            item.get("name").s(),
            item.get("location").s(),
            LandmarkCategory.valueOf(item.get("category").s()),
            null, // Associated report will be set below if exists
            item.get("createdBy").s()
        );
        
        landmark.setLatitude(latitude);
        landmark.setLongitude(longitude);
        
        // Only set description if it exists
        if (item.containsKey("description")) {
            landmark.setDescription(item.get("description").s());
        }
        
        landmark.setActive(item.get("isActive").bool());
        landmark.setLastUpdated(LocalDateTime.parse(item.get("lastUpdated").s()));
        
        // Set associated report if exists
        if (item.containsKey("associatedReportId")) {
            // TODO: Implement report retrieval from Report service
            // This would require cross-service communication
        }
        
        return landmark;
    }

    private int getGeohashPrecision(double radiusKm) {
        // Return appropriate geohash precision based on radius
        if (radiusKm <= 0.5) return 7;      // ~76m
        if (radiusKm <= 2.0) return 6;      // ~610m
        if (radiusKm <= 7.0) return 5;      // ~2.4km
        if (radiusKm <= 30.0) return 4;     // ~20km
        return 3;                           // ~78km
    }

    private boolean isWithinRadius(double lat1, double lon1, 
                                 double lat2, double lon2, 
                                 double radiusKm) {
        // Haversine formula implementation
        double earthRadius = 6371.0; // km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon/2) * Math.sin(dLon/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return earthRadius * c <= radiusKm;
    }
}