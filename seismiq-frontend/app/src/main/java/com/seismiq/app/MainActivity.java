package com.seismiq.app;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.appbar.MaterialToolbar;
import com.seismiq.app.databinding.ActivityMainBinding;
import com.seismiq.app.utils.FCMUtils;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        MaterialToolbar toolbar = binding.toolbar;
        setSupportActionBar(toolbar);

        BottomNavigationView navView = binding.navView;
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment_activity_main);
        NavController navController = navHostFragment.getNavController();

        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_map, R.id.navigation_post,
                R.id.navigation_landmark, R.id.navigation_settings
        ).build();

        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(navView, navController);

        // Initialize Firebase Cloud Messaging for notifications
        FCMUtils.initializeFCM(this);
        
        // Handle notification click if app was opened from notification
        handleNotificationIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleNotificationIntent(intent);
    }

    /**
     * Handle notification click - navigate to specific landmark if provided
     */
    private void handleNotificationIntent(Intent intent) {
        if (intent != null && intent.hasExtra("landmarkId")) {
            String landmarkId = intent.getStringExtra("landmarkId");
            String latitude = intent.getStringExtra("latitude");
            String longitude = intent.getStringExtra("longitude");
            
            Log.i("MainActivity", "Opened from notification - Landmark: " + landmarkId);
            
            // Navigate to map fragment with landmark location
            NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.nav_host_fragment_activity_main);
            NavController navController = navHostFragment.getNavController();
            
            Bundle args = new Bundle();
            args.putString("landmarkId", landmarkId);
            args.putString("latitude", latitude);
            args.putString("longitude", longitude);
            
            navController.navigate(R.id.navigation_map, args);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment_activity_main);
        NavController navController = navHostFragment.getNavController();
        return navController.navigateUp() || super.onSupportNavigateUp();
    }
}
