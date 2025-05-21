package com.example.safewomen.fragments;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.safewomen.R;
import com.example.safewomen.databinding.FragmentMapBinding;
import com.example.safewomen.models.entities.LocationHistoryEntity;
import com.example.safewomen.services.LocationTrackingService;
import com.example.safewomen.viewmodels.LocationViewModel;
import com.example.safewomen.viewmodels.MapViewModel;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.snackbar.Snackbar;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MapFragment extends androidx.fragment.app.Fragment implements OnMapReadyCallback {
    private static final String TAG = "MapFragment";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };

    private FragmentMapBinding binding;
    private LocationViewModel locationViewModel;
    private MapViewModel mapViewModel;
    private GoogleMap googleMap;
    private boolean locationPermissionGranted = false;

    // Map of markers to location entities for easy lookup
    private final Map<Marker, LocationHistoryEntity> markerMap = new HashMap<>();

    public MapFragment() {
        // Required empty public constructor
    }

    public static MapFragment newInstance() {
        return new MapFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize ViewModels with AndroidViewModelFactory
        ViewModelProvider.Factory factory = ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().getApplication());
        locationViewModel = new ViewModelProvider(requireActivity(), factory).get(LocationViewModel.class);
        mapViewModel = new ViewModelProvider(requireActivity(), factory).get(MapViewModel.class);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout using view binding
        binding = FragmentMapBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize map
        binding.mapView.onCreate(savedInstanceState);
        binding.mapView.getMapAsync(this);

        // Set up UI controls
        setupUIControls();

        // Observe ViewModel data
        observeViewModelData();
    }
    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;

        // Check location permission
        checkLocationPermission();

        // Set up map settings
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setCompassEnabled(true);
        googleMap.getUiSettings().setMapToolbarEnabled(true);

        // Set up map click listeners
        googleMap.setOnMarkerClickListener(marker -> {
            LocationHistoryEntity location = markerMap.get(marker);
            if (location != null) {
                mapViewModel.selectLocation(location);
                showLocationDetails(location);
                return true;
            }
            return false;
        });

        // Load location history
        mapViewModel.loadLocationHistory();

        // Move camera to current location if available
        locationViewModel.getCurrentLocation().observe(getViewLifecycleOwner(), latLng -> {
            if (latLng != null) {
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f));
            }
        });
    }

    private void setupUIControls() {
        // Location tracking switch
        binding.switchLocationTracking.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                startLocationTracking();
            } else {
                stopLocationTracking();
            }
        });

        // Filter buttons
        binding.buttonToday.setOnClickListener(v -> {
            mapViewModel.filterLocationsByToday();
            updateFilterButtonsUI(v);
        });

        binding.buttonWeek.setOnClickListener(v -> {
            mapViewModel.filterLocationsByThisWeek();
            updateFilterButtonsUI(v);
        });

        binding.buttonMonth.setOnClickListener(v -> {
            mapViewModel.filterLocationsByThisMonth();
            updateFilterButtonsUI(v);
        });

        binding.buttonAllTime.setOnClickListener(v -> {
            mapViewModel.clearFilters();
            updateFilterButtonsUI(v);
        });

        // Show history switch
        binding.switchShowHistory.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updateMapWithLocationHistory();
        });

        // Safety zones switch
        binding.switchSafetyZones.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                updateMapWithSafetyZones();
            } else {
                // Clear safety zones by redrawing the map
                googleMap.clear();
                markerMap.clear();
                updateMapWithLocationHistory();
            }
        });

        // My location FAB
        binding.fabMyLocation.setOnClickListener(v -> {
            if (googleMap != null && locationPermissionGranted) {
                locationViewModel.loadCurrentLocation();
            } else {
                checkLocationPermission();
            }
        });
    }

    private void observeViewModelData() {
        // Observe location service status
        locationViewModel.getIsLocationServiceRunning().observe(getViewLifecycleOwner(), isRunning -> {
            binding.switchLocationTracking.setChecked(isRunning);
        });

        // Observe location history
        mapViewModel.getLocationHistory().observe(getViewLifecycleOwner(), locations -> {
            if (locations != null) {
                binding.textViewLocationCount.setText(String.format(Locale.getDefault(),
                        "%d locations", locations.size()));
                updateMapWithLocationHistory();
            }
        });

        // Observe selected location
        mapViewModel.getSelectedLocation().observe(getViewLifecycleOwner(), this::showLocationDetails);

        // Observe safety zones
        mapViewModel.getSafetyZones().observe(getViewLifecycleOwner(), safetyZones -> {
            if (binding.switchSafetyZones.isChecked()) {
                updateMapWithSafetyZones();
            }
        });

        // Observe loading state
        mapViewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });

        // Observe error messages
        mapViewModel.getErrorMessage().observe(getViewLifecycleOwner(), errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                Snackbar.make(binding.getRoot(), errorMessage, Snackbar.LENGTH_LONG).show();
            }
        });
    }
    private void updateMapWithLocationHistory() {
        if (googleMap == null || !binding.switchShowHistory.isChecked()) return;

        // Clear existing markers
        googleMap.clear();
        markerMap.clear();

        List<LocationHistoryEntity> locations = mapViewModel.getLocationHistory().getValue();
        if (locations == null || locations.isEmpty()) return;

        // Add markers for each location
        for (LocationHistoryEntity location : locations) {
            LatLng position = new LatLng(location.getLatitude(), location.getLongitude());

            MarkerOptions markerOptions = new MarkerOptions()
                    .position(position)
                    .title(formatDate(location.getTimestamp()))
                    .snippet(location.getAddress())
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));

            Marker marker = googleMap.addMarker(markerOptions);
            if (marker != null) {
                markerMap.put(marker, location);
            }
        }

        // If safety zones are enabled, add them too
        if (binding.switchSafetyZones.isChecked()) {
            updateMapWithSafetyZones();
        }

        // If there's only one location, zoom to it
        if (locations.size() == 1) {
            LocationHistoryEntity location = locations.get(0);
            LatLng position = new LatLng(location.getLatitude(), location.getLongitude());
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(position, 15f));
        }
    }

    private void updateMapWithSafetyZones() {
        if (googleMap == null || !binding.switchSafetyZones.isChecked()) return;

        List<MapViewModel.SafetyZone> safetyZones = mapViewModel.getSafetyZones().getValue();
        if (safetyZones == null || safetyZones.isEmpty()) return;

        for (MapViewModel.SafetyZone zone : safetyZones) {
            // Determine color based on safety rating
            int color;
            if (zone.getSafetyRating() >= 0.7f) {
                // Safe zone (green)
                color = Color.argb(70, 0, 255, 0);
            } else if (zone.getSafetyRating() >= 0.4f) {
                // Warning zone (yellow)
                color = Color.argb(70, 255, 255, 0);
            } else {
                // Danger zone (red)
                color = Color.argb(70, 255, 0, 0);
            }

            // Add circle for safety zone
            googleMap.addCircle(new CircleOptions()
                    .center(zone.getCenter())
                    .radius(zone.getRadiusMeters())
                    .strokeWidth(2)
                    .strokeColor(color)
                    .fillColor(color));
        }
    }

    private void showLocationDetails(LocationHistoryEntity location) {
        if (location == null) return;

        // Format date and time
        String dateTime = formatDate(location.getTimestamp());

        // Show location details in bottom sheet
        binding.textViewLocationAddress.setText(location.getAddress());
        binding.textViewLocationDateTime.setText(dateTime);
        binding.textViewLocationCoordinates.setText(
                String.format(Locale.getDefault(), "%.6f, %.6f",
                        location.getLatitude(), location.getLongitude()));

        // Show bottom sheet
        binding.bottomSheetLocationDetails.setVisibility(View.VISIBLE);

        // Set up close button
        binding.buttonCloseDetails.setOnClickListener(v -> {
            binding.bottomSheetLocationDetails.setVisibility(View.GONE);
        });

        // Set up share location button
        binding.buttonShareLocation.setOnClickListener(v -> {
            shareLocation(location);
        });

        // Set up navigate button
        binding.buttonNavigate.setOnClickListener(v -> {
            navigateToLocation(location);
        });

        // Set up mark as safe/unsafe buttons
        binding.buttonMarkSafe.setOnClickListener(v -> {
            mapViewModel.markLocationAsSafe(location);
            Snackbar.make(binding.getRoot(), "Location marked as safe", Snackbar.LENGTH_SHORT).show();
            binding.bottomSheetLocationDetails.setVisibility(View.GONE);
        });

        binding.buttonMarkUnsafe.setOnClickListener(v -> {
            mapViewModel.markLocationAsUnsafe(location);
            Snackbar.make(binding.getRoot(), "Location marked as unsafe", Snackbar.LENGTH_SHORT).show();
            binding.bottomSheetLocationDetails.setVisibility(View.GONE);
        });
    }
    private void shareLocation(LocationHistoryEntity location) {
        if (location == null) return;

        String shareText = String.format(Locale.getDefault(),
                "My location: %s\nCoordinates: %.6f, %.6f\nTime: %s",
                location.getAddress(),
                location.getLatitude(),
                location.getLongitude(),
                formatDate(location.getTimestamp()));

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "My Location");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);

        startActivity(Intent.createChooser(shareIntent, "Share Location"));
    }
    private void navigateToLocation(LocationHistoryEntity location) {
        if (location == null) return;

        // Create a Uri for Google Maps navigation
        String uri = String.format(Locale.getDefault(),
                "google.navigation:q=%f,%f",
                location.getLatitude(),
                location.getLongitude());

        Intent mapIntent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(uri));
        mapIntent.setPackage("com.google.android.apps.maps");

        if (mapIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
            startActivity(mapIntent);
        } else {
            Snackbar.make(binding.getRoot(), "Google Maps app not installed", Snackbar.LENGTH_LONG).show();
        }
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            locationPermissionGranted = true;
            enableMyLocation();
        } else {
            ActivityCompat.requestPermissions(requireActivity(), REQUIRED_PERMISSIONS, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    private void enableMyLocation() {
        if (googleMap == null) return;

        try {
            if (locationPermissionGranted) {
                googleMap.setMyLocationEnabled(true);
                googleMap.getUiSettings().setMyLocationButtonEnabled(false); // We use our own FAB
            } else {
                googleMap.setMyLocationEnabled(false);
                googleMap.getUiSettings().setMyLocationButtonEnabled(false);
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Error enabling my location: " + e.getMessage());
        }
    }

    private void startLocationTracking() {
        if (!locationPermissionGranted) {
            checkLocationPermission();
            return;
        }

        locationViewModel.startLocationTracking();
    }

    private void stopLocationTracking() {
        locationViewModel.stopLocationTracking();
    }

    private void updateFilterButtonsUI(View selectedButton) {
        // Reset all buttons to outlined style
        binding.buttonToday.setBackgroundTintList(null);
        binding.buttonWeek.setBackgroundTintList(null);
        binding.buttonMonth.setBackgroundTintList(null);
        binding.buttonAllTime.setBackgroundTintList(null);

        // Highlight selected button
        if (selectedButton != null) {
            selectedButton.setBackgroundTintList(
                    ContextCompat.getColorStateList(requireContext(), R.color.colorPrimaryLight));
        }
    }

    private String formatDate(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                locationPermissionGranted = true;
                enableMyLocation();
                locationViewModel.loadCurrentLocation();
            } else {
                locationPermissionGranted = false;
                Snackbar.make(binding.getRoot(), "Location permission is required to show your location on the map", Snackbar.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        binding.mapView.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        binding.mapView.onResume();

        // Check location service status
        locationViewModel.checkLocationServiceStatus();
    }

    @Override
    public void onPause() {
        binding.mapView.onPause();
        super.onPause();
    }

    @Override
    public void onStop() {
        binding.mapView.onStop();
        super.onStop();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        binding.mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        binding.mapView.onLowMemory();
    }

    @Override
    public void onDestroyView() {
        binding.mapView.onDestroy();
        binding = null;
        super.onDestroyView();
    }
}
