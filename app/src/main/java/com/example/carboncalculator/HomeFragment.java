package com.example.carboncalculator;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class HomeFragment extends Fragment {

    private Spinner spTravelMode, spElectricSource;
    private EditText etTravelDistance, etElectricityKwh, etWasteKg;
    private Button btnCalculate, btnSave;
    private TextView tvResult;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String uid;

    // emission factors
    private static final double EF_TRAVEL_FOOT = 0.0;
    private static final double EF_TRAVEL_BICYCLE = 0.0;
    private static final double EF_TRAVEL_MOTORCYCLE = 0.10;
    private static final double EF_TRAVEL_CAR = 0.21;
    private static final double EF_TRAVEL_PUBLIC = 0.09;

    private static final double EF_ELECTRIC_CONVENTIONAL = 0.475;
    private static final double EF_ELECTRIC_SOLAR = 0.05;
    private static final double EF_WASTE = 0.10;

    private double lastTravelEmission = 0.0;
    private double lastElectricEmission = 0.0;
    private double lastWasteEmission = 0.0;
    private double lastTotalEmission = 0.0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            uid = user.getUid();
        }

        spTravelMode = view.findViewById(R.id.spTravelMode);
        etTravelDistance = view.findViewById(R.id.etTravelDistance);
        spElectricSource = view.findViewById(R.id.spElectricSource);
        etElectricityKwh = view.findViewById(R.id.etElectricityKwh);
        etWasteKg = view.findViewById(R.id.etWasteKg);
        btnCalculate = view.findViewById(R.id.btnCalculate);
        btnSave = view.findViewById(R.id.btnSave);
        tvResult = view.findViewById(R.id.tvResult);

        setupSpinners();

        btnCalculate.setOnClickListener(v -> doCalculation());
        btnSave.setOnClickListener(v -> saveCalculation());

        return view;
    }

    private void setupSpinners() {
        String[] travelModes = {"By foot", "Bicycle", "Motorcycle", "Car", "Public transport"};
        ArrayAdapter<String> travelAdapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_spinner_item, travelModes);
        travelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spTravelMode.setAdapter(travelAdapter);

        String[] electricSources = {"Conventional / Grid", "Solar / Renewable"};
        ArrayAdapter<String> electricAdapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_spinner_item, electricSources);
        electricAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spElectricSource.setAdapter(electricAdapter);
    }

    private void doCalculation() {
        String distStr = etTravelDistance.getText().toString().trim();
        double distanceKm = TextUtils.isEmpty(distStr) ? 0.0 : Double.parseDouble(distStr);

        String kwhStr = etElectricityKwh.getText().toString().trim();
        double kwh = TextUtils.isEmpty(kwhStr) ? 0.0 : Double.parseDouble(kwhStr);

        String wasteStr = etWasteKg.getText().toString().trim();
        double wasteKg = TextUtils.isEmpty(wasteStr) ? 0.0 : Double.parseDouble(wasteStr);

        int travelPos = spTravelMode.getSelectedItemPosition();
        double travelFactor;
        switch (travelPos) {
            case 0: travelFactor = EF_TRAVEL_FOOT; break;
            case 1: travelFactor = EF_TRAVEL_BICYCLE; break;
            case 2: travelFactor = EF_TRAVEL_MOTORCYCLE; break;
            case 3: travelFactor = EF_TRAVEL_CAR; break;
            case 4:
            default: travelFactor = EF_TRAVEL_PUBLIC; break;
        }
        double travelEmission = distanceKm * travelFactor;

        int elecPos = spElectricSource.getSelectedItemPosition();
        double elecFactor = (elecPos == 0) ? EF_ELECTRIC_CONVENTIONAL : EF_ELECTRIC_SOLAR;
        double electricEmission = kwh * elecFactor;

        double wasteEmission = wasteKg * EF_WASTE;

        double total = travelEmission + electricEmission + wasteEmission;

        lastTravelEmission = travelEmission;
        lastElectricEmission = electricEmission;
        lastWasteEmission = wasteEmission;
        lastTotalEmission = total;

        tvResult.setText(String.format("Total: %.2f kg CO₂", total));
        btnSave.setEnabled(true);
    }

    private void saveCalculation() {
        if (uid == null) {
            Toast.makeText(getContext(), "Not logged in.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (lastTotalEmission == 0.0) {
            Toast.makeText(getContext(), "Calculate first.", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("travelEmission", lastTravelEmission);
        data.put("electricEmission", lastElectricEmission);
        data.put("wasteEmission", lastWasteEmission);
        data.put("totalEmission", lastTotalEmission);
        data.put("createdAt", System.currentTimeMillis());

        db.collection("users")
                .document(uid)
                .collection("emissions")
                .add(data)
                .addOnSuccessListener(docRef ->
                        Toast.makeText(getContext(), "Saved to cloud.", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
