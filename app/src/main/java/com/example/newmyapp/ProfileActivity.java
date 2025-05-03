package com.example.newmyapp;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ProfileActivity extends AppCompatActivity {

    private ShapeableImageView imageViewProfile;
    private Button buttonSelectImage, buttonEdit, buttonSave;
    private EditText editName, editBirthday, editEmail, editAddress, editPhone;
    private TextView textAge;
    private RadioGroup radioGroupGender;
    private RadioButton radioMale, radioFemale;

    private Uri selectedImageUri;
    private Calendar birthdayCal = Calendar.getInstance();

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private String userId;

    private final String IMGUR_CLIENT_ID = "2d0f7eb17d2371b";

    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Firebase
        auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            finish();
            return;
        }
        userId = auth.getCurrentUser().getUid();
        db = FirebaseFirestore.getInstance();

        // Bind views
        imageViewProfile = findViewById(R.id.imageViewProfile);
        buttonSelectImage = findViewById(R.id.buttonSelectProfileImage);
        buttonEdit = findViewById(R.id.buttonEditProfile);
        buttonSave = findViewById(R.id.buttonSaveProfile);
        editName = findViewById(R.id.editTextName);
        editBirthday = findViewById(R.id.editTextBirthday);
        editEmail = findViewById(R.id.editTextEmail);
        editAddress = findViewById(R.id.editTextAddress);
        editPhone = findViewById(R.id.editTextPhone);
        textAge = findViewById(R.id.textViewAge);
        radioGroupGender = findViewById(R.id.radioGroupGender);
        radioMale = findViewById(R.id.radioMale);
        radioFemale = findViewById(R.id.radioFemale);

        // Back button
        findViewById(R.id.buttonBack).setOnClickListener(v -> finish());

        // Bottom Navigation
        BottomNavigationView nav = findViewById(R.id.bottom_navigation);
        nav.setSelectedItemId(R.id.navigation_profile);
        nav.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.navigation_home) {
                startActivity(new Intent(this, PetActivity.class));
                finish();
                return true;
            } else if (item.getItemId() == R.id.navigation_calendar) {
                startActivity(new Intent(this, CalendarPageActivity.class));
                finish();
                return true;
            } else if (item.getItemId() == R.id.navigation_community) {
                startActivity(new Intent(this, PetListActivity.class));
                finish();
                return true;
            } else if (item.getItemId() == R.id.navigation_profile) {
                return true;
            }
            return false;
        });

        // Date picker
        editBirthday.setOnClickListener(v -> showDatePicker());

        // Image Picker Launcher
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        selectedImageUri = result.getData().getData();
                        imageViewProfile.setImageURI(selectedImageUri);
                    }
                });

        // Image button
        buttonSelectImage.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            i.setType("image/*");
            imagePickerLauncher.launch(i);
        });

        buttonEdit.setOnClickListener(v -> enterEditMode());
        buttonSave.setOnClickListener(v -> saveProfile());

        // Load user info
        loadUserProfile();
    }

    private void loadUserProfile() {
        db.collection("users").document(userId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        editName.setText(doc.getString("name"));
                        editEmail.setText(doc.getString("email"));
                        editAddress.setText(doc.getString("address"));
                        editPhone.setText(doc.getString("phone"));
                        String birthday = doc.getString("birthday");
                        editBirthday.setText(birthday);

                        String gender = doc.getString("gender");
                        if ("Male".equals(gender)) radioMale.setChecked(true);
                        else if ("Female".equals(gender)) radioFemale.setChecked(true);

                        String image = doc.getString("profileImageUrl");
                        if (image != null && !image.isEmpty()) {
                            Glide.with(this).load(image).into(imageViewProfile);
                        } else {
                            imageViewProfile.setImageResource(R.drawable.default_profile);
                        }

                        try {
                            if (birthday != null && birthday.contains("/")) {
                                String[] parts = birthday.split("/");
                                birthdayCal.set(Integer.parseInt(parts[2]), Integer.parseInt(parts[1]) - 1, Integer.parseInt(parts[0]));
                                int[] age = calculateAge(birthdayCal);
                                textAge.setText("Age: " + age[0] + "y " + age[1] + "m");
                            }
                        } catch (Exception e) {
                            textAge.setText("Age: -");
                        }
                    }
                });
    }

    private void enterEditMode() {
        editName.setEnabled(true);
        editBirthday.setEnabled(true);
        editEmail.setEnabled(true);
        editAddress.setEnabled(true);
        editPhone.setEnabled(true);
        radioMale.setEnabled(true);
        radioFemale.setEnabled(true);
        buttonSelectImage.setVisibility(View.VISIBLE);

        buttonEdit.setVisibility(View.GONE);
        buttonSave.setVisibility(View.VISIBLE);
    }

    private void saveProfile() {
        String name = editName.getText().toString().trim();
        String birthday = editBirthday.getText().toString().trim();
        String email = editEmail.getText().toString().trim();
        String address = editAddress.getText().toString().trim();
        String phone = editPhone.getText().toString().trim();
        String gender = radioMale.isChecked() ? "Male" : "Female";

        if (name.isEmpty() || birthday.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "Name, birthday & email required", Toast.LENGTH_SHORT).show();
            return;
        }

        DocumentReference userRef = db.collection("users").document(userId);
        userRef.update(
                "name", name,
                "birthday", birthday,
                "email", email,
                "address", address,
                "phone", phone,
                "gender", gender
        ).addOnSuccessListener(unused -> {
            if (selectedImageUri != null) {
                uploadImageToImgur(selectedImageUri, new OnImgurUploadComplete() {
                    @Override
                    public void onUploadSuccess(String url) {
                        userRef.update("profileImageUrl", url);
                        runOnUiThread(ProfileActivity.this::finishSaveMode);
                    }

                    @Override
                    public void onUploadFailed() {
                        runOnUiThread(() -> {
                            Toast.makeText(ProfileActivity.this, "Image upload failed", Toast.LENGTH_SHORT).show();
                            finishSaveMode();
                        });
                    }
                });
            } else {
                finishSaveMode();
            }
        });
    }

    private void finishSaveMode() {
        editName.setEnabled(false);
        editBirthday.setEnabled(false);
        editEmail.setEnabled(false);
        editAddress.setEnabled(false);
        editPhone.setEnabled(false);
        radioMale.setEnabled(false);
        radioFemale.setEnabled(false);
        buttonSelectImage.setVisibility(View.GONE);

        buttonSave.setVisibility(View.GONE);
        buttonEdit.setVisibility(View.VISIBLE);
        Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show();
    }

    private void showDatePicker() {
        Calendar now = Calendar.getInstance();
        new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    birthdayCal.set(year, month, dayOfMonth);
                    String s = dayOfMonth + "/" + (month + 1) + "/" + year;
                    editBirthday.setText(s);
                    int[] age = calculateAge(birthdayCal);
                    textAge.setText("Age: " + age[0] + "y " + age[1] + "m");
                },
                now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    private int[] calculateAge(Calendar dob) {
        Calendar today = Calendar.getInstance();
        int y = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR);
        int m = today.get(Calendar.MONTH) - dob.get(Calendar.MONTH);
        if (today.get(Calendar.DAY_OF_MONTH) < dob.get(Calendar.DAY_OF_MONTH)) m--;
        if (m < 0) {
            y--;
            m += 12;
        }
        return new int[]{y, m};
    }

    private void uploadImageToImgur(Uri uri, OnImgurUploadComplete listener) {
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int r;
            while ((r = is.read(buffer)) != -1) bos.write(buffer, 0, r);
            is.close();

            String base64 = Base64.encodeToString(bos.toByteArray(), Base64.DEFAULT);

            OkHttpClient client = new OkHttpClient();
            RequestBody body = new FormBody.Builder().add("image", base64).build();
            Request req = new Request.Builder()
                    .url("https://api.imgur.com/3/image")
                    .addHeader("Authorization", "Client-ID " + IMGUR_CLIENT_ID)
                    .post(body).build();

            client.newCall(req).enqueue(new Callback() {
                public void onFailure(Call c, IOException e) {
                    listener.onUploadFailed();
                }

                public void onResponse(Call c, Response r) throws IOException {
                    if (!r.isSuccessful()) {
                        listener.onUploadFailed();
                        return;
                    }
                    try {
                        String js = r.body().string();
                        String link = new JSONObject(js).getJSONObject("data").getString("link");
                        listener.onUploadSuccess(link);
                    } catch (JSONException e) {
                        listener.onUploadFailed();
                    }
                }
            });

        } catch (Exception e) {
            listener.onUploadFailed();
        }
    }

    interface OnImgurUploadComplete {
        void onUploadSuccess(String url);
        void onUploadFailed();
    }
}
