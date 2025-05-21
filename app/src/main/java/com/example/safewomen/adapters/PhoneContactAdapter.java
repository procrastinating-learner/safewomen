package com.example.safewomen.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.safewomen.databinding.ItemPhoneContactBinding;
import com.example.safewomen.viewmodels.EmergencyContactsViewModel.PhoneContact;

public class PhoneContactAdapter extends ListAdapter<PhoneContact, PhoneContactAdapter.PhoneContactViewHolder> {

    private OnItemClickListener onItemClickListener;

    public PhoneContactAdapter() {
        super(DIFF_CALLBACK);
    }

    private static final DiffUtil.ItemCallback<PhoneContact> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<PhoneContact>() {
                @Override
                public boolean areItemsTheSame(@NonNull PhoneContact oldItem, @NonNull PhoneContact newItem) {
                    return oldItem.getId().equals(newItem.getId());
                }

                @Override
                public boolean areContentsTheSame(@NonNull PhoneContact oldItem, @NonNull PhoneContact newItem) {
                    return oldItem.getName().equals(newItem.getName()) &&
                            oldItem.getPhoneNumber().equals(newItem.getPhoneNumber());
                }
            };

    @NonNull
    @Override
    public PhoneContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemPhoneContactBinding binding = ItemPhoneContactBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new PhoneContactViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull PhoneContactViewHolder holder, int position) {
        PhoneContact contact = getItem(position);
        holder.bind(contact);
    }

    public class PhoneContactViewHolder extends RecyclerView.ViewHolder {
        private final ItemPhoneContactBinding binding;

        public PhoneContactViewHolder(ItemPhoneContactBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

            // Set click listener
            binding.getRoot().setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && onItemClickListener != null) {
                    onItemClickListener.onItemClick(getItem(position));
                }
            });
        }

        public void bind(PhoneContact contact) {
            binding.textViewName.setText(contact.getName());
            binding.textViewPhone.setText(contact.getPhoneNumber());
        }
    }

    // Interface for item click
    public interface OnItemClickListener {
        void onItemClick(PhoneContact contact);
    }

    // Setter for listener
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }
}
