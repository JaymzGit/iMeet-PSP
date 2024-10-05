package com.james.imeetpolycc;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

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
import com.yalantis.ucrop.UCrop;

import java.util.HashMap;
import java.util.Map;

public class EditProfile extends AppCompatActivity {

    // Firebase instances
    private FirebaseAuth fAuth;
    private FirebaseFirestore fStore;
    private StorageReference storageReference;

    // UI elements
    private ImageButton btnBack;
    private EditText etFullName, etEmail, etPhone, etPass;
    private TextView tvfname, tvemail, tvphone;
    private Button btnUpdate;
    private ImageView profileImage;
    private ProgressBar progressBar;

    private Uri image;
    private String userID;
    private String imageURL;

    // Activity result launcher for image picking
    private final ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == RESULT_OK) {
                        Uri selectedImageUri = result.getData() != null ? result.getData().getData() : null;
                        if (selectedImageUri != null) {
                            UCrop.Options options = new UCrop.Options();
                            options.setCompressionQuality(80);
                            options.setFreeStyleCropEnabled(true);
                            UCrop uCrop = UCrop.of(selectedImageUri, Uri.fromFile(new java.io.File(getCacheDir(), "cropped_image.jpg")));
                            uCrop.withOptions(options);
                            uCropResultLauncher.launch(uCrop.getIntent(EditProfile.this));
                        }
                    }
                }
            }
    );

    private final ActivityResultLauncher<Intent> uCropResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == RESULT_OK) {
                        Uri resultUri = UCrop.getOutput(result.getData());
                        if (resultUri != null) {
                            image = resultUri;
                            // Show the cropped image in the ImageView
                            Glide.with(EditProfile.this)
                                    .load(image)
                                    .placeholder(R.drawable.default_user_image)
                                    .error(R.drawable.default_user_image)
                                    .into(profileImage);
                        }
                    } else if (result.getResultCode() == UCrop.RESULT_ERROR) {
                        Throwable cropError = UCrop.getError(result.getData());
                        Log.e("UCrop", "Crop error: ", cropError);
                        Toast.makeText(EditProfile.this, "Gagal memangkas imej", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        // Apply system-wide dark mode setting
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);

        // Clear Glide cache
        Glide.get(this).clearMemory();
        new Thread(() -> Glide.get(EditProfile.this).clearDiskCache()).start();

        // Initialize Firebase instances and UI elements
        fAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();
        storageReference = FirebaseStorage.getInstance().getReference();

        btnBack = findViewById(R.id.buttonBack);
        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmailAddress);
        etPhone = findViewById(R.id.etPhoneNumber);
        etPass = findViewById(R.id.etPasswordConfirmation);
        tvfname = findViewById(R.id.tvUserName);
        tvemail = findViewById(R.id.tvUserEmail);
        tvphone = findViewById(R.id.tvUserPhoneNumber);
        profileImage = findViewById(R.id.imageViewProfile);
        btnUpdate = findViewById(R.id.btnUpdate);
        progressBar = findViewById(R.id.progressBar);

        // Set initial UI state
        btnUpdate.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.INVISIBLE);

        // Set the email EditText as non-editable
        etEmail.setFocusable(false);
        etEmail.setClickable(false);
        etEmail.setLongClickable(false);
        etEmail.setCursorVisible(false);
        etEmail.setInputType(InputType.TYPE_NULL);

        // Retrieve user ID
        userID = fAuth.getCurrentUser().getUid();

        // Set up button listeners
        btnBack.setOnClickListener(v -> startActivity(new Intent(getApplicationContext(), UserProfile.class)));

        profileImage.setOnClickListener(v -> {
            Intent intent = new Intent(MediaStore.ACTION_PICK_IMAGES);
            intent.setType("image/*");
            activityResultLauncher.launch(intent);
        });

        // Set up Firestore document listener
        DocumentReference documentReference = fStore.collection("users").document(userID);
        documentReference.addSnapshotListener(this, new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot value, @Nullable FirebaseFirestoreException error) {
                if (error != null) {
                    Log.e("User Settings", "Error fetching document: ", error);
                    return;
                }

                if (value != null && value.exists()) {
                    String fnameValue = value.getString("fname");
                    String emailValue = value.getString("email");
                    String phoneValue = value.getString("phoneNo");
                    imageURL = value.getString("imageUrl");

                    tvfname.setText(fnameValue);
                    tvemail.setText(emailValue);
                    tvphone.setText(phoneValue);

                    Glide.with(getApplication())
                            .load(imageURL)
                            .placeholder(R.drawable.default_user_image)
                            .error(R.drawable.default_user_image)
                            .into(profileImage);

                    etFullName.setText(fnameValue);
                    etEmail.setText(emailValue);
                    String phoneWithoutPrefix = phoneValue.substring(3);
                    etPhone.setText(phoneWithoutPrefix);
                } else {
                    Log.d("User Settings", "Document does not exist or is null");
                }
            }
        });

        btnUpdate.setOnClickListener(v -> {
            String password = etPass.getText().toString().trim();
            String fullName = etFullName.getText().toString().trim();
            String phone = etPhone.getText().toString();
            String phoneNumber = "+60" + phone;
            String newEmail = etEmail.getText().toString().trim();
            String currentEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();

            if (TextUtils.isEmpty(fullName)) {
                etFullName.setError("Full Name is required.");
                return;
            }

            if (TextUtils.isEmpty(newEmail) || !Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
                etEmail.setError("Email address does not match the proper format");
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

            // Authenticate the user and update the profile
            fAuth.signInWithEmailAndPassword(currentEmail, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (task.isSuccessful()) {
                        DocumentReference documentReference = fStore.collection("users").document(userID);
                        Map<String, Object> updates = new HashMap<>();
                        // updates.put("email", newEmail); // Uncomment if email update is required
                        updates.put("fname", fullName);
                        updates.put("phoneNo", phoneNumber);

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

                        // Upload the profile image if there is one
                        if (image != null) {
                            uploadImage(image);
                        } else {
                            // No image selected, still update the profile
                            updateFirestoreWithImageUrl(imageURL);
                        }

                        // Redirect the user to the profile page after updates
                        startActivity(new Intent(getApplicationContext(), UserProfile.class));
                        finish();
                        Toast.makeText(EditProfile.this, "Profil berjaya dikemaskini!", Toast.LENGTH_SHORT).show();
                    } else {
                        Log.d("Error", task.getException().getMessage());
                        Toast.makeText(EditProfile.this, "Gagal mengemaskini profil. Sila cuba lagi.", Toast.LENGTH_SHORT).show();
                        btnUpdate.setVisibility(View.VISIBLE);
                        progressBar.setVisibility(View.INVISIBLE);
                    }
                }
            });
        });
    }

    private void uploadImage(Uri image) {
        if (image != null) {
            StorageReference reference = storageReference.child("images/" + userID);
            reference.putFile(image)
                    .addOnSuccessListener(taskSnapshot ->
                            reference.getDownloadUrl().addOnSuccessListener(uri -> {
                                String imageUrl = uri.toString();
                                updateFirestoreWithImageUrl(imageUrl);
                            })
                    )
                    .addOnFailureListener(e ->
                            Toast.makeText(EditProfile.this, "Gagal memuatnaik imej", Toast.LENGTH_SHORT).show()
                    );
        } else {
            Log.d("TAG", "Image URI is null, skipping upload");
            // If the image URI is null, use the existing image URL from Firestore
            if (imageURL != null) {
                updateFirestoreWithImageUrl(imageURL);
            } else {
                Log.d("TAG", "No existing image URL found, skipping image update");
            }
        }
    }

    private void updateFirestoreWithImageUrl(String imageUrl) {
        DocumentReference documentReference = fStore.collection("users").document(userID);
        Map<String, Object> updates = new HashMap<>();
        updates.put("imageUrl", imageUrl);

        documentReference.update(updates).addOnSuccessListener(unused ->
                Log.d("TAG", "User profile updated successfully with image URL for " + userID)
        ).addOnFailureListener(e ->
                Log.e("TAG", "Error updating user profile with image URL for " + userID, e)
        );
    }
}