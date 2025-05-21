package com.example.safewomen.fragments;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.safewomen.R;
import com.example.safewomen.adapters.EmergencyContactAdapter;
import com.example.safewomen.adapters.PhoneContactAdapter;
import com.example.safewomen.databinding.DialogAddImportBinding;
import com.example.safewomen.databinding.FragmentContactBinding;
import com.example.safewomen.databinding.DialogAddContactBinding;
import com.example.safewomen.databinding.DialogImportContactBinding;
import com.example.safewomen.models.entities.EmergencyContactEntity;
import com.example.safewomen.viewmodels.EmergencyContactsViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

public class ContactFragment extends androidx.fragment.app.Fragment {
    private static final String TAG = "ContactFragment";
    private static final int READ_CONTACTS_PERMISSION_REQUEST_CODE = 1002;

    private FragmentContactBinding binding;
    private EmergencyContactsViewModel viewModel;
    private EmergencyContactAdapter contactAdapter;

    public ContactFragment() {
        // Required empty public constructor
    }

    public static ContactFragment newInstance() {
        return new ContactFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize ViewModel with custom factory
        ViewModelProvider.Factory factory = new ViewModelProvider.AndroidViewModelFactory(getActivity().getApplication());
        viewModel = new ViewModelProvider(requireActivity(), factory).get(EmergencyContactsViewModel.class);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout using view binding
        binding = FragmentContactBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Set up RecyclerView
        setupRecyclerView();

        // Set up UI controls
        setupUIControls();

        // Observe ViewModel data
        observeViewModelData();
    }
    private void setupRecyclerView() {
        contactAdapter = new EmergencyContactAdapter();

        // Set up click listeners for the adapter
        contactAdapter.setOnItemClickListener(this::showEditContactDialog);
        contactAdapter.setOnDeleteClickListener(this::showDeleteConfirmationDialog);
        contactAdapter.setOnPrimaryClickListener(this::setPrimaryContact);

        // Set up RecyclerView
        binding.recyclerViewContacts.setAdapter(contactAdapter);
        binding.recyclerViewContacts.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerViewContacts.addItemDecoration(
                new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL));
    }

    private void setupUIControls() {
        // Add contact button
        binding.fabAddContact.setOnClickListener(v -> showAddContactDialog());

        // Import contacts button
        binding.buttonImportContacts.setOnClickListener(v -> {
            if (checkContactsPermission()) {
                viewModel.loadPhoneContacts();
                showImportContactsDialog();
            }
        });

        // Refresh contacts
        binding.swipeRefreshLayout.setOnRefreshListener(() -> {
            viewModel.refreshContacts();
        });
    }

    private void observeViewModelData() {
        // Observe emergency contacts
        viewModel.getContacts().observe(getViewLifecycleOwner(), contacts -> {
            contactAdapter.submitList(contacts);
            updateEmptyState(contacts);
            binding.swipeRefreshLayout.setRefreshing(false);
        });

        // Observe loading state
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            if (!isLoading) {
                binding.swipeRefreshLayout.setRefreshing(false);
            }
        });

        // Observe error messages
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                Snackbar.make(binding.getRoot(), errorMessage, Snackbar.LENGTH_LONG).show();
            }
        });

        // Observe success messages
        viewModel.getSuccessMessage().observe(getViewLifecycleOwner(), successMessage -> {
            if (successMessage != null && !successMessage.isEmpty()) {
                Snackbar.make(binding.getRoot(), successMessage, Snackbar.LENGTH_SHORT).show();
            }
        });

        // Observe phone contacts import state
        viewModel.getIsImportingContacts().observe(getViewLifecycleOwner(), isImporting -> {
            if (isImporting) {
                binding.progressBar.setVisibility(View.VISIBLE);
            }
        });
    }

    private void updateEmptyState(List<EmergencyContactEntity> contacts) {
        if (contacts == null || contacts.isEmpty()) {
            binding.emptyStateLayout.setVisibility(View.VISIBLE);
            binding.recyclerViewContacts.setVisibility(View.GONE);
        } else {
            binding.emptyStateLayout.setVisibility(View.GONE);
            binding.recyclerViewContacts.setVisibility(View.VISIBLE);
        }
    }
    private void showAddContactDialog() {
        DialogAddContactBinding dialogBinding = DialogAddContactBinding.inflate(LayoutInflater.from(requireContext()));

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.add_emergency_contact)
            .setView(dialogBinding.getRoot())
            .create();

        dialog.show();

        dialogBinding.textSave.setOnClickListener(v -> {
            String name = dialogBinding.editTextName.getText().toString().trim();
            String phone = dialogBinding.editTextPhone.getText().toString().trim();
            String relationship = dialogBinding.editTextRelationship.getText().toString().trim();
            boolean isPrimary = dialogBinding.checkBoxPrimary.isChecked();

            if (name.isEmpty() || phone.isEmpty()) {
                Snackbar.make(dialogBinding.getRoot(), R.string.error_name_phone_required, Snackbar.LENGTH_SHORT).show();
                return;
            }

            viewModel.addContact(name, phone, relationship, isPrimary);
            dialog.dismiss();
        });

        dialogBinding.textCancel.setOnClickListener(v -> dialog.dismiss());
    }

    private void showEditContactDialog(EmergencyContactEntity contact) {
        DialogAddContactBinding dialogBinding = DialogAddContactBinding.inflate(LayoutInflater.from(requireContext()));

        // Pre-fill with contact data
        dialogBinding.editTextName.setText(contact.getName());
        dialogBinding.editTextPhone.setText(contact.getPhone());
        dialogBinding.editTextRelationship.setText(contact.getRelationship());
        dialogBinding.checkBoxPrimary.setChecked(contact.isPrimary());

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.edit_emergency_contact)
            .setView(dialogBinding.getRoot())
            .create();

        dialog.show();

        dialogBinding.textSave.setOnClickListener(v -> {
            String name = dialogBinding.editTextName.getText().toString().trim();
            String phone = dialogBinding.editTextPhone.getText().toString().trim();
            String relationship = dialogBinding.editTextRelationship.getText().toString().trim();
            boolean isPrimary = dialogBinding.checkBoxPrimary.isChecked();

            if (name.isEmpty() || phone.isEmpty()) {
                Snackbar.make(dialogBinding.getRoot(), R.string.error_name_phone_required, Snackbar.LENGTH_SHORT).show();
                return;
            }

            viewModel.updateContact(contact.getId(), name, phone, relationship, isPrimary);
            dialog.dismiss();
        });

        dialogBinding.textCancel.setOnClickListener(v -> dialog.dismiss());
    }

    private void showDeleteConfirmationDialog(EmergencyContactEntity contact) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.delete_contact)
                .setMessage("Are you sure you want to delete " + contact.getName() + " from your emergency contacts?")
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    viewModel.deleteContact(contact.getId());
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void setPrimaryContact(EmergencyContactEntity contact) {
        if (!contact.isPrimary()) {
            viewModel.setPrimaryContact(contact.getId(), contact.getName(), contact.getPhone(), contact.getRelationship());
        }
    }
    private void showImportContactsDialog() {
        // Observe phone contacts
        viewModel.getPhoneContacts().observe(getViewLifecycleOwner(), phoneContacts -> {
            if (phoneContacts != null && !phoneContacts.isEmpty()) {
                // Inflate dialog layout
                DialogImportContactBinding dialogBinding = DialogImportContactBinding.inflate(LayoutInflater.from(requireContext()));

                // Set up RecyclerView for phone contacts
                PhoneContactAdapter phoneContactAdapter = new PhoneContactAdapter();
                dialogBinding.recyclerViewPhoneContacts.setAdapter(phoneContactAdapter);
                dialogBinding.recyclerViewPhoneContacts.setLayoutManager(new LinearLayoutManager(requireContext()));
                dialogBinding.recyclerViewPhoneContacts.addItemDecoration(
                        new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL));

                // Submit list to adapter
                phoneContactAdapter.submitList(phoneContacts);

                // Create dialog
                AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.import_contacts)
                        .setView(dialogBinding.getRoot())
                        .setNegativeButton(R.string.close, (dialogInterface, i) -> dialogInterface.dismiss())
                        .create();

                // Set click listener for phone contacts
                phoneContactAdapter.setOnItemClickListener(phoneContact -> {
                    showAddImportedContactDialog(phoneContact, dialog);
                });

                // Show dialog
                dialog.show();

                // Hide progress bar
                binding.progressBar.setVisibility(View.GONE);
            } else {
                // Hide progress bar
                binding.progressBar.setVisibility(View.GONE);

                // Show message if no contacts found
                Snackbar.make(binding.getRoot(), R.string.no_contacts_found, Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    private void showAddImportedContactDialog(EmergencyContactsViewModel.PhoneContact phoneContact, AlertDialog parentDialog) {
        // Inflate dialog layout
        DialogAddImportBinding dialogBinding = DialogAddImportBinding.inflate(LayoutInflater.from(requireContext()));

        // Pre-fill with phone contact data
        dialogBinding.editTextName1.setText(phoneContact.getName());
        dialogBinding.editTextPhone1.setText(phoneContact.getPhoneNumber());

        // Create dialog
        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.add_as_emergency_contact)
                .setView(dialogBinding.getRoot())
                .setPositiveButton(R.string.save, null) // Set listener later to prevent auto-dismiss
                .setNegativeButton(R.string.cancel, (dialogInterface, i) -> dialogInterface.dismiss())
                .create();

        // Show dialog and set positive button listener
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            // Validate input
            String name = dialogBinding.editTextName1.getText().toString().trim();
            String phone = dialogBinding.editTextPhone1.getText().toString().trim();
            String relationship = dialogBinding.editTextRelationship1.getText().toString().trim();
            boolean isPrimary = dialogBinding.checkBoxPrimary1.isChecked();

            if (name.isEmpty() || phone.isEmpty()) {
                Snackbar.make(dialogBinding.getRoot(), R.string.error_name_phone_required, Snackbar.LENGTH_SHORT).show();
                return;
            }

            // Add contact
            viewModel.addPhoneContactAsEmergencyContact(phoneContact, relationship, isPrimary);
            dialog.dismiss();
            parentDialog.dismiss();
        });
    }

    private boolean checkContactsPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.READ_CONTACTS},
                    READ_CONTACTS_PERMISSION_REQUEST_CODE);
            return false;
        }
        return true;
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == READ_CONTACTS_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, load contacts
                viewModel.loadPhoneContacts();
                showImportContactsDialog();
            } else {
                // Permission denied
                Snackbar.make(binding.getRoot(), R.string.contacts_permission_denied, Snackbar.LENGTH_LONG)
                        .setAction(R.string.settings, v -> {
                            // Open app settings
                            openAppSettings();
                        })
                        .show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void openAppSettings() {
        android.content.Intent intent = new android.content.Intent();
        intent.setAction(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        android.net.Uri uri = android.net.Uri.fromParts("package", requireActivity().getPackageName(), null);
        intent.setData(uri);
        startActivity(intent);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh contacts when returning to the fragment
        viewModel.refreshContacts();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
