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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.seismiq.app.R;
import com.seismiq.app.api.EarthquakeApiService;
import com.seismiq.app.api.RetrofitClient;
import com.seismiq.app.auth.AuthService;
import com.seismiq.app.databinding.FragmentHomeBinding;
import com.seismiq.app.model.Earthquake;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private EarthquakeAdapter adapter;
    private List<Earthquake> earthquakes = new ArrayList<>();
    private AuthService authService;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        
        authService = new AuthService();
        
        recyclerView = binding.recyclerViewEarthquakes;
        progressBar = binding.progressBarHome;
        
        // Set up RecyclerView
        adapter = new EarthquakeAdapter(earthquakes);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
        
        // Load earthquakes data
        loadEarthquakes();
        
        return root;
    }

    private void loadEarthquakes() {
        progressBar.setVisibility(View.VISIBLE);
        
        // Get token for API call
        authService.getIdToken()
                .thenAccept(token -> {
                    // Create API service
                    EarthquakeApiService apiService = RetrofitClient.getClient(token)
                            .create(EarthquakeApiService.class);
                    
                    // Call the API
                    apiService.getEarthquakes().enqueue(new Callback<List<Earthquake>>() {
                        @Override
                        public void onResponse(Call<List<Earthquake>> call, Response<List<Earthquake>> response) {
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    progressBar.setVisibility(View.GONE);
                                    
                                    if (response.isSuccessful() && response.body() != null) {
                                        earthquakes.clear();
                                        earthquakes.addAll(response.body());
                                        adapter.notifyDataSetChanged();
                                    } else {
                                        Toast.makeText(getContext(), "Failed to load earthquakes", Toast.LENGTH_SHORT).show();
                                    }
                                });
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
    
    // Simple adapter for Earthquake RecyclerView
    private static class EarthquakeAdapter extends RecyclerView.Adapter<EarthquakeAdapter.ViewHolder> {
        
        private List<Earthquake> earthquakes;
        
        public EarthquakeAdapter(List<Earthquake> earthquakes) {
            this.earthquakes = earthquakes;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // This is a simplified adapter without actual item layout
            // In a real app, you would inflate a layout for earthquake items
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(android.R.layout.simple_list_item_2, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Earthquake earthquake = earthquakes.get(position);
            holder.textView1.setText("Magnitude: " + earthquake.getMagnitude());
            holder.textView2.setText(earthquake.getLocation() + " - " + earthquake.getTimestamp());
        }

        @Override
        public int getItemCount() {
            return earthquakes.size();
        }
        
        static class ViewHolder extends RecyclerView.ViewHolder {
            android.widget.TextView textView1;
            android.widget.TextView textView2;
            
            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                textView1 = itemView.findViewById(android.R.id.text1);
                textView2 = itemView.findViewById(android.R.id.text2);
            }
        }
    }
}
