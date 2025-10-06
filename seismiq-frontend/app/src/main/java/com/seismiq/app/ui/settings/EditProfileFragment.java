package com.seismiq.app.ui.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.android.material.textfield.TextInputEditText;
import com.seismiq.app.R;
import com.seismiq.app.api.RetrofitClient;
import com.seismiq.app.api.UserApiService;
import com.seismiq.app.auth.AuthService;
import com.seismiq.app.model.User;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditProfileFragment extends Fragment {

    private TextInputEditText etName;
    private TextInputEditText etAddress;
    private TextView tvEmail;
    private SwitchCompat switchVolunteer;
    private SwitchCompat switchSocialWorker;
    private Button btnSaveProfile;
    private Button btnCancel;
    private ProgressBar progressBar;

    private AuthService authService;
    private String currentUserId;
    private User currentUser;
    private boolean isNewProfile = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_edit_profile, container, false);

        initializeViews(root);
        authService = new AuthService();
        
        loadUserProfile();
        setupClickListeners();

        return root;
    }

    private void initializeViews(View root) {
        etName = root.findViewById(R.id.etName);
        etAddress = root.findViewById(R.id.etAddress);
        tvEmail = root.findViewById(R.id.tvEmail);
        switchVolunteer = root.findViewById(R.id.switchVolunteer);
        switchSocialWorker = root.findViewById(R.id.switchSocialWorker);
        btnSaveProfile = root.findViewById(R.id.btnSaveProfile);
        btnCancel = root.findViewById(R.id.btnCancel);
        progressBar = root.findViewById(R.id.progressBar);
    }

    private void loadUserProfile() {
        progressBar.setVisibility(View.VISIBLE);
        
        authService.getCurrentUserId()
                .thenAccept(userId -> {
                    currentUserId = userId;
                    authService.getIdToken()
                            .thenAccept(token -> {
                                if (getActivity() == null) return;
                                
                                UserApiService service = RetrofitClient.getClient(token)
                                        .create(UserApiService.class);
                                
                                service.getUserById(userId).enqueue(new Callback<User>() {
                                    @Override
                                    public void onResponse(Call<User> call, Response<User> response) {
                                        if (getActivity() == null) return;
                                        
                                        getActivity().runOnUiThread(() -> {
                                            progressBar.setVisibility(View.GONE);
                                            
                                            if (response.isSuccessful() && response.body() != null) {
                                                currentUser = response.body();
                                                isNewProfile = false; // Profile exists, use update
                                                populateFields(currentUser);
                                            } else if (response.code() == 404) {
                                                // Profile doesn't exist yet - this is a new profile
                                                isNewProfile = true;
                                                populateFieldsFromCognito();
                                                Toast.makeText(requireContext(), 
                                                        "Please complete your profile", 
                                                        Toast.LENGTH_SHORT).show();
                                            } else {
                                                // For any other error, assume update mode to be safe
                                                isNewProfile = false;
                                                populateFieldsFromCognito();
                                                Toast.makeText(requireContext(), 
                                                        "Loading profile...", 
                                                        Toast.LENGTH_SHORT).show();
                                            }
                                        });
                                    }

                                    @Override
                                    public void onFailure(Call<User> call, Throwable t) {
                                        if (getActivity() == null) return;
                                        
                                        getActivity().runOnUiThread(() -> {
                                            progressBar.setVisibility(View.GONE);
                                            Toast.makeText(requireContext(), 
                                                    "Error: " + t.getMessage(), 
                                                    Toast.LENGTH_SHORT).show();
                                        });
                                    }
                                });
                            })
                            .exceptionally(error -> {
                                if (getActivity() != null) {
                                    getActivity().runOnUiThread(() -> {
                                        progressBar.setVisibility(View.GONE);
                                        Toast.makeText(requireContext(), 
                                                "Authentication error", 
                                                Toast.LENGTH_SHORT).show();
                                    });
                                }
                                return null;
                            });
                })
                .exceptionally(error -> {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(requireContext(), 
                                    "Error getting user ID", 
                                    Toast.LENGTH_SHORT).show();
                        });
                    }
                    return null;
                });
    }

    private void populateFields(User user) {
        if (user.getName() != null) {
            etName.setText(user.getName());
        }
        
        if (user.getAddress() != null) {
            etAddress.setText(user.getAddress());
        }
        
        if (user.getEmail() != null) {
            tvEmail.setText(user.getEmail());
        }
        
        switchVolunteer.setChecked(user.isVolunteer());
        switchSocialWorker.setChecked(user.isSocialWorker());
    }

    private void populateFieldsFromCognito() {
        // Get user info from Cognito/AuthService
        authService.getUserName()
                .thenAccept(name -> {
                    if (getActivity() != null && name != null) {
                        getActivity().runOnUiThread(() -> etName.setText(name));
                    }
                });
        
        authService.getUserEmail()
                .thenAccept(email -> {
                    if (getActivity() != null && email != null) {
                        getActivity().runOnUiThread(() -> tvEmail.setText(email));
                    }
                });
    }

    private void setupClickListeners() {
        btnSaveProfile.setOnClickListener(v -> saveProfile());
        
        btnCancel.setOnClickListener(v -> {
            Navigation.findNavController(v).navigateUp();
        });
    }

    private void saveProfile() {
        // Validate input
        String name = etName.getText() != null ? etName.getText().toString().trim() : "";
        String address = etAddress.getText() != null ? etAddress.getText().toString().trim() : "";
        
        if (name.isEmpty()) {
            etName.setError("Name is required");
            etName.requestFocus();
            return;
        }
        
        if (address.isEmpty()) {
            etAddress.setError("Address is required");
            etAddress.requestFocus();
            return;
        }
        
        // Disable button and show progress
        btnSaveProfile.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);
        
        // Get email for new profiles
        authService.getUserEmail()
                .thenAccept(email -> {
                    // Create user object
                    User user = new User();
                    user.setUserId(currentUserId);
                    user.setName(name);
                    user.setAddress(address);
                    user.setEmail(email != null ? email : (currentUser != null ? currentUser.getEmail() : null));
                    user.setVolunteer(switchVolunteer.isChecked());
                    user.setSocialWorker(switchSocialWorker.isChecked());
                    
                    // Save profile (create or update)
                    authService.getIdToken()
                            .thenAccept(token -> {
                                if (getActivity() == null) return;
                                
                                UserApiService service = RetrofitClient.getClient(token)
                                        .create(UserApiService.class);
                                
                                // Always use UPDATE (PUT) since user is already logged in
                                // If they can access this screen, their profile exists
                                Call<User> call = service.updateUser(currentUserId, user);
                                
                                call.enqueue(new Callback<User>() {
                                    @Override
                                    public void onResponse(Call<User> call, Response<User> response) {
                                        if (getActivity() == null) return;
                                        
                                        getActivity().runOnUiThread(() -> {
                                            progressBar.setVisibility(View.GONE);
                                            btnSaveProfile.setEnabled(true);
                                            
                                            if (response.isSuccessful()) {
                                                Toast.makeText(requireContext(), 
                                                        "Profile updated successfully", 
                                                        Toast.LENGTH_SHORT).show();
                                                Navigation.findNavController(requireView()).navigateUp();
                                            } else {
                                                Toast.makeText(requireContext(), 
                                                        "Failed to save profile: " + response.code(), 
                                                        Toast.LENGTH_SHORT).show();
                                            }
                                        });
                                    }

                                    @Override
                                    public void onFailure(Call<User> call, Throwable t) {
                                        if (getActivity() == null) return;
                                        
                                        getActivity().runOnUiThread(() -> {
                                            progressBar.setVisibility(View.GONE);
                                            btnSaveProfile.setEnabled(true);
                                            Toast.makeText(requireContext(), 
                                                    "Error: " + t.getMessage(), 
                                                    Toast.LENGTH_SHORT).show();
                                        });
                                    }
                                });
                            })
                            .exceptionally(error -> {
                                if (getActivity() != null) {
                                    getActivity().runOnUiThread(() -> {
                                        progressBar.setVisibility(View.GONE);
                                        btnSaveProfile.setEnabled(true);
                                        Toast.makeText(requireContext(), 
                                                "Authentication error", 
                                                Toast.LENGTH_SHORT).show();
                                    });
                                }
                                return null;
                            });
                })
                .exceptionally(error -> {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            btnSaveProfile.setEnabled(true);
                            Toast.makeText(requireContext(), 
                                    "Error getting user info", 
                                    Toast.LENGTH_SHORT).show();
                        });
                    }
                    return null;
                });
    }
}

