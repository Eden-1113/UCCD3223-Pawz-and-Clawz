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
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AddPetActivity extends AppCompatActivity {

    private static final String TAG = "AddPetActivity";
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private EditText petNameEditText, petBreedEditText, petInfoEditText, petBirthdayEditText;
    private ImageView petImageView;
    private Uri imageUri;
    private ActivityResultLauncher<Intent> pickImageLauncher;
    private ActivityResultLauncher<Intent> takePhotoLauncher;
    private Button savePetButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_pet);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Toolbar with Up button
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Add Pet");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        petNameEditText = findViewById(R.id.petNameEditText);
        petBreedEditText = findViewById(R.id.petBreedEditText);
        petInfoEditText = findViewById(R.id.petInfoEditText);
        petBirthdayEditText = findViewById(R.id.petBirthdayEditText);
        petImageView = findViewById(R.id.petImageView);
        Button selectImageButton = findViewById(R.id.selectImageButton);
        Button takePhotoButton = findViewById(R.id.takePhotoButton);
        savePetButton = findViewById(R.id.savePetButton);

        petBirthdayEditText.setOnClickListener(v -> showDatePickerDialog(petBirthdayEditText));

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
                        petImageView.setImageBitmap(imageBitmap);
                        imageUri = getImageUriFromBitmap(imageBitmap);
                    }
                });

        selectImageButton.setOnClickListener(v -> openGallery());
        takePhotoButton.setOnClickListener(v -> openCamera());
        savePetButton.setOnClickListener(v -> savePet());
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
        DatePickerDialog dialog = new DatePickerDialog(this,
                (view, year, month, day) -> {
                    String date = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, day);
                    targetField.setText(date);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        dialog.show();
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickImageLauncher.launch(intent);
    }

    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        takePhotoLauncher.launch(intent);
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
            Log.e(TAG, "Failed to create temp file", e);
            return null;
        }
    }

    private void savePet() {
        String petName = petNameEditText.getText().toString().trim();
        String petBreed = petBreedEditText.getText().toString().trim();
        String petInfo = petInfoEditText.getText().toString().trim();
        String petBirthday = petBirthdayEditText.getText().toString().trim();

        if (petName.isEmpty() || petBreed.isEmpty() || petBirthday.isEmpty()) {
            Toast.makeText(this, "Name, breed, and birthday are required!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (imageUri == null) {
            Toast.makeText(this, "Please select or take a photo", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (userId == null) {
            Toast.makeText(this, "User not authenticated!", Toast.LENGTH_SHORT).show();
            return;
        }

        savePetButton.setEnabled(false);
        uploadImageToImgur(imageUri, new OnImgurUploadComplete() {
            @Override
            public void onUploadSuccess(String imageUrl) {
                savePetToFirestore(imageUrl, petName, petBreed, petInfo, petBirthday, userId);
            }
            @Override
            public void onUploadFailed() {
                runOnUiThread(() -> {
                    Toast.makeText(AddPetActivity.this, "Image upload failed", Toast.LENGTH_SHORT).show();
                    savePetButton.setEnabled(true);
                });
            }
        });
    }

    private void uploadImageToImgur(Uri imageUri, OnImgurUploadComplete listener) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            byte[] imageBytes = new byte[inputStream.available()];
            inputStream.read(imageBytes);
            inputStream.close();
            String base64Image = Base64.encodeToString(imageBytes, Base64.DEFAULT);

            OkHttpClient client = new OkHttpClient();
            RequestBody requestBody = new FormBody.Builder()
                    .add("image", base64Image)
                    .build();
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
                    } catch (JSONException e) {
                        listener.onUploadFailed();
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Failed to upload image", e);
            listener.onUploadFailed();
        }
    }

    private void savePetToFirestore(String imageUrl, String petName, String petBreed, String petInfo, String petBirthday, String userId) {
        Pet pet = new Pet(null, petName, petBreed, petBirthday, petInfo, imageUrl);
        DocumentReference userRef = db.collection("users").document(userId);
        userRef.collection("pets")
                .add(pet)
                .addOnSuccessListener(docRef -> {
                    String petId = docRef.getId();
                    pet.setPetId(petId);
                    docRef.update("petId", petId)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Pet saved successfully!", Toast.LENGTH_SHORT).show();
                                Intent i = new Intent(this, PetActivity.class);
                                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(i);
                                finish();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error saving pet: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    savePetButton.setEnabled(true);
                });
    }

    interface OnImgurUploadComplete {
        void onUploadSuccess(String imageUrl);
        void onUploadFailed();
    }
}
