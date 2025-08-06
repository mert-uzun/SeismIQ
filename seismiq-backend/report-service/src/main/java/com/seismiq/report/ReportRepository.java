package com.seismiq.report;

import com.seismiq.common.model.Report;
import com.seismiq.common.model.User;
import com.seismiq.common.repository.DynamoDBRepository;
import software.amazon.awssdk.services.dynamodb.model.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

public class ReportRepository extends DynamoDBRepository {
    private static final String REPORTS_TABLE = "Reports";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;

    public ReportRepository() {
        super(REPORTS_TABLE);
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
            item.put("category", AttributeValue.builder().s(report.getCategory().name()).build());
        }
        if (report.getDescription() != null) {
            item.put("description", AttributeValue.builder().s(report.getDescription()).build());
        }
        if (report.getLocation() != null) {
            item.put("location", AttributeValue.builder().s(report.getLocation()).build());
        }
        item.put("isCurrentLocation", AttributeValue.builder().bool(report.isCurrentLocation()).build());
        if (report.getStatus() != null) {
            item.put("status", AttributeValue.builder().s(report.getStatus().name()).build());
        }
        if (report.getTimestamp() != null) {
            item.put("timestamp", AttributeValue.builder().s(report.getTimestamp().format(DATE_FORMATTER)).build());
        }
        if (report.getLastUpdated() != null) {
            item.put("lastUpdated", AttributeValue.builder().s(report.getLastUpdated().format(DATE_FORMATTER)).build());
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
            .tableName(REPORTS_TABLE)
            .indexName("UserReportsIndex")
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

    private Report mapToReport(Map<String, AttributeValue> item) {
        // Create a user from the nested map first
        User user = null;
        if (item.containsKey("user")) {
            Map<String, AttributeValue> userMap = item.get("user").m();
            user = new User();
            user.setUserId(userMap.get("userId").s());
            user.setName(userMap.get("name").s());
            if (userMap.containsKey("address")) {
                user.setAddress(userMap.get("address").s());
            }
            user.setVolunteer(userMap.get("isVolunteer").bool());
            user.setSocialWorker(userMap.get("isSocialWorker").bool());
        }

        // Create the report with all required fields
        Report report = new Report(
            item.get("reportId").s(),
            user,
            item.containsKey("category") ? Report.ReportCategory.valueOf(item.get("category").s()) : null,
            item.containsKey("description") ? item.get("description").s() : null,
            item.containsKey("location") ? item.get("location").s() : null,
            item.containsKey("isCurrentLocation") ? item.get("isCurrentLocation").bool() : false,
            item.containsKey("status") ? Report.ReportStatus.valueOf(item.get("status").s()) : Report.ReportStatus.PENDING,
            LocalDateTime.parse(item.get("timestamp").s(), DATE_FORMATTER)
        );

        return report;
    }
}