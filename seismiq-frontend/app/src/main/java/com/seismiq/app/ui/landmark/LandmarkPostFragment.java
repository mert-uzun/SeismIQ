package com.seismiq.app.ui.landmark;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.seismiq.app.R;

public class LandmarkPostFragment extends Fragment {

    private EditText etName, etDescription;
    private Spinner spinnerCategory;
    private Button buttonSubmit;
    private ProgressBar progressBar;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_landmark_post, container, false);

        // Reference views
        etName = root.findViewById(R.id.et_landmark_name);
        etDescription = root.findViewById(R.id.et_landmark_description);
        spinnerCategory = root.findViewById(R.id.spinnerLandmarkCategory);
        buttonSubmit = root.findViewById(R.id.buttonSubmitLandmark);
        progressBar = root.findViewById(R.id.progressBarLandmark);

        // Example usage
        buttonSubmit.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String description = etDescription.getText().toString().trim();
            String category = spinnerCategory.getSelectedItem().toString();
            // handle submission logic here
        });

        return root;
    }
}