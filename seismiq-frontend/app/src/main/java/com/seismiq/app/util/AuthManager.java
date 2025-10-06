package com.seismiq.app.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.amplifyframework.auth.AuthSession;
import com.amplifyframework.auth.cognito.AWSCognitoAuthSession;
import com.amplifyframework.auth.cognito.result.AWSCognitoAuthSignOutResult;
import com.amplifyframework.auth.options.AuthSignOutOptions;
import com.amplifyframework.core.Amplify;
import com.seismiq.app.SeismIQApplication;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Utility class to manage authentication tokens and user sessions.
 * Supports both static and instance methods for flexibility.
 */
public class AuthManager {
    
    private static final String PREFS_NAME = "SeismIQPrefs";
    private static final String KEY_AUTH_TOKEN = "auth_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_USER_EMAIL = "user_email";
    
    private final Context context;
    
    /**
     * Get the current authentication token
     */
    public static String getAuthToken() {
        CompletableFuture<String> future = new CompletableFuture<>();

        Amplify.Auth.fetchAuthSession(
                result -> {
                    if (result instanceof AWSCognitoAuthSession) {
                        AWSCognitoAuthSession session = (AWSCognitoAuthSession) result;
                        if (session.isSignedIn() && session.getUserPoolTokensResult().getValue().getAccessToken() != null) {
                            future.complete(session.getUserPoolTokensResult()
                                    .getValue()
                                    .getAccessToken());
                        } else {
                            future.completeExceptionally(
                                    new Exception("User not signed in or tokens unavailable"));
                        }
                    }
                },
                error -> future.completeExceptionally(error)
        );

        try {
            return future.get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            Log.e("Auth", "Failed to get token from Amplify", e);
        }

        // Fall back to SharedPreferences
        Context context = SeismIQApplication.getAppContext();
        if (context != null) {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            return prefs.getString(KEY_AUTH_TOKEN, null);
        }
        return null;
    }
    
    /**
     * Save authentication tokens to preferences
     */
    public static void saveAuthToken(String authToken, String refreshToken, String userId) {
        Context context = SeismIQApplication.getAppContext();
        if (context != null) {
            SharedPreferences.Editor editor = 
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
            
            editor.putString(KEY_AUTH_TOKEN, authToken);
            editor.putString(KEY_REFRESH_TOKEN, refreshToken);
            editor.putString(KEY_USER_ID, userId);
            editor.apply();
        }
    }
    
    /**
     * Check if the user is currently authenticated
     */
    public static CompletableFuture<Boolean> isAuthenticated() {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        Amplify.Auth.fetchAuthSession(
                result -> {
                    future.complete(result.isSignedIn());
                },
                error -> {
                    Log.e("Auth", "Failed to fetch auth session", error);
                    // Fallback to stored token
                    String token = getAuthToken();
                    future.complete(token != null && !token.isEmpty());
                }
        );

        return future;
    }
    
    /**
     * Clear all authentication data (logout)
     */
    public static void clearAuthData() {
        // Clear local storage
        Context context = SeismIQApplication.getAppContext();
        if (context != null) {
            SharedPreferences.Editor editor = 
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
            editor.remove(KEY_AUTH_TOKEN);
            editor.remove(KEY_REFRESH_TOKEN);
            editor.remove(KEY_USER_ID);
            editor.apply();
        }
        
        // Sign out of Amplify
        try {
            AuthSignOutOptions options = AuthSignOutOptions.builder()
                    .globalSignOut(true)
                    .build();

            Amplify.Auth.signOut(options, signOutResult -> {
                if (signOutResult instanceof AWSCognitoAuthSignOutResult.CompleteSignOut) {
                    Log.i("AuthManager", "successfully signed out");
                } else if (signOutResult instanceof AWSCognitoAuthSignOutResult.PartialSignOut) {
                    Log.i("AuthManager", "Partially signed out");
                } else if (signOutResult instanceof AWSCognitoAuthSignOutResult.FailedSignOut) {
                    Log.i("AuthManager", "Failed to sign out");
                }
            });        } catch (Exception e) {
        }
    }

    /**
     * Get the current user ID asynchronously
     */
    public static CompletableFuture<String> getUserIdAsync() {
        CompletableFuture<String> future = new CompletableFuture<>();

        Amplify.Auth.fetchAuthSession(
                result -> {
                    if (result.isSignedIn() && result instanceof AWSCognitoAuthSession) {
                        AWSCognitoAuthSession cognitoSession = (AWSCognitoAuthSession) result;
                        if (cognitoSession.getUserSubResult().getValue().isEmpty()) {
                            String userId = cognitoSession.getUserSubResult().getValue();
                            future.complete(userId);
                            return;
                        }
                    }
                    // Not signed in or no user ID
                    future.complete(null);
                },
                error -> {
                    Log.e("Auth", "Failed to fetch user ID", error);
                    // Fallback to SharedPreferences
                    Context context = SeismIQApplication.getAppContext();
                    if (context != null) {
                        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                        future.complete(prefs.getString(KEY_USER_ID, null));
                    } else {
                        future.complete(null);
                    }
                }
        );

        return future;
    }
    
    // ========== Instance Methods ==========
    
    /**
     * Constructor for instance-based usage
     */
    public AuthManager(Context context) {
        this.context = context;
    }
    
    /**
     * Get authentication token (instance method)
     */
    public String getToken() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String token = prefs.getString(KEY_AUTH_TOKEN, null);
        if (token != null) {
            return token;
        }
        // Fallback to static method
        return getAuthToken();
    }
    
    /**
     * Get user ID (instance method, synchronous)
     */
    public String getUserId() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_USER_ID, null);
    }
    
    /**
     * Get user name (instance method)
     */
    public String getUserName() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_USER_NAME, "User");
    }
    
    /**
     * Get user email (instance method)
     */
    public String getUserEmail() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_USER_EMAIL, null);
    }
    
    /**
     * Save user info
     */
    public void saveUserInfo(String userId, String userName, String userEmail) {
        SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.putString(KEY_USER_ID, userId);
        editor.putString(KEY_USER_NAME, userName);
        editor.putString(KEY_USER_EMAIL, userEmail);
        editor.apply();
    }
    
    /**
     * Clear authentication data (instance method)
     */
    public void clearAuth() {
        SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.clear();
        editor.apply();
        clearAuthData(); // Also call static method for Amplify
    }
    
    /**
     * Save a preference
     */
    public void savePreference(String key, boolean value) {
        SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.putBoolean(key, value);
        editor.apply();
    }
    
    /**
     * Get a boolean preference
     */
    public boolean getPreference(String key, boolean defaultValue) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(key, defaultValue);
    }
}
