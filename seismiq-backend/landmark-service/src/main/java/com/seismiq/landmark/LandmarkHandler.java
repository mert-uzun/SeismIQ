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
        } catch (Exception e) {
            return new APIGatewayProxyResponseEvent()
                .withStatusCode(404)
                .withHeaders(getCorsHeaders())
                .withBody("Landmark not found");
        }
    }

    private APIGatewayProxyResponseEvent listLandmarks(APIGatewayProxyRequestEvent input) {
        Map<String, String> queryParams = input.getQueryStringParameters();
        try {
            return new APIGatewayProxyResponseEvent()
                .withStatusCode(200)
                .withHeaders(getCorsHeaders())
                .withBody(gson.toJson(landmarkRepository.listLandmarks(queryParams)));
        } catch (Exception e) {
            return new APIGatewayProxyResponseEvent()
                .withStatusCode(500)
                .withHeaders(getCorsHeaders())
                .withBody("Error retrieving landmarks");
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

            Landmark landmark = gson.fromJson(input.getBody(), Landmark.class);
            
            // Validate required fields
            if (landmark.getName() == null || landmark.getName().trim().isEmpty() ||
                landmark.getLocation() == null || landmark.getLocation().trim().isEmpty() ||
                landmark.getCategory() == null) {
                return new APIGatewayProxyResponseEvent()
                    .withStatusCode(400)
                    .withHeaders(getCorsHeaders())
                    .withBody("Name, location, and category are required fields");
            }

            landmark.setLandmarkId(UUID.randomUUID().toString());
            landmarkRepository.saveLandmark(landmark);

            return new APIGatewayProxyResponseEvent()
                .withStatusCode(201)
                .withHeaders(getCorsHeaders())
                .withBody(gson.toJson(landmark));
        } catch (Exception e) {
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
        } catch (Exception e) {
            return new APIGatewayProxyResponseEvent()
                .withStatusCode(404)
                .withHeaders(getCorsHeaders())
                .withBody("Landmark not found");
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
        } catch (Exception e) {
            return new APIGatewayProxyResponseEvent()
                .withStatusCode(404)
                .withHeaders(getCorsHeaders())
                .withBody("Landmark not found");
        }
    }
}