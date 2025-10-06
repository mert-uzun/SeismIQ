package com.seismiq.app.ui.report;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.seismiq.app.R;
import com.seismiq.app.api.ReportApiService;
import com.seismiq.app.api.RetrofitClient;
import com.seismiq.app.databinding.FragmentReportBinding;
import com.seismiq.app.model.Category;
import com.seismiq.app.model.Report;
import com.seismiq.app.model.User;
import com.seismiq.app.util.AuthManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Fragment for submitting emergency reports in the SeismIQ system.
 * Allows users to report various types of emergencies with their current location.
 */
public class ReportFragment extends Fragment {
    
    private FragmentReportBinding binding;
    private FusedLocationProviderClient fusedLocationClient;
    private AuthManager authManager;
    
    private Spinner categorySpinner;
    private EditText descriptionEditText;
    private EditText locationEditText;
    private Button submitButton;
    private Button getCurrentLocationButton;
    private ProgressBar progressBar;
    
    private double currentLatitude = 0.0;
    private double currentLongitude = 0.0;
    
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    fetchCurrentLocation();
                } else {
                    Toast.makeText(requireContext(), "Location permission denied", Toast.LENGTH_SHORT).show();
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, 
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentReportBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        initializeViews();
        setupCategorySpinner();
        setupClickListeners();

        return root;
    }

    private void initializeViews() {
        categorySpinner = binding.spinnerCategory;
        descriptionEditText = binding.editTextDescription;
        locationEditText = binding.editTextLocation;
        submitButton = binding.buttonSubmitReport;
        getCurrentLocationButton = binding.buttonGetCurrentLocation;
        progressBar = binding.progressBarReport;
        
        authManager = new AuthManager(requireContext());
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
    }

    private void setupCategorySpinner() {
        String[] categories = getResources().getStringArray(R.array.report_categories);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                categories
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(adapter);
    }

    private void setupClickListeners() {
        getCurrentLocationButton.setOnClickListener(v -> fetchCurrentLocation());
        submitButton.setOnClickListener(v -> submitReport());
    }

    private void fetchCurrentLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(), 
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        getCurrentLocationButton.setEnabled(false);

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    progressBar.setVisibility(View.GONE);
                    getCurrentLocationButton.setEnabled(true);
                    
                    if (location != null) {
                        currentLatitude = location.getLatitude();
                        currentLongitude = location.getLongitude();
                        
                        String locationText = String.format(java.util.Locale.US, "Lat: %.6f, Lon: %.6f", 
                                currentLatitude, currentLongitude);
                        locationEditText.setText(locationText);
                        Toast.makeText(requireContext(), "Location obtained", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(requireContext(), "Unable to get location", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    getCurrentLocationButton.setEnabled(true);
                    Toast.makeText(requireContext(), "Error getting location: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void submitReport() {
        // Validate inputs
        String description = descriptionEditText.getText().toString().trim();
        String location = locationEditText.getText().toString().trim();
        String selectedCategory = categorySpinner.getSelectedItem().toString();

        if (description.isEmpty()) {
            descriptionEditText.setError("Description is required");
            return;
        }

        if (location.isEmpty()) {
            locationEditText.setError("Location is required");
            return;
        }

        // Get auth token
        String token = authManager.getToken();
        if (token == null || token.isEmpty()) {
            Toast.makeText(requireContext(), "Not authenticated. Please login.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create report object
        Report report = new Report();
        report.setDescription(description);
        report.setCategory(mapCategoryToBackend(selectedCategory));
        report.setLatitude(currentLatitude);
        report.setLongitude(currentLongitude);
        
        // Create a simple user object (in production, fetch from auth)
        User user = new User();
        user.setUserId(authManager.getUserId());
        user.setName(authManager.getUserName());
        // Note: The backend expects nested user object based on ReportHandler.java

        // Show progress
        progressBar.setVisibility(View.VISIBLE);
        submitButton.setEnabled(false);

        // Submit to API
        ReportApiService apiService = RetrofitClient.getClient(token).create(ReportApiService.class);
        apiService.createReport(report).enqueue(new Callback<Report>() {
            @Override
            public void onResponse(Call<Report> call, Response<Report> response) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        submitButton.setEnabled(true);

                        if (response.isSuccessful() && response.body() != null) {
                            Toast.makeText(requireContext(), 
                                    "Report submitted successfully!", 
                                    Toast.LENGTH_LONG).show();
                            clearForm();
                        } else {
                            Toast.makeText(requireContext(), 
                                    "Failed to submit report: " + response.code(), 
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }

            @Override
            public void onFailure(Call<Report> call, Throwable t) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        submitButton.setEnabled(true);
                        Toast.makeText(requireContext(), 
                                "Error: " + t.getMessage(), 
                                Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    private String mapCategoryToBackend(String frontendCategory) {
        // Map frontend categories to backend expected values
        return switch (frontendCategory) {
            case "Immediate Help Needed" -> "RESCUE";
            case "Structural Damage" -> "INFRASTRUCTURE";
            case "Medical Assistance" -> "MEDICAL_HELP";
            case "Shelter Needed" -> "SHELTER";
            case "Food and Water" -> "FOOD_WATER";
            case "Missing Person" -> "OTHER";
            default -> "OTHER";
        };
    }

    private void clearForm() {
        descriptionEditText.setText("");
        locationEditText.setText("");
        categorySpinner.setSelection(0);
        currentLatitude = 0.0;
        currentLongitude = 0.0;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

