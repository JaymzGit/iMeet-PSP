package com.james.imeetpolycc;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class Register extends AppCompatActivity {

    // Firebase instances
    private FirebaseAuth fAuth;
    private FirebaseFirestore fStore;

    // UI elements
    private ImageButton btnBack;
    private EditText etEmail, etFullName, etPhone, etPass, etConfirmPass;
    private Button btnRegister;
    private ProgressBar progressBar;
    private TextView tvLogin;
    private TextView tvRegisterWithGoogle;

    // User ID
    private String userID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Apply system-wide dark mode setting
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);

        // Initialize Firebase instances and UI elements
        fAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();
        btnBack = findViewById(R.id.btnBack);
        etEmail = findViewById(R.id.etEmailAddress);
        etFullName = findViewById(R.id.etFullName);
        etPhone = findViewById(R.id.etPhoneNumber);
        etPass = findViewById(R.id.etPassword);
        etConfirmPass = findViewById(R.id.etConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);
        progressBar = findViewById(R.id.progressBar);
        tvLogin = findViewById(R.id.tvLogin);
        tvRegisterWithGoogle = findViewById(R.id.registerWithGoogle);

        btnRegister.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.INVISIBLE);

        // Set up the "Register Now" text with a different color
        String text = "Don’t have an account? Register Now";
        SpannableString spannableString = new SpannableString(text);
        ForegroundColorSpan colorSpan = new ForegroundColorSpan(Color.parseColor("#CBAA8D"));
        spannableString.setSpan(colorSpan, 23, 35, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        tvLogin.setText(spannableString);

        // Check if a user is logged in; if yes, redirect to MainActivity
        if (fAuth.getCurrentUser() != null) {
            startActivity(new Intent(getApplicationContext(), MainActivity.class));
            finish();
        }

        // Handle back button
        btnBack.setOnClickListener(v -> startActivity(new Intent(getApplicationContext(), StartActivity.class)));

        // Handle register button
        btnRegister.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPass.getText().toString().trim();
            String confirm = etConfirmPass.getText().toString().trim();

            // Additional data
            String fullName = etFullName.getText().toString().trim();
            String phone = etPhone.getText().toString();
            String phoneNumber = "+60" + phone;

            //TODO: Update text utils/error messages to better filter bad inputs
            // Validate inputs
            if (TextUtils.isEmpty(email)) {
                etEmail.setError("Email is required.");
                return;
            }
            if (TextUtils.isEmpty(fullName)) {
                etFullName.setError("Full Name is required.");
                return;
            }
            if (TextUtils.isEmpty(phone)) {
                etPhone.setError("Phone number is required.");
                return;
            }
            if (!phone.matches("\\d{9,10}")) {
                etPhone.setError("Phone number must follow format: +60 xxxxxxxxx");
                return;
            }
            if (TextUtils.isEmpty(password)) {
                etPass.setError("Password is required.");
                return;
            }
            if (TextUtils.isEmpty(confirm)) {
                etConfirmPass.setError("Confirmation Password is required.");
                return;
            }
            if (password.length() < 6) {
                etPass.setError("Password must be at least 6 characters.");
                return;
            }
            if (!password.equals(confirm)) {
                return;
            }

            progressBar.setVisibility(View.VISIBLE);
            btnRegister.setVisibility(View.INVISIBLE);

            // Register user in Firebase
            fAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(Register.this, "Account registered successfully.", Toast.LENGTH_SHORT).show();

                            userID = fAuth.getCurrentUser().getUid();
                            DocumentReference documentReference = fStore.collection("users").document(userID);
                            Map<String, Object> user = new HashMap<>();
                            user.put("email", email);
                            user.put("fname", fullName);
                            user.put("phoneNo", phoneNumber);
                            documentReference.set(user).addOnSuccessListener(unused ->
                                    Log.d("TAG", "onSuccess: userProfile is created for " + userID)
                            );

                            startActivity(new Intent(getApplicationContext(), MainActivity.class));
                        } else {
                            Log.d("Error", task.getException().getMessage());
                            Toast.makeText(Register.this, "An error has occurred. Please try again", Toast.LENGTH_SHORT).show();
                            btnRegister.setVisibility(View.VISIBLE);
                            progressBar.setVisibility(View.INVISIBLE);
                        }
                    });
        });

        tvRegisterWithGoogle.setOnClickListener(v -> handleGoogleRegister());

        // Handle back press using OnBackPressedDispatcher
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                startActivity(new Intent(getApplicationContext(),StartActivity.class));
                finish(); // Optionally close the current activity
            }
        });
    }

    private void handleGoogleRegister() {
        // TODO: Add sign-in with Google functionality
        Toast.makeText(Register.this, "This feature is not available right now.", Toast.LENGTH_SHORT).show();
    }
}
