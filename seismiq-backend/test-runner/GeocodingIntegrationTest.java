package com.seismiq.test;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import com.seismiq.report.ReportHandler;

import java.util.HashMap;
import java.util.Map;

/**
 * Test class to verify geocoding integration in report creation
 */
public class GeocodingIntegrationTest {
    
    public static void main(String[] args) {
        testReportWithCoordinates();
        testReportWithCityProvince();
        testReportWithMissingLocation();
    }
    
    private static void testReportWithCoordinates() {
        System.out.println("=== Testing Report Creation with Coordinates ===");
        
        ReportHandler handler = new ReportHandler();
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setPath("/reports");
        request.setHttpMethod("POST");
        
        // Create test data with coordinates (Istanbul coordinates)
        Map<String, Object> reportData = new HashMap<>();
        reportData.put("categoryType", "MEDICAL_HELP");
        reportData.put("description", "Need medical assistance");
        reportData.put("location", "Near Taksim Square");
        reportData.put("currentLocation", true);
        reportData.put("latitude", 41.0082);
        reportData.put("longitude", 28.9784);
        
        Map<String, Object> userData = new HashMap<>();
        userData.put("userId", "user123");
        userData.put("name", "Test User");
        userData.put("address", "Test Address");
        userData.put("isVolunteer", false);
        userData.put("isSocialWorker", false);
        reportData.put("user", userData);
        
        Gson gson = new Gson();
        request.setBody(gson.toJson(reportData));
        
        try {
            APIGatewayProxyResponseEvent response = handler.handleRequest(request, null);
            System.out.println("Response Status: " + response.getStatusCode());
            System.out.println("Response Body: " + response.getBody());
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println();
    }
    
    private static void testReportWithCityProvince() {
        System.out.println("=== Testing Report Creation with City/Province ===");
        
        ReportHandler handler = new ReportHandler();
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setPath("/reports");
        request.setHttpMethod("POST");
        
        // Create test data with city/province
        Map<String, Object> reportData = new HashMap<>();
        reportData.put("categoryType", "SHELTER");
        reportData.put("description", "Need temporary shelter");
        reportData.put("location", "City center");
        reportData.put("currentLocation", false);
        reportData.put("city", "Ankara");
        reportData.put("province", "Ankara");
        
        Map<String, Object> userData = new HashMap<>();
        userData.put("userId", "user456");
        userData.put("name", "Test User 2");
        userData.put("address", "Test Address 2");
        userData.put("isVolunteer", false);
        userData.put("isSocialWorker", true);
        reportData.put("user", userData);
        
        Gson gson = new Gson();
        request.setBody(gson.toJson(reportData));
        
        try {
            APIGatewayProxyResponseEvent response = handler.handleRequest(request, null);
            System.out.println("Response Status: " + response.getStatusCode());
            System.out.println("Response Body: " + response.getBody());
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println();
    }
    
    private static void testReportWithMissingLocation() {
        System.out.println("=== Testing Report Creation with Missing Location ===");
        
        ReportHandler handler = new ReportHandler();
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setPath("/reports");
        request.setHttpMethod("POST");
        
        // Create test data without coordinates or city/province
        Map<String, Object> reportData = new HashMap<>();
        reportData.put("categoryType", "FOOD_WATER");
        reportData.put("description", "Need food and water");
        reportData.put("location", "Somewhere");
        reportData.put("currentLocation", true);
        
        Map<String, Object> userData = new HashMap<>();
        userData.put("userId", "user789");
        userData.put("name", "Test User 3");
        userData.put("address", "Test Address 3");
        userData.put("isVolunteer", true);
        userData.put("isSocialWorker", false);
        reportData.put("user", userData);
        
        Gson gson = new Gson();
        request.setBody(gson.toJson(reportData));
        
        try {
            APIGatewayProxyResponseEvent response = handler.handleRequest(request, null);
            System.out.println("Response Status: " + response.getStatusCode());
            System.out.println("Response Body: " + response.getBody());
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println();
    }
}