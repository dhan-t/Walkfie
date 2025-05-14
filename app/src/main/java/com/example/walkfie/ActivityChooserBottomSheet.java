package com.example.walkfie;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Arrays;
import java.util.List;

public class ActivityChooserBottomSheet extends BottomSheetDialogFragment {

    // Callback listener interface
    public interface OnActivitySelectedListener {
        void onActivitySelected(ActivityItem activity);
    }

    private OnActivitySelectedListener listener;

    public void setOnActivitySelectedListener(OnActivitySelectedListener listener) {
        this.listener = listener;
    }

    public ActivityChooserBottomSheet() {
        // Required empty constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_activity_chooser, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Find views
        ImageView ivRun = view.findViewById(R.id.ivIcon1);
        ImageView ivRide = view.findViewById(R.id.ivIcon2);
        RecyclerView recyclerView = view.findViewById(R.id.recyclerViewActivities);

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        List<ActivityItem> activityList = Arrays.asList(
                new ActivityItem("Walk", "Start Walk"),
                new ActivityItem("Hike", "Start Hike"),
                new ActivityItem("Swim", "Start Swim")
        );

        // Adapter setup
        ActivityAdapter adapter = new ActivityAdapter(activityList, activity -> {
            Toast.makeText(getContext(), "Selected: " + activity.getName(), Toast.LENGTH_SHORT).show();
            if (listener != null) {
                listener.onActivitySelected(activity);
            }
            dismiss();
        });
        recyclerView.setAdapter(adapter);
        recyclerView.setVisibility(View.VISIBLE);  // Always show RecyclerView

        // Run icon click
        ivRun.setOnClickListener(v -> {
            if (listener != null) {
                listener.onActivitySelected(new ActivityItem("Run", "Start Run"));
            }
            dismiss();
        });

        // Ride icon click
        ivRide.setOnClickListener(v -> {
            if (listener != null) {
                listener.onActivitySelected(new ActivityItem("Ride", "Start Ride"));
            }
            dismiss();
        });
    }

    // Optional — always expand fully on open
    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() instanceof com.google.android.material.bottomsheet.BottomSheetDialog) {
            com.google.android.material.bottomsheet.BottomSheetDialog dialog =
                    (com.google.android.material.bottomsheet.BottomSheetDialog) getDialog();

            View bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                com.google.android.material.bottomsheet.BottomSheetBehavior<?> behavior =
                        com.google.android.material.bottomsheet.BottomSheetBehavior.from(bottomSheet);
                behavior.setPeekHeight(bottomSheet.getHeight()); // Expand fully
                behavior.setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED);
            }
        }
    }
}