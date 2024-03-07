package com.james.imeetpsp;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;
import java.util.Map;

public class EditProfile extends AppCompatActivity {
    EditText etEmail, etFullName, etPhone, etPass;
    TextView tvfname, tvemail, tvphone;
    Button btnUpdate;
    FirebaseAuth fAuth;
    FirebaseFirestore fStore;
    StorageReference storageReference;
    Uri image;
    ImageView profileImage;
    ProgressBar progressBar;
    String userID, imageURL;

    private final ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult result) {
            if (result.getResultCode() == RESULT_OK) {
                if (result.getData() != null) {
                    image = result.getData().getData();
                    Glide.with(getApplication()).load(image).into(profileImage);
                }
            }
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        //apply dark mode if the phone is set to dark mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);

        fAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();
        storageReference = FirebaseStorage.getInstance().getReference();
        progressBar = findViewById(R.id.progressBar);

        userID = fAuth.getCurrentUser().getUid();

        tvfname = findViewById(R.id.textViewParticipantName);
        tvemail = findViewById(R.id.textViewParticipantEmail);
        tvphone = findViewById(R.id.textViewPhoneNumber);

        profileImage = findViewById(R.id.imageViewProfile);

        etEmail = findViewById(R.id.editTextEmailAddress);
        etPass = findViewById(R.id.editTextPassword);
        etFullName = findViewById(R.id.editTextName);
        etPhone = findViewById(R.id.editTextNumber);
        btnUpdate = findViewById(R.id.buttonUpdate);

        btnUpdate.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.INVISIBLE);

        DocumentReference documentReference = fStore.collection("users").document(userID);
        documentReference.addSnapshotListener(this, new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot value, @Nullable FirebaseFirestoreException error) {
                if (error != null) {
                    // Handle any errors that occurred during the snapshot listener
                    Log.e("User Settings", "Error fetching document: ", error);
                    return;
                }

                if (value != null && value.exists()) {
                    // Document exists, retrieve its fields
                    String fnameValue = value.getString("fname");
                    String emailValue = value.getString("email");
                    String phoneValue = value.getString("phoneNo");
                    imageURL = value.getString("imageUrl");

                    // Update UI with the retrieved values
                    tvfname.setText(fnameValue);
                    tvemail.setText(emailValue);
                    tvphone.setText(phoneValue);

                    Glide.with(getApplication())
                            .load(imageURL)
                            .placeholder(R.drawable.default_image)
                            .error(R.drawable.default_image)
                            .into(profileImage);

                    etEmail.setText(emailValue);
                    etEmail.setEnabled(false);
                    etFullName.setText(fnameValue);
                    String phoneWithoutPrefix = phoneValue.substring(3); // Remove the first 3 characters (prefix "+60")
                    etPhone.setText(phoneWithoutPrefix); // Set the phone number without prefix to the EditText
                } else {
                    // Document doesn't exist or is null, handle accordingly
                    Log.d("User Settings", "Document does not exist or is null");
                }
            }
        });

        profileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                activityResultLauncher.launch(intent);
            }
        });

        btnUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = etEmail.getText().toString().trim();
                String password = etPass.getText().toString().trim();
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

                if (TextUtils.isEmpty(password)) {
                    etPass.setError("Please re-enter your password.");
                    return;
                }

                if (!phone.matches("\\d{9,10}")) {
                    etPhone.setError("Phone number must follow format: +60 xxxxxxxxx");
                    return;
                }

                if (password.length() < 6) {
                    etPass.setError("Password must be at least 6 characters.");
                    return;
                }

                progressBar.setVisibility(View.VISIBLE);
                btnUpdate.setVisibility(View.INVISIBLE);


                //if all data fields pass requirements, register the user in Firebase
                fAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            DocumentReference documentReference = fStore.collection("users").document(userID);
                            Map<String,Object> updates = new HashMap<>();
                            // updates.put("email", email); // Update the email field
                            updates.put("fname", fullName); // Update the full name field
                            updates.put("phoneNo", phoneNumber); // Update the phone number field

                            documentReference.update(updates).addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void unused) {
                                    Log.d("TAG", "User profile updated successfully for " + userID);
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Log.e("TAG", "Error updating user profile for " + userID, e);
                                }
                            });

                            uploadImage(image);
                            startActivity(new Intent(getApplicationContext(), UserProfile.class));
                            Toast.makeText(EditProfile.this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                        } else {
                            Log.d("Error", task.getException().getMessage());
                            Toast.makeText(EditProfile.this, "Failed to update user profile. Please try again", Toast.LENGTH_SHORT).show();
                            btnUpdate.setVisibility(View.VISIBLE);
                            progressBar.setVisibility(View.INVISIBLE);
                        }
                    }
                });
            }
        });
    }

    private void uploadImage(Uri image) {
        StorageReference reference = storageReference.child("images/" + userID);
        reference.putFile(image).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                // Get the download URL of the uploaded image
                reference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        String imageUrl = uri.toString(); // Get the URL as a string

                        // Update user profile with the download URL
                        DocumentReference documentReference = fStore.collection("users").document(userID);
                        Map<String,Object> updates = new HashMap<>();
                        updates.put("imageUrl", imageUrl); // Update the imageUrl field with the download URL

                        documentReference.update(updates).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void unused) {
                                Log.d("TAG", "User profile updated successfully for " + userID);
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.e("TAG", "Error updating user profile for " + userID, e);
                            }
                        });
                    }
                });
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(EditProfile.this, "An error occurred while uploading an image", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onBackPressed() {
        startActivity(new Intent(getApplicationContext(), UserProfile.class));
        // Call super method to allow default back button behavior
        // super.onBackPressed();
    }
}