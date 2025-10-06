package com.seismiq.app.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
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
import com.seismiq.app.databinding.ActivityRegisterBinding;
import com.seismiq.app.model.User;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;
    private AuthService authService;
    private EditText editTextUsername, editTextPassword, editTextEmail, editTextName, editTextAddress;
    private CheckBox checkBoxVolunteer, checkBoxSocialWorker;
    private Button buttonRegister;
    private TextView textViewLoginLink;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        authService = new AuthService();
        
        // Initialize UI components
        editTextUsername = binding.editTextRegUsername;
        editTextPassword = binding.editTextRegPassword;
        editTextEmail = binding.editTextRegEmail;
        editTextName = binding.editTextRegName;
        editTextAddress = binding.editTextRegAddress;
        checkBoxVolunteer = binding.checkBoxVolunteer;
        checkBoxSocialWorker = binding.checkBoxSocialWorker;
        buttonRegister = binding.buttonRegister;
        textViewLoginLink = binding.textViewLoginLink;
        progressBar = binding.progressBarRegister;
        
        // Set button click listener for registration
        buttonRegister.setOnClickListener(view -> {
            String username = editTextUsername.getText().toString().trim();
            String password = editTextPassword.getText().toString().trim();
            String email = editTextEmail.getText().toString().trim();
            String name = editTextName.getText().toString().trim();
            String address = editTextAddress.getText().toString().trim();
            boolean isVolunteer = checkBoxVolunteer.isChecked();
            boolean isSocialWorker = checkBoxSocialWorker.isChecked();
            
            // Basic validation
            if (username.isEmpty() || password.isEmpty() || email.isEmpty() || 
                name.isEmpty() || address.isEmpty()) {
                Toast.makeText(RegisterActivity.this, "Please enter all required fields", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Show progress bar and disable button
            progressBar.setVisibility(View.VISIBLE);
            buttonRegister.setEnabled(false);
            
            // Register with Cognito
            authService.registerUser(username, password, email, name, address, isVolunteer, isSocialWorker)
                    .thenAccept(result -> {
                        if (result) {
                            // Registration complete, create user profile in backend
                            createUserInBackend(username, email, name, address, isVolunteer, isSocialWorker);
                        } else {
                            // Email verification required
                            runOnUiThread(() -> {
                                progressBar.setVisibility(View.GONE);
                                buttonRegister.setEnabled(true);
                                Toast.makeText(RegisterActivity.this, 
                                    "Registration successful! Please check your email to verify your account before logging in.", 
                                    Toast.LENGTH_LONG).show();
                                
                                // Navigate back to login
                                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                                startActivity(intent);
                                finish();
                            });
                        }
                    })
                    .exceptionally(error -> {
                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            buttonRegister.setEnabled(true);
                            Toast.makeText(RegisterActivity.this, "Registration failed: " + error.getMessage(), Toast.LENGTH_LONG).show();
                        });
                        return null;
                    });
        });
        
        // Navigate back to login screen
        textViewLoginLink.setOnClickListener(view -> {
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }
    
    // Create user record in your backend after successful Cognito registration
    private void createUserInBackend(String username, String email, String name, String address, 
                                    boolean isVolunteer, boolean isSocialWorker) {
        // First get the Cognito user ID, then get the ID token for API authentication
        authService.getCurrentUserId()
                .thenCompose(cognitoUserId -> {
                    // Now get the ID token for API authentication
                    return authService.getIdToken()
                            .thenAccept(token -> {
                                // Create user object with Cognito user ID
                                User user = new User(cognitoUserId, name, address, isVolunteer, isSocialWorker);
                                user.setEmail(email); // Set email separately if needed
                                
                                // Create API service with auth token
                                UserApiService apiService = RetrofitClient.getClient(token).create(UserApiService.class);
                    
                    // Call the API to create user
                    apiService.createUser(user).enqueue(new Callback<User>() {
                        @Override
                        public void onResponse(Call<User> call, Response<User> response) {
                            runOnUiThread(() -> {
                                progressBar.setVisibility(View.GONE);
                                buttonRegister.setEnabled(true);
                                
                                if (response.isSuccessful()) {
                                    Toast.makeText(RegisterActivity.this, "Registration successful!", Toast.LENGTH_SHORT).show();
                                    
                                    // Navigate to the main activity
                                    Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(intent);
                                } else {
                                    Toast.makeText(RegisterActivity.this, "User creation in backend failed", Toast.LENGTH_LONG).show();
                                }
                            });
                        }

                        @Override
                        public void onFailure(Call<User> call, Throwable t) {
                            runOnUiThread(() -> {
                                progressBar.setVisibility(View.GONE);
                                buttonRegister.setEnabled(true);
                                Toast.makeText(RegisterActivity.this, "User creation in backend failed: " + t.getMessage(), Toast.LENGTH_LONG).show();
                            });
                        }
                    });
                })
                .exceptionally(error -> {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        buttonRegister.setEnabled(true);
                        Toast.makeText(RegisterActivity.this, "Failed to get authentication token: " + error.getMessage(), Toast.LENGTH_LONG).show();
                    });
                    return null;
                });
    }
}
