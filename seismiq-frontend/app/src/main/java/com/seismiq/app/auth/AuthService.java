package com.seismiq.app.auth;

import android.util.Log;

import com.amplifyframework.auth.AuthUserAttribute;
import com.amplifyframework.auth.AuthUserAttributeKey;
import com.amplifyframework.auth.cognito.AWSCognitoAuthSession;
import com.amplifyframework.auth.cognito.result.AWSCognitoAuthSignOutResult;
import com.amplifyframework.auth.options.AuthSignOutOptions;
import com.amplifyframework.auth.options.AuthSignUpOptions;
import com.amplifyframework.core.Amplify;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class AuthService {
    private static final String TAG = "AuthService";

    public CompletableFuture<Boolean> registerUser(String username, String password, String email,
                                                   String name, String address,
                                                   boolean isVolunteer, boolean isSocialWorker) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        // Create user attributes with only standard Cognito attributes
        List<AuthUserAttribute> attributes = new ArrayList<>();
        attributes.add(new AuthUserAttribute(AuthUserAttributeKey.email(), email));
        
        // Add name if provided (standard Cognito attribute)
        if (name != null && !name.trim().isEmpty()) {
            attributes.add(new AuthUserAttribute(AuthUserAttributeKey.name(), name));
        }
        
        // Note: isVolunteer and isSocialWorker will be stored in your backend database
        // after successful Cognito registration, not as Cognito user attributes
        
        AuthSignUpOptions options = AuthSignUpOptions.builder()
                .userAttributes(attributes)
                .build();

        Log.i(TAG, "Attempting to register user: " + username + " with email: " + email);
        
        Amplify.Auth.signUp(username, password, options,
                result -> {
                    Log.i(TAG, "Sign up successful. Completion status: " + result.isSignUpComplete());
                    if (!result.isSignUpComplete()) {
                        Log.i(TAG, "Email verification required for user: " + username);
                    }
                    future.complete(result.isSignUpComplete());
                },
                error -> {
                    String errorType = error.getClass().getSimpleName();
                    String errorMessage = error.getMessage();
                    Log.e(TAG, "Registration failed - Type: " + errorType + ", Message: " + errorMessage, error);
                    
                    // Provide specific error context
                    if (errorMessage != null) {
                        if (errorMessage.contains("UsernameExistsException")) {
                            Log.e(TAG, "Username '" + username + "' already exists");
                        } else if (errorMessage.contains("InvalidParameterException")) {
                            Log.e(TAG, "Invalid parameters - check User Pool attribute configuration");
                        } else if (errorMessage.contains("NetworkException") || errorMessage.contains("Connection")) {
                            Log.e(TAG, "Network connectivity issue - check internet connection and AWS endpoints");
                        }
                    }
                    future.completeExceptionally(new Exception("Registration failed: " + errorType + " - " + errorMessage, error));
                }
        );

        return future;
    }

    public CompletableFuture<Boolean> confirmSignUp(String username, String code) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        Amplify.Auth.confirmSignUp(username, code,
                result -> future.complete(result.isSignUpComplete()),
                error -> future.completeExceptionally(error)
        );

        return future;
    }

    public CompletableFuture<Boolean> login(String username, String password) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        Log.i(TAG, "Attempting to sign in user: " + username);
        
        Amplify.Auth.signIn(username, password,
                result -> {
                    Log.i(TAG, "Sign in successful for user: " + username + ", Status: " + result.isSignedIn());
                    if (result.getNextStep() != null) {
                        Log.i(TAG, "Additional step required: " + result.getNextStep().getSignInStep());
                    }
                    future.complete(result.isSignedIn());
                },
                error -> {
                    String errorType = error.getClass().getSimpleName();
                    String errorMessage = error.getMessage();
                    Log.e(TAG, "Login failed - Type: " + errorType + ", Message: " + errorMessage, error);
                    
                    // Provide specific error context
                    if (errorMessage != null) {
                        if (errorMessage.contains("NotAuthorizedException")) {
                            Log.e(TAG, "Invalid username or password for user: " + username);
                        } else if (errorMessage.contains("UserNotFoundException")) {
                            Log.e(TAG, "User '" + username + "' does not exist");
                        } else if (errorMessage.contains("UserNotConfirmedException")) {
                            Log.e(TAG, "User '" + username + "' email not verified - check email for confirmation code");
                        } else if (errorMessage.contains("NetworkException") || errorMessage.contains("Connection")) {
                            Log.e(TAG, "Network issue - Cognito endpoint: cognito-idp.eu-north-1.amazonaws.com");
                        } else if (errorMessage.contains("InvalidParameterException")) {
                            Log.e(TAG, "Invalid auth flow - check User Pool Client configuration");
                        }
                    }
                    future.completeExceptionally(new Exception("Login failed: " + errorType + " - " + errorMessage, error));
                }
        );

        return future;
    }

    public CompletableFuture<Boolean> logout() {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        AuthSignOutOptions options = AuthSignOutOptions.builder()
                .globalSignOut(true)
                .build();

        Amplify.Auth.signOut(options, signOutResult -> {
            if (signOutResult instanceof AWSCognitoAuthSignOutResult.CompleteSignOut) {
                Log.i(TAG, "successfully signed out");
                future.complete(true);
            } else if (signOutResult instanceof AWSCognitoAuthSignOutResult.PartialSignOut) {
                Log.i(TAG, "Partially signed out");
                future.complete(true);
            } else if (signOutResult instanceof AWSCognitoAuthSignOutResult.FailedSignOut) {
                Log.i(TAG, "Failed to sign out");
                future.complete(false);
            }
        });
        return future;
    }

    public CompletableFuture<String> getIdToken() {
        CompletableFuture<String> future = new CompletableFuture<>();

        Amplify.Auth.fetchAuthSession(
                result -> {
                    if (result instanceof AWSCognitoAuthSession) {
                        AWSCognitoAuthSession session = (AWSCognitoAuthSession) result;

                        if (session.isSignedIn()) {
                            try {
                                String token = session.getUserPoolTokensResult().getValue().getIdToken();
                                future.complete(token);
                                Log.i(TAG, "Token retrieved");
                            } catch (Exception e) {
                                future.completeExceptionally(new Exception("Failed to get IdToken", e));
                            }
                        } else {
                            future.completeExceptionally(new Exception("User not signed in"));
                        }
                    } else {
                        future.completeExceptionally(new Exception("Session is not AWSCognitoAuthSession"));
                    }
                },
                error -> future.completeExceptionally(error)
        );

        return future;
    }


    public CompletableFuture<Boolean> isLoggedIn() {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        Amplify.Auth.fetchAuthSession(
                result -> {
                    Log.i(TAG, "Auth session check - Signed in: " + result.isSignedIn());
                    future.complete(result.isSignedIn());
                },
                error -> {
                    Log.e(TAG, "Failed to check auth session: " + error.getMessage(), error);
                    future.completeExceptionally(error);
                }
        );

        return future;
    }

    /**
     * Get the current user's Cognito username/sub ID
     * This is needed to create user profile in backend with proper user identification
     */
    public CompletableFuture<String> getCurrentUserId() {
        CompletableFuture<String> future = new CompletableFuture<>();

        Amplify.Auth.fetchAuthSession(
                result -> {
                    if (result instanceof AWSCognitoAuthSession) {
                        AWSCognitoAuthSession session = (AWSCognitoAuthSession) result;
                        if (session.isSignedIn()) {
                            try {
                                String userId = session.getUserSubResult().getValue();
                                Log.i(TAG, "Retrieved user ID: " + userId);
                                future.complete(userId);
                            } catch (Exception e) {
                                Log.e(TAG, "Failed to get user ID", e);
                                future.completeExceptionally(new Exception("Failed to get user ID", e));
                            }
                        } else {
                            future.completeExceptionally(new Exception("User not signed in"));
                        }
                    } else {
                        future.completeExceptionally(new Exception("Invalid session type"));
                    }
                },
                error -> {
                    Log.e(TAG, "Failed to fetch user ID: " + error.getMessage(), error);
                    future.completeExceptionally(error);
                }
        );

        return future;
    }

}
