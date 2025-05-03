package com.example.newmyapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;

public class MenuActivity extends AppCompatActivity {

    private Button button1, button2, logoutButton;
    private TextView welcomeText;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        button1 = findViewById(R.id.button1);
        button2 = findViewById(R.id.button2);
        logoutButton = findViewById(R.id.buttonLogout);
        welcomeText = findViewById(R.id.welcomeText);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        loadUserName();

        button1.setOnClickListener(v -> {
            Intent intent = new Intent(MenuActivity.this, PetAdoptionActivity.class);
            startActivity(intent);
        });

        button2.setOnClickListener(v -> {
            Intent intent = new Intent(MenuActivity.this, PetListActivity.class);
            startActivity(intent);
        });




        logoutButton.setOnClickListener(v -> {
            auth.signOut();
            Intent intent = new Intent(this, SplashActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }

    private void loadUserName() {
        String uid = auth.getCurrentUser().getUid();
        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("name");
                        welcomeText.setText("Welcome, " + name + "!");
                    }
                });
    }
}
