package com.seismiq.app.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.seismiq.app.MainActivity;
import com.seismiq.app.R;
import com.seismiq.app.api.RetrofitClient;
import com.seismiq.app.api.UserApiService;
import com.seismiq.app.auth.AuthService;
import com.seismiq.app.databinding.ActivityLoginBinding;
import com.seismiq.app.model.User;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private AuthService authService;
    private EditText editTextUsername, editTextPassword;
    private Button buttonLogin;
    private TextView textViewRegister;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        authService = new AuthService();
        
        // Initialize UI components
        editTextUsername = binding.editTextUsername;
        editTextPassword = binding.editTextPassword;
        buttonLogin = binding.buttonLogin;
        textViewRegister = binding.textViewRegister;
        progressBar = binding.progressBar;
        
        // Check if user is already logged in
        checkAuthStatus();
        
        // Set button click listener for login
        buttonLogin.setOnClickListener(view -> {
            String username = editTextUsername.getText().toString().trim();
            String password = editTextPassword.getText().toString().trim();
            
            // Basic validation
            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(LoginActivity.this, "Please enter all fields", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Show progress bar and disable button
            progressBar.setVisibility(View.VISIBLE);
            buttonLogin.setEnabled(false);
            
            // Login with Cognito
            authService.login(username, password)
                    .thenAccept(result -> {
                        // After successful login, ensure user profile exists in DynamoDB
                        ensureUserProfileExists();
                    })
                    .exceptionally(error -> {
                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            buttonLogin.setEnabled(true);
                            
                            String errorMessage = error.getMessage();
                            
                            // Check if user needs to verify email
                            if (errorMessage != null && errorMessage.contains("UserNotConfirmedException")) {
                                Toast.makeText(LoginActivity.this, 
                                    "Please verify your email first.", 
                                    Toast.LENGTH_SHORT).show();
                                
                                // Navigate to verification screen
                                Intent intent = new Intent(LoginActivity.this, VerifyEmailActivity.class);
                                intent.putExtra("username", username);
                                intent.putExtra("password", password);
                                startActivity(intent);
                            } else {
                                Toast.makeText(LoginActivity.this, 
                                    "Login failed: " + errorMessage, 
                                    Toast.LENGTH_LONG).show();
                            }
                        });
                        return null;
                    });
        });
        
        // Navigate to registration screen
        textViewRegister.setOnClickListener(view -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }
    
    private void checkAuthStatus() {
        progressBar.setVisibility(View.VISIBLE);
        
        authService.isLoggedIn()
                .thenAccept(isLoggedIn -> {
                    if (isLoggedIn) {
                        // User is already logged in, ensure profile exists before continuing
                        ensureUserProfileExists();
                    } else {
                        runOnUiThread(() -> progressBar.setVisibility(View.GONE));
                    }
                })
                .exceptionally(error -> {
                    runOnUiThread(() -> progressBar.setVisibility(View.GONE));
                    return null;
                });
    }

    /**
     * Ensures that the user profile exists in DynamoDB.
     * If the profile doesn't exist, it creates one with data from Cognito.
     */
    private void ensureUserProfileExists() {
        authService.getCurrentUserId()
                .thenAccept(userId -> {
                    authService.getIdToken()
                            .thenAccept(token -> {
                                UserApiService service = RetrofitClient.getClient(token)
                                        .create(UserApiService.class);
                                
                                // Try to get the user profile
                                service.getUserById(userId).enqueue(new Callback<User>() {
                                    @Override
                                    public void onResponse(Call<User> call, Response<User> response) {
                                        if (response.isSuccessful()) {
                                            // Profile exists, navigate to main activity
                                            navigateToMainActivity();
                                        } else if (response.code() == 404) {
                                            // Profile doesn't exist, create it
                                            createUserProfile(userId, token);
                                        } else {
                                            // Some other error, but let user continue
                                            runOnUiThread(() -> {
                                                Toast.makeText(LoginActivity.this, 
                                                        "Warning: Could not load profile", 
                                                        Toast.LENGTH_SHORT).show();
                                            });
                                            navigateToMainActivity();
                                        }
                                    }

                                    @Override
                                    public void onFailure(Call<User> call, Throwable t) {
                                        // Network error, but let user continue
                                        runOnUiThread(() -> {
                                            Toast.makeText(LoginActivity.this, 
                                                    "Warning: Could not verify profile", 
                                                    Toast.LENGTH_SHORT).show();
                                        });
                                        navigateToMainActivity();
                                    }
                                });
                            })
                            .exceptionally(error -> {
                                // Token error, but let user continue
                                navigateToMainActivity();
                                return null;
                            });
                })
                .exceptionally(error -> {
                    // User ID error, but let user continue
                    navigateToMainActivity();
                    return null;
                });
    }

    /**
     * Creates a new user profile in DynamoDB with data from Cognito
     */
    private void createUserProfile(String userId, String token) {
        // Get user info from Cognito
        authService.getUserName()
                .thenAccept(name -> {
                    authService.getUserEmail()
                            .thenAccept(email -> {
                                // Create user object with Cognito data
                                User newUser = new User();
                                newUser.setUserId(userId);
                                newUser.setName(name != null ? name : "User");
                                newUser.setEmail(email);
                                newUser.setAddress(""); // Will be filled in profile editing
                                newUser.setVolunteer(false);
                                newUser.setSocialWorker(false);
                                
                                // Create profile in DynamoDB
                                UserApiService service = RetrofitClient.getClient(token)
                                        .create(UserApiService.class);
                                
                                service.createUser(newUser).enqueue(new Callback<User>() {
                                    @Override
                                    public void onResponse(Call<User> call, Response<User> response) {
                                        if (response.isSuccessful()) {
                                            runOnUiThread(() -> {
                                                Toast.makeText(LoginActivity.this, 
                                                        "Welcome! Please complete your profile", 
                                                        Toast.LENGTH_LONG).show();
                                            });
                                        }
                                        navigateToMainActivity();
                                    }

                                    @Override
                                    public void onFailure(Call<User> call, Throwable t) {
                                        runOnUiThread(() -> {
                                            Toast.makeText(LoginActivity.this, 
                                                    "Profile creation warning: " + t.getMessage(), 
                                                    Toast.LENGTH_SHORT).show();
                                        });
                                        navigateToMainActivity();
                                    }
                                });
                            })
                            .exceptionally(error -> {
                                // Email fetch failed, continue anyway
                                navigateToMainActivity();
                                return null;
                            });
                })
                .exceptionally(error -> {
                    // Name fetch failed, continue anyway
                    navigateToMainActivity();
                    return null;
                });
    }

    /**
     * Navigate to main activity
     */
    private void navigateToMainActivity() {
        runOnUiThread(() -> {
            progressBar.setVisibility(View.GONE);
            buttonLogin.setEnabled(true);
            
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}
