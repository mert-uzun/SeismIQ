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

import com.amplifyframework.core.Amplify;
import com.seismiq.app.MainActivity;
import com.seismiq.app.R;
import com.seismiq.app.api.RetrofitClient;
import com.seismiq.app.api.UserApiService;
import com.seismiq.app.auth.AuthService;
import com.seismiq.app.model.User;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VerifyEmailActivity extends AppCompatActivity {

    private EditText editTextVerificationCode;
    private Button buttonVerify;
    private Button buttonResendCode;
    private TextView textViewBackToLogin;
    private TextView tvVerificationMessage;
    private ProgressBar progressBar;
    
    private AuthService authService;
    private String username;
    private String password;
    private String email;
    private String name;
    private String address;
    private boolean isVolunteer;
    private boolean isSocialWorker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_email);
        
        authService = new AuthService();
        
        // Initialize views
        editTextVerificationCode = findViewById(R.id.editTextVerificationCode);
        buttonVerify = findViewById(R.id.buttonVerify);
        buttonResendCode = findViewById(R.id.buttonResendCode);
        textViewBackToLogin = findViewById(R.id.textViewBackToLogin);
        tvVerificationMessage = findViewById(R.id.tvVerificationMessage);
        progressBar = findViewById(R.id.progressBarVerify);
        
        // Get data from intent
        Intent intent = getIntent();
        username = intent.getStringExtra("username");
        password = intent.getStringExtra("password");
        email = intent.getStringExtra("email");
        name = intent.getStringExtra("name");
        address = intent.getStringExtra("address");
        isVolunteer = intent.getBooleanExtra("isVolunteer", false);
        isSocialWorker = intent.getBooleanExtra("isSocialWorker", false);
        
        // Update message with email
        if (email != null) {
            tvVerificationMessage.setText("We've sent a verification code to " + email + ". Please enter it below.");
        }
        
        // Set up verify button
        buttonVerify.setOnClickListener(v -> verifyCode());
        
        // Set up resend code button
        buttonResendCode.setOnClickListener(v -> resendCode());
        
        // Set up back to login link
        textViewBackToLogin.setOnClickListener(v -> {
            Intent loginIntent = new Intent(VerifyEmailActivity.this, LoginActivity.class);
            startActivity(loginIntent);
            finish();
        });
    }
    
    private void verifyCode() {
        String code = editTextVerificationCode.getText().toString().trim();
        
        if (code.isEmpty()) {
            editTextVerificationCode.setError("Please enter the verification code");
            editTextVerificationCode.requestFocus();
            return;
        }
        
        // Show progress
        progressBar.setVisibility(View.VISIBLE);
        buttonVerify.setEnabled(false);
        
        // Confirm sign up with Cognito
        authService.confirmSignUp(username, code)
                .thenAccept(isConfirmed -> {
                    if (isConfirmed) {
                        // Now log in the user automatically
                        authService.login(username, password)
                                .thenAccept(isLoggedIn -> {
                                    if (isLoggedIn) {
                                        // Create user profile in backend
                                        createUserInBackend();
                                    } else {
                                        runOnUiThread(() -> {
                                            progressBar.setVisibility(View.GONE);
                                            buttonVerify.setEnabled(true);
                                            Toast.makeText(VerifyEmailActivity.this, 
                                                "Verification successful! Please log in.", 
                                                Toast.LENGTH_SHORT).show();
                                            navigateToLogin();
                                        });
                                    }
                                })
                                .exceptionally(error -> {
                                    runOnUiThread(() -> {
                                        progressBar.setVisibility(View.GONE);
                                        buttonVerify.setEnabled(true);
                                        Toast.makeText(VerifyEmailActivity.this, 
                                            "Verification successful! Please log in.", 
                                            Toast.LENGTH_SHORT).show();
                                        navigateToLogin();
                                    });
                                    return null;
                                });
                    } else {
                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            buttonVerify.setEnabled(true);
                            Toast.makeText(VerifyEmailActivity.this, 
                                "Verification incomplete. Please try again.", 
                                Toast.LENGTH_SHORT).show();
                        });
                    }
                })
                .exceptionally(error -> {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        buttonVerify.setEnabled(true);
                        
                        String errorMessage = error.getMessage();
                        if (errorMessage != null && errorMessage.contains("CodeMismatchException")) {
                            Toast.makeText(VerifyEmailActivity.this, 
                                "Invalid verification code. Please try again.", 
                                Toast.LENGTH_LONG).show();
                        } else if (errorMessage != null && errorMessage.contains("ExpiredCodeException")) {
                            Toast.makeText(VerifyEmailActivity.this, 
                                "Verification code expired. Please request a new one.", 
                                Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(VerifyEmailActivity.this, 
                                "Verification failed: " + errorMessage, 
                                Toast.LENGTH_LONG).show();
                        }
                    });
                    return null;
                });
    }
    
    private void resendCode() {
        progressBar.setVisibility(View.VISIBLE);
        buttonResendCode.setEnabled(false);
        
        Amplify.Auth.resendSignUpCode(username,
                result -> {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        buttonResendCode.setEnabled(true);
                        Toast.makeText(VerifyEmailActivity.this, 
                            "Verification code resent to " + email, 
                            Toast.LENGTH_SHORT).show();
                    });
                },
                error -> {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        buttonResendCode.setEnabled(true);
                        Toast.makeText(VerifyEmailActivity.this, 
                            "Failed to resend code: " + error.getMessage(), 
                            Toast.LENGTH_LONG).show();
                    });
                }
        );
    }
    
    private void createUserInBackend() {
        authService.getCurrentUserId()
                .thenCompose(cognitoUserId -> {
                    return authService.getIdToken()
                            .thenAccept(token -> {
                                User user = new User(cognitoUserId, name, address, isVolunteer, isSocialWorker);
                                user.setEmail(email);
                                
                                UserApiService apiService = RetrofitClient.getClient(token).create(UserApiService.class);
                                
                                apiService.createUser(user).enqueue(new Callback<User>() {
                                    @Override
                                    public void onResponse(Call<User> call, Response<User> response) {
                                        runOnUiThread(() -> {
                                            progressBar.setVisibility(View.GONE);
                                            buttonVerify.setEnabled(true);
                                            
                                            if (response.isSuccessful()) {
                                                Toast.makeText(VerifyEmailActivity.this, 
                                                    "Registration successful! Welcome to SeismIQ", 
                                                    Toast.LENGTH_SHORT).show();
                                                navigateToMainActivity();
                                            } else {
                                                Toast.makeText(VerifyEmailActivity.this, 
                                                    "Profile creation warning. Please complete your profile later.", 
                                                    Toast.LENGTH_SHORT).show();
                                                navigateToMainActivity();
                                            }
                                        });
                                    }

                                    @Override
                                    public void onFailure(Call<User> call, Throwable t) {
                                        runOnUiThread(() -> {
                                            progressBar.setVisibility(View.GONE);
                                            buttonVerify.setEnabled(true);
                                            Toast.makeText(VerifyEmailActivity.this, 
                                                "Profile creation warning. Please complete your profile later.", 
                                                Toast.LENGTH_SHORT).show();
                                            navigateToMainActivity();
                                        });
                                    }
                                });
                            });
                })
                .exceptionally(error -> {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        buttonVerify.setEnabled(true);
                        Toast.makeText(VerifyEmailActivity.this, 
                            "Authentication warning: " + error.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                        navigateToMainActivity();
                    });
                    return null;
                });
    }
    
    private void navigateToLogin() {
        Intent intent = new Intent(VerifyEmailActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
    
    private void navigateToMainActivity() {
        Intent intent = new Intent(VerifyEmailActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}

