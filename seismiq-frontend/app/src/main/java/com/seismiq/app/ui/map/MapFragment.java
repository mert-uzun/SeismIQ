package com.seismiq.app.ui.map;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
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
import com.seismiq.app.api.RetrofitClient;
import com.seismiq.app.auth.AuthService;
import com.seismiq.app.model.Earthquake;
import com.seismiq.app.model.Landmark;

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

    private AuthService authService;
    private EarthquakeApiService earthquakeApiService;
    private LandmarkApiService landmarkApiService;

    private List<Earthquake> earthquakes = new ArrayList<>();
    private List<Landmark> landmarks = new ArrayList<>();
    private final Map<String, Marker> earthquakeMarkers = new HashMap<>();
    private final Map<String, Marker> landmarkMarkers = new HashMap<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_map, container, false);

        // Initialize AuthService
        authService = new AuthService();

        // Initialize UI components
        toggleGroup = root.findViewById(R.id.toggle_map_view);
        btnEarthquakes = root.findViewById(R.id.btn_earthquakes);
        btnLandmarks = root.findViewById(R.id.btn_landmarks);
        btnAll = root.findViewById(R.id.btn_all);

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

        // Get ID token and initialize API services
        authService.getIdToken()
            .thenAccept(token -> {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        // Initialize services with token
                        earthquakeApiService = RetrofitClient.getClient(token).create(EarthquakeApiService.class);
                        landmarkApiService = RetrofitClient.getClient(token).create(LandmarkApiService.class);
                        
                        // Load data
                        loadEarthquakes();
                        loadLandmarks();
                    });
                }
            })
            .exceptionally(ex -> {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "Authentication error: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
                return null;
            });

        mMap.setOnInfoWindowClickListener(marker -> {
            Object tag = marker.getTag();
            if (tag instanceof Earthquake) {
                Earthquake eq = (Earthquake) tag;
                Toast.makeText(requireContext(), "Earthquake: " + eq.getMagnitude(), Toast.LENGTH_SHORT).show();
            } else if (tag instanceof Landmark) {
                Landmark lm = (Landmark) tag;
                Toast.makeText(requireContext(), "Landmark: " + lm.getName(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadEarthquakes() {
        earthquakeApiService.getRecentEarthquakes().enqueue(new Callback<List<Earthquake>>() {
            @Override
            public void onResponse(Call<List<Earthquake>> call, Response<List<Earthquake>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    earthquakes = response.body();
                    int checkedId = toggleGroup.getCheckedButtonId();
                    if (checkedId == R.id.btn_earthquakes || checkedId == R.id.btn_all) {
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
        landmarkApiService.getLandmarks().enqueue(new Callback<List<Landmark>>() {
            @Override
            public void onResponse(Call<List<Landmark>> call, Response<List<Landmark>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    landmarks = response.body();
                    int checkedId = toggleGroup.getCheckedButtonId();
                    if (checkedId == R.id.btn_landmarks || checkedId == R.id.btn_all) {
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

        for (Earthquake eq : earthquakes) {
            String id = eq.getEarthquakeId();
            LatLng pos = new LatLng(eq.getLatitude(), eq.getLongitude());

            Marker marker = earthquakeMarkers.get(id);
            if (marker != null) {
                marker.setVisible(show);
            } else if (show) {
                float hue = Math.max(0, Math.min(120, 120 - (float) eq.getMagnitude() * 15));
                Marker newMarker = mMap.addMarker(new MarkerOptions()
                        .position(pos)
                        .title("M" + eq.getMagnitude())
                        .snippet(eq.getLocation())
                        .icon(BitmapDescriptorFactory.defaultMarker(hue)));
                newMarker.setTag(eq);
                earthquakeMarkers.put(id, newMarker);
            }
        }
    }

    private void showLandmarks(boolean show) {
        if (mMap == null) return;

        for (Landmark lm : landmarks) {
            String id = lm.getLandmarkId();
            LatLng pos = new LatLng(lm.getLatitude(), lm.getLongitude());

            Marker marker = landmarkMarkers.get(id);
            if (marker != null) {
                marker.setVisible(show);
            } else if (show) {
                String categoryStr = lm.getCategory().toString(); // Convert Category to String
                Marker newMarker = mMap.addMarker(new MarkerOptions()
                        .position(pos)
                        .title(lm.getName())
                        .snippet(categoryStr)
                        .icon(BitmapDescriptorFactory.defaultMarker(getLandmarkHue(categoryStr))));
                newMarker.setTag(lm);
                landmarkMarkers.put(id, newMarker);
            }
        }
    }

    private float getLandmarkHue(String category) {
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
