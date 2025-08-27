package com.seismiq.earthquake;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;

/**
 * AWS Lambda handler for processing earthquake-related API requests.
 * Handles creation, retrieval, and management of earthquake data
 * through RESTful endpoints.
 *
 * @author Sıla Bozkurt
 */
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.seismiq.common.model.Earthquake;
import com.seismiq.common.util.LocalDateTimeAdapter;



/**
 * Handler for processing earthquake-related API requests in the SeismIQ system.
 * 
 * @author Sıla
 */
public class EarthquakeHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private final EarthquakeRepository earthquakeRepository;
    private final Gson gson;

    public EarthquakeHandler() {
        this(new EarthquakeRepository());
    }

    public EarthquakeHandler(EarthquakeRepository earthquakeRepository) {
        this.earthquakeRepository = earthquakeRepository;
        this.gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .create();
    }

    public EarthquakeHandler(EarthquakeRepository earthquakeRepository, Gson gson) {
        this.earthquakeRepository = earthquakeRepository;
        this.gson = gson;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        String path = input.getPath();
        String httpMethod = input.getHttpMethod();

        return switch (path) {
            case "/earthquakes" -> switch (httpMethod) {
                case "POST" -> createEarthquake(input);
                case "GET" -> listEarthquakes();
                default -> notFound();
            };
            default -> {
                if (path.matches("/earthquakes/[^/]+")) {
                    String earthquakeId = path.substring("/earthquakes/".length());
                    yield httpMethod.equals("GET") ? getEarthquake(earthquakeId) : notFound();
                }
                if (path.equals("/earthquakes/location")) {
                    yield httpMethod.equals("GET") ? getEarthquakesByLocation(input) : notFound();
                }
                if (path.equals("/earthquakes/cleanup")) {
                    yield httpMethod.equals("POST") ? deactivateOldEarthquakes() : notFound();
                }
                yield notFound();
            }
        };
    }

    private APIGatewayProxyResponseEvent createEarthquake(APIGatewayProxyRequestEvent input) {
        try {
            Earthquake earthquake = gson.fromJson(input.getBody(), Earthquake.class);
            earthquake.setEarthquakeId(UUID.randomUUID().toString());
            earthquake.setActive(true);

            earthquakeRepository.saveEarthquake(earthquake);

            return new APIGatewayProxyResponseEvent()
                .withStatusCode(201)
                .withBody(gson.toJson(earthquake));
        } catch (JsonSyntaxException e) {
            return new APIGatewayProxyResponseEvent()
                .withStatusCode(400)
                .withBody("Invalid earthquake format: " + e.getMessage());
        }
    }

    private APIGatewayProxyResponseEvent listEarthquakes() {
        List<Earthquake> earthquakes = earthquakeRepository.getActiveEarthquakes();
        return new APIGatewayProxyResponseEvent()
            .withStatusCode(200)
            .withBody(gson.toJson(earthquakes));
    }

    private APIGatewayProxyResponseEvent getEarthquake(String earthquakeId) {
        Earthquake earthquake = earthquakeRepository.getEarthquake(earthquakeId);
        if (earthquake == null) {
            return new APIGatewayProxyResponseEvent()
                .withStatusCode(404)
                .withBody("Earthquake not found");
        }
        return new APIGatewayProxyResponseEvent()
            .withStatusCode(200)
            .withBody(gson.toJson(earthquake));
    }

    private APIGatewayProxyResponseEvent getEarthquakesByLocation(APIGatewayProxyRequestEvent input) {
        try {
            Map<String, String> queryParams = input.getQueryStringParameters();
            if (queryParams == null || !queryParams.containsKey("lat") || 
                !queryParams.containsKey("lon") || !queryParams.containsKey("radius")) {
                return new APIGatewayProxyResponseEvent()
                    .withStatusCode(400)
                    .withBody("Missing required parameters: lat, lon, radius");
            }

            double latitude = Double.parseDouble(queryParams.get("lat"));
            double longitude = Double.parseDouble(queryParams.get("lon"));
            double radius = Double.parseDouble(queryParams.get("radius"));

            if (latitude < -90 || latitude > 90) {
                return new APIGatewayProxyResponseEvent()
                    .withStatusCode(400)
                    .withBody("Invalid latitude: must be between -90 and 90");
            }
            if (longitude < -180 || longitude > 180) {
                return new APIGatewayProxyResponseEvent()
                    .withStatusCode(400)
                    .withBody("Invalid longitude: must be between -180 and 180");
            }
            if (radius <= 0 || radius > 1000) {
                return new APIGatewayProxyResponseEvent()
                    .withStatusCode(400)
                    .withBody("Invalid radius: must be between 0 and 1000 km");
            }

            List<Earthquake> earthquakes = earthquakeRepository.getEarthquakesByLocation(latitude, longitude, radius);
            return new APIGatewayProxyResponseEvent()
                .withStatusCode(200)
                .withBody(gson.toJson(earthquakes));
        } catch (NumberFormatException e) {
            return new APIGatewayProxyResponseEvent()
                .withStatusCode(400)
                .withBody("Invalid parameter format: lat, lon, and radius must be numeric values");
        }
    }

    private APIGatewayProxyResponseEvent deactivateOldEarthquakes() {
        try {
            earthquakeRepository.deactivateOldEarthquakes();
            return new APIGatewayProxyResponseEvent()
                .withStatusCode(200)
                .withBody("Successfully deactivated old earthquakes");
        } catch (Exception e) {
            return new APIGatewayProxyResponseEvent()
                .withStatusCode(500)
                .withBody("Error deactivating old earthquakes: " + e.getMessage());
        }
    }

    private APIGatewayProxyResponseEvent notFound() {
        return new APIGatewayProxyResponseEvent()
            .withStatusCode(404)
            .withBody("Not Found");
    }
}
