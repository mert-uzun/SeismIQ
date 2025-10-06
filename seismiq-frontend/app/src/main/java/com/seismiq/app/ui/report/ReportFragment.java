package com.seismiq.app.ui.report;

import android.Manifest;
import android.app.AlertDialog;
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
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.seismiq.app.R;
import com.seismiq.app.api.ReportApiService;
import com.seismiq.app.api.RetrofitClient;
import com.seismiq.app.auth.AuthService;
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
public class ReportFragment extends Fragment implements OnMapReadyCallback {
    
    private FragmentReportBinding binding;
    private FusedLocationProviderClient fusedLocationClient;
    private AuthManager authManager;
    private AuthService authService;
    
    private Spinner categorySpinner;
    private EditText descriptionEditText;
    private EditText locationEditText;
    private Button submitButton;
    private Button getCurrentLocationButton;
    private Button searchLocationButton;
    private Button selectFromMapButton;
    private ProgressBar progressBar;
    private CardView mapCardView;
    
    private GoogleMap googleMap;
    private Marker selectedMarker;
    private PlacesClient placesClient;
    
    private double currentLatitude = 0.0;
    private double currentLongitude = 0.0;
    private String selectedLocationName = "";
    
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
        searchLocationButton = binding.buttonSearchLocation;
        selectFromMapButton = binding.buttonSelectFromMap;
        progressBar = binding.progressBarReport;
        mapCardView = binding.mapCardView;
        
        authManager = new AuthManager(requireContext());
        authService = new AuthService();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        
        // Initialize Google Places API
        if (!Places.isInitialized()) {
            Places.initialize(requireContext(), getString(R.string.google_maps_key));
        }
        placesClient = Places.createClient(requireContext());
        
