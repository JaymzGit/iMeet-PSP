package com.james.imeetpolycc;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

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

import com.google.firebase.auth.FirebaseAuth;

public class Login extends AppCompatActivity {

    // Firebase instance
    FirebaseAuth fAuth;

    // UI elements
    private ImageButton btnBack;
    private EditText etEmail;
    private EditText etPass;
    private Button btnLogin;
    private ProgressBar progressBar;
    private TextView tvForgotPassword;
    private TextView tvLoginWithGoogle;
    private TextView tvRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Apply system-wide dark mode setting
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);

        // Initialize Firebase instance
        fAuth = FirebaseAuth.getInstance();

        // Initialize UI elements
        btnBack = findViewById(R.id.btnBack);
        etEmail = findViewById(R.id.etEmailAddress);
        etPass = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.buttonLogin);
        progressBar = findViewById(R.id.progressBar);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        tvLoginWithGoogle = findViewById(R.id.signInWithGoogle);
        tvRegister = findViewById(R.id.tvRegister);

        // Set up the "Register Now" text with a different color
        String text = "Don’t have an account? Register Now";
        SpannableString spannableString = new SpannableString(text);
        ForegroundColorSpan colorSpan = new ForegroundColorSpan(Color.parseColor("#CBAA8D"));
        spannableString.setSpan(colorSpan, 23, 35, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        tvRegister.setText(spannableString);

        // Check if a user is already logged in
        if (fAuth.getCurrentUser() != null) {
            startActivity(new Intent(getApplicationContext(), MainActivity.class));
            return;
        }

        // Set up button listeners
        btnBack.setOnClickListener(v -> startActivity(new Intent(getApplicationContext(), StartActivity.class)));
        btnLogin.setOnClickListener(v -> handleLogin());
        tvForgotPassword.setOnClickListener(v -> startActivity(new Intent(getApplicationContext(), ForgotPassword.class)));
        tvLoginWithGoogle.setOnClickListener(v -> handleGoogleSignIn());
        tvRegister.setOnClickListener(v -> startActivity(new Intent(getApplicationContext(), Register.class)));

        // Handle back press
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                startActivity(new Intent(getApplicationContext(), StartActivity.class));
            }
        });
    }

    private void handleLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPass.getText().toString().trim();

        //TODO: Update text utils/error messages to better filter bad inputs
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email is required.");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etPass.setError("Password is required.");
            return;
        }

        if (password.length() < 6) {
            etPass.setError("Password must be at least 6 characters.");
            return;
        }

        // Show progress bar and hide login button
        progressBar.setVisibility(View.VISIBLE);
        btnLogin.setVisibility(View.INVISIBLE);

        // Authenticate user with Firebase
        fAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(Login.this, "Logged in successfully.", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(getApplicationContext(), MainActivity.class));
            } else {
                Log.d("Error", task.getException().getMessage());
                Toast.makeText(Login.this, "Invalid username or password", Toast.LENGTH_SHORT).show();
                btnLogin.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.INVISIBLE);
            }
        });
    }

    private void handleGoogleSignIn() {
        // TODO: Add sign-in with Google functionality
        Toast.makeText(Login.this, "This feature is not available right now.", Toast.LENGTH_SHORT).show();
    }
}