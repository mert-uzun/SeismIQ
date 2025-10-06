package com.seismiq.app.ui.home;

import android.app.AlertDialog;
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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.tabs.TabLayout;
import com.seismiq.app.R;
import com.seismiq.app.api.EarthquakeApiService;
import com.seismiq.app.api.LandmarkApiService;
import com.seismiq.app.api.ReportApiService;
import com.seismiq.app.api.RetrofitClient;
import com.seismiq.app.auth.AuthService;
import com.seismiq.app.model.Earthquake;
import com.seismiq.app.model.Landmark;
import com.seismiq.app.model.MyPost;
import com.seismiq.app.model.Report;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {

    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar progressBar;
    private TabLayout tabLayout;
    private FeedAdapter feedAdapter;
    private MyPostsAdapter myPostsAdapter;
    private AuthService authService;
    private String currentUserId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.recyclerViewEarthquakes);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        progressBar = view.findViewById(R.id.progressBar);
        tabLayout = view.findViewById(R.id.tabLayout);

        authService = new AuthService();
        feedAdapter = new FeedAdapter();
        myPostsAdapter = new MyPostsAdapter();

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(feedAdapter); // Default to all posts

        // Set up delete listener for My Posts adapter
        myPostsAdapter.setDeleteListener(this::showDeleteConfirmation);

        // Set up tab selection listener
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    // All Posts tab
                    recyclerView.setAdapter(feedAdapter);
                    loadFeed();
                } else {
                    // My Posts tab
                    recyclerView.setAdapter(myPostsAdapter);
                    loadMyPosts();
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        // Set up swipe to refresh
        swipeRefreshLayout.setOnRefreshListener(() -> {
            if (tabLayout.getSelectedTabPosition() == 0) {
                loadFeed();
            } else {
                loadMyPosts();
            }
        });

        // Get current user ID and load initial data
        authService.getCurrentUserId()
                .thenAccept(userId -> {
                    currentUserId = userId;
                    loadFeed();
                })
                .exceptionally(error -> {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(requireContext(), 
                                    "Error getting user info: " + error.getMessage(), 
                                    Toast.LENGTH_SHORT).show();
                            loadFeed(); // Load anyway, just can't filter by user
                        });
                    }
                    return null;
                });
    }

    private void loadFeed() {
        progressBar.setVisibility(View.VISIBLE);

        authService.getIdToken()
                .thenAccept(token -> {
                    if (getActivity() == null) return;

                    // Create API services with token
                    EarthquakeApiService earthquakeService = RetrofitClient.getClient(token)
                            .create(EarthquakeApiService.class);
                    ReportApiService reportService = RetrofitClient.getClient(token)
                            .create(ReportApiService.class);

                    // Fetch both reports and earthquakes
                    loadReports(reportService);
                    loadEarthquakes(earthquakeService);
                })
                .exceptionally(error -> {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            swipeRefreshLayout.setRefreshing(false);
                            Toast.makeText(requireContext(), 
                                    "Authentication error: " + error.getMessage(), 
                                    Toast.LENGTH_SHORT).show();
                        });
                    }
                    return null;
                });
    }

    private final List<Report> reports = new ArrayList<>();
    private final List<Earthquake> earthquakes = new ArrayList<>();

    private void loadReports(ReportApiService service) {
        service.getReports().enqueue(new Callback<List<Report>>() {
            @Override
            public void onResponse(Call<List<Report>> call, Response<List<Report>> response) {
                if (getActivity() == null) return;

                getActivity().runOnUiThread(() -> {
                    if (response.isSuccessful() && response.body() != null) {
                        reports.clear();
                        reports.addAll(response.body());
                        combineFeed();
                    }
                    progressBar.setVisibility(View.GONE);
                    swipeRefreshLayout.setRefreshing(false);
                });
            }

            @Override
            public void onFailure(Call<List<Report>> call, Throwable t) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        swipeRefreshLayout.setRefreshing(false);
                        combineFeed(); // Still show earthquakes if reports fail
                    });
                }
            }
        });
    }

    private void loadEarthquakes(EarthquakeApiService service) {
        service.getEarthquakes().enqueue(new Callback<List<Earthquake>>() {
            @Override
            public void onResponse(Call<List<Earthquake>> call, Response<List<Earthquake>> response) {
                if (getActivity() == null) return;

                getActivity().runOnUiThread(() -> {
                    if (response.isSuccessful() && response.body() != null) {
                        earthquakes.clear();
                        earthquakes.addAll(response.body());
                        combineFeed();
                    }
                });
            }

            @Override
            public void onFailure(Call<List<Earthquake>> call, Throwable t) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        combineFeed(); // Still show reports if earthquakes fail
                    });
                }
            }
        });
    }

    private void combineFeed() {
        List<FeedItem> feedItems = new ArrayList<>();

        // Add reports to feed
        for (Report report : reports) {
            feedItems.add(FeedItem.fromReport(report));
        }

        // Add earthquakes to feed
        for (Earthquake earthquake : earthquakes) {
            feedItems.add(FeedItem.fromEarthquake(earthquake));
        }

        // Sort by timestamp (most recent first)
        List<FeedItem> sortedItems = feedItems.stream()
                .sorted(Comparator.comparing(FeedItem::getTimestamp).reversed())
                .collect(Collectors.toList());

        feedAdapter.setItems(sortedItems);
    }

    private final List<Report> myReports = new ArrayList<>();
    private final List<Landmark> myLandmarks = new ArrayList<>();

    private void loadMyPosts() {
        if (currentUserId == null) {
            Toast.makeText(requireContext(), "User ID not available", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        authService.getIdToken()
                .thenAccept(token -> {
                    if (getActivity() == null) return;

                    // Create API services with token
                    ReportApiService reportService = RetrofitClient.getClient(token)
                            .create(ReportApiService.class);
                    LandmarkApiService landmarkService = RetrofitClient.getClient(token)
                            .create(LandmarkApiService.class);

                    // Load user's reports and landmarks
                    loadMyReports(reportService);
                    loadMyLandmarks(landmarkService);
                })
                .exceptionally(error -> {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            swipeRefreshLayout.setRefreshing(false);
                            Toast.makeText(requireContext(), 
                                    "Authentication error: " + error.getMessage(), 
                                    Toast.LENGTH_SHORT).show();
                        });
                    }
                    return null;
                });
    }

    private void loadMyReports(ReportApiService service) {
        service.getReportsByUserId(currentUserId).enqueue(new Callback<List<Report>>() {
            @Override
            public void onResponse(Call<List<Report>> call, Response<List<Report>> response) {
                if (getActivity() == null) return;

                getActivity().runOnUiThread(() -> {
                    if (response.isSuccessful() && response.body() != null) {
                        myReports.clear();
                        myReports.addAll(response.body());
                        combineMyPosts();
                    }
                    progressBar.setVisibility(View.GONE);
                    swipeRefreshLayout.setRefreshing(false);
                });
            }

            @Override
            public void onFailure(Call<List<Report>> call, Throwable t) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        swipeRefreshLayout.setRefreshing(false);
                        Toast.makeText(requireContext(), 
                                "Error loading reports: " + t.getMessage(), 
                                Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    private void loadMyLandmarks(LandmarkApiService service) {
        service.getLandmarks().enqueue(new Callback<List<Landmark>>() {
            @Override
            public void onResponse(Call<List<Landmark>> call, Response<List<Landmark>> response) {
                if (getActivity() == null) return;

                getActivity().runOnUiThread(() -> {
                    if (response.isSuccessful() && response.body() != null) {
                        myLandmarks.clear();
                        // Filter landmarks created by current user
                        for (Landmark landmark : response.body()) {
                            if (currentUserId.equals(landmark.getCreatedBy())) {
                                myLandmarks.add(landmark);
                            }
                        }
                        combineMyPosts();
                    }
                });
            }

            @Override
            public void onFailure(Call<List<Landmark>> call, Throwable t) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        combineMyPosts(); // Still show reports if landmarks fail
                    });
                }
            }
        });
    }

    private void combineMyPosts() {
        List<MyPost> myPosts = new ArrayList<>();

        // Add user's reports
        for (Report report : myReports) {
            myPosts.add(new MyPost(report));
        }

        // Add user's landmarks
        for (Landmark landmark : myLandmarks) {
            myPosts.add(new MyPost(landmark));
        }

        // Sort by timestamp (most recent first)
        List<MyPost> sortedPosts = myPosts.stream()
                .sorted(Comparator.comparing(MyPost::getCreatedAt, 
                        Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .collect(Collectors.toList());

        myPostsAdapter.setPosts(sortedPosts);
    }

    private void showDeleteConfirmation(MyPost post) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete " + post.getTypeString())
                .setMessage("Are you sure you want to delete this " + 
                        post.getTypeString().toLowerCase() + "?")
                .setPositiveButton("Delete", (dialog, which) -> deletePost(post))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deletePost(MyPost post) {
        progressBar.setVisibility(View.VISIBLE);

        authService.getIdToken()
                .thenAccept(token -> {
                    if (getActivity() == null) return;

                    if (post.getType() == MyPost.TYPE_REPORT) {
                        deleteReport(token, post);
                    } else {
                        deleteLandmark(token, post);
                    }
                })
                .exceptionally(error -> {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(requireContext(), 
                                    "Authentication error: " + error.getMessage(), 
                                    Toast.LENGTH_SHORT).show();
                        });
                    }
                    return null;
                });
    }

    private void deleteReport(String token, MyPost post) {
        ReportApiService service = RetrofitClient.getClient(token)
                .create(ReportApiService.class);

        service.deleteReport(post.getId()).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (getActivity() == null) return;

                getActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    if (response.isSuccessful()) {
                        myPostsAdapter.removePost(post);
                        Toast.makeText(requireContext(), 
                                "Report deleted successfully", 
                                Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(requireContext(), 
                                "Failed to delete report", 
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(requireContext(), 
                                "Error deleting report: " + t.getMessage(), 
                                Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    private void deleteLandmark(String token, MyPost post) {
        LandmarkApiService service = RetrofitClient.getClient(token)
                .create(LandmarkApiService.class);

        service.deleteLandmark(post.getId()).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (getActivity() == null) return;

                getActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    if (response.isSuccessful()) {
                        myPostsAdapter.removePost(post);
                        Toast.makeText(requireContext(), 
                                "Landmark deleted successfully", 
                                Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(requireContext(), 
                                "Failed to delete landmark", 
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(requireContext(), 
                                "Error deleting landmark: " + t.getMessage(), 
                                Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }
}
