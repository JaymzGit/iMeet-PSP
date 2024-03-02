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
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class Register extends AppCompatActivity {
    EditText etEmail, etFullName, etPhone, etPass, etConfirmPass;
    Button btnRegister;
    FirebaseAuth fAuth;
    FirebaseFirestore fStore;
    ProgressBar progressBar;
    String userID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        //apply dark mode if the phone is set to dark mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);

        etEmail = findViewById(R.id.editTextEmailAddress);
        etFullName = findViewById(R.id.editTextName);
        etPhone = findViewById(R.id.editTextNumber);
        etPass = findViewById(R.id.editTextPassword);
        etConfirmPass = findViewById(R.id.editTextConfirm);
        btnRegister = findViewById(R.id.buttonRegister);

        fAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();
        progressBar = findViewById(R.id.progressBar);

        btnRegister.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.INVISIBLE);

        //check if a user is logged in, if yes, send them to MainActivity
        if (fAuth.getCurrentUser() != null) {
            startActivity(new Intent(getApplicationContext(), MainActivity.class));
            finish();
        }

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = etEmail.getText().toString().trim();
                String password = etPass.getText().toString().trim();
                String confirm = etConfirmPass.getText().toString().trim();

                //additional data
                String fullName = etFullName.getText().toString().trim();
                String phone = etPhone.getText().toString();
                String phoneNumber = "+60" + phone;

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

                if(!phone.matches("\\d{9,10}")){
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
                    etPass.setError("Passwords do not match");
                    etConfirmPass.setError("Passwords do not match");
                    return;
                }

                progressBar.setVisibility(View.VISIBLE);
                btnRegister.setVisibility(View.INVISIBLE);

                //if all data fields pass requirements, register the user in Firebase
                fAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(Register.this, "Account registered successfully.", Toast.LENGTH_SHORT).show();

                            userID = fAuth.getCurrentUser().getUid();
                            DocumentReference documentReference = fStore.collection("users").document(userID);
                            Map<String,Object> user = new HashMap<>();
                            user.put("email", email);
                            user.put("fname", fullName);
                            user.put("phoneNo", phoneNumber);
                            documentReference.set(user).addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void unused) {
                                    Log.d("TAG", "onSuccess: userProfile is created for" + userID);
                                }
                            });

                            startActivity(new Intent(getApplicationContext(), MainActivity.class));
                        } else {
                            Log.d("Error", task.getException().getMessage());
                            Toast.makeText(Register.this, "An error has occured. Please try again", Toast.LENGTH_SHORT).show();
                            btnRegister.setVisibility(View.VISIBLE);
                            progressBar.setVisibility(View.INVISIBLE);
                        }
                    }
                });
            }
        });
    }
}