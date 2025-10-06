package com.seismiq.app.ui.settings;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.seismiq.app.R;
import com.seismiq.app.auth.AuthService;
import com.seismiq.app.ui.auth.LoginActivity;
import com.seismiq.app.databinding.FragmentSettingsBinding;
import com.seismiq.app.util.AuthManager;

/**
 * Fragment for user settings and preferences in the SeismIQ application.
 * Provides options for notifications, account management, and logout.
 */
public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;
    private AuthManager authManager;
    private AuthService authService;
    
    private TextView userNameTextView;
    private TextView userEmailTextView;
    private SwitchCompat notificationsSwitch;
    private SwitchCompat locationSwitch;
    private Button logoutButton;
    private Button editProfileButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, 
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        initializeViews();
        loadUserInfo();
        setupClickListeners();

        return root;
    }

    private void initializeViews() {
        userNameTextView = binding.textViewUserName;
        userEmailTextView = binding.textViewUserEmail;
        notificationsSwitch = binding.switchNotifications;
        locationSwitch = binding.switchLocation;
        logoutButton = binding.buttonLogout;
        editProfileButton = binding.buttonEditProfile;
        
        authManager = new AuthManager(requireContext());
        authService = new AuthService();
    }

    private void loadUserInfo() {
        // Load user information from Cognito
        authService.getUserName()
                .thenAccept(userName -> {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            if (userName != null && !userName.isEmpty()) {
                                userNameTextView.setText(userName);
                            } else {
                                userNameTextView.setText("User");
                            }
                        });
                    }
                })
                .exceptionally(error -> {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> userNameTextView.setText("User"));
                    }
                    return null;
                });
        
        authService.getUserEmail()
                .thenAccept(userEmail -> {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            if (userEmail != null && !userEmail.isEmpty()) {
                                userEmailTextView.setText(userEmail);
                            } else {
                                userEmailTextView.setText("Not available");
                            }
                        });
                    }
                })
                .exceptionally(error -> {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> userEmailTextView.setText("Not available"));
                    }
                    return null;
                });
        
        // Load saved preferences
        loadSavedPreferences();
    }

    private void loadSavedPreferences() {
        // Load notification preference
        boolean notificationsEnabled = authManager.getPreference("notifications_enabled", true);
        notificationsSwitch.setChecked(notificationsEnabled);
        
        // Load location sharing preference
        boolean locationEnabled = authManager.getPreference("location_enabled", true);
        locationSwitch.setChecked(locationEnabled);
    }

    private void setupClickListeners() {
        // Notifications switch
        notificationsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            authManager.savePreference("notifications_enabled", isChecked);
            String status = isChecked ? "enabled" : "disabled";
            Toast.makeText(requireContext(), 
                    "Notifications " + status, 
                    Toast.LENGTH_SHORT).show();
        });
        
        // Location switch
        locationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            authManager.savePreference("location_enabled", isChecked);
            String status = isChecked ? "enabled" : "disabled";
            Toast.makeText(requireContext(), 
                    "Location sharing " + status, 
                    Toast.LENGTH_SHORT).show();
        });
        
        // Edit profile button
        editProfileButton.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.action_settings_to_edit_profile);
        });
        
        // Logout button
        logoutButton.setOnClickListener(v -> performLogout());
    }

    private void performLogout() {
        // Clear authentication data
        authManager.clearAuth();
        
        // Show confirmation
        Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show();
        
        // Navigate to login screen
        Intent intent = new Intent(requireActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

