package com.example.newmyapp;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class EditPetActivity extends AppCompatActivity {

    private static final String TAG = "EditPetActivity";
    private EditText petNameEditText, petBreedEditText, petBirthdayEditText, petInfoEditText;
    private ImageView petImageView;
    private Button updatePetButton;
    private Uri imageUri;
    private String currentImageUrl, petId, userId;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private ActivityResultLauncher<Intent> pickImageLauncher;
    private ActivityResultLauncher<Intent> takePhotoLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_pet);

        // Toolbar with Up button
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Edit Pet");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        petNameEditText = findViewById(R.id.petNameEditText);
        petBreedEditText = findViewById(R.id.petBreedEditText);
        petBirthdayEditText = findViewById(R.id.petBirthdayEditText);
        petInfoEditText = findViewById(R.id.petInfoEditText);
        petImageView = findViewById(R.id.petImageView);
        updatePetButton = findViewById(R.id.updatePetButton);

        Button selectImageButton = findViewById(R.id.selectImageButton);
        Button takePhotoButton = findViewById(R.id.takePhotoButton);

        petBirthdayEditText.setOnClickListener(v -> showDatePickerDialog(petBirthdayEditText));

        // Retrieve data from Intent
        Intent intent = getIntent();
        petId = intent.getStringExtra("petId");
        userId = intent.getStringExtra("userId");
        petNameEditText.setText(intent.getStringExtra("petName"));
        petBreedEditText.setText(intent.getStringExtra("petBreed"));
        petBirthdayEditText.setText(intent.getStringExtra("petBirthday"));
        petInfoEditText.setText(intent.getStringExtra("petInfo"));
        currentImageUrl = intent.getStringExtra("petImageUrl");
        Glide.with(this).load(currentImageUrl).circleCrop().into(petImageView);

        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        imageUri = result.getData().getData();
                        petImageView.setImageURI(imageUri);
                    }
                });

        takePhotoLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Bitmap imageBitmap = (Bitmap) result.getData().getExtras().get("data");
                        imageUri = getImageUriFromBitmap(imageBitmap);
                        petImageView.setImageBitmap(imageBitmap);
                    }
                });

        selectImageButton.setOnClickListener(v -> openGallery());
        takePhotoButton.setOnClickListener(v -> openCamera());

        updatePetButton.setOnClickListener(v -> editPet());
    }

    @Override
    public boolean onSupportNavigateUp() {
        // Navigate back to PetActivity
        Intent up = new Intent(this, PetActivity.class);
        up.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(up);
        finish();
        return true;
    }

    private void showDatePickerDialog(EditText targetField) {
        Calendar calendar = Calendar.getInstance();
        new DatePickerDialog(this,
                (view, year, month, day) -> targetField.setText(String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, day)),
                calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    private void openGallery() {
        pickImageLauncher.launch(new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI));
    }

    private void openCamera() {
        takePhotoLauncher.launch(new Intent(MediaStore.ACTION_IMAGE_CAPTURE));
    }

    private Uri getImageUriFromBitmap(Bitmap bitmap) {
        try {
            File file = new File(getCacheDir(), System.currentTimeMillis() + ".jpg");
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();
            return Uri.fromFile(file);
        } catch (IOException e) {
            Log.e(TAG, "Failed to create temp file for bitmap", e);
            return null;
        }
    }

    private void editPet() {
        String petName = petNameEditText.getText().toString().trim();
        String petBreed = petBreedEditText.getText().toString().trim();
        String petBirthday = petBirthdayEditText.getText().toString().trim();
        String petInfo = petInfoEditText.getText().toString().trim();

        if (petName.isEmpty() || petBreed.isEmpty() || petBirthday.isEmpty()) {
            Toast.makeText(this, "Pet name, breed, and birthday are required!", Toast.LENGTH_SHORT).show();
            return;
        }

        updatePetButton.setEnabled(false);
        if (imageUri == null) {
            updatePetInFirestore(currentImageUrl, petName, petBreed, petInfo, petBirthday);
        } else {
            uploadImageToImgur(imageUri, new OnImgurUploadComplete() {
                @Override public void onUploadSuccess(String imageUrl) {
                    updatePetInFirestore(imageUrl, petName, petBreed, petInfo, petBirthday);
                }
                @Override public void onUploadFailed() {
                    runOnUiThread(() -> {
                        Toast.makeText(EditPetActivity.this, "Image upload failed", Toast.LENGTH_SHORT).show();
                        updatePetButton.setEnabled(true);
                    });
                }
            });
        }
    }

    private void uploadImageToImgur(Uri imageUri, OnImgurUploadComplete listener) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            byte[] imageBytes = new byte[inputStream.available()];
            inputStream.read(imageBytes);
            inputStream.close();

            String base64Image = Base64.encodeToString(imageBytes, Base64.DEFAULT);
            OkHttpClient client = new OkHttpClient();
            RequestBody requestBody = new FormBody.Builder().add("image", base64Image).build();
            Request request = new Request.Builder()
                    .url("https://api.imgur.com/3/image")
                    .addHeader("Authorization", "Client-ID 2d0f7eb17d2371b")
                    .post(requestBody)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override public void onFailure(Call call, IOException e) { listener.onUploadFailed(); }
                @Override public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) { listener.onUploadFailed(); return; }
                    try {
                        JSONObject json = new JSONObject(response.body().string());
                        String link = json.getJSONObject("data").getString("link");
                        listener.onUploadSuccess(link);
                    } catch (JSONException e) { listener.onUploadFailed(); }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Imgur upload error", e);
            listener.onUploadFailed();
        }
    }

    private void updatePetInFirestore(String imageUrl, String name, String breed, String info, String birthday) {
        Map<String, Object> updatedPet = new HashMap<>();
        updatedPet.put("name", name);
        updatedPet.put("breed", breed);
        updatedPet.put("birthday", birthday);
        updatedPet.put("info", info);
        updatedPet.put("imageUrl", imageUrl);

        DocumentReference petRef = db.collection("users").document(userId)
                .collection("pets").document(petId);
        petRef.update(updatedPet)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Pet updated successfully!", Toast.LENGTH_SHORT).show();
                    Intent i = new Intent(this, PetActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(i);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error updating pet: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    updatePetButton.setEnabled(true);
                });
    }

    interface OnImgurUploadComplete {
        void onUploadSuccess(String imageUrl);
        void onUploadFailed();
    }
}
