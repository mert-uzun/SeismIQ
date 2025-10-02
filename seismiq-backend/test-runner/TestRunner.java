package com.seismiq.test;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.seismiq.earthquake.EarthquakeHandler;
import com.seismiq.user.UserHandler;

/**
 * Simple test runner to test Lambda functions locally without Docker
 */
public class TestRunner {
    
    public static void main(String[] args) {
        testUserService();
        testEarthquakeService();
        // Add more tests as needed
    }
    
    private static void testUserService() {
        System.out.println("Testing User Service...");
        
        UserHandler handler = new UserHandler();
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setPath("/users");
        request.setHttpMethod("GET");
        
        try {
            APIGatewayProxyResponseEvent response = handler.handleRequest(request, null);
            System.out.println("User Service Response: " + response.getStatusCode());
        } catch (Exception e) {
            System.err.println("User Service Error: " + e.getMessage());
        }
    }
    
    private static void testEarthquakeService() {
        System.out.println("Testing Earthquake Service...");
        
        EarthquakeHandler handler = new EarthquakeHandler();
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setPath("/earthquakes");
        request.setHttpMethod("GET");
        
        try {
            APIGatewayProxyResponseEvent response = handler.handleRequest(request, null);
            System.out.println("Earthquake Service Response: " + response.getStatusCode());
        } catch (Exception e) {
            System.err.println("Earthquake Service Error: " + e.getMessage());
        }
    }
}
