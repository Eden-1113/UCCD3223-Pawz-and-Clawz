package com.example.newmyapp;

public interface OnImgurUploadComplete {
    void onUploadSuccess(String imageUrl);
    void onUploadFailed();
}
