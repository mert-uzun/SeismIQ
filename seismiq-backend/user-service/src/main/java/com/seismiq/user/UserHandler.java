package com.seismiq.user;

import com.UserService;
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
 * Existing methods maintained by Sıla Bozkurt.
 * Additional methods and changes added by Ayşe Ece Bilgi.
 * 
 * @author Sıla Bozkurt
 * @author Ayşe Ece Bilgi
 */
public class UserHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private final UserService userService;
    private final Gson gson;

    public UserHandler() {
        this.userService = new UserService();
        this.gson = new Gson();
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        String path = input.getPath();
        String httpMethod = input.getHttpMethod();

        if ("/users".equals(path) && "POST".equals(httpMethod)) {
            return createUser(input);
        } else if (path.startsWith("/users/")) {
            String userId = path.substring("/users/".length());
            
            switch (httpMethod) {
                case "GET":
                    return getUser(userId);
                
                case "PUT":
                    return updateUser(input, userId);

                case "DELETE":
                    return deleteUser(userId);
            
                default:
                    break;
            }
            return getUser(userId);
        }

        return new APIGatewayProxyResponseEvent()
                .withStatusCode(404)
                .withBody("Not Found");
    }

    private APIGatewayProxyResponseEvent createUser(APIGatewayProxyRequestEvent input) {
        try {
            User user = gson.fromJson(input.getBody(), User.class);
            userService.registerUser(user);
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
            User user = userService.getUserProfile(userId);
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

    private APIGatewayProxyResponseEvent updateUser(APIGatewayProxyRequestEvent input, String userId){
        try {
            User user = gson.fromJson(input.getBody(), User.class);
            user.setUserId(userId);
            userService.updateUserProfile(user);

            return new APIGatewayProxyResponseEvent()
                        .withStatusCode(200)
                        .withBody(gson.toJson(user));
        } catch (Exception e) {
            return new APIGatewayProxyResponseEvent()
                        .withStatusCode(500)
                        .withBody("Error updating user: " + e.getMessage());
        }
    }

    private APIGatewayProxyResponseEvent deleteUser(String userId){
        try {
            userService.deleteUserProfile(userId);

            return new APIGatewayProxyResponseEvent()
                        .withStatusCode(200)
                        .withBody("User deleted successfully");         
        } catch (Exception e) {
            return new APIGatewayProxyResponseEvent()
                        .withStatusCode(500)
                        .withBody("Error deleting user" + e.getMessage());
        }
    }
}