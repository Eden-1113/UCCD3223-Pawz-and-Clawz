package com.example.newmyapp;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.IOException;
import java.util.Calendar;

import okhttp3.*;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import android.location.Location;
import com.google.firebase.firestore.FieldValue;






public class PetAdoptionActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private Uri selectedImageUri;
    private EditText petNameEditText, petDescEditText;
    private ImageView petImageView;
    private Button selectImageButton, uploadButton;
    private Spinner spinnerSpecies, spinnerBreed;
    private RadioGroup radioGroupGender; // Declare the RadioGroup
    private RadioButton radioMale, radioFemale; // Declare the RadioButtons
    private EditText editTextBirthday;
    private TextView textViewPetAge;
    private Calendar selectedBirthday;
    private EditText editTextContactNumber;  // Declare contact number EditText

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener;

    private FusedLocationProviderClient fusedLocationClient;
    private String userLocation = "";


    private final String IMGUR_CLIENT_ID = "2d0f7eb17d2371b"; // Replace with your Imgur Client ID

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pet_adoption);

        // Initialize BottomNavigationView
        BottomNavigationView navigation = findViewById(R.id.bottom_navigation);
        // Set the "Community" item as selected by default, or when you return to this page
        navigation.setSelectedItemId(R.id.navigation_community);
        mOnNavigationItemSelectedListener = new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                // Use if-else instead of switch for resource IDs
                if (item.getItemId() == R.id.navigation_home) {
                    Intent intent = new Intent(PetAdoptionActivity.this, PetActivity.class);
                    startActivity(intent);
                    finish();
                    return true;
                } else if (item.getItemId() == R.id.navigation_calendar) {
                    Intent intent = new Intent(PetAdoptionActivity.this, CalendarPageActivity.class);
                    startActivity(intent);
                    finish();
                    return true;
                } else if (item.getItemId() == R.id.navigation_community) {
                    // Navigate to PetListActivity when "Community" is clicked
                    Intent intent = new Intent(PetAdoptionActivity.this, PetListActivity.class);
                    startActivity(intent);
                    finish();
                    return true;
                } else if (item.getItemId() == R.id.navigation_profile) {
                    Intent intent = new Intent(PetAdoptionActivity.this, ProfileActivity.class);
                    startActivity(intent);
                    return true;
                }
                return false;
            }
        };
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);





        petNameEditText = findViewById(R.id.editTextPetName);
        petDescEditText = findViewById(R.id.editTextPetDescription);
        petImageView = findViewById(R.id.imageViewPet);
        selectImageButton = findViewById(R.id.buttonSelectImage);
        uploadButton = findViewById(R.id.buttonUpload);
        spinnerSpecies = findViewById(R.id.spinnerPetSpecies);
        spinnerBreed = findViewById(R.id.spinnerPetBreed);
        editTextBirthday = findViewById(R.id.editTextBirthday);
        textViewPetAge = findViewById(R.id.textViewPetAge);
        selectedBirthday = Calendar.getInstance();

        editTextBirthday.setOnClickListener(v -> showDatePickerDialog());

        radioGroupGender = findViewById(R.id.radioGroupGender);
        radioMale = findViewById(R.id.radioMale);
        radioFemale = findViewById(R.id.radioFemale);

        RadioGroup radioGroupPostType = findViewById(R.id.radioGroupPostType);
        RadioButton radioAdoption = findViewById(R.id.radioAdoption);
        RadioButton radioMissing = findViewById(R.id.radioMissing);
        editTextContactNumber = findViewById(R.id.editTextContactNumber); // Initialize EditText



        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // ✅ Fetch location early
        fetchUserLocation();

        setupSpeciesSpinner();

        selectImageButton.setOnClickListener(v -> openFileChooser());
        uploadButton.setOnClickListener(v -> uploadPetInfo());

        ImageButton buttonBack = findViewById(R.id.buttonBack);
        buttonBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(PetAdoptionActivity.this, PetListActivity.class);
                startActivity(intent);
                finish(); // Optional: close current activity so it doesn't stay in back stack
            }
        });



    }


    private void setupSpeciesSpinner() {
        ArrayAdapter<CharSequence> speciesAdapter = ArrayAdapter.createFromResource(
                this,
                R.array.pet_species_array,
                android.R.layout.simple_spinner_item
        );
        speciesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSpecies.setAdapter(speciesAdapter);

        spinnerSpecies.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateBreedSpinner(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void updateBreedSpinner(int speciesPosition) {
        int breedArrayId;
        switch (speciesPosition) {
            case 0: breedArrayId = R.array.dog_breeds; break;
            case 1: breedArrayId = R.array.cat_breeds; break;
            case 2: breedArrayId = R.array.rabbit_breeds; break;
            case 3: breedArrayId = R.array.bird_breeds; break;
            default: breedArrayId = R.array.other_breeds; break;
        }

        ArrayAdapter<CharSequence> breedAdapter = ArrayAdapter.createFromResource(
                this,
                breedArrayId,
                android.R.layout.simple_spinner_item
        );
        breedAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBreed.setAdapter(breedAdapter);
    }

    private void openFileChooser() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            selectedImageUri = data.getData();
            petImageView.setImageURI(selectedImageUri);
        }
    }

    private void uploadPetInfo() {
        if (selectedImageUri == null) {
            Toast.makeText(this, "Please select an image", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get the contact number entered by the user
        String contactNumber = editTextContactNumber.getText().toString().trim();

// Check if the contact number is empty
        if (contactNumber.isEmpty()) {
            Toast.makeText(this, "Please enter your contact number", Toast.LENGTH_SHORT).show();
            return;
        }

// Check if the contact number has exactly 10 digits and contains only numeric values
        if (!contactNumber.matches("[0-9]{10}")) {
            Toast.makeText(this, "Please enter a valid 10-digit contact number", Toast.LENGTH_SHORT).show();
            return;
        }

// If the contact number is valid, proceed with the next step
// You can use the contactNumber here to save or send to the database


        // Get the selected gender
        RadioGroup radioGroupGender = findViewById(R.id.radioGroupGender);
        int selectedGenderId = radioGroupGender.getCheckedRadioButtonId();
        final String gender;

        // Check if a gender is selected
        if (selectedGenderId == R.id.radioMale) {
            gender = "Male";
        } else if (selectedGenderId == R.id.radioFemale) {
            gender = "Female";
        } else {
            Toast.makeText(this, "Please select a gender", Toast.LENGTH_SHORT).show();
            return;  // Return if no gender is selected
        }

        RadioGroup radioGroupPostType = findViewById(R.id.radioGroupPostType);
        int selectedPostTypeId = radioGroupPostType.getCheckedRadioButtonId();
        String postType;

        if (selectedPostTypeId == R.id.radioAdoption) {
            postType = "adoption";
        } else if (selectedPostTypeId == R.id.radioMissing) {
            postType = "missing";
        } else {
            Toast.makeText(this, "Please select a post type", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get the pet's birthday from the input field
        String birthdayStr = editTextBirthday.getText().toString().trim();
        if (birthdayStr.isEmpty()) {
            Toast.makeText(this, "Please select a birthday", Toast.LENGTH_SHORT).show();
            return;
        }

        // Parse the birthday string to Calendar object
        Calendar selectedBirthday = Calendar.getInstance();
        String[] dateParts = birthdayStr.split("/");
        if (dateParts.length == 3) {
            selectedBirthday.set(Calendar.DAY_OF_MONTH, Integer.parseInt(dateParts[0]));
            selectedBirthday.set(Calendar.MONTH, Integer.parseInt(dateParts[1]) - 1); // Month is 0-based
            selectedBirthday.set(Calendar.YEAR, Integer.parseInt(dateParts[2]));
        } else {
            Toast.makeText(this, "Invalid birthday format", Toast.LENGTH_SHORT).show();
            return;
        }

        // Calculate age in years and months
        int[] ageAndMonths = calculateAgeAndMonths(selectedBirthday);
        int ageInYears = ageAndMonths[0];
        int ageInMonths = ageAndMonths[1];

        // Disable the upload button to prevent multiple clicks
        uploadButton.setEnabled(false);
        uploadButton.setText("Uploading...");

        // Fetch location and upload the image
        fetchUserLocation();

        // Upload the image to Imgur
        uploadImageToImgur(selectedImageUri, new OnImgurUploadComplete() {
            @Override
            public void onUploadSuccess(String imageUrl) {
                // After successful upload, save pet info to Firestore
                savePetToFirestore(imageUrl, gender, userLocation, postType, ageInYears, ageInMonths, contactNumber);

                // Update UI on the main thread after image upload success
                runOnUiThread(() -> {
                    uploadButton.setEnabled(true);
                    uploadButton.setText("Upload");
                });
            }

            @Override
            public void onUploadFailed() {
                // Handle upload failure on the main thread
                runOnUiThread(() -> {
                    Toast.makeText(PetAdoptionActivity.this, "Image upload failed", Toast.LENGTH_SHORT).show();
                    uploadButton.setEnabled(true);
                    uploadButton.setText("Upload");
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
                    .addHeader("Authorization", "Client-ID " + IMGUR_CLIENT_ID)
                    .post(requestBody)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    e.printStackTrace();
                    listener.onUploadFailed();
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        listener.onUploadFailed();
                        return;
                    }

                    String result = response.body().string();
                    try {
                        JSONObject json = new JSONObject(result);
                        String link = json.getJSONObject("data").getString("link");
                        listener.onUploadSuccess(link);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        listener.onUploadFailed();
                    }
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            listener.onUploadFailed();
        }
    }

    private void savePetToFirestore(String imageUrl, String gender, String userLocation, String status, int ageInYears, int ageInMonths, String contactNumber) {
        String name = petNameEditText.getText().toString().trim();
        String species = spinnerSpecies.getSelectedItem().toString();
        String breed = spinnerBreed.getSelectedItem().toString();
        String desc = petDescEditText.getText().toString().trim();
        String birthday = editTextBirthday.getText().toString().trim(); // Get birthday

        // Check if required fields are filled
        if (name.isEmpty() || desc.isEmpty() || gender.isEmpty() || birthday.isEmpty()) {
            Toast.makeText(this, "Please fill all fields including birthday", Toast.LENGTH_SHORT).show();
            return;
        }

        // Assuming "userId123" is retrieved from FirebaseAuth.getInstance().getCurrentUser().getUid()
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Create PetPosts object without the postId (empty postId)
        PetPosts petPost = new PetPosts(
                null, // No postId yet
                userId,
                name,
                species,
                breed,
                status,
                desc,
                imageUrl,
                userLocation, // User location
                gender,
                birthday,
                ageInYears, // Store age in years
                ageInMonths,
                contactNumber// Store age in months
        );

        // Reference to the user’s pet posts collection in Firestore
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference userDocRef = db.collection("users").document(userId);

        // Add the pet post to the "petPosts" subcollection under the user document
        userDocRef.collection("petPosts")
                .add(petPost)
                .addOnSuccessListener(documentReference -> {
                    // Set the generated postId to the petPost object
                    String postId = documentReference.getId();

                    // Now update the Firestore document with the postId
                    petPost.setPostId(postId); // Set the postId in the PetPosts object

                    // Update the Firestore document with the postId
                    documentReference.update("postId", postId)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Pet post saved successfully!", Toast.LENGTH_SHORT).show();
                                clearInputFields();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Error updating postId: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error saving pet post: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }


    private void clearInputFields() {
        petNameEditText.setText("");
        petDescEditText.setText("");
        petImageView.setImageResource(0); // Clear image
        selectedImageUri = null;
        spinnerSpecies.setSelection(0);
        spinnerBreed.setSelection(0);
        radioGroupGender.clearCheck();
        RadioGroup radioGroupPostType = findViewById(R.id.radioGroupPostType);
        radioGroupPostType.clearCheck();
        editTextBirthday.setText("");
        textViewPetAge.setText("");
        editTextContactNumber.setText("");
    }




    private void showDatePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, year1, month1, dayOfMonth) -> {
            selectedBirthday.set(year1, month1, dayOfMonth);
            String birthdayStr = dayOfMonth + "/" + (month1 + 1) + "/" + year1;
            editTextBirthday.setText(birthdayStr);

            // Calculate age and months
            int[] ageAndMonths = calculateAgeAndMonths(selectedBirthday);
            int age = ageAndMonths[0];
            int months = ageAndMonths[1];

            // Set the text with age and months
            textViewPetAge.setText("Age: " + age + " years and " + months + " months");
        }, year, month, day);

        datePickerDialog.show();
    }

    private int[] calculateAgeAndMonths(Calendar selectedBirthday) {
        // Get the current date
        Calendar currentDate = Calendar.getInstance();

        // Check if the selected date is in the future
        if (selectedBirthday.after(currentDate)) {
            Toast.makeText(this, "Selected date cannot be in the future!", Toast.LENGTH_SHORT).show();
            disableUpload();
            return new int[]{0, 0};
        } else {
            enableUpload();
        }

        int years = currentDate.get(Calendar.YEAR) - selectedBirthday.get(Calendar.YEAR);
        int months = currentDate.get(Calendar.MONTH) - selectedBirthday.get(Calendar.MONTH);
        int days = currentDate.get(Calendar.DAY_OF_MONTH) - selectedBirthday.get(Calendar.DAY_OF_MONTH);

        if (days < 0) {
            months--; // Not yet completed this month's date
        }

        if (months < 0) {
            years--;
            months += 12; // Normalize negative month difference
        }

        return new int[]{years, months};
    }


    private void disableUpload() {
        // Disable the button so the user cannot upload
        uploadButton.setEnabled(false);

        // Optionally, you can show the button as disabled visually (grayed out)
        uploadButton.setAlpha(0.5f);
    }

    private void enableUpload() {
        // Enable the button so the user can upload
        uploadButton.setEnabled(true);

        // Restore the button's visual state to active
        uploadButton.setAlpha(1.0f);
    }






    private void fetchUserLocation() {
        if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(location -> {
                        if (location != null) {
                            userLocation = "Lat: " + location.getLatitude() + ", Long: " + location.getLongitude();
                        } else {
                            Toast.makeText(this, "Unable to get location", Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1001);
        }
    }



    interface OnImgurUploadComplete {
        void onUploadSuccess(String imageUrl);
        void onUploadFailed();
    }
}
