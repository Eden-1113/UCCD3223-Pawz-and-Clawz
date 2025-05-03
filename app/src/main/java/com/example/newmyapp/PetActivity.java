package com.example.newmyapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class PetActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ListenerRegistration petListener;

    private RecyclerView petRecyclerView;
    private TextView emptyMessageTextView;
    private BottomNavigationView bottomNavigationView;
    private FloatingActionButton addPetButton;

    private List<Pet> petList;
    private PetAdapter2 petAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pet);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        setupViews();
        setupRecyclerView();
        setupBottomNavigation();

        addPetButton.setOnClickListener(v -> startActivity(new Intent(this, AddPetActivity.class)));
    }

    @Override
    protected void onStart() {
        super.onStart();
        attachPetListener();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (petListener != null) {
            petListener.remove();
        }
    }

    private void setupViews() {
        petRecyclerView = findViewById(R.id.petRecyclerView);
        emptyMessageTextView = findViewById(R.id.emptyMessageTextView);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        addPetButton = findViewById(R.id.addPetButton);
    }

    private void setupRecyclerView() {
        petList = new ArrayList<>();
        petAdapter = new PetAdapter2(this, petList);
        petRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        petRecyclerView.setAdapter(petAdapter);
    }

    private void attachPetListener() {
        String uid = mAuth.getCurrentUser().getUid();
        petListener = db.collection("users")
                .document(uid)
                .collection("pets")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        emptyMessageTextView.setText("Failed to load pets. Please try again.");
                        showEmptyMessage(true);
                        return;
                    }

                    petList.clear();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        Pet pet = doc.toObject(Pet.class);
                        if (pet != null) {
                            pet.setPetId(doc.getId());
                            petList.add(pet);
                        }
                    }

                    if (petList.isEmpty()) {
                        showEmptyMessage(true);
                    } else {
                        showEmptyMessage(false);
                    }

                    petAdapter.notifyDataSetChanged();
                });
    }

    private void showEmptyMessage(boolean show) {
        emptyMessageTextView.setVisibility(show ? View.VISIBLE : View.GONE);
        petRecyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_home) {
                return true;
            } else if (itemId == R.id.navigation_calendar) {
                startActivity(new Intent(this, CalendarPageActivity.class));
                return true;
            } else if (itemId == R.id.navigation_community) {
                startActivity(new Intent(this, PetListActivity.class));
                return true;
            } else if (itemId == R.id.navigation_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                return true;
            }
            return false;
        });
        bottomNavigationView.setSelectedItemId(R.id.navigation_home);
    }
}
