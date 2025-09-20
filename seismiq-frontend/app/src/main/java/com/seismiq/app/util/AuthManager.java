package com.seismiq.app.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.amplifyframework.auth.AuthSession;
import com.amplifyframework.auth.cognito.AWSCognitoAuthSession;
import com.amplifyframework.core.Amplify;

/**
 * Utility class to manage authentication tokens and user sessions
 */
public class AuthManager {
    
    private static final String PREFS_NAME = "SeismIQPrefs";
    private static final String KEY_AUTH_TOKEN = "auth_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_USER_ID = "user_id";
    
    /**
     * Get the current authentication token
     */
    public static String getAuthToken() {
        // Try to get token from Amplify first
        try {
            AuthSession session = Amplify.Auth.fetchAuthSession();
            if (session instanceof AWSCognitoAuthSession) {
                AWSCognitoAuthSession cognitoSession = (AWSCognitoAuthSession) session;
                if (cognitoSession.isSignedIn()) {
                    return cognitoSession.getUserPoolTokens().getValue().getAccessToken();
                }
            }
        } catch (Exception e) {
            // Fall back to stored token if Amplify is not available
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
    public static boolean isAuthenticated() {
        try {
            // Check Amplify auth status
            AuthSession session = Amplify.Auth.fetchAuthSession();
            return session.isSignedIn();
        } catch (Exception e) {
            // Fall back to stored token check
            String token = getAuthToken();
            return token != null && !token.isEmpty();
        }
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
            Amplify.Auth.signOut();
        } catch (Exception e) {
            // Ignore Amplify errors during logout
        }
    }
    
    /**
     * Get the current user ID
     */
    public static String getUserId() {
        try {
            // Try Amplify first
            return Amplify.Auth.getCurrentUser().getUserId();
        } catch (Exception e) {
            // Fall back to stored user ID
            Context context = SeismIQApplication.getAppContext();
            if (context != null) {
                SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                return prefs.getString(KEY_USER_ID, null);
            }
            return null;
        }
    }
}
