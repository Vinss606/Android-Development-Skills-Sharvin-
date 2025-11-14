package com.example.carboncalculator;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private EditText etFirstName, etLastName, etEmailReg, etPasswordReg, etConfirmReg;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        etFirstName   = findViewById(R.id.etFirstName);
        etLastName    = findViewById(R.id.etLastName);
        etEmailReg    = findViewById(R.id.etEmailReg);
        etPasswordReg = findViewById(R.id.etPasswordReg);
        etConfirmReg  = findViewById(R.id.etConfirmReg);
        Button btnRegister = findViewById(R.id.btnRegister);
        TextView tvBackToLogin = findViewById(R.id.tvBackToLogin);

        btnRegister.setOnClickListener(v -> createAccount());
        tvBackToLogin.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void createAccount() {
        String first = etFirstName.getText().toString().trim();
        String last  = etLastName.getText().toString().trim();
        String email = etEmailReg.getText().toString().trim();
        String pass  = etPasswordReg.getText().toString().trim();
        String confirm = etConfirmReg.getText().toString().trim();

        if (TextUtils.isEmpty(first)) {
            etFirstName.setError("Required");
            return;
        }
        if (TextUtils.isEmpty(last)) {
            etLastName.setError("Required");
            return;
        }
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(pass)) {
            Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!pass.equals(confirm)) {
            etConfirmReg.setError("Passwords don't match");
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, pass)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            saveUserProfile(user.getUid(), first, last, email);
                        }
                        // go to main
                        startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                        finish();
                    } else {
                        Toast.makeText(this,
                                "Failed: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveUserProfile(String uid, String first, String last, String email) {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("firstName", first);
        userMap.put("lastName", last);
        userMap.put("email", email);
        userMap.put("createdAt", System.currentTimeMillis());

        db.collection("users")
                .document(uid)
                .set(userMap)
                .addOnSuccessListener(aVoid -> {
                    // optional toast
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Profile save failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
