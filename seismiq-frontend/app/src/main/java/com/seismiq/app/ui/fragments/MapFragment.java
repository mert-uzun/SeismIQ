package com.seismiq.app.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
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
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.maps.android.heatmaps.HeatmapTileProvider;
import com.google.maps.android.heatmaps.WeightedLatLng;
import com.seismiq.app.R;
import com.seismiq.app.api.EarthquakeApiService;
import com.seismiq.app.api.ReportApiService;
import com.seismiq.app.api.RetrofitClient;
import com.seismiq.app.auth.AuthService;
import com.seismiq.app.databinding.FragmentMapBinding;
import com.seismiq.app.model.Earthquake;
import com.seismiq.app.model.Report;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MapFragment extends Fragment implements OnMapReadyCallback {

    private FragmentMapBinding binding;
    private GoogleMap googleMap;
    private ProgressBar progressBar;
    private AuthService authService;
    private List<Earthquake> earthquakes = new ArrayList<>();
    private List<Report> reports = new ArrayList<>();
    private HeatmapTileProvider heatmapProvider;
    private TileOverlay heatmapOverlay;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentMapBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        
        authService = new AuthService();
        progressBar = binding.progressBarMap;
        
        // Get the SupportMapFragment and request notification when map is ready
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.mapFragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
        
        return root;
    }

    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;
        
        // Set map UI settings
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setMyLocationButtonEnabled(true);
        
        // Set initial map position (Turkey as default)
        LatLng turkey = new LatLng(38.9637, 35.2433);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(turkey, 6));
        
        // Load data for map
        loadDataForMap();
    }
    
    private void loadDataForMap() {
        progressBar.setVisibility(View.VISIBLE);
        
        // Get token for API calls
        authService.getIdToken()
                .thenAccept(token -> {
                    // Create API services
                    EarthquakeApiService earthquakeApiService = RetrofitClient.getClient(token)
                            .create(EarthquakeApiService.class);
                    ReportApiService reportApiService = RetrofitClient.getClient(token)
                            .create(ReportApiService.class);
                    
                    // Load earthquakes
                    earthquakeApiService.getEarthquakes().enqueue(new Callback<List<Earthquake>>() {
                        @Override
                        public void onResponse(Call<List<Earthquake>> call, Response<List<Earthquake>> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                earthquakes.clear();
                                earthquakes.addAll(response.body());
                                
                                // Load reports after earthquakes
                                loadReports(reportApiService);
                            } else {
                                if (getActivity() != null) {
                                    getActivity().runOnUiThread(() -> {
                                        progressBar.setVisibility(View.GONE);
                                        Toast.makeText(getContext(), "Failed to load earthquakes", Toast.LENGTH_SHORT).show();
                                    });
                                }
                            }
                        }

                        @Override
                        public void onFailure(Call<List<Earthquake>> call, Throwable t) {
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    progressBar.setVisibility(View.GONE);
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
                            Toast.makeText(getContext(), "Authentication error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        });
                    }
                    return null;
                });
    }
    
    private void loadReports(ReportApiService reportApiService) {
        // Load all reports
        reportApiService.getReports().enqueue(new Callback<List<Report>>() {
            @Override
            public void onResponse(Call<List<Report>> call, Response<List<Report>> response) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        
                        if (response.isSuccessful() && response.body() != null) {
                            reports.clear();
                            reports.addAll(response.body());
                            
                            // Update the map with all data
                            updateMapWithData();
                        } else {
                            Toast.makeText(getContext(), "Failed to load reports", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }

            @Override
            public void onFailure(Call<List<Report>> call, Throwable t) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(getContext(), "Error loading reports: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }
    
    private void updateMapWithData() {
        if (googleMap == null) return;
        
        // Clear previous markers
        googleMap.clear();
        if (heatmapOverlay != null) {
            heatmapOverlay.remove();
        }
        
        // Add earthquake markers
        for (Earthquake earthquake : earthquakes) {
            LatLng position = new LatLng(earthquake.getLatitude(), earthquake.getLongitude());
            googleMap.addMarker(new MarkerOptions()
                    .position(position)
                    .title("Magnitude: " + earthquake.getMagnitude())
                    .snippet(earthquake.getLocation()));
        }
        
        // Create heatmap from reports
        List<WeightedLatLng> heatmapData = new ArrayList<>();
        for (Report report : reports) {
            LatLng position = new LatLng(report.getLatitude(), report.getLongitude());
            heatmapData.add(new WeightedLatLng(position, 1.0)); // Weight can be based on report category
        }
        
        // Only create heatmap if we have data
        if (!heatmapData.isEmpty()) {
            // Create heatmap provider and add to map
            heatmapProvider = new HeatmapTileProvider.Builder()
                    .weightedData(heatmapData)
                    .build();
            
            heatmapOverlay = googleMap.addTileOverlay(new TileOverlayOptions().tileProvider(heatmapProvider));
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
