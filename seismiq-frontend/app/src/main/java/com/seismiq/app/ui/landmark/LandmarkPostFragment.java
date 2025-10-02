package com.seismiq.app.ui.landmark;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.seismiq.app.R;
import com.seismiq.app.api.LandmarkApiService;
import com.seismiq.app.model.Landmark;
import com.seismiq.app.util.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LandmarkPostFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap mMap;
    private EditText etName;
    private EditText etDescription;
    private EditText etLatitude;
    private EditText etLongitude;
    private Spinner spinnerCategory;
    private Button btnSubmit;
    private String selectedCategory;
    private LatLng selectedLocation;

    private LandmarkApiService landmarkApiService;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, 
                            ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_landmark_post, container, false);
        
        // Initialize UI components
        etName = root.findViewById(R.id.et_landmark_name);
        etDescription = root.findViewById(R.id.et_landmark_description);
        etLatitude = root.findViewById(R.id.et_latitude);
        etLongitude = root.findViewById(R.id.et_longitude);
        spinnerCategory = root.findViewById(R.id.spinner_landmark_category);
        btnSubmit = root.findViewById(R.id.btn_submit_landmark);
        
        // Initialize map
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map_landmark);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
        
        // Initialize API service
        landmarkApiService = RetrofitClient.getClient().create(LandmarkApiService.class);
        
        // Setup category spinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(requireContext(),
                R.array.landmark_categories, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);
        
        spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedCategory = parent.getItemAtPosition(position).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
        
        // Submit button click listener
        btnSubmit.setOnClickListener(v -> submitLandmark());
        
        return root;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        
        // Set default location (Istanbul, Turkey)
        LatLng istanbul = new LatLng(41.0082, 28.9784);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(istanbul, 10));
        
        // Set map click listener to select location
        mMap.setOnMapClickListener(latLng -> {
            // Clear previous markers
            mMap.clear();
            
            // Add marker at clicked position
            mMap.addMarker(new MarkerOptions().position(latLng).title("Selected Location"));
            
            // Update coordinates in the form
            etLatitude.setText(String.valueOf(latLng.latitude));
            etLongitude.setText(String.valueOf(latLng.longitude));
            
            // Save selected location
            selectedLocation = latLng;
        });
    }
    
    private void submitLandmark() {
        // Validate inputs
        String name = etName.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String latitudeText = etLatitude.getText().toString().trim();
        String longitudeText = etLongitude.getText().toString().trim();
        
        if (name.isEmpty()) {
            etName.setError("Please enter a name");
            return;
        }
        
        if (description.isEmpty()) {
            etDescription.setError("Please enter a description");
            return;
        }
        
        if (latitudeText.isEmpty() || longitudeText.isEmpty()) {
            Toast.makeText(requireContext(), "Please select a location on the map", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Create landmark object
        double latitude = Double.parseDouble(latitudeText);
        double longitude = Double.parseDouble(longitudeText);
        
        Landmark landmark = new Landmark();
        landmark.setName(name);
        landmark.setDescription(description);
        landmark.setLatitude(latitude);
        landmark.setLongitude(longitude);
        landmark.setCategory(selectedCategory);
        
        // Show loading state
        btnSubmit.setEnabled(false);
        btnSubmit.setText(R.string.loading);
        
        // Send landmark to API
        landmarkApiService.createLandmark(landmark).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                // Reset loading state
                btnSubmit.setEnabled(true);
                btnSubmit.setText(R.string.submit_landmark);
                
                if (response.isSuccessful()) {
                    Toast.makeText(requireContext(), "Landmark submitted successfully", Toast.LENGTH_SHORT).show();
                    clearForm();
                } else {
                    Toast.makeText(requireContext(), "Failed to submit landmark", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                // Reset loading state
                btnSubmit.setEnabled(true);
                btnSubmit.setText(R.string.submit_landmark);
                
                Toast.makeText(requireContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void clearForm() {
        etName.setText("");
        etDescription.setText("");
        etLatitude.setText("");
        etLongitude.setText("");
        spinnerCategory.setSelection(0);
        mMap.clear();
    }
}
