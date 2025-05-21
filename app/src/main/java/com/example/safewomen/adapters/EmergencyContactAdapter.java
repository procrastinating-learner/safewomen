package com.example.safewomen.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.safewomen.R;
import com.example.safewomen.databinding.ItemEmergencyContactBinding;
import com.example.safewomen.models.entities.EmergencyContactEntity;

public class EmergencyContactAdapter extends ListAdapter<EmergencyContactEntity, EmergencyContactAdapter.ContactViewHolder> {

    private OnItemClickListener onItemClickListener;
    private OnDeleteClickListener onDeleteClickListener;
    private OnPrimaryClickListener onPrimaryClickListener;

    public EmergencyContactAdapter() {
        super(DIFF_CALLBACK);
    }

    private static final DiffUtil.ItemCallback<EmergencyContactEntity> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<EmergencyContactEntity>() {
                @Override
                public boolean areItemsTheSame(@NonNull EmergencyContactEntity oldItem, @NonNull EmergencyContactEntity newItem) {
                    return oldItem.getId().equals(newItem.getId());
                }

                @Override
                public boolean areContentsTheSame(@NonNull EmergencyContactEntity oldItem, @NonNull EmergencyContactEntity newItem) {
                    return oldItem.getName().equals(newItem.getName()) &&
                            oldItem.getPhone().equals(newItem.getPhone()) &&
                            oldItem.getRelationship().equals(newItem.getRelationship()) &&
                            oldItem.isPrimary() == newItem.isPrimary() &&
                            oldItem.getSyncStatus().equals(newItem.getSyncStatus());
                }
            };

    @NonNull
    @Override
    public ContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemEmergencyContactBinding binding = ItemEmergencyContactBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ContactViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ContactViewHolder holder, int position) {
        EmergencyContactEntity contact = getItem(position);
        holder.bind(contact);
    }

    public class ContactViewHolder extends RecyclerView.ViewHolder {
        private final ItemEmergencyContactBinding binding;

        public ContactViewHolder(ItemEmergencyContactBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

            // Set click listeners
            binding.getRoot().setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && onItemClickListener != null) {
                    onItemClickListener.onItemClick(getItem(position));
                }
            });

            binding.buttonDelete.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && onDeleteClickListener != null) {
                    onDeleteClickListener.onDeleteClick(getItem(position));
                }
            });

            binding.checkBoxPrimary.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && onPrimaryClickListener != null) {
                    onPrimaryClickListener.onPrimaryClick(getItem(position));
                }
            });
        }

        public void bind(EmergencyContactEntity contact) {
            binding.textViewName.setText(contact.getName());
            binding.textViewPhone.setText(contact.getPhone());

            if (contact.getRelationship() != null && !contact.getRelationship().isEmpty()) {
                binding.textViewRelationship.setText(contact.getRelationship());
                binding.textViewRelationship.setVisibility(View.VISIBLE);
            } else {
                binding.textViewRelationship.setVisibility(View.GONE);
            }

            binding.checkBoxPrimary.setChecked(contact.isPrimary());

            // Show sync status indicator
            if ("pending".equals(contact.getSyncStatus())) {
                binding.imageSyncStatus.setImageResource(R.drawable.ic_sync_pending);
                binding.imageSyncStatus.setVisibility(View.VISIBLE);
            } else if ("failed".equals(contact.getSyncStatus())) {
                binding.imageSyncStatus.setImageResource(R.drawable.ic_sync_failed);
                binding.imageSyncStatus.setVisibility(View.VISIBLE);
            } else {
                binding.imageSyncStatus.setVisibility(View.GONE);
            }
        }
    }

    // Interface for item click
    public interface OnItemClickListener {
        void onItemClick(EmergencyContactEntity contact);
    }

    // Interface for delete click
    public interface OnDeleteClickListener {
        void onDeleteClick(EmergencyContactEntity contact);
    }

    // Interface for primary click
    public interface OnPrimaryClickListener {
        void onPrimaryClick(EmergencyContactEntity contact);
    }

    // Setters for listeners
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    public void setOnDeleteClickListener(OnDeleteClickListener listener) {
        this.onDeleteClickListener = listener;
    }

    public void setOnPrimaryClickListener(OnPrimaryClickListener listener) {
        this.onPrimaryClickListener = listener;
    }
}
