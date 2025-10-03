package com.seismiq.app.ui.map;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.seismiq.app.R;
import com.seismiq.app.api.EarthquakeApiService;
import com.seismiq.app.api.LandmarkApiService;
import com.seismiq.app.model.Earthquake;
import com.seismiq.app.model.Landmark;
import com.seismiq.app.util.RetrofitClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MapFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap mMap;
    private MaterialButtonToggleGroup toggleGroup;
    private MaterialButton btnEarthquakes;
    private MaterialButton btnLandmarks;
    private MaterialButton btnAll;
    
    private EarthquakeApiService earthquakeApiService;
    private LandmarkApiService landmarkApiService;
    
    private List<Earthquake> earthquakes = new ArrayList<>();
    private List<Landmark> landmarks = new ArrayList<>();
    private Map<String, Marker> earthquakeMarkers = new HashMap<>();
    private Map<String, Marker> landmarkMarkers = new HashMap<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_map, container, false);
        
        // Initialize services
        earthquakeApiService = RetrofitClient.getClient().create(EarthquakeApiService.class);
        landmarkApiService = RetrofitClient.getClient().create(LandmarkApiService.class);
        
        // Initialize UI components
        toggleGroup = root.findViewById(R.id.toggle_map_view);
        btnEarthquakes = root.findViewById(R.id.btn_earthquakes);
        btnLandmarks = root.findViewById(R.id.btn_landmarks);
        btnAll = root.findViewById(R.id.btn_all);
        
        // Set up the toggle button group
        toggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btn_earthquakes) {
                    showEarthquakes(true);
                    showLandmarks(false);
                } else if (checkedId == R.id.btn_landmarks) {
                    showEarthquakes(false);
                    showLandmarks(true);
                } else if (checkedId == R.id.btn_all) {
                    showEarthquakes(true);
                    showLandmarks(true);
                }
            }
        });
        
        // Initialize map
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
        
        return root;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        
        // Set default location (Turkey)
        LatLng turkey = new LatLng(39.0, 35.0);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(turkey, 6));
        
        // Load data
        loadEarthquakes();
        loadLandmarks();
        
        // Set up info window click listener
        mMap.setOnInfoWindowClickListener(marker -> {
            // Handle info window click (can navigate to detail page)
            Object tag = marker.getTag();
            if (tag instanceof Earthquake) {
                Earthquake earthquake = (Earthquake) tag;
                // Navigate to earthquake detail
                Toast.makeText(requireContext(), "Earthquake: " + earthquake.getMagnitude(), Toast.LENGTH_SHORT).show();
            } else if (tag instanceof Landmark) {
                Landmark landmark = (Landmark) tag;
                // Navigate to landmark detail
                Toast.makeText(requireContext(), "Landmark: " + landmark.getName(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void loadEarthquakes() {
        earthquakeApiService.getRecentEarthquakes().enqueue(new Callback<List<Earthquake>>() {
            @Override
            public void onResponse(Call<List<Earthquake>> call, Response<List<Earthquake>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    earthquakes = response.body();
                    if (btnEarthquakes.isChecked() || btnAll.isChecked()) {
                        showEarthquakes(true);
                    }
                }
            }

            @Override
            public void onFailure(Call<List<Earthquake>> call, Throwable t) {
                Toast.makeText(requireContext(), "Error loading earthquakes: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void loadLandmarks() {
        landmarkApiService.getAllLandmarks().enqueue(new Callback<List<Landmark>>() {
            @Override
            public void onResponse(Call<List<Landmark>> call, Response<List<Landmark>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    landmarks = response.body();
                    if (btnLandmarks.isChecked() || btnAll.isChecked()) {
                        showLandmarks(true);
                    }
                }
            }

            @Override
            public void onFailure(Call<List<Landmark>> call, Throwable t) {
                Toast.makeText(requireContext(), "Error loading landmarks: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void showEarthquakes(boolean show) {
        if (mMap == null) return;
        
        // Clear existing earthquake markers if hiding
        if (!show) {
            for (Marker marker : earthquakeMarkers.values()) {
                marker.setVisible(false);
            }
            return;
        }
        
        // Show existing markers or create new ones
        for (Earthquake earthquake : earthquakes) {
            String id = earthquake.getEarthquakeId();
            LatLng position = new LatLng(earthquake.getLatitude(), earthquake.getLongitude());
            
            if (earthquakeMarkers.containsKey(id)) {
                // Show existing marker
                earthquakeMarkers.get(id).setVisible(true);
            } else {
                // Create new marker
                float magnitude = (float) earthquake.getMagnitude();
                float hue = 120 - (magnitude * 15); // Green to red based on magnitude
                hue = Math.max(0, Math.min(120, hue)); // Clamp between 0-120
                
                MarkerOptions markerOptions = new MarkerOptions()
                        .position(position)
                        .title("M" + magnitude)
                        .snippet(earthquake.getLocation())
                        .icon(BitmapDescriptorFactory.defaultMarker(hue));
                
                Marker marker = mMap.addMarker(markerOptions);
                marker.setTag(earthquake);
                earthquakeMarkers.put(id, marker);
            }
        }
    }
    
    private void showLandmarks(boolean show) {
        if (mMap == null) return;
        
        // Clear existing landmark markers if hiding
        if (!show) {
            for (Marker marker : landmarkMarkers.values()) {
                marker.setVisible(false);
            }
            return;
        }
        
        // Show existing markers or create new ones
        for (Landmark landmark : landmarks) {
            String id = landmark.getLandmarkId();
            LatLng position = new LatLng(landmark.getLatitude(), landmark.getLongitude());
            
            if (landmarkMarkers.containsKey(id)) {
                // Show existing marker
                landmarkMarkers.get(id).setVisible(true);
            } else {
                // Create new marker with custom icon based on category
                float hue = getLandmarkHue(landmark.getCategory());
                
                MarkerOptions markerOptions = new MarkerOptions()
                        .position(position)
                        .title(landmark.getName())
                        .snippet(landmark.getCategory())
                        .icon(BitmapDescriptorFactory.defaultMarker(hue));
                
                Marker marker = mMap.addMarker(markerOptions);
                marker.setTag(landmark);
                landmarkMarkers.put(id, marker);
            }
        }
    }
    
    private float getLandmarkHue(String category) {
        // Assign different colors based on landmark category
        switch (category.toLowerCase()) {
            case "hospital": return BitmapDescriptorFactory.HUE_RED;
            case "medical aid station": return BitmapDescriptorFactory.HUE_ROSE;
            case "shelter": return BitmapDescriptorFactory.HUE_AZURE;
            case "food distribution": return BitmapDescriptorFactory.HUE_ORANGE;
            case "water supply": return BitmapDescriptorFactory.HUE_BLUE;
            case "damaged building": return BitmapDescriptorFactory.HUE_MAGENTA;
            case "evacuation point": return BitmapDescriptorFactory.HUE_GREEN;
            case "road closure": return BitmapDescriptorFactory.HUE_YELLOW;
            case "safe zone": return BitmapDescriptorFactory.HUE_CYAN;
            default: return BitmapDescriptorFactory.HUE_VIOLET;
        }
    }
}
