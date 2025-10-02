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
import com.seismiq.app.auth.AuthService;
import com.seismiq.app.databinding.ActivityLoginBinding;

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
                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            buttonLogin.setEnabled(true);
                            
                            // Navigate to main activity
                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                        });
                    })
                    .exceptionally(error -> {
                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            buttonLogin.setEnabled(true);
                            Toast.makeText(LoginActivity.this, "Login failed: " + error.getMessage(), Toast.LENGTH_LONG).show();
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
                        // User is already logged in, navigate to main activity
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    } else {
                        runOnUiThread(() -> progressBar.setVisibility(View.GONE));
                    }
                })
                .exceptionally(error -> {
                    runOnUiThread(() -> progressBar.setVisibility(View.GONE));
                    return null;
                });
    }
}
