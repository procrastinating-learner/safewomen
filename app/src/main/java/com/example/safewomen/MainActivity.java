package com.example.safewomen;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.safewomen.activities.LoginActivity;
import com.example.safewomen.databinding.ActivityMainBinding;
import com.example.safewomen.models.entities.UserEntity;
import com.example.safewomen.repositories.AuthRepository;
import com.example.safewomen.utils.PreferenceManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;

/**
 * Main container activity with navigation
 * Hosts all main fragments and handles navigation between them
 */
public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private static final String TAG = "MainActivity";

    // UI components
    private ActivityMainBinding binding;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private BottomNavigationView bottomNavigationView;
    private Toolbar toolbar;

    // Navigation
    private NavController navController;
    private AppBarConfiguration appBarConfiguration;

    // Repositories
    private AuthRepository authRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: Starting MainActivity initialization");

        try {
            // Initialize view binding
            Log.d(TAG, "onCreate: Initializing view binding");
            binding = ActivityMainBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());

            // Initialize repositories
            Log.d(TAG, "onCreate: Initializing repositories");
            AuthRepository.init(getApplication());
            authRepository = AuthRepository.getInstance();

            // Set up UI components
            Log.d(TAG, "onCreate: Setting up UI components");
            setupToolbar();
            setupNavigationDrawer();
            setupBottomNavigation();

            // Check authentication status
            Log.d(TAG, "onCreate: Checking authentication status");
            checkAuthStatus();

            // Handle intent (for deep linking)
            Log.d(TAG, "onCreate: Handling intent");
            handleIntent(getIntent());

            Log.d(TAG, "onCreate: MainActivity initialization completed successfully");
        } catch (Exception e) {
            Log.e(TAG, "onCreate: Error initializing MainActivity", e);
            throw e; // Re-throw to see the actual crash
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // Handle new intents (for deep linking when app is already running)
        handleIntent(intent);
    }

    private void setupToolbar() {
        try {
            Log.d(TAG, "setupToolbar: Setting up toolbar");
            toolbar = binding.toolbar;
            setSupportActionBar(toolbar);
            Log.d(TAG, "setupToolbar: Toolbar setup completed");
        } catch (Exception e) {
            Log.e(TAG, "setupToolbar: Error setting up toolbar", e);
            throw e;
        }
    }

    private void setupNavigationDrawer() {
        try {
            Log.d(TAG, "setupNavigationDrawer: Setting up navigation drawer");
            drawerLayout = binding.drawerLayout;
            navigationView = binding.navView;

            // Set up the navigation drawer toggle
            ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                    this, drawerLayout, toolbar,
                    R.string.navigation_drawer_open,
                    R.string.navigation_drawer_close);
            drawerLayout.addDrawerListener(toggle);
            toggle.syncState();

            // Set up navigation item selection listener
            navigationView.setNavigationItemSelectedListener(this);

            // Define top-level destinations
            appBarConfiguration = new AppBarConfiguration.Builder(
                    R.id.nav_home, R.id.nav_map, R.id.nav_contacts)
                    .setOpenableLayout(drawerLayout)
                    .build();
            Log.d(TAG, "setupNavigationDrawer: Navigation drawer setup completed");
        } catch (Exception e) {
            Log.e(TAG, "setupNavigationDrawer: Error setting up navigation drawer", e);
            throw e;
        }
    }

    private void setupBottomNavigation() {
        try {
            Log.d(TAG, "setupBottomNavigation: Setting up bottom navigation");
            bottomNavigationView = binding.bottomNavView;

            // Get the NavController
            navController = Navigation.findNavController(this, R.id.nav_host_fragment);
            Log.d(TAG, "setupBottomNavigation: NavController found");

            // Set up the ActionBar with the NavController
            NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
            Log.d(TAG, "setupBottomNavigation: ActionBar setup completed");

            // Set up the NavigationView with the NavController
            NavigationUI.setupWithNavController(navigationView, navController);
            Log.d(TAG, "setupBottomNavigation: NavigationView setup completed");

            // Set up the BottomNavigationView with the NavController
            NavigationUI.setupWithNavController(bottomNavigationView, navController);
            Log.d(TAG, "setupBottomNavigation: BottomNavigationView setup completed");

            // Handle visibility of bottom navigation based on destination
            navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                Log.d(TAG, "onDestinationChanged: Navigating to " + destination.getLabel());
                int destinationId = destination.getId();
                if (destinationId == R.id.nav_home ||
                        destinationId == R.id.nav_map ||
                        destinationId == R.id.nav_contacts) {
                    bottomNavigationView.setVisibility(View.VISIBLE);
                } else {
                    bottomNavigationView.setVisibility(View.GONE);
                }

                if (getSupportActionBar() != null) {
                    getSupportActionBar().setTitle(destination.getLabel());
                }
            });
            Log.d(TAG, "setupBottomNavigation: Bottom navigation setup completed");
        } catch (Exception e) {
            Log.e(TAG, "setupBottomNavigation: Error setting up bottom navigation", e);
            throw e;
        }
    }

    private void checkAuthStatus() {
        try {
            Log.d(TAG, "checkAuthStatus: Skipping authentication for development");
            // Skip authentication check and directly update navigation header
            UserEntity mockUser = new UserEntity(
                "dev_user",
                "Test User",
                "test@example.com",
                "1234567890",
                "dev_token",
                System.currentTimeMillis()
            );
            updateNavigationHeader(mockUser);

            // Set logged in status in preferences
            PreferenceManager.getInstance().setLoggedIn(true);
            PreferenceManager.getInstance().setUserId(mockUser.getId());
            PreferenceManager.getInstance().setAuthToken(mockUser.getAuth_token());

        } catch (Exception e) {
            Log.e(TAG, "checkAuthStatus: Error in development mode", e);
            throw e;
        }
    }

    private void updateNavigationHeader(UserEntity user) {
        try {
            Log.d(TAG, "updateNavigationHeader: Updating navigation header for user: " + user.getName());
            View headerView = navigationView.getHeaderView(0);

            if (headerView != null) {
                TextView nameTextView = headerView.findViewById(R.id.textViewUserName);
                TextView emailTextView = headerView.findViewById(R.id.textViewUserEmail);

                if (nameTextView != null && user.getName() != null) {
                    nameTextView.setText(user.getName());
                }

                if (emailTextView != null && user.getEmail() != null) {
                    emailTextView.setText(user.getEmail());
                }
                Log.d(TAG, "updateNavigationHeader: Navigation header updated successfully");
            } else {
                Log.w(TAG, "updateNavigationHeader: Header view is null");
            }
        } catch (Exception e) {
            Log.e(TAG, "updateNavigationHeader: Error updating navigation header", e);
        }
    }

    private void handleIntent(Intent intent) {
        if (intent == null) {
            Log.d(TAG, "handleIntent: Intent is null");
            return;
        }

        try {
            String action = intent.getAction();
            Log.d(TAG, "handleIntent: Processing intent with action: " + action);

            if (Intent.ACTION_VIEW.equals(action)) {
                String uri = intent.getDataString();
                if (uri != null) {
                    Log.d(TAG, "handleIntent: Deep link URI: " + uri);
                }
            } else if ("OPEN_SOS".equals(action)) {
                Log.d(TAG, "handleIntent: Navigating to SOS screen");
                navController.navigate(R.id.nav_sos);
            } else if ("OPEN_MAP".equals(action)) {
                Log.d(TAG, "handleIntent: Navigating to Map screen");
                navController.navigate(R.id.nav_map);
            } else if ("OPEN_CONTACTS".equals(action)) {
                Log.d(TAG, "handleIntent: Navigating to Contacts screen");
                navController.navigate(R.id.nav_contacts);
            } else if ("OPEN_ALERT_DETAILS".equals(action)) {
                Log.d(TAG, "handleIntent: Navigating to Alert Details screen");
                String alertId = intent.getStringExtra("ALERT_ID");
                if (alertId != null) {
                    Bundle args = new Bundle();
                    args.putString("alertId", alertId);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "handleIntent: Error handling intent", e);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        try {
            Log.d(TAG, "onSupportNavigateUp: Navigating up");
            return NavigationUI.navigateUp(navController, appBarConfiguration)
                    || super.onSupportNavigateUp();
        } catch (Exception e) {
            Log.e(TAG, "onSupportNavigateUp: Error navigating up", e);
            return super.onSupportNavigateUp();
        }
    }

    @Override
    public void onBackPressed() {
        try {
            Log.d(TAG, "onBackPressed: Handling back press");
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START);
            } else {
                super.onBackPressed();
            }
        } catch (Exception e) {
            Log.e(TAG, "onBackPressed: Error handling back press", e);
            super.onBackPressed();
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        try {
            Log.d(TAG, "onNavigationItemSelected: Item selected: " + item.getTitle());
            int id = item.getItemId();
            
            // Handle navigation view item clicks here
            if (id == R.id.nav_home) {
                navController.navigate(R.id.nav_home);
            } else if (id == R.id.nav_map) {
                navController.navigate(R.id.nav_map);
            } else if (id == R.id.nav_contacts) {
                navController.navigate(R.id.nav_contacts);
            } else if (id == R.id.nav_logout) {
                authRepository.logout();
            } else if (id == R.id.nav_profile) {
                navController.navigate(R.id.nav_profile);
            } else if (id == R.id.nav_settings) {
                navController.navigate(R.id.nav_settings);
            } else if (id == R.id.nav_about) {
                navController.navigate(R.id.nav_about);
            } else if (id == R.id.nav_help) {
                navController.navigate(R.id.nav_help);
            }

            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "onNavigationItemSelected: Error handling navigation item selection", e);
            return false;
        }
    }
}
