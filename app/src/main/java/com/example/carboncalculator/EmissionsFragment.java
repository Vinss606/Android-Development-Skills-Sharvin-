package com.example.carboncalculator;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class EmissionsFragment extends Fragment {

    private PieChart pieChart;
    private TextView tvPrevMonth, tvNextMonth, tvMonthTitle;
    private TextView tvTravelValue, tvElectricValue, tvWasteValue, tvTotalMonth;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String uid;

    // list of all months we have data for
    private List<MonthEmission> monthList = new ArrayList<>();
    private int currentIndex = 0; // 0 = latest

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_emissions, container, false);

        pieChart = view.findViewById(R.id.pieChart);
        tvPrevMonth = view.findViewById(R.id.tvPrevMonth);
        tvNextMonth = view.findViewById(R.id.tvNextMonth);
        tvMonthTitle = view.findViewById(R.id.tvMonthTitle);
        tvTravelValue = view.findViewById(R.id.tvTravelValue);
        tvElectricValue = view.findViewById(R.id.tvElectricValue);
        tvWasteValue = view.findViewById(R.id.tvWasteValue);
        tvTotalMonth = view.findViewById(R.id.tvTotalMonth);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (mAuth.getCurrentUser() != null) {
            uid = mAuth.getCurrentUser().getUid();
        }

        setupPieAppearance();
        setupMonthButtons();

        loadAllEmissions();

        return view;
    }

    private void setupPieAppearance() {
        pieChart.setUsePercentValues(false);
        pieChart.getDescription().setEnabled(false);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleRadius(55f);
        pieChart.setTransparentCircleRadius(60f);
        pieChart.setCenterText("Emissions");
        pieChart.setCenterTextSize(14f);

        Legend legend = pieChart.getLegend();
        legend.setEnabled(true);
        legend.setForm(Legend.LegendForm.CIRCLE);
    }

    private void setupMonthButtons() {
        tvPrevMonth.setOnClickListener(v -> {
            if (currentIndex < monthList.size() - 1) {
                currentIndex++;
                showMonth(currentIndex);
            }
        });

        tvNextMonth.setOnClickListener(v -> {
            if (currentIndex > 0) {
                currentIndex--;
                showMonth(currentIndex);
            }
        });
    }

    private void loadAllEmissions() {
        if (uid == null) {
            Toast.makeText(getContext(), "Not logged in", Toast.LENGTH_SHORT).show();
            showEmpty();
            return;
        }

        db.collection("users")
                .document(uid)
                .collection("emissions")
                .get()
                .addOnSuccessListener(query -> {
                    if (query.isEmpty()) {
                        showEmpty();
                        return;
                    }

                    // group by month-year
                    HashMap<String, MonthEmission> map = new HashMap<>();

                    for (DocumentSnapshot doc : query.getDocuments()) {
                        Double travel = doc.getDouble("travelEmission");
                        Double electric = doc.getDouble("electricEmission");
                        Double waste = doc.getDouble("wasteEmission");
                        Double total = doc.getDouble("totalEmission");
                        Long createdAt = doc.getLong("createdAt");

                        if (createdAt == null) {
                            // skip if no time
                            continue;
                        }

                        Calendar cal = Calendar.getInstance();
                        cal.setTimeInMillis(createdAt);

                        int y = cal.get(Calendar.YEAR);
                        int m = cal.get(Calendar.MONTH); // 0-based

                        String key = y + "-" + m; // e.g. 2025-9

                        MonthEmission me = map.get(key);
                        if (me == null) {
                            me = new MonthEmission(y, m);
                            map.put(key, me);
                        }

                        me.travel += (travel != null ? travel : 0.0);
                        me.electric += (electric != null ? electric : 0.0);
                        me.waste += (waste != null ? waste : 0.0);

                        // we can compute total from parts, but keep adding if present
                        if (total != null) {
                            me.total += total;
                        } else {
                            me.total = me.travel + me.electric + me.waste;
                        }
                    }

                    // convert to list and sort newest → oldest
                    monthList = new ArrayList<>(map.values());
                    Collections.sort(monthList, new Comparator<MonthEmission>() {
                        @Override
                        public int compare(MonthEmission o1, MonthEmission o2) {
                            // newer first
                            if (o1.year != o2.year) {
                                return o2.year - o1.year;
                            }
                            return o2.month - o1.month;
                        }
                    });

                    currentIndex = 0;
                    showMonth(currentIndex);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to load emissions", Toast.LENGTH_SHORT).show();
                    showEmpty();
                });
    }

    private void showEmpty() {
        tvMonthTitle.setText("No data");
        tvTravelValue.setText("0 kg");
        tvElectricValue.setText("0 kg");
        tvWasteValue.setText("0 kg");
        tvTotalMonth.setText("Total: 0 kg CO₂");

        List<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry(1f, "No data"));

        PieDataSet set = new PieDataSet(entries, "");
        set.setColors(Color.LTGRAY);

        PieData data = new PieData(set);
        data.setDrawValues(false);

        pieChart.setData(data);
        pieChart.invalidate();
    }

    private void showMonth(int index) {
        if (monthList == null || monthList.isEmpty()) {
            showEmpty();
            return;
        }

        MonthEmission me = monthList.get(index);

        String monthName = new DateFormatSymbols().getMonths()[me.month];
        tvMonthTitle.setText(monthName + " " + me.year);

        tvTravelValue.setText(String.format("%.2f kg", me.travel));
        tvElectricValue.setText(String.format("%.2f kg", me.electric));
        tvWasteValue.setText(String.format("%.2f kg", me.waste));
        tvTotalMonth.setText(String.format("Total: %.2f kg CO₂", me.total));

        // draw pie for this month
        List<PieEntry> entries = new ArrayList<>();
        if (me.travel > 0) entries.add(new PieEntry((float) me.travel, "Travel"));
        if (me.electric > 0) entries.add(new PieEntry((float) me.electric, "Electricity"));
        if (me.waste > 0) entries.add(new PieEntry((float) me.waste, "Waste"));

        if (entries.isEmpty()) {
            showEmpty();
            return;
        }

        PieDataSet set = new PieDataSet(entries, "");
        ArrayList<Integer> colors = new ArrayList<>();
        colors.add(Color.parseColor("#40916C")); // travel
        colors.add(Color.parseColor("#74C69D")); // electricity
        colors.add(Color.parseColor("#A98467")); // waste
        set.setColors(colors);
        set.setSliceSpace(2f);
        set.setValueTextSize(12f);

        PieData data = new PieData(set);
        pieChart.setData(data);
        pieChart.invalidate();

        // enable/disable arrows
        tvNextMonth.setEnabled(index > 0);
        tvPrevMonth.setEnabled(index < monthList.size() - 1);
    }

    // helper class
    private static class MonthEmission {
        int year;
        int month; // 0-based
        double travel = 0;
        double electric = 0;
        double waste = 0;
        double total = 0;

        MonthEmission(int year, int month) {
            this.year = year;
            this.month = month;
        }
    }
}
