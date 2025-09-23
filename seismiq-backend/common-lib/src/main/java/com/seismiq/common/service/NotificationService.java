package com.seismiq.common.service;

import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.*;
import com.seismiq.common.model.Landmark;
import com.seismiq.common.model.Report;
import com.seismiq.common.model.User;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import com.google.gson.Gson;
import java.util.logging.Logger;
import java.util.logging.Level;
import software.amazon.awssdk.regions.Region;

/**
 * Service for managing location-based notifications for landmarks
 * using Amazon SNS for Android devices.
 *
 * @author Sıla Bozkurt
 */
public class NotificationService {
    private static final Logger LOGGER = Logger.getLogger(NotificationService.class.getName());
    private final SnsClient snsClient;
    private final String platformApplicationArn;
    private static final double NOTIFICATION_RADIUS_KM = 1.5;
    private static final int MAX_RETRIES = 3;
    private final Gson gson;

    public NotificationService() {
        Region region = Region.of(System.getenv("AWS_REGION") != null ? 
            System.getenv("AWS_REGION") : "eu-north-1");
        
        // Build client with proper configuration
        this.snsClient = SnsClient.builder()
            .region(region)
            .build();
            
        // Get platform ARN from environment or use default
        this.platformApplicationArn = System.getenv("ANDROID_PLATFORM_APPLICATION_ARN") != null ? 
            System.getenv("ANDROID_PLATFORM_APPLICATION_ARN") : 
            "arn:aws:sns:eu-north-1:account-id:app/GCM/SeismIQApp";
            
        this.gson = new Gson();
    }

    // Constructor for testing with dependency injection
    public NotificationService(SnsClient snsClient, String platformApplicationArn) {
        this.snsClient = snsClient;
        this.platformApplicationArn = platformApplicationArn;
        this.gson = new Gson();
    }

    /**
     * Sends notifications to Android users near a newly created landmark
     * 
     * @param landmark The landmark that was created
     * @param nearbyUsers List of users within notification radius
     */
    public void notifyNearbyUsers(Landmark landmark, List<User> nearbyUsers) {
        if (landmark == null) {
            LOGGER.warning("Cannot send notification for null landmark");
            return;
        }
        
        if (nearbyUsers == null || nearbyUsers.isEmpty()) {
            LOGGER.info("No nearby users to notify about new landmark");
            return;
        }
        LOGGER.info("Notifying " + nearbyUsers.size() + " users about new landmark: " + landmark.getName());
        
        // Create the Android FCM message
        String message = createAndroidMessage(landmark);
        
        for (User user : nearbyUsers) {
            if (user != null && user.getDeviceToken() != null && !user.getDeviceToken().isEmpty()) {
                sendNotificationWithRetry(user.getDeviceToken(), message, 0);
            }
        }
    }

    /**
     * Creates an Android FCM notification message
     */
    private String createAndroidMessage(Landmark landmark) {
        // Get additional information from the report
        String additionalInfo = getAdditionalInfoFromReport(landmark);
        
        // Create message wrapper (SNS expects this specific structure for FCM)
        Map<String, String> wrapper = new HashMap<>();
        
        // Default message for platforms that don't support specific formats
        wrapper.put("default", "New landmark alert: " + landmark.getName());
        
        // For FCM format - prepare the data structure
        Map<String, Object> fcmPayload = new HashMap<>();
        
        // Notification part (visible to user)
        Map<String, Object> notification = new HashMap<>();
        notification.put("title", "New Landmark Alert");
        notification.put("body", String.format(
            "New %s landmark created: %s%s",
            landmark.getCategory(),
            landmark.getName(),
            additionalInfo
        ));
        notification.put("android_channel_id", "seismiq_landmarks");
        notification.put("icon", "ic_notification");
        notification.put("sound", "default");
        fcmPayload.put("notification", notification);
        
        // Data part (for app processing)
        Map<String, String> data = new HashMap<>();
        data.put("landmarkId", landmark.getLandmarkId());
        data.put("name", landmark.getName());
        data.put("location", landmark.getLocation());
        data.put("category", landmark.getCategory().toString());
        data.put("latitude", String.valueOf(landmark.getLatitude()));
        data.put("longitude", String.valueOf(landmark.getLongitude()));
        
        // Add additional info as a separate field
        if (!additionalInfo.isEmpty()) {
            data.put("additionalInfo", additionalInfo.substring(2)); // Remove ": " prefix
        }
        fcmPayload.put("data", data);
        
        // Set high priority for emergency-related notifications
        fcmPayload.put("priority", "high");
        fcmPayload.put("time_to_live", 86400);
        
        // SNS expects GCM/FCM messages to be wrapped as a JSON string
        wrapper.put("GCM", gson.toJson(fcmPayload));
        
        return gson.toJson(wrapper);
    }

