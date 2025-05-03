package com.example.newmyapp;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FieldValue;
import android.icu.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PetPosts {

    public static final String STATUS_ADOPTION = "adoption";
    public static final String STATUS_MISSING = "missing";

    public String postId;
    public String userId;
    public String petName;
    public String petType; // Species: Dog, Cat, Rabbit, etc.
    public String breed;
    public String status; // "adoption" or "missing"
    public String description;
    public String photoUrl;
    public String lastSeenLocation;
    public String gender; // Male/Female
    private String birthday;
    public int ageInYears;   // Store age in years
    public int ageInMonths;  // Store age in months
    public String contactNumber; // Add contact number

    // Required empty constructor for Firestore deserialization
    public PetPosts() {}

    // Constructor for Firestore data
    public PetPosts(String postId, String userId, String petName, String petType, String breed,
                    String status, String description, String photoUrl, String lastSeenLocation,
                    String gender, String birthday, int ageInYears, int ageInMonths, String contactNumber) {
        this.postId = postId;
        this.userId = userId;
        this.petName = petName;
        this.petType = petType;
        this.breed = breed;
        this.status = status;
        this.description = description;
        this.photoUrl = photoUrl;
        this.lastSeenLocation = lastSeenLocation;
        this.gender = gender;
        this.birthday = birthday;
        this.ageInYears = ageInYears;
        this.ageInMonths = ageInMonths;
        this.contactNumber = contactNumber; // Initialize contactNumber
    }

    // Getters and Setters

    public int getAgeInYears() {
        return ageInYears;
    }

    public String getDob() {
        try {
            // Try formatting birthday if it's in yyyy-MM-dd format
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date date = inputFormat.parse(birthday);

            SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
            return outputFormat.format(date);
        } catch (Exception e) {
            // Return the raw string if parsing fails
            return birthday != null ? birthday : "Unknown";
        }
    }

    public void setAgeInYears(int ageInYears) {
        this.ageInYears = ageInYears;
    }

    public int getAgeInMonths() {
        return ageInMonths;
    }

    public void setAgeInMonths(int ageInMonths) {
        this.ageInMonths = ageInMonths;
    }

    public String getPostId() {
        return postId;
    }

    public void setPostId(String postId) {
        this.postId = postId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getPetName() {
        return petName;
    }

    public void setPetName(String petName) {
        this.petName = petName;
    }

    public String getPetType() {
        return petType;
    }

    public void setPetType(String petType) {
        this.petType = petType;
    }

    public String getBreed() {
        return breed;
    }

    public void setBreed(String breed) {
        this.breed = breed;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public String getLastSeenLocation() {
        return lastSeenLocation;
    }

    public void setLastSeenLocation(String lastSeenLocation) {
        this.lastSeenLocation = lastSeenLocation;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getBirthday() {
        return birthday;
    }

    public void setBirthday(String birthday) {
        this.birthday = birthday;
    }

    // Getter and Setter for contactNumber
    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }
}
