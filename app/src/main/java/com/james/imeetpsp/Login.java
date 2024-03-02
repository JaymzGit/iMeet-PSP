package com.james.imeetpsp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class Login extends AppCompatActivity {
    EditText etEmail, etPass;
    Button btnLogin;
    FirebaseAuth fAuth;
    ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        //apply dark mode if the phone is set to dark mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);

        etEmail = findViewById(R.id.editTextEmailAddress);
        etPass = findViewById(R.id.editTextPassword);
        btnLogin = findViewById(R.id.buttonLogin);
        fAuth = FirebaseAuth.getInstance();
        progressBar = findViewById(R.id.progressBar);

        btnLogin.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.INVISIBLE);

        //check if a user is logged in, if yes, send them to MainActivity
        if(fAuth.getCurrentUser() != null) {
            startActivity(new Intent(getApplicationContext(), MainActivity.class));
            finish();
        }

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = etEmail.getText().toString().trim();
                String password = etPass.getText().toString().trim();

                if (TextUtils.isEmpty(email)) {
                    etEmail.setError("Email is required.");
                    return;
                }

                if (TextUtils.isEmpty(password)) {
                    etPass.setError("Password is required.");
                }

                if (password.length() < 6) {
                    etPass.setError("Password must be at least 6 characters.");
                }

                progressBar.setVisibility(View.VISIBLE);
                btnLogin.setVisibility(View.INVISIBLE);

                //if all data fields pass requirements, register the user in Firebase
                fAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(Login.this, "Logged in successfully.", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(getApplicationContext(), MainActivity.class));
                        } else {
                            Log.d("Error", task.getException().getMessage());
                            Toast.makeText(Login.this, "Invalid username or password", Toast.LENGTH_SHORT).show();
                            btnLogin.setVisibility(View.VISIBLE);
                            progressBar.setVisibility(View.INVISIBLE);
                        }
                    }
                });
            }
        });
    }

    public void redirectRegister(View view){
        startActivity(new Intent(getApplicationContext(), Register.class));
    }

    @Override
    public void onBackPressed() {
        finishAffinity();
    }
}