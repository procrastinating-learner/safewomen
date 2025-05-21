package com.example.safewomen.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.safewomen.R;
import com.example.safewomen.models.SafetyTip;

import java.util.List;

public class SafetyTipsAdapter extends RecyclerView.Adapter<SafetyTipsAdapter.ViewHolder> {

    private List<SafetyTip> safetyTips;

    public SafetyTipsAdapter(List<SafetyTip> safetyTips) {
        this.safetyTips = safetyTips;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_safety_tip, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SafetyTip tip = safetyTips.get(position);
        holder.titleTextView.setText(tip.getTitle());
        holder.descriptionTextView.setText(tip.getDescription());
    }

    @Override
    public int getItemCount() {
        return safetyTips.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView;
        TextView descriptionTextView;

        ViewHolder(View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.textViewTipTitle);
            descriptionTextView = itemView.findViewById(R.id.textViewTipDescription);
        }
    }
}
