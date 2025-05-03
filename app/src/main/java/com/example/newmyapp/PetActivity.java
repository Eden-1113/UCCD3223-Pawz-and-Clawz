

package com.example.newmyapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class PetActivity extends AppCompatActivity {

    FirebaseFirestore db;
    FirebaseAuth mAuth;
    TextView statusTextView;
    RecyclerView petRecyclerView;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener;
    PetAdapter2 petAdapter;
    List<Pet> petList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pet);


        // Initialize BottomNavigationView
        BottomNavigationView navigation = findViewById(R.id.bottom_navigation);
        mOnNavigationItemSelectedListener = new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                // Use if-else instead of switch for resource IDs
                if (item.getItemId() == R.id.navigation_home) {
                    Intent intent = new Intent(PetActivity.this, PetActivity.class);
                    startActivity(intent);
                    return true;
                } else if (item.getItemId() == R.id.navigation_calendar) {
                    Intent intent = new Intent(PetActivity.this, CalendarPageActivity.class);
                    startActivity(intent);
                    return true;
                } else if (item.getItemId() == R.id.navigation_community) {
                    // Navigate to PetListActivity when "Community" is clicked
                    Intent intent = new Intent(PetActivity.this, PetListActivity.class);
                    startActivity(intent);
                    return true;
                } else if (item.getItemId() == R.id.navigation_profile) {
                    //Intent intent = new Intent(PetActivity.this, PetListActivity.class);
                    //startActivity(intent);
                    return true;
                }
                return false;
            }
        };
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);


        statusTextView = findViewById(R.id.statusTextView);
        petRecyclerView = findViewById(R.id.petRecyclerView);
        petRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        petList = new ArrayList<>();
        petAdapter = new PetAdapter2(petList);
        petRecyclerView.setAdapter(petAdapter);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        checkUserPets();

        // FloatingActionButton to add new pet
        findViewById(R.id.addPetButton).setOnClickListener(v -> {
            Intent intent = new Intent(PetActivity.this, AddPetActivity.class);
            startActivity(intent);
        });




    }


    private void checkUserPets() {
        String uid = mAuth.getCurrentUser().getUid();
        db.collection("users")
                .document(uid)  // 使用用户ID
                .collection("pets")  // 用户的宠物集合
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        statusTextView.setText("You have not added any pet yet.");
                    } else {
                        statusTextView.setVisibility(View.GONE);
                        petRecyclerView.setVisibility(View.VISIBLE);
                        petList.clear();
                        for (DocumentSnapshot document : queryDocumentSnapshots) {
                            Pet pet = document.toObject(Pet.class);
                            pet.setId(document.getId());  // 获取 petId 并设置到 Pet 对象
                            petList.add(pet);  // 将 Pet 对象加入列表
                        }
                        petAdapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(e -> {
                    statusTextView.setText("Failed to load pet data.");
                });
    }


}