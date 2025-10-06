package com.seismiq.app.utils;

import android.content.Context;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessaging;
import com.seismiq.app.api.RetrofitClient;
import com.seismiq.app.api.UserApiService;
import com.seismiq.app.auth.AuthService;
import com.seismiq.app.model.User;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Utility class for Firebase Cloud Messaging operations
 */
public class FCMUtils {
    private static final String TAG = "FCMUtils";

    /**
     * Initialize FCM and get the current token
     * This should be called when the app starts and user is logged in
     */
    public static void initializeFCM(Context context) {
        FirebaseMessaging.getInstance().getToken()
            .addOnCompleteListener(task -> {
                if (!task.isSuccessful()) {
                    Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                    return;
                }

                // Get new FCM Registration Token
                String token = task.getResult();
                Log.d(TAG, "FCM Token: " + token);

                // Store locally
                context.getSharedPreferences("SeismIQFCM", Context.MODE_PRIVATE)
                    .edit()
                    .putString("fcm_token", token)
                    .apply();

                // Send to backend if user is logged in
                sendTokenToBackend(token);
            });
    }

    /**
     * Send FCM token to backend
     */
    public static void sendTokenToBackend(String fcmToken) {
        AuthService authService = new AuthService();
        
        authService.isLoggedIn()
            .thenAccept(isLoggedIn -> {
                if (isLoggedIn) {
                    authService.getCurrentUserId()
                        .thenCompose(userId -> 
                            authService.getIdToken()
                                .thenAccept(authToken -> {
                                    updateUserDeviceToken(userId, fcmToken, authToken);
                                })
                        )
                        .exceptionally(error -> {
                            Log.e(TAG, "Failed to get user info: " + error.getMessage());
                            return null;
                        });
                }
            })
            .exceptionally(error -> {
                Log.e(TAG, "Failed to check login status: " + error.getMessage());
                return null;
            });
    }

    /**
     * Update user's device token in backend
     */
    private static void updateUserDeviceToken(String userId, String deviceToken, String authToken) {
        try {
            UserApiService apiService = RetrofitClient.getClient(authToken).create(UserApiService.class);
            
            User user = new User();
            user.setUserId(userId);
            user.setDeviceToken(deviceToken);
            
            apiService.updateUser(userId, user).enqueue(new Callback<User>() {
                @Override
                public void onResponse(Call<User> call, Response<User> response) {
                    if (response.isSuccessful()) {
                        Log.i(TAG, "Device token updated successfully");
                    } else {
                        Log.w(TAG, "Failed to update device token: " + response.code());
                    }
                }

                @Override
                public void onFailure(Call<User> call, Throwable t) {
                    Log.e(TAG, "Error updating device token: " + t.getMessage());
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Exception updating device token: " + e.getMessage());
        }
    }

    /**
     * Get stored FCM token
     */
    public static String getStoredToken(Context context) {
        return context.getSharedPreferences("SeismIQFCM", Context.MODE_PRIVATE)
            .getString("fcm_token", null);
    }
}