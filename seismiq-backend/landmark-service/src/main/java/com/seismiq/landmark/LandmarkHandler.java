package com.seismiq.landmark;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.seismiq.common.model.Category;
import com.seismiq.common.model.Landmark;
import com.seismiq.common.util.LocalDateTimeAdapter;

/**
 * AWS Lambda handler for processing landmark-related API requests.
 * Handles creation, updates, and retrieval of landmarks in the SeismIQ system.
 *
 * @author Sıla Bozkurt
 */
public class LandmarkHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private final LandmarkRepository landmarkRepository;
    private final Gson gson;

    public LandmarkHandler() {
        this(new LandmarkRepository());
    }

    public LandmarkHandler(LandmarkRepository landmarkRepository) {
        this.landmarkRepository = landmarkRepository;
        this.gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .create();
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        switch (input.getHttpMethod()) {
            case "GET":
                if (input.getPath().matches("/landmarks/[^/]+")) {
                    return getLandmark(input);
                }
                return listLandmarks(input);
            case "POST":
                return createLandmark(input, context);
            case "PUT":
                return updateLandmark(input);
            case "DELETE":
                return deleteLandmark(input);
            default:
                return new APIGatewayProxyResponseEvent()
                    .withStatusCode(405)
                    .withBody("Method not supported");
        }
    }

    private Map<String, String> getCorsHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Access-Control-Allow-Origin", "*");
        headers.put("Access-Control-Allow-Headers", "Content-Type,Authorization");
        headers.put("Access-Control-Allow-Methods", "OPTIONS,GET,PUT,POST,DELETE");
        return headers;
    }

    private APIGatewayProxyResponseEvent getLandmark(APIGatewayProxyRequestEvent input) {
        String landmarkId = input.getPath().split("/")[2];
        try {
            Landmark landmark = landmarkRepository.getLandmark(landmarkId);
            return new APIGatewayProxyResponseEvent()
                .withStatusCode(200)
                .withHeaders(getCorsHeaders())
                .withBody(gson.toJson(landmark));
        } catch (IllegalArgumentException e) {
            return new APIGatewayProxyResponseEvent()
                .withStatusCode(400)
                .withHeaders(getCorsHeaders())
                .withBody("Invalid landmark ID format: " + e.getMessage());
        } catch (RuntimeException e) {
            return new APIGatewayProxyResponseEvent()
                .withStatusCode(404)
                .withHeaders(getCorsHeaders())
                .withBody("Landmark not found: " + e.getMessage());
        }
    }

    private APIGatewayProxyResponseEvent listLandmarks(APIGatewayProxyRequestEvent input) {
        Map<String, String> queryParams = input.getQueryStringParameters();
        try {
            return new APIGatewayProxyResponseEvent()
                .withStatusCode(200)
                .withHeaders(getCorsHeaders())
                .withBody(gson.toJson(landmarkRepository.listLandmarks(queryParams)));
        } catch (IllegalArgumentException e) {
            return new APIGatewayProxyResponseEvent()
                .withStatusCode(400)
                .withHeaders(getCorsHeaders())
                .withBody("Invalid query parameters: " + e.getMessage());
        } catch (RuntimeException e) {
            return new APIGatewayProxyResponseEvent()
                .withStatusCode(500)
                .withHeaders(getCorsHeaders())
                .withBody("Error retrieving landmarks: " + e.getMessage());
        }
    }

    private APIGatewayProxyResponseEvent createLandmark(APIGatewayProxyRequestEvent input, Context context) {
        try {
            if (input.getBody() == null) {
                return new APIGatewayProxyResponseEvent()
                    .withStatusCode(400)
                    .withHeaders(getCorsHeaders())
                    .withBody("Request body is required");
            }

            context.getLogger().log("Parsing landmark from body: " + input.getBody());
            Landmark landmark;
            try {
                landmark = gson.fromJson(input.getBody(), Landmark.class);
                context.getLogger().log("Successfully parsed landmark: " + landmark);
            } catch (com.google.gson.JsonSyntaxException e) {
                context.getLogger().log("JSON syntax error parsing landmark: " + e.getMessage());
                return new APIGatewayProxyResponseEvent()
                    .withStatusCode(400)
                    .withHeaders(getCorsHeaders())
                    .withBody("Invalid JSON format: " + e.getMessage());
            } catch (com.google.gson.JsonParseException e) {
                context.getLogger().log("JSON parsing error: " + e.getMessage());
                return new APIGatewayProxyResponseEvent()
                    .withStatusCode(400)
                    .withHeaders(getCorsHeaders())
                    .withBody("Invalid landmark format: " + e.getMessage());
            }
            
            // Handle category if it's provided as a string
            if (landmark.getCategory() == null) {
                try {
                    // Try to parse category from JSON if it exists as a string
                    @SuppressWarnings("unchecked")
                    Map<String, Object> jsonMap = gson.fromJson(input.getBody(), Map.class);
                    if (jsonMap.containsKey("category") && jsonMap.get("category") instanceof String) {
                        String categoryStr = (String) jsonMap.get("category");
                        try {
                            landmark.setCategory(Category.valueOf(categoryStr));
                            context.getLogger().log("Converted category string to Category object: " + categoryStr);
                        } catch (Exception ex) {
                            context.getLogger().log("Could not convert category string: " + ex.getMessage());
                            // Default to OTHER
                            landmark.setCategory(Category.valueOf("OTHER"));
                        }
                    } else {
                        landmark.setCategory(Category.valueOf("OTHER"));
                    }
                } catch (IllegalArgumentException | IllegalStateException ex) {
                    context.getLogger().log("Error processing category: " + ex.getMessage());
                    landmark.setCategory(Category.valueOf("OTHER"));
                }
            }
            
            // Validate required fields with more flexible handling
            boolean missingRequired = false;
            StringBuilder missingFields = new StringBuilder("Missing required fields: ");
            
            if (landmark.getName() == null || landmark.getName().trim().isEmpty()) {
                landmark.setName("Unnamed Landmark");
            }
            
            if (landmark.getLocation() == null || landmark.getLocation().trim().isEmpty()) {
                // If latitude and longitude are provided, generate a location from them
                if (landmark.getLatitude() != 0 && landmark.getLongitude() != 0) {
                    landmark.setLocation("Location at " + landmark.getLatitude() + ", " + landmark.getLongitude());
                } else {
                    missingRequired = true;
                    missingFields.append("location, ");
                }
            }
            
            if (landmark.getLatitude() == 0 && landmark.getLongitude() == 0) {
                missingRequired = true;
                missingFields.append("latitude/longitude, ");
            }
            
            if (missingRequired) {
                return new APIGatewayProxyResponseEvent()
                    .withStatusCode(400)
                    .withHeaders(getCorsHeaders())
                    .withBody(missingFields.toString().replaceAll(", $", ""));
            }
            
            // Generate a UUID if not provided
            if (landmark.getLandmarkId() == null || landmark.getLandmarkId().trim().isEmpty()) {
                landmark.setLandmarkId(UUID.randomUUID().toString());
            }
       
            if (landmark.getCreatedBy() == null || landmark.getCreatedBy().trim().isEmpty()) {
                // Try to get from request context, otherwise use 'system'
                String username = "system";
                
                // Try to extract username from request context if available
                if (input.getRequestContext() != null && input.getRequestContext().getAuthorizer() != null) {
                    Map<String, Object> authorizer = input.getRequestContext().getAuthorizer();
                    if (authorizer != null && authorizer.containsKey("username")) {
                        username = authorizer.get("username").toString();
                    }
                }
                
                landmark.setCreatedBy(username);
            }
            
            // Ensure timestamps are set
            if (landmark.getCreatedAt() == null) {
                // This should already be set by the default constructor, but just to be safe
                landmark.setLastUpdated(LocalDateTime.now());
            }
            
            // Ensure timestamps are properly set before saving
            landmark.setLastUpdated(LocalDateTime.now());
            
            context.getLogger().log("Saving landmark: " + landmark);
            landmarkRepository.saveLandmark(landmark);

            return new APIGatewayProxyResponseEvent()
                .withStatusCode(201)
                .withHeaders(getCorsHeaders())
                .withBody(gson.toJson(landmark));
        } catch (IllegalArgumentException e) {
            context.getLogger().log("Invalid argument while creating landmark: " + e.getMessage());
            return new APIGatewayProxyResponseEvent()
                .withStatusCode(400)
                .withHeaders(getCorsHeaders())
                .withBody("Invalid landmark data: " + e.getMessage());
        } catch (RuntimeException e) {
            context.getLogger().log("Error creating landmark: " + e.getMessage());
            return new APIGatewayProxyResponseEvent()
                .withStatusCode(500)
                .withHeaders(getCorsHeaders())
                .withBody("Error creating landmark: " + e.getMessage());
        }
    }

    private APIGatewayProxyResponseEvent updateLandmark(APIGatewayProxyRequestEvent input) {
        String landmarkId = input.getPath().split("/")[2];
        try {
            Landmark landmark = gson.fromJson(input.getBody(), Landmark.class);
            landmark.setLandmarkId(landmarkId);
            landmarkRepository.updateLandmark(landmark);
            return new APIGatewayProxyResponseEvent()
                .withStatusCode(200)
                .withHeaders(getCorsHeaders())
                .withBody(gson.toJson(landmark));
        } catch (com.google.gson.JsonParseException e) {
            return new APIGatewayProxyResponseEvent()
                .withStatusCode(400)
                .withHeaders(getCorsHeaders())
                .withBody("Invalid landmark format: " + e.getMessage());
        } catch (RuntimeException e) {
            return new APIGatewayProxyResponseEvent()
                .withStatusCode(404)
                .withHeaders(getCorsHeaders())
                .withBody("Landmark not found: " + e.getMessage());
        }
    }

    private APIGatewayProxyResponseEvent deleteLandmark(APIGatewayProxyRequestEvent input) {
        String landmarkId = input.getPath().split("/")[2];
        try {
            landmarkRepository.deleteLandmark(landmarkId);
            return new APIGatewayProxyResponseEvent()
                .withStatusCode(204)
                .withHeaders(getCorsHeaders())
                .withBody("");
        } catch (IllegalArgumentException e) {
            return new APIGatewayProxyResponseEvent()
                .withStatusCode(400)
                .withHeaders(getCorsHeaders())
                .withBody("Invalid landmark ID format");
        } catch (RuntimeException e) {
            return new APIGatewayProxyResponseEvent()
                .withStatusCode(404)
                .withHeaders(getCorsHeaders())
                .withBody("Landmark not found: " + e.getMessage());
        }
    }
}