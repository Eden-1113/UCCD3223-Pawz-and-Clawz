package com.example.newmyapp;

public class Pet {
    private String petId;
    private String name;
    private String breed;
    private String birthday;
    private String info;
    private String imageUrl;

    public Pet() {
        // Required empty constructor for Firestore
    }

    public Pet(String petId, String name, String breed, String birthday, String info, String imageUrl) {
        this.petId = petId;
        this.name = name;
        this.breed = breed;
        this.birthday = birthday;
        this.info = info;
        this.imageUrl = imageUrl;
    }

    public String getPetId() {
        return petId;
    }

    public void setPetId(String petId) {
        this.petId = petId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBreed() {
        return breed;
    }

    public void setBreed(String breed) {
        this.breed = breed;
    }

    public String getBirthday() {
        return birthday;
    }

    public void setBirthday(String birthday) {
        this.birthday = birthday;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}
