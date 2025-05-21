package com.example.safewomen.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.safewomen.R;
import com.example.safewomen.adapters.SafetyTipsAdapter;
import com.example.safewomen.databinding.FragmentHomeBinding;
import com.example.safewomen.models.SafetyTip;
import com.example.safewomen.services.SosAlertService;
import com.example.safewomen.viewmodels.AlertViewModel;
import com.example.safewomen.viewmodels.DashboardViewModel;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends androidx.fragment.app.Fragment {

    private FragmentHomeBinding binding;
    private DashboardViewModel dashboardViewModel;
    private AlertViewModel alertViewModel;
    private SafetyTipsAdapter tipsAdapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Initialize ViewModels
        dashboardViewModel = new ViewModelProvider(requireActivity()).get(DashboardViewModel.class);
        alertViewModel = new ViewModelProvider(this).get(AlertViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Set up UI components
        setupUI();

        // Set up observers for LiveData from ViewModels
        setupObservers();

        // Load data
        dashboardViewModel.refreshDashboard();
    }

    private void setupUI() {
        // Set up SOS button
        binding.buttonSos.setOnClickListener(v -> {
            triggerSosAlert();
        });

        // Set up feature navigation cards
        binding.cardMap.setOnClickListener(v -> {
            navigateTo(R.id.nav_map);
        });

        binding.cardContacts.setOnClickListener(v -> {
            navigateTo(R.id.nav_contacts);
        });

        binding.cardAlerts.setOnClickListener(v -> {
         //   navigateTo(R.id.nav_alerts);
        });

        binding.cardSettings.setOnClickListener(v -> {
            navigateTo(R.id.nav_settings);
        });

        // Set up safety tips RecyclerView
        setupSafetyTips();
    }
    private void setupSafetyTips() {
        tipsAdapter = new SafetyTipsAdapter(getSafetyTips());
        binding.recyclerViewSafetyTips.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerViewSafetyTips.setAdapter(tipsAdapter);
    }

    private void setupObservers() {
        // Observe user data
        dashboardViewModel.getCurrentUser().observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                binding.textViewWelcome.setText("Welcome, " + user.getName());
            }
        });

        // Observe safety status
        dashboardViewModel.getSafetyStatus().observe(getViewLifecycleOwner(), safetyStatus -> {
            updateSafetyStatusUI(safetyStatus);
        });

        // Observe active alert status
        alertViewModel.isAlertActive().observe(getViewLifecycleOwner(), isActive -> {
            binding.buttonSos.setText(isActive ? "CANCEL" : "SOS");
            binding.buttonSos.setBackgroundResource(isActive ?
                    R.drawable.circle_button_active : R.drawable.circle_button);
        });
    }

    private void updateSafetyStatusUI(DashboardViewModel.SafetyStatus status) {
        switch (status) {
            case SAFE:
                binding.textViewSafetyStatus.setText("You are safe");
                binding.textViewSafetyStatus.setTextColor(getResources().getColor(R.color.safeGreen));
                binding.imageSafetyStatus.setImageResource(R.drawable.ic_safe);
                break;
            case WARNING:
                binding.textViewSafetyStatus.setText("Safety services inactive");
                binding.textViewSafetyStatus.setTextColor(getResources().getColor(R.color.warningYellow));
                binding.imageSafetyStatus.setImageResource(R.drawable.ic_warning);
                break;
            case DANGER:
                binding.textViewSafetyStatus.setText("Alert active");
                binding.textViewSafetyStatus.setTextColor(getResources().getColor(R.color.dangerRed));
                binding.imageSafetyStatus.setImageResource(R.drawable.ic_danger);
                break;
            default:
                binding.textViewSafetyStatus.setText("Status unknown");
                binding.textViewSafetyStatus.setTextColor(getResources().getColor(android.R.color.darker_gray));
                binding.imageSafetyStatus.setImageResource(R.drawable.ic_help);
                break;
        }
    }


    private void triggerSosAlert() {
        if (alertViewModel.isAlertActive().getValue() != null &&
                alertViewModel.isAlertActive().getValue()) {
            // Cancel active alert
            alertViewModel.cancelAlert();
        } else {
            // Trigger new alert
            Intent intent = new Intent(requireContext(), SosAlertService.class);
            intent.setAction("TRIGGER_SOS");
            intent.putExtra("TRIGGER_METHOD", "manual_button");
            requireContext().startService(intent);
        }
    }

    private void navigateTo(int destinationId) {
        NavController navController = Navigation.findNavController(requireView());
        navController.navigate(destinationId);
    }

    private List<SafetyTip> getSafetyTips() {
        List<SafetyTip> tips = new ArrayList<>();
        tips.add(new SafetyTip("Share your location",
                "Always share your location with trusted contacts when traveling alone"));
        tips.add(new SafetyTip("Stay aware of surroundings",
                "Avoid using headphones or being distracted by your phone in isolated areas"));
        tips.add(new SafetyTip("Use well-lit routes",
                "Stick to well-lit and populated areas, especially at night"));
        tips.add(new SafetyTip("Keep emergency contacts ready",
                "Have emergency contacts on speed dial and easily accessible"));
        tips.add(new SafetyTip("Trust your instincts",
                "If something feels wrong, trust your gut and move to a safer location"));
        return tips;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // Avoid memory leaks
    }
}
