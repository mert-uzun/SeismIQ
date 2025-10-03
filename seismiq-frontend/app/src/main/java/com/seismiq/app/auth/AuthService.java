package com.seismiq.app.auth;

import android.content.Context;
import android.util.Log;

import com.amplifyframework.auth.AuthException;
import com.amplifyframework.auth.AuthSession;
import com.amplifyframework.auth.cognito.AWSCognitoAuthSession;
import com.amplifyframework.auth.result.AuthSignInResult;
import com.amplifyframework.auth.result.AuthSignUpResult;
import com.amplifyframework.core.Amplify;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class AuthService {
    private static final String TAG = "AuthService";

    // Register a new user
    public CompletableFuture<Boolean> registerUser(String username, String password, String email, 
                                                 String name, String address,
                                                 boolean isVolunteer, boolean isSocialWorker) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        
        Map<String, String> attributes = new HashMap<>();
        attributes.put("email", email);
        attributes.put("name", name);
        attributes.put("address", address);
        attributes.put("isVolunteer", String.valueOf(isVolunteer));
        attributes.put("isSocialWorker", String.valueOf(isSocialWorker));
        
        Amplify.Auth.signUp(
            username,
            password,
            attributes,
            result -> {
                Log.i(TAG, "Sign up succeeded");
                future.complete(true);
            },
            error -> {
                Log.e(TAG, "Sign up failed", error);
                future.completeExceptionally(error);
            }
        );
        
        return future;
    }

    // Login a user
    public CompletableFuture<Boolean> login(String username, String password) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        
        Amplify.Auth.signIn(
            username,
            password,
            result -> {
                Log.i(TAG, "Login succeeded");
                future.complete(true);
            },
            error -> {
                Log.e(TAG, "Login failed", error);
                future.completeExceptionally(error);
            }
        );
        
        return future;
    }

    // Logout the current user
    public CompletableFuture<Boolean> logout() {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        
        Amplify.Auth.signOut(
            () -> {
                Log.i(TAG, "Logout succeeded");
                future.complete(true);
            },
            error -> {
                Log.e(TAG, "Logout failed", error);
                future.completeExceptionally(error);
            }
        );
        
        return future;
    }

    // Get current user's session (JWT token for API calls)
    public CompletableFuture<String> getIdToken() {
        CompletableFuture<String> future = new CompletableFuture<>();
        
        Amplify.Auth.fetchAuthSession(
            result -> {
                AWSCognitoAuthSession cognitoAuthSession = (AWSCognitoAuthSession) result;
                
                if (cognitoAuthSession.isSignedIn() && 
                    cognitoAuthSession.getIdToken().getValue() != null) {
                    String token = cognitoAuthSession.getIdToken().getValue();
                    Log.i(TAG, "Token successfully retrieved");
                    future.complete(token);
                } else {
                    Log.e(TAG, "User is not signed in or token is null");
                    future.completeExceptionally(
                        new Exception("User is not signed in or token is null"));
                }
            },
            error -> {
                Log.e(TAG, "Failed to get user token", error);
                future.completeExceptionally(error);
            }
        );
        
        return future;
    }

    // Check if user is logged in
    public CompletableFuture<Boolean> isLoggedIn() {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        
        Amplify.Auth.fetchAuthSession(
            result -> {
                future.complete(result.isSignedIn());
            },
            error -> {
                Log.e(TAG, "Failed to check auth status", error);
                future.completeExceptionally(error);
            }
        );
        
        return future;
    }
}