    /**
     * Extract additional information from the associated report
     * 
     * @param landmark The landmark with associated report
     * @return A string with the additional information, or empty string if none
     */
    private String getAdditionalInfoFromReport(Landmark landmark) {
        try {
            if (landmark.getAssociatedReport() != null) {
                Report report = landmark.getAssociatedReport();
                if (report.getAdditionalInfo() != null && !report.getAdditionalInfo().trim().isEmpty()) {
                    return ": " + report.getAdditionalInfo();
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error getting additional info: {0}", e.getMessage());
        }
        return "";
    }

    /**
     * Sends a notification to a single Android device with retry logic
     */
    private void sendNotificationWithRetry(String deviceToken, String message, int attempt) {
        if (attempt > MAX_RETRIES) {
            LOGGER.log(Level.WARNING, "Max retries reached for deviceToken={0}", deviceToken);
            return;
        }
        
        try {
            // First ensure endpoint exists for this device token
            String endpointArn = getOrCreateEndpointArn(deviceToken);
            if (endpointArn == null) {
                LOGGER.log(Level.SEVERE, "Failed to create endpoint for device token: {0}", deviceToken);
                return;
            }
            
            // Publish the message to the endpoint
            PublishRequest request = PublishRequest.builder()
                .message(message)
                .messageStructure("json")  // Important: tells SNS this is a JSON structure
                .targetArn(endpointArn)
                .build();
            
            snsClient.publish(request);
            LOGGER.info("Notification sent to Android device: " + deviceToken);
        } catch (SnsException e) {
            LOGGER.log(Level.SEVERE, "Error publishing notification to " + deviceToken + ": " + e.getMessage());
            
            // Handle common SNS errors
            if (e.awsErrorDetails() != null && 
                e.awsErrorDetails().errorCode() != null && 
                e.awsErrorDetails().errorCode().equals("EndpointDisabled")) {
                LOGGER.info("Endpoint disabled for device token: " + deviceToken);
                return;
            }
            
            // Exponential backoff strategy for retry
            try {
                Thread.sleep((long) Math.pow(2, attempt) * 1000);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
            sendNotificationWithRetry(deviceToken, message, attempt + 1);
        }
    }
    
    /**
     * Gets or creates an SNS endpoint for the device token
     */
    private String getOrCreateEndpointArn(String deviceToken) {
        try {
            // Create a platform endpoint for this device
            CreatePlatformEndpointRequest endpointRequest = CreatePlatformEndpointRequest.builder()
                .platformApplicationArn(platformApplicationArn)
                .token(deviceToken)
                .build();
            
            CreatePlatformEndpointResponse response = snsClient.createPlatformEndpoint(endpointRequest);
            return response.endpointArn();
        } catch (SnsException e) {
            LOGGER.log(Level.SEVERE, "Error creating platform endpoint: " + e.getMessage());
            return null;
        }
    }

    /**
     * Calculates if a user is within the notification radius of a landmark
     */
    public boolean isWithinRadius(double userLat, double userLon, 
                                double landmarkLat, double landmarkLon) {
        // Haversine formula for distance calculation
        double earthRadius = 6371; // km
        double dLat = Math.toRadians(landmarkLat - userLat);
        double dLon = Math.toRadians(landmarkLon - userLon);
        
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                   Math.cos(Math.toRadians(userLat)) * Math.cos(Math.toRadians(landmarkLat)) *
                   Math.sin(dLon/2) * Math.sin(dLon/2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        double distance = earthRadius * c;
        
        return distance <= NOTIFICATION_RADIUS_KM;
    }
    
    /**
     * Find nearby users for a given landmark
     * 
     * @param landmark The landmark to find nearby users for
     * @param allUsers List of all users to check
     * @return List of users within notification radius
     */
    public List<User> findNearbyUsers(Landmark landmark, List<User> allUsers) {
        if (landmark == null || allUsers == null) {
            return Collections.emptyList();
        }
        
        return allUsers.stream()
            .filter(user -> user != null && user.getLatitude() != 0.0 && user.getLongitude() != 0.0)
            .filter(user -> isWithinRadius(
                user.getLatitude(), 
                user.getLongitude(),
                landmark.getLatitude(), 
                landmark.getLongitude()))
            .toList();
    }
}