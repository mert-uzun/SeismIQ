package com.seismiq.app.ui.landmark;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.seismiq.app.R;
import com.seismiq.app.api.LandmarkApiService;
import com.seismiq.app.api.RetrofitClient;
import com.seismiq.app.auth.AuthService;
import com.seismiq.app.databinding.FragmentLandmarkPostBinding;
import com.seismiq.app.model.Landmark;

import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LandmarkPostFragment extends Fragment implements OnMapReadyCallback {

    private FragmentLandmarkPostBinding binding;
    private GoogleMap googleMap;
    private TextView textViewLatitude, textViewLongitude;
    private Spinner spinnerCategory;
    private EditText editTextName, editTextDescription;
    private Button buttonSubmit;
    private ProgressBar progressBar;
    private AuthService authService;
    private Marker currentMarker;
    private double selectedLatitude = 0;
    private double selectedLongitude = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        android.util.Log.d("LandmarkPostFragment", "onCreateView called");
        binding = FragmentLandmarkPostBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        authService = new AuthService();

        // Initialize UI components
        textViewLatitude = binding.textViewLatitude;
        textViewLongitude = binding.textViewLongitude;
        spinnerCategory = binding.spinnerLandmarkCategory;
        editTextName = binding.etLandmarkName;
        editTextDescription = binding.etLandmarkDescription;
        buttonSubmit = binding.buttonSubmitLandmark;
        progressBar = binding.progressBarLandmark;

        // Set up the category spinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                getContext(),
                R.array.landmark_categories,
                android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);

        // Get the SupportMapFragment and request notification when map is ready
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.landmarkMapFragment);
        android.util.Log.d("LandmarkPostFragment", "Map fragment: " + (mapFragment != null ? "found" : "NULL!"));
        if (mapFragment != null) {
            android.util.Log.d("LandmarkPostFragment", "Calling getMapAsync");
            mapFragment.getMapAsync(this);
        } else {
            android.util.Log.e("LandmarkPostFragment", "ERROR: Map fragment is null!");
        }

        // Set submit button click listener
        buttonSubmit.setOnClickListener(v -> submitLandmark());

        return root;
    }

    @Override
    public void onMapReady(GoogleMap map) {
        android.util.Log.d("LandmarkPostFragment", "onMapReady called");
        googleMap = map;

        // Set map UI settings
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setMyLocationButtonEnabled(true);

        // Set initial map position (Turkey as default)
        LatLng turkey = new LatLng(38.9637, 35.2433);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(turkey, 6));

        // Set tap listener for selecting location
        googleMap.setOnMapClickListener(this::updateSelectedLocation);
        android.util.Log.d("LandmarkPostFragment", "Map click listener set");
    }

    private void updateSelectedLocation(LatLng latLng) {
        android.util.Log.d("LandmarkPostFragment", "Map clicked at: " + latLng.latitude + ", " + latLng.longitude);
        
        selectedLatitude = latLng.latitude;
        selectedLongitude = latLng.longitude;

        textViewLatitude.setText(String.format(Locale.US, "%.6f", selectedLatitude));
        textViewLongitude.setText(String.format(Locale.US, "%.6f", selectedLongitude));

        if (currentMarker != null) {
            currentMarker.remove();
        }
        currentMarker = googleMap.addMarker(new MarkerOptions()
                .position(latLng)
                .title("Selected Location"));

        googleMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
        
        android.util.Log.d("LandmarkPostFragment", "Marker placed and camera moved");
    }

    private void submitLandmark() {
        if (selectedLatitude == 0 && selectedLongitude == 0) {
            Toast.makeText(getContext(), "Please select a location on the map", Toast.LENGTH_SHORT).show();
            return;
        }

        String name = editTextName.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(getContext(), "Please enter a landmark name", Toast.LENGTH_SHORT).show();
            return;
        }

        String description = editTextDescription.getText().toString().trim();
        if (description.isEmpty()) {
            Toast.makeText(getContext(), "Please enter a description", Toast.LENGTH_SHORT).show();
            return;
        }

        String category = spinnerCategory.getSelectedItem().toString();

        progressBar.setVisibility(View.VISIBLE);
        buttonSubmit.setEnabled(false);

        // Create landmark object using the fixed constructor
        Landmark landmark = new Landmark(category, description, name, selectedLatitude, selectedLongitude);

        authService.getIdToken()
                .thenAccept(token -> {
                    LandmarkApiService apiService = RetrofitClient.getClient(token)
                            .create(LandmarkApiService.class);

                    apiService.createLandmark(landmark).enqueue(new Callback<Landmark>() {
                        @Override
                        public void onResponse(Call<Landmark> call, Response<Landmark> response) {
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    progressBar.setVisibility(View.GONE);
                                    buttonSubmit.setEnabled(true);

                                    if (response.isSuccessful() && response.body() != null) {
                                        Toast.makeText(getContext(), "Landmark submitted successfully", Toast.LENGTH_SHORT).show();
                                        // Reset form
                                        editTextName.setText("");
                                        editTextDescription.setText("");
                                        if (currentMarker != null) {
                                            currentMarker.remove();
                                            currentMarker = null;
                                        }
                                        selectedLatitude = 0;
                                        selectedLongitude = 0;
                                        textViewLatitude.setText("0.0000");
                                        textViewLongitude.setText("0.0000");
                                        spinnerCategory.setSelection(0);
                                    } else {
                                        Toast.makeText(getContext(), "Failed to submit landmark", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        }

                        @Override
                        public void onFailure(Call<Landmark> call, Throwable t) {
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    progressBar.setVisibility(View.GONE);
                                    buttonSubmit.setEnabled(true);
                                    Toast.makeText(getContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                            }
                        }
                    });
                })
                .exceptionally(error -> {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            buttonSubmit.setEnabled(true);
                            Toast.makeText(getContext(), "Authentication error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        });
                    }
                    return null;
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}