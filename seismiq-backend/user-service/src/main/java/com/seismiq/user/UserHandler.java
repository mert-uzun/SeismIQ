package com.seismiq.user;

import java.util.Map;

import com.UserService;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import com.seismiq.common.model.User;

/**
 * AWS Lambda handler for processing user-related API requests.
 * Uses AWS Cognito for authentication. User profiles are stored in DynamoDB.
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

        // Handle public endpoints (registration and login)
        if ("/users".equals(path) && "POST".equals(httpMethod)) {
            return registerUser(input);
        } else if ("/users/login".equals(path) && "POST".equals(httpMethod)) {
            return loginUser(input);
        }

        // For protected endpoints, extract user ID from Cognito JWT token
        String cognitoUserId = extractUserIdFromToken(input);
        if (cognitoUserId == null) {
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(401)
                    .withBody("Invalid or missing authorization token");
        }

        if (path.startsWith("/users/")) {
            String userId = path.substring("/users/".length());
            
            switch (httpMethod) {
                case "GET":
                    return getUser(userId, cognitoUserId);
                
                case "PUT":
                    return updateUser(input, userId, cognitoUserId);

                case "DELETE":
                    return deleteUser(userId, cognitoUserId);
            
                default:
                    break;
            }
        }

        return new APIGatewayProxyResponseEvent()
                .withStatusCode(404)
                .withBody("Not Found");
    }

    /**
     * Extract user ID from Cognito JWT token in the request context
     */
    private String extractUserIdFromToken(APIGatewayProxyRequestEvent input) {
        try {
            // API Gateway puts Cognito user claims in the request context
            if (input.getRequestContext() != null && 
                input.getRequestContext().getAuthorizer() != null) {
                
                Map<String, Object> claims = input.getRequestContext().getAuthorizer();
                if (claims.containsKey("claims")) {
                    Map<String, String> cognitoClaims = (Map<String, String>) claims.get("claims");
                    return cognitoClaims.get("sub"); // Cognito user ID
                }
                
                // Alternative: sometimes it's directly in authorizer
                if (claims.containsKey("sub")) {
                    return (String) claims.get("sub");
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private APIGatewayProxyResponseEvent registerUser(APIGatewayProxyRequestEvent input) {
        try {
            User user = gson.fromJson(input.getBody(), User.class);
            userService.registerUser(user);
            // Don't return the password hash in response
            user.setPasswordHash(null);
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(201)
                    .withBody(gson.toJson(user));
        } catch (Exception e) {
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(500)
                    .withBody("Error registering user: " + e.getMessage());
        }
    }

    private APIGatewayProxyResponseEvent createUser(APIGatewayProxyRequestEvent input, String cognitoUserId) {
        try {
            User user = gson.fromJson(input.getBody(), User.class);
            user.setUserId(cognitoUserId); // Use Cognito user ID
            userService.createUserProfile(user);
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(201)
                    .withBody(gson.toJson(user));
        } catch (Exception e) {
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(500)
                    .withBody("Error creating user profile: " + e.getMessage());
        }
    }

    private APIGatewayProxyResponseEvent getUser(String userId, String cognitoUserId) {
        try {
            // Users can only access their own profile or this could be admin access
            if (!userId.equals(cognitoUserId)) {
                return new APIGatewayProxyResponseEvent()
                        .withStatusCode(403)
                        .withBody("Access denied");
            }
            
            User user = userService.getUserProfile(userId);
            if (user == null) {
                return new APIGatewayProxyResponseEvent()
                        .withStatusCode(404)
                        .withBody("User profile not found");
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

    private APIGatewayProxyResponseEvent updateUser(APIGatewayProxyRequestEvent input, String userId, String cognitoUserId) {
        try {
            // Users can only update their own profile
            if (!userId.equals(cognitoUserId)) {
                return new APIGatewayProxyResponseEvent()
                        .withStatusCode(403)
                        .withBody("Access denied");
            }
            
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

    private APIGatewayProxyResponseEvent deleteUser(String userId, String cognitoUserId) {
        try {
            // Users can only delete their own profile
            if (!userId.equals(cognitoUserId)) {
                return new APIGatewayProxyResponseEvent()
                        .withStatusCode(403)
                        .withBody("Access denied");
            }
            
            userService.deleteUserProfile(userId);

            return new APIGatewayProxyResponseEvent()
                        .withStatusCode(200)
                        .withBody("User profile deleted successfully");         
        } catch (Exception e) {
            return new APIGatewayProxyResponseEvent()
                        .withStatusCode(500)
                        .withBody("Error deleting user: " + e.getMessage());
        }
    }

    private APIGatewayProxyResponseEvent loginUser(APIGatewayProxyRequestEvent input) {
        try {
            User loginRequest = gson.fromJson(input.getBody(), User.class);
            String email = loginRequest.getEmail();
            String password = loginRequest.getPasswordHash(); // Raw password from frontend

            User authenticatedUser = userService.loginUser(email, password);

            if (authenticatedUser == null) {
                return new APIGatewayProxyResponseEvent()
                        .withStatusCode(401)
                        .withBody("Invalid email or password");
            }

            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withBody(gson.toJson(authenticatedUser));

        } catch (Exception e) {
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(500)
                    .withBody("Error during login: " + e.getMessage());
        }
    }
}