package com.seismiq.user;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import com.seismiq.common.model.User;

/**
 * AWS Lambda handler for processing user-related API requests.
 * Manages user registration, updates, and retrieval in the SeismIQ system.
 *
 * @author Sıla Bozkurt
 */
public class UserHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private final UserRepository userRepository;
    private final Gson gson;

    public UserHandler() {
        this.userRepository = new UserRepository();
        this.gson = new Gson();
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        String path = input.getPath();
        String httpMethod = input.getHttpMethod();

        if ("/users".equals(path) && "POST".equals(httpMethod)) {
            return createUser(input);
        } else if (path.startsWith("/users/") && "GET".equals(httpMethod)) {
            String userId = path.substring("/users/".length());
            return getUser(userId);
        }

        return new APIGatewayProxyResponseEvent()
                .withStatusCode(404)
                .withBody("Not Found");
    }

    private APIGatewayProxyResponseEvent createUser(APIGatewayProxyRequestEvent input) {
        try {
            User user = gson.fromJson(input.getBody(), User.class);
            userRepository.saveUser(user);
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(201)
                    .withBody(gson.toJson(user));
        } catch (Exception e) {
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(500)
                    .withBody("Error creating user: " + e.getMessage());
        }
    }

    private APIGatewayProxyResponseEvent getUser(String userId) {
        try {
            User user = userRepository.getUser(userId);
            if (user == null) {
                return new APIGatewayProxyResponseEvent()
                        .withStatusCode(404)
                        .withBody("User not found");
            }
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withBody(gson.toJson(user));
        } catch (Exception e) {
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(500)
                    .withBody("Error retrieving user: " + e.getMessage());
        }
    }
}