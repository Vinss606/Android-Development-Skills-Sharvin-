package com.example.carboncalculator;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private TextView tvTitle;
    private Button btnLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        tvTitle = findViewById(R.id.tvTitle);
        btnLogout = findViewById(R.id.btnLogout);

        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
        });

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);

        if (savedInstanceState == null) {
            // default tab
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new HomeFragment())
                    .commit();
            tvTitle.setText("CarbonCalculator");  // or "Home"
        }

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selected;
            switch (item.getItemId()) {
                case R.id.nav_emissions:
                    selected = new EmissionsFragment();
                    tvTitle.setText("Emissions");
                    break;
                case R.id.nav_profile:
                    selected = new ProfileFragment();
                    tvTitle.setText("Profile");
                    break;
                case R.id.nav_home:
                default:
                    selected = new HomeFragment();
                    tvTitle.setText("Home");
                    break;
            }

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, selected)
                    .commit();
            return true;
        });
    }
}