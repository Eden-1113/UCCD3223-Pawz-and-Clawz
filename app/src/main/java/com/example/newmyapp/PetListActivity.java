package com.example.newmyapp;

import static androidx.core.location.LocationManagerCompat.getCurrentLocation;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Geocoder;
import android.location.Address;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.location.Location;
import android.widget.TextView;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;



public class PetListActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    PetAdapter adapter;
    List<PetPosts> petList;
    FirebaseFirestore db;

    private String filterStatus = "missing";  // Default filter is "missing"
    private String currentLocation = "Kampar, Perak";  // Example location (can be dynamic)

    private FusedLocationProviderClient fusedLocationClient; // For getting the location
    private TextView locationText; // TextView to display location
    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pet_list);

        // Initialize BottomNavigationView
        BottomNavigationView navigation = findViewById(R.id.bottom_navigation);
        mOnNavigationItemSelectedListener = new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                // Use if-else instead of switch for resource IDs
                if (item.getItemId() == R.id.navigation_home) {
                    Intent intent = new Intent(PetListActivity.this, PetActivity.class);
                    startActivity(intent);
                    return true;
                } else if (item.getItemId() == R.id.navigation_calendar) {
                    Intent intent = new Intent(PetListActivity.this, CalendarPageActivity.class);
                    startActivity(intent);
                    return true;
                } else if (item.getItemId() == R.id.navigation_community) {
                    // Navigate to PetListActivity when "Community" is clicked
                    Intent intent = new Intent(PetListActivity.this, PetListActivity.class);
                    startActivity(intent);
                    return true;
                } else if (item.getItemId() == R.id.navigation_profile) {
                    // Navigate to Profile Activity (or Fragment)
                    return true;
                }
                return false;
            }
        };
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        recyclerView = findViewById(R.id.recyclerViewPets);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        petList = new ArrayList<>();
        adapter = new PetAdapter(this, petList);
        recyclerView.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();

        // Fetch pet posts based on the selected filter status and location
        fetchPetPostsByStatusAndLocation(filterStatus, currentLocation);

        // Set up the filter button to allow users to choose a filter
        ImageView filterIcon = findViewById(R.id.filter_icon);
        filterIcon.setOnClickListener(view -> showFilterDialog());

        locationText = findViewById(R.id.location_text);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Get the new icon for pet adoption activity
        ImageView adoptIcon = findViewById(R.id.adopt_icon);

        // Set click listener for the icon
        adoptIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Start PetAdoptionActivity
                Intent intent = new Intent(PetListActivity.this, PetAdoptionActivity.class);
                startActivity(intent);
            }
        });

        // Get current location
        getCurrentLocation();
    }

    private void fetchPetPostsByStatusAndLocation(String status, String location) {
        db.collection("users")
                .get()
                .addOnSuccessListener(usersSnapshot -> {
                    petList.clear();  // Clear the existing list of pet posts

                    // Loop through each user document
                    for (DocumentSnapshot userDoc : usersSnapshot) {
                        String userId = userDoc.getId();  // Get userId of each user

                        // Query petPosts subcollection for each user with the given status and location
                        db.collection("users")
                                .document(userId)
                                .collection("petPosts")
                                .whereEqualTo("status", status)  // Filter by status (missing or adoption)
                                .get()
                                .addOnSuccessListener(petPostsSnapshot -> {
                                    for (DocumentSnapshot petPostDoc : petPostsSnapshot) {
                                        PetPosts post = petPostDoc.toObject(PetPosts.class);  // Convert to PetPosts object
                                        if (post != null) {
                                            double[] coords = extractCoordinates(post.getLastSeenLocation());
                                            if (coords != null && coords.length == 2) {
                                                String locationString = getLocationFromCoordinates(coords[0], coords[1]);
                                                post.setLastSeenLocation(locationString); // This updates the display text
                                            }

                                            petList.add(post);  // Add the post to the list
                                        }
                                    }

                                    // After all posts are fetched, update the adapter
                                    adapter.notifyDataSetChanged();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(PetListActivity.this, "Failed to load pet posts: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    }


                })
                .addOnFailureListener(e -> {
                    Toast.makeText(PetListActivity.this, "Failed to load users: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }


    // Method to convert latitude and longitude to a human-readable location
    private String getLocationFromCoordinates(double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                // Get the address and return a string (e.g., "Kampar, Perak")
                return address.getLocality() + ", " + address.getAdminArea();
            } else {
                return "Unknown Location";
            }
        } catch (IOException e) {
            e.printStackTrace();
            return "Location Error";
        }
    }

    private double[] extractCoordinates(String lastSeenLocation) {
        double[] coords = new double[2]; // index 0 = lat, index 1 = long
        try {
            String[] parts = lastSeenLocation.split(",");
            if (parts.length == 2) {
                String latPart = parts[0].trim().replace("Lat:", "").trim();
                String longPart = parts[1].trim().replace("Long:", "").trim();
                coords[0] = Double.parseDouble(latPart);
                coords[1] = Double.parseDouble(longPart);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return coords;
    }



    private void showFilterDialog() {
        String[] filterOptions = {"Pet Adoption", "Pet Missing"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Filter")
                .setItems(filterOptions, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            filterStatus = "adoption";  // Pet Adoption selected
                        } else if (which == 1) {
                            filterStatus = "missing";  // Pet Missing selected
                        }

                        // Fetch posts with the selected filter status and current location
                        fetchPetPostsByStatusAndLocation(filterStatus, currentLocation);
                    }
                })
                .show();
    }

    private void getCurrentLocation() {
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            double latitude = location.getLatitude();
                            double longitude = location.getLongitude();

                            // Convert latitude and longitude to a human-readable address
                            String address = getLocationFromCoordinates(latitude, longitude);

                            // Display the readable location in the TextView
                            locationText.setText(address);  // e.g., "Kampar, Perak"
                        } else {
                            locationText.setText("Unable to get location.");
                        }
                    }
                });
    }

}