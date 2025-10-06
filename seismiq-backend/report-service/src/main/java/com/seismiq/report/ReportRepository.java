package com.seismiq.report;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.seismiq.common.model.Category;
import com.seismiq.common.model.Report;
import com.seismiq.common.model.User;
import com.seismiq.common.repository.DynamoDBRepository;

import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import software.amazon.awssdk.services.dynamodb.model.ReturnValue;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemResponse;

public class ReportRepository extends DynamoDBRepository {
    private static final String USER_REPORTS_INDEX = "UserReportsIndex";
    private static final String CATEGORY_STATUS_INDEX = "CategoryStatusIndex";
    private static final String TIMESTAMP_INDEX = "TimestampIndex";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;

    public ReportRepository() {
        super("seismiq-Reports"); // Use the service-specific Reports table
    }

    public void saveReport(Report report) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("reportId", AttributeValue.builder().s(report.getReportId()).build());
        
        // Save user details as a nested map
        Map<String, AttributeValue> userMap = new HashMap<>();
        User user = report.getUser();
        if (user != null) {
            item.put("userId", AttributeValue.builder().s(user.getUserId()).build());
            userMap.put("userId", AttributeValue.builder().s(user.getUserId()).build());
            userMap.put("name", AttributeValue.builder().s(user.getName()).build());
            if (user.getAddress() != null) {
                userMap.put("address", AttributeValue.builder().s(user.getAddress()).build());
            }
            userMap.put("isVolunteer", AttributeValue.builder().bool(user.isVolunteer()).build());
            userMap.put("isSocialWorker", AttributeValue.builder().bool(user.isSocialWorker()).build());
            item.put("user", AttributeValue.builder().m(userMap).build());
        }
        
        if (report.getCategory() != null) {
            Map<String, AttributeValue> categoryMap = Map.of(
                "categoryID", AttributeValue.builder().s(report.getCategory().getCategoryID()).build(),
                "categoryType", AttributeValue.builder().s(report.getCategory().getCategoryType()).build()
            );
            item.put("category", AttributeValue.builder().m(categoryMap).build());
        }
        if (report.getDescription() != null) {
            item.put("description", AttributeValue.builder().s(report.getDescription()).build());
        }
        if (report.getLocation() != null) {
            item.put("location", AttributeValue.builder().s(report.getLocation()).build());
        }
        item.put("isCurrentLocation", AttributeValue.builder().bool(report.isCurrentLocation()).build());
        
        // Add location coordinates and description
        item.put("latitude", AttributeValue.builder().n(String.valueOf(report.getLatitude())).build());
        item.put("longitude", AttributeValue.builder().n(String.valueOf(report.getLongitude())).build());
        if (report.getLocationDescription() != null) {
            item.put("locationDescription", AttributeValue.builder().s(report.getLocationDescription()).build());
        }
        
        if (report.getStatus() != null) {
            item.put("status", AttributeValue.builder().s(report.getStatus().name()).build());
        }
        if (report.getTimestamp() != null) {
            item.put("timestamp", AttributeValue.builder().s(report.getTimestamp().format(DATE_FORMATTER)).build());
        }
        if (report.getLastUpdated() != null) {
            item.put("lastUpdated", AttributeValue.builder().s(report.getLastUpdated().format(DATE_FORMATTER)).build());
        }
        if (report.getCity() != null) {
            item.put("city", AttributeValue.builder().s(report.getCity()).build());
        }
        if (report.getProvince() != null) {
            item.put("province", AttributeValue.builder().s(report.getProvince()).build());
        }
        