        // Initialize map
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.reportMapFragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
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
        searchLocationButton.setOnClickListener(v -> showPlacesSearchDialog());
        selectFromMapButton.setOnClickListener(v -> toggleMapView());
        submitButton.setOnClickListener(v -> submitReport());
    }
    
    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;
        
        // Set map UI settings
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setMyLocationButtonEnabled(false);
        
        // Set initial position (Turkey as default)
        LatLng turkey = new LatLng(39.0, 35.0);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(turkey, 6));
        
        // Set map click listener
        googleMap.setOnMapClickListener(this::onMapClicked);
    }
    
    private void toggleMapView() {
        if (mapCardView.getVisibility() == View.VISIBLE) {
            mapCardView.setVisibility(View.GONE);
            selectFromMapButton.setText("Map");
        } else {
            mapCardView.setVisibility(View.VISIBLE);
            selectFromMapButton.setText("Hide Map");
            
            // If we already have coordinates, show them on map
            if (currentLatitude != 0.0 || currentLongitude != 0.0) {
                LatLng position = new LatLng(currentLatitude, currentLongitude);
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(position, 15));
            }
        }
    }
    
    private void onMapClicked(LatLng latLng) {
        currentLatitude = latLng.latitude;
        currentLongitude = latLng.longitude;
        
        // Update location text
        updateLocationText(String.format(java.util.Locale.US, "%.6f, %.6f", 
                currentLatitude, currentLongitude));
        
        // Clear previous marker
        if (selectedMarker != null) {
            selectedMarker.remove();
        }
        
        // Add new marker
        selectedMarker = googleMap.addMarker(new MarkerOptions()
                .position(latLng)
                .title("Selected Location"));
        
        // Move camera
        googleMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
        
        Toast.makeText(requireContext(), "Location selected from map", Toast.LENGTH_SHORT).show();
    }
    
    private void showPlacesSearchDialog() {
        // Create a simple dialog with an AutoCompleteTextView for Places search
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Search Location");
        
        View dialogView = getLayoutInflater().inflate(android.R.layout.select_dialog_item, null);
        EditText searchEditText = new EditText(requireContext());
        searchEditText.setHint("Enter place name or address");
        searchEditText.setPadding(50, 40, 50, 40);
        
        builder.setView(searchEditText);
        builder.setPositiveButton("Search", (dialog, which) -> {
            String searchQuery = searchEditText.getText().toString().trim();
            if (!searchQuery.isEmpty()) {
                performPlacesSearch(searchQuery);
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    
    private void performPlacesSearch(String query) {
        // For now, use a simple geocoding approach via Places API
        // In production, you'd use the full Places Autocomplete API
        
        // Show a simple implementation using Geocoder as fallback
        try {
            android.location.Geocoder geocoder = new android.location.Geocoder(requireContext());
            java.util.List<android.location.Address> addresses = geocoder.getFromLocationName(query, 1);
            
            if (addresses != null && !addresses.isEmpty()) {
                android.location.Address address = addresses.get(0);
                currentLatitude = address.getLatitude();
                currentLongitude = address.getLongitude();
                
                String locationText = address.getAddressLine(0);
                if (locationText == null || locationText.isEmpty()) {
                    locationText = String.format(java.util.Locale.US, "%.6f, %.6f", 
                            currentLatitude, currentLongitude);
                }
                
                updateLocationText(locationText);
                
                // Update map if visible
                if (mapCardView.getVisibility() == View.VISIBLE && googleMap != null) {
                    LatLng position = new LatLng(currentLatitude, currentLongitude);
                    
                    if (selectedMarker != null) {
                        selectedMarker.remove();
                    }
                    selectedMarker = googleMap.addMarker(new MarkerOptions()
                            .position(position)
                            .title(locationText));
                    
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(position, 15));
                }
                
                Toast.makeText(requireContext(), "Location found: " + locationText, 
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "Location not found. Try a different search.", 
                        Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Search error: " + e.getMessage(), 
                    Toast.LENGTH_SHORT).show();
        }
    }
    
    private void updateLocationText(String text) {
        locationEditText.setText(text);
        selectedLocationName = text;
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
                        
                        String locationText = String.format(java.util.Locale.US, "%.6f, %.6f", 
                                currentLatitude, currentLongitude);
                        updateLocationText(locationText);
                        
                        // Update map if visible
                        if (mapCardView.getVisibility() == View.VISIBLE && googleMap != null) {
                            LatLng position = new LatLng(currentLatitude, currentLongitude);
                            
                            if (selectedMarker != null) {
                                selectedMarker.remove();
                            }
                            selectedMarker = googleMap.addMarker(new MarkerOptions()
                                    .position(position)
                                    .title("Current Location"));
                            
                            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(position, 15));
                        }
                        
                        Toast.makeText(requireContext(), "Current location obtained", Toast.LENGTH_SHORT).show();
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

        // Show progress
        progressBar.setVisibility(View.VISIBLE);
        submitButton.setEnabled(false);

        // Get ID token and user attributes from Cognito session
        authService.getIdToken()
                .thenAccept(token -> {
                    // Get user info from Amplify
                    authService.getCurrentUserId()
                            .thenAccept(userId -> {
                                authService.getCurrentUserName()
                                        .thenAccept(userName -> {
                                            // Create report object
                                            Report report = new Report();
                                            report.setDescription(description);
                                            
                                            // Set category type (backend expects string)
                                            String categoryType = mapCategoryToBackend(selectedCategory);
                                            report.setCategoryType(categoryType);
                                            
                                            report.setLatitude(currentLatitude);
                                            report.setLongitude(currentLongitude);
                                            
                                            // Create user object (backend expects user object)
                                            User user = new User();
                                            user.setUserId(userId);
                                            user.setName(userName);
                                            report.setUser(user);

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
                                        })
                                        .exceptionally(error -> {
                                            if (getActivity() != null) {
                                                getActivity().runOnUiThread(() -> {
                                                    progressBar.setVisibility(View.GONE);
                                                    submitButton.setEnabled(true);
                                                    Toast.makeText(requireContext(), 
                                                            "Failed to get user name: " + error.getMessage(), 
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
                                        submitButton.setEnabled(true);
                                        Toast.makeText(requireContext(), 
                                                "Failed to get user ID: " + error.getMessage(), 
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
                            submitButton.setEnabled(true);
                            Toast.makeText(requireContext(), 
                                    "Failed to get auth token: " + error.getMessage(), 
                                    Toast.LENGTH_SHORT).show();
                        });
                    }
                    return null;
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

