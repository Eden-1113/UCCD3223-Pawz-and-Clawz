package com.example.newmyapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;
import java.util.Map;

public class RegisterUserProfileActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;

    private EditText nameEditText, ageEditText, birthdayEditText, genderEditText;
    private Button saveButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_user_profile);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        // Initialize views
        nameEditText = findViewById(R.id.editTextName);
        ageEditText = findViewById(R.id.editTextAge);
        birthdayEditText = findViewById(R.id.editTextBirthday);
        genderEditText = findViewById(R.id.editTextGender);
        saveButton = findViewById(R.id.buttonSave);


        // Handle save button click
        saveButton.setOnClickListener(v -> {
            String name = nameEditText.getText().toString();
            String age = ageEditText.getText().toString();
            String birthday = birthdayEditText.getText().toString();
            String gender = genderEditText.getText().toString();
            Uri avatarUri = null; // This should be set after picking an image

            // Call the method to upload user data
            uploadUserProfileData(name, age, birthday, gender, avatarUri);
        });
    }

    private void uploadUserProfileData(String name, String age, String birthday, String gender, Uri avatarUri) {
        String userId = mAuth.getCurrentUser().getUid();

        // Create a map with user data
        Map<String, Object> userData = new HashMap<>();
        userData.put("name", name);
        userData.put("age", age);
        userData.put("birthday", birthday);
        userData.put("gender", gender);

        // Upload user data to Firestore
        db.collection("users").document(userId)
                .set(userData)
                .addOnSuccessListener(aVoid -> {
                    if (avatarUri != null) {
                        StorageReference avatarRef = storage.getReference().child("avatars/" + userId + ".jpg");
                        UploadTask uploadTask = avatarRef.putFile(avatarUri);

                        uploadTask.addOnSuccessListener(taskSnapshot -> {
                            avatarRef.getDownloadUrl().addOnSuccessListener(uri -> {
                                // Store avatar URL in Firestore
                                db.collection("users").document(userId)
                                        .update("avatarUrl", uri.toString())
                                        .addOnSuccessListener(aVoid1 -> {
                                            Toast.makeText(RegisterUserProfileActivity.this, "User profile uploaded successfully!", Toast.LENGTH_SHORT).show();

                                            // After successful upload, navigate to MenuActivity
                                            startActivity(new Intent(RegisterUserProfileActivity.this, PetActivity.class));
                                            finish();  // Close the current activity
                                        });
                            });
                        }).addOnFailureListener(e -> {
                            Toast.makeText(RegisterUserProfileActivity.this, "Avatar upload failed!", Toast.LENGTH_SHORT).show();
                        });
                    } else {
                        // If no avatar, just move to MenuActivity
                        startActivity(new Intent(RegisterUserProfileActivity.this, ProfileActivity.class));
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(RegisterUserProfileActivity.this, "User profile upload failed!", Toast.LENGTH_SHORT).show();
                });
    }
}
