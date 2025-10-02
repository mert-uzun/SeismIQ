package com.seismiq.common.repository;

import com.seismiq.common.model.Report;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ReportRepository {

    private final DynamoDbTable<Report> reportTable;

    public ReportRepository(DynamoDbEnhancedClient enhancedClient, String tableName) {
        this.reportTable = enhancedClient.table(tableName, TableSchema.fromBean(Report.class));
    }

    public void save(Report report) {
        reportTable.putItem(report);
    }

    public Optional<Report> findById(String reportId) {
        Key key = Key.builder().partitionValue(reportId).build();
        return Optional.ofNullable(reportTable.getItem(key));
    }

    public List<Report> findAll() {
        return reportTable.scan().items().stream().toList();
    }

    public List<Report> findByStatus(Report.ReportStatus status) {
        // For simplicity in the test class, we'll manually filter after getting all reports
        List<Report> allReports = findAll();
        List<Report> filteredReports = new ArrayList<>();
        
        for (Report report : allReports) {
            if (report.getStatus() == status) {
                filteredReports.add(report);
            }
        }
        
        return filteredReports;
    }
    
    public List<Report> findByLocationRadius(double latitude, double longitude, double radiusInKm) {
        // This is a simplified approach. In a real implementation, you would:
        // 1. Calculate the bounding box for the radius
        // 2. Use GSI to efficiently query by location
        // 3. Filter the results to only include reports within the radius
        
        List<Report> allReports = findAll();
        List<Report> reportsInRadius = new ArrayList<>();
        
        for (Report report : allReports) {
            if (calculateDistance(latitude, longitude, report.getLatitude(), report.getLongitude()) <= radiusInKm) {
                reportsInRadius.add(report);
            }
        }
        
        return reportsInRadius;
    }
    
    // Haversine formula to calculate distance between two points on Earth
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Earth radius in kilometers
        
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return R * c;
    }
}
