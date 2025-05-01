package com.example.walkfie;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ActivityAdapter extends RecyclerView.Adapter<ActivityAdapter.ActivityViewHolder> {

    private List<ActivityItem> activities;
    private OnActivityClickListener listener;

    public ActivityAdapter(List<ActivityItem> activities, OnActivityClickListener listener) {
        this.activities = activities;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ActivityViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_activity, parent, false);
        return new ActivityViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ActivityViewHolder holder, int position) {
        ActivityItem currentActivity = activities.get(position);
        holder.tvActivityName.setText(currentActivity.getName());
        holder.btnActivity.setText(currentActivity.getButtonText());
        holder.btnActivity.setOnClickListener(v -> listener.onActivityClick(currentActivity));
    }

    @Override
    public int getItemCount() {
        return activities.size();
    }

    public static class ActivityViewHolder extends RecyclerView.ViewHolder {
        public TextView tvActivityName;
        public Button btnActivity;

        public ActivityViewHolder(View itemView) {
            super(itemView);
            tvActivityName = itemView.findViewById(R.id.tvActivityName);
            btnActivity = itemView.findViewById(R.id.btnActivity);
        }
    }

    public interface OnActivityClickListener {
        void onActivityClick(ActivityItem activity);
    }
}