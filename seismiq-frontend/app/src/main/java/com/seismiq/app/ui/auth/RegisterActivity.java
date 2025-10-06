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
                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            buttonRegister.setEnabled(true);
                            
                            if (result) {
                                // Registration complete without verification (shouldn't happen with email verification enabled)
                                Toast.makeText(RegisterActivity.this, 
                                    "Registration successful!", 
                                    Toast.LENGTH_SHORT).show();
                                
                                // Navigate to verify email screen anyway
                                navigateToVerifyEmail(username, password, email, name, address, isVolunteer, isSocialWorker);
                            } else {
                                // Email verification required (expected flow)
                                Toast.makeText(RegisterActivity.this, 
                                    "Registration successful! Please verify your email.", 
                                    Toast.LENGTH_SHORT).show();
                                
                                // Navigate to email verification screen
                                navigateToVerifyEmail(username, password, email, name, address, isVolunteer, isSocialWorker);
                            }
                        });
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
    
    private void navigateToVerifyEmail(String username, String password, String email, 
                                       String name, String address, 
                                       boolean isVolunteer, boolean isSocialWorker) {
        Intent intent = new Intent(RegisterActivity.this, VerifyEmailActivity.class);
        intent.putExtra("username", username);
        intent.putExtra("password", password);
        intent.putExtra("email", email);
        intent.putExtra("name", name);
        intent.putExtra("address", address);
        intent.putExtra("isVolunteer", isVolunteer);
        intent.putExtra("isSocialWorker", isSocialWorker);
        startActivity(intent);
        finish();
    }
}