        putItem(item);
    }

    public Report getReport(String reportId) {
        Map<String, AttributeValue> key = new HashMap<>();
        key.put("reportId", AttributeValue.builder().s(reportId).build());
        
        GetItemResponse response = getItem(key);
        if (!response.hasItem()) {
            return null;
        }

        return mapToReport(response.item());
    }

    public List<Report> getReportsByUser(String userId) {
        QueryRequest request = QueryRequest.builder()
            .tableName(this.tableName)
            .indexName(USER_REPORTS_INDEX)
            .keyConditionExpression("userId = :userId")
            .expressionAttributeValues(Map.of(":userId", AttributeValue.builder().s(userId).build()))
            .build();

        QueryResponse response = dynamoDbClient.query(request);
        List<Report> reports = new ArrayList<>();
        
        for (Map<String, AttributeValue> item : response.items()) {
            reports.add(mapToReport(item));
        }
        return reports;
    }

    public Report createReport(Report report) {
        saveReport(report);
        return report;
    }

    public Report updateReportStatus(String reportId, Report.ReportStatus status) {
        Map<String, AttributeValue> key = new HashMap<>();
        key.put("reportId", AttributeValue.builder().s(reportId).build());

        Map<String, AttributeValue> attributeValues = new HashMap<>();
        attributeValues.put(":status", AttributeValue.builder().s(status.name()).build());
        attributeValues.put(":lastUpdated", AttributeValue.builder().s(LocalDateTime.now().format(DATE_FORMATTER)).build());

        UpdateItemRequest request = UpdateItemRequest.builder()
            .tableName(this.tableName)
            .key(key)
            .updateExpression("SET status = :status, lastUpdated = :lastUpdated")
            .expressionAttributeValues(attributeValues)
            .returnValues(ReturnValue.ALL_NEW)
            .build();

        UpdateItemResponse response = dynamoDbClient.updateItem(request);
        return mapToReport(response.attributes());
    }

    public void deleteReport(String reportId) {
        Map<String, AttributeValue> key = new HashMap<>();
        key.put("reportId", AttributeValue.builder().s(reportId).build());

        DeleteItemRequest request = DeleteItemRequest.builder()
            .tableName(this.tableName)
            .key(key)
            .build();

        dynamoDbClient.deleteItem(request);
    }

    public List<Report> getAllReports() {
        ScanRequest scanRequest = ScanRequest.builder()
            .tableName(this.tableName)
            .build();

        ScanResponse response = dynamoDbClient.scan(scanRequest);
        List<Report> reports = new ArrayList<>();

        for (Map<String, AttributeValue> item : response.items()) {
            reports.add(mapToReport(item));
        }

        return reports;
    }

    public List<Report> getReportsByCategory(String category) {
        QueryRequest request = QueryRequest.builder()
            .tableName(this.tableName)
            .indexName(CATEGORY_STATUS_INDEX)
            .keyConditionExpression("category.type = :categoryType")
            .expressionAttributeValues(Map.of(":categoryType", AttributeValue.builder().s(category).build()))
            .build();

        QueryResponse response = dynamoDbClient.query(request);
        List<Report> reports = new ArrayList<>();

        for (Map<String, AttributeValue> item : response.items()) {
            reports.add(mapToReport(item));
        }

        return reports;
    }

    public List<Report> getReportsByStatus(Report.ReportStatus status) {
        QueryRequest request = QueryRequest.builder()
            .tableName(this.tableName)
            .indexName(CATEGORY_STATUS_INDEX)
            .keyConditionExpression("status = :status")
            .expressionAttributeValues(Map.of(":status", AttributeValue.builder().s(status.name()).build()))
            .build();

        QueryResponse response = dynamoDbClient.query(request);
        List<Report> reports = new ArrayList<>();

        for (Map<String, AttributeValue> item : response.items()) {
            reports.add(mapToReport(item));
        }

        return reports;
    }

    public List<Report> getReportsByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(":startTime", AttributeValue.builder().s(startTime.format(DATE_FORMATTER)).build());
        expressionValues.put(":endTime", AttributeValue.builder().s(endTime.format(DATE_FORMATTER)).build());

        QueryRequest request = QueryRequest.builder()
            .tableName(this.tableName)
            .indexName(TIMESTAMP_INDEX)
            .keyConditionExpression("timestamp BETWEEN :startTime AND :endTime")
            .expressionAttributeValues(expressionValues)
            .build();

        QueryResponse response = dynamoDbClient.query(request);
        List<Report> reports = new ArrayList<>();

        for (Map<String, AttributeValue> item : response.items()) {
            reports.add(mapToReport(item));
        }
        
        return reports;
    }

    public List<Report> getReportsByCity(String city) {
        ScanRequest request = ScanRequest.builder()
            .tableName(this.tableName)
            .filterExpression("city = :city")
            .expressionAttributeValues(Map.of(":city", AttributeValue.builder().s(city).build()))
            .build();

        ScanResponse response = dynamoDbClient.scan(request);
        List<Report> reports = new ArrayList<>();

        for (Map<String, AttributeValue> item : response.items()) {
            reports.add(mapToReport(item));
        }

        return reports;
    }

    public List<Report> getReportsByProvince(String province) {
        ScanRequest request = ScanRequest.builder()
            .tableName(this.tableName)
            .filterExpression("province = :province")
            .expressionAttributeValues(Map.of(":province", AttributeValue.builder().s(province).build()))
            .build();

        ScanResponse response = dynamoDbClient.scan(request);
        List<Report> reports = new ArrayList<>();

        for (Map<String, AttributeValue> item : response.items()) {
            reports.add(mapToReport(item));
        }

        return reports;
    }

    public Report updateReportLocation(String reportId, double latitude, double longitude, String description) {
        Map<String, AttributeValue> key = new HashMap<>();
        key.put("reportId", AttributeValue.builder().s(reportId).build());

        Map<String, AttributeValue> values = new HashMap<>();
        values.put(":latitude", AttributeValue.builder().n(String.valueOf(latitude)).build());
        values.put(":longitude", AttributeValue.builder().n(String.valueOf(longitude)).build());
        values.put(":lastUpdated", AttributeValue.builder().s(LocalDateTime.now().format(DATE_FORMATTER)).build());

        StringBuilder updateExpression = new StringBuilder("SET latitude = :latitude, longitude = :longitude, lastUpdated = :lastUpdated");
        
        if (description != null) {
            values.put(":description", AttributeValue.builder().s(description).build());
            updateExpression.append(", locationDescription = :description");
        }

        UpdateItemRequest request = UpdateItemRequest.builder()
            .tableName(this.tableName)
            .key(key)
            .updateExpression(updateExpression.toString())
            .expressionAttributeValues(values)
            .returnValues(ReturnValue.ALL_NEW)
            .build();

        UpdateItemResponse response = dynamoDbClient.updateItem(request);
        return mapToReport(response.attributes());
    }

    private Report mapToReport(Map<String, AttributeValue> item) {
        // Create a user from the nested map first
        User user = null;
        if (item.containsKey("user") && item.get("user").m() != null) {
            Map<String, AttributeValue> userMap = item.get("user").m();
            user = new User();
            if (userMap.containsKey("userId") && userMap.get("userId") != null) {
                user.setUserId(userMap.get("userId").s());
            }
            if (userMap.containsKey("name") && userMap.get("name") != null) {
                user.setName(userMap.get("name").s());
            }
            if (userMap.containsKey("address") && userMap.get("address") != null) {
                user.setAddress(userMap.get("address").s());
            }
            if (userMap.containsKey("isVolunteer") && userMap.get("isVolunteer") != null) {
                user.setVolunteer(userMap.get("isVolunteer").bool());
            }
            if (userMap.containsKey("isSocialWorker") && userMap.get("isSocialWorker") != null) {
                user.setSocialWorker(userMap.get("isSocialWorker").bool());
            }
        }

        Category category = null;
        if(item.containsKey("category") && item.get("category").m() != null){
            Map<String, AttributeValue> categoryMap = item.get("category").m();
            if (categoryMap.containsKey("categoryID") && categoryMap.get("categoryID") != null &&
                categoryMap.containsKey("categoryType") && categoryMap.get("categoryType") != null) {
                category = new Category(
                    categoryMap.get("categoryID").s(),
                    categoryMap.get("categoryType").s()
                );
            }
        }

        // Create the report with all required fields
        LocalDateTime timestamp = LocalDateTime.now(); // Default to now if not present
        if (item.containsKey("timestamp") && item.get("timestamp") != null) {
            try {
                timestamp = LocalDateTime.parse(item.get("timestamp").s(), DATE_FORMATTER);
            } catch (Exception e) {
                // If parsing fails, use current time
                timestamp = LocalDateTime.now();
            }
        }
        
        Report report = new Report(
            item.get("reportId").s(),
            user,
            category,
            item.containsKey("description") && item.get("description") != null ? item.get("description").s() : null,
            item.containsKey("location") && item.get("location") != null ? item.get("location").s() : null,
            item.containsKey("isCurrentLocation") && item.get("isCurrentLocation") != null && item.get("isCurrentLocation").bool(),
            item.containsKey("status") && item.get("status") != null ? Report.ReportStatus.valueOf(item.get("status").s()) : Report.ReportStatus.PENDING,
            timestamp
        );

        // Set location coordinates and description with null safety
        if (item.containsKey("latitude") && item.get("latitude") != null) {
            try {
                report.setLatitude(Double.parseDouble(item.get("latitude").n()));
            } catch (Exception e) {
                // If parsing fails, leave as default 0.0
            }
        }
        if (item.containsKey("longitude") && item.get("longitude") != null) {
            try {
                report.setLongitude(Double.parseDouble(item.get("longitude").n()));
            } catch (Exception e) {
                // If parsing fails, leave as default 0.0
            }
        }
        if (item.containsKey("locationDescription") && item.get("locationDescription") != null) {
            report.setLocationDescription(item.get("locationDescription").s());
        }
        if (item.containsKey("city") && item.get("city") != null) {
            report.setCity(item.get("city").s());
        }
        if (item.containsKey("province") && item.get("province") != null) {
            report.setProvince(item.get("province").s());
        }

        return report;
    }
}