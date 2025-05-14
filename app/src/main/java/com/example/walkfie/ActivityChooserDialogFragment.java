package com.example.walkfie;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

public class ActivityChooserDialogFragment extends DialogFragment {

    public interface ActivitySelectionListener {
        void onActivitySelected(String activityType);
    }

    private ActivitySelectionListener listener;

    public void setActivitySelectionListener(ActivitySelectionListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_activity_chooser, null);

        LinearLayout btnRun = view.findViewById(R.id.ivIcon1);
        LinearLayout btnRide = view.findViewById(R.id.ivIcon2);

        btnRun.setOnClickListener(v -> {
            if (listener != null) listener.onActivitySelected("Run");
            dismiss();
        });

        btnRide.setOnClickListener(v -> {
            if (listener != null) listener.onActivitySelected("Ride");
            dismiss();
        });

        return new AlertDialog.Builder(requireContext())
                .setView(view)
                .create();
    }
}
