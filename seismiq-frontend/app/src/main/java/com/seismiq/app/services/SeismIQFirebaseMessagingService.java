package com.seismiq.app.services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.seismiq.app.MainActivity;
import com.seismiq.app.R;
import com.seismiq.app.api.RetrofitClient;
import com.seismiq.app.api.UserApiService;
import com.seismiq.app.auth.AuthService;
import com.seismiq.app.model.User;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Firebase Cloud Messaging service for handling push notifications
 * Handles both notification display and device token management
 */
public class SeismIQFirebaseMessagingService extends FirebaseMessagingService {
    private static final String TAG = "FCMService";
    private static final String CHANNEL_ID = "seismiq_landmarks";
    private static final int NOTIFICATION_ID = 1001;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Log.d(TAG, "Message received from: " + remoteMessage.getFrom());

        // Handle notification payload
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Notification title: " + remoteMessage.getNotification().getTitle());
            Log.d(TAG, "Notification body: " + remoteMessage.getNotification().getBody());
            
            showNotification(
                remoteMessage.getNotification().getTitle(),
                remoteMessage.getNotification().getBody(),
                remoteMessage.getData()
            );
        }

        // Handle data payload (for background processing)
        if (!remoteMessage.getData().isEmpty()) {
            Log.d(TAG, "Data payload: " + remoteMessage.getData());
            handleDataPayload(remoteMessage.getData());
        }
    }

    @Override
    public void onNewToken(String token) {
        Log.d(TAG, "New FCM token received: " + token);
        
        // Send token to your backend server
        sendTokenToServer(token);
        
        // Store token locally for immediate use
        getSharedPreferences("SeismIQFCM", MODE_PRIVATE)
            .edit()
            .putString("fcm_token", token)
            .apply();
    }

    /**
     * Send FCM token to backend server to associate with user
     */
    private void sendTokenToServer(String token) {
        AuthService authService = new AuthService();
        
        // Check if user is logged in
        authService.isLoggedIn()
            .thenAccept(isLoggedIn -> {
                if (isLoggedIn) {
                    // Get user ID and auth token
                    authService.getCurrentUserId()
                        .thenCompose(userId -> 
                            authService.getIdToken()
                                .thenAccept(authToken -> {
                                    // Update user with device token
                                    updateUserDeviceToken(userId, token, authToken);
                                })
                        )
                        .exceptionally(error -> {
                            Log.e(TAG, "Failed to get user info for token update: " + error.getMessage());
                            return null;
                        });
                } else {
                    Log.i(TAG, "User not logged in, will update token after login");
                }
            })
            .exceptionally(error -> {
                Log.e(TAG, "Failed to check login status: " + error.getMessage());
                return null;
            });
    }

    /**
     * Update user's device token in the backend
     */
    private void updateUserDeviceToken(String userId, String deviceToken, String authToken) {
        try {
            UserApiService apiService = RetrofitClient.getClient(authToken).create(UserApiService.class);
            
            // Create user object with device token
            User user = new User();
            user.setUserId(userId);
            user.setDeviceToken(deviceToken);
            
            apiService.updateUser(userId, user).enqueue(new Callback<User>() {
                @Override
                public void onResponse(Call<User> call, Response<User> response) {
                    if (response.isSuccessful()) {
                        Log.i(TAG, "Device token updated successfully for user: " + userId);
                    } else {
                        Log.w(TAG, "Failed to update device token. Response code: " + response.code());
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
     * Handle data payload from notification
     */
    private void handleDataPayload(java.util.Map<String, String> data) {
        // Extract landmark information from data payload
        String landmarkId = data.get("landmarkId");
        String name = data.get("name");
        String category = data.get("category");
        String latitude = data.get("latitude");
        String longitude = data.get("longitude");
        
        Log.i(TAG, "Received landmark notification - ID: " + landmarkId + ", Name: " + name);
        
        // You can add custom logic here for handling specific landmark types
        // For example, different handling for emergency vs. regular landmarks
    }

    /**
     * Show notification to user
     */
    private void showNotification(String title, String body, java.util.Map<String, String> data) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        
        // Add landmark data to intent if available
        if (data != null && data.containsKey("landmarkId")) {
            intent.putExtra("landmarkId", data.get("landmarkId"));
            intent.putExtra("latitude", data.get("latitude"));
            intent.putExtra("longitude", data.get("longitude"));
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 0, intent, 
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title != null ? title : "SeismIQ Alert")
            .setContentText(body != null ? body : "New landmark notification")
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent);

        NotificationManager notificationManager = 
            (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
    }

    /**
     * Create notification channel for Android 8.0+
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "SeismIQ Landmarks";
            String description = "Notifications for new landmarks and emergency alerts";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            channel.enableVibration(true);
            channel.enableLights(true);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    /**
     * Get current FCM token
     */
    public static String getCurrentToken(Context context) {
        return context.getSharedPreferences("SeismIQFCM", MODE_PRIVATE)
            .getString("fcm_token", null);
    }
}