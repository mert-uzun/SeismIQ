package com.seismiq.report;

import com.seismiq.common.model.Report;
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
        item.put("userId", AttributeValue.builder().s(report.getUserId()).build());
        item.put("description", AttributeValue.builder().s(report.getDescription()).build());
        item.put("location", AttributeValue.builder().s(report.getLocation()).build());
        item.put("status", AttributeValue.builder().s(report.getStatus()).build());
        item.put("timestamp", AttributeValue.builder().s(report.getTimestamp().format(DATE_FORMATTER)).build());
        
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
        Report report = new Report();
        report.setReportId(item.get("reportId").s());
        report.setUserId(item.get("userId").s());
        report.setDescription(item.get("description").s());
        report.setLocation(item.get("location").s());
        report.setStatus(item.get("status").s());
        report.setTimestamp(LocalDateTime.parse(item.get("timestamp").s(), DATE_FORMATTER));
        return report;
    }
}