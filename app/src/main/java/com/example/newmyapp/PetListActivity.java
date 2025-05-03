package com.example.newmyapp;

import static androidx.core.location.LocationManagerCompat.getCurrentLocation;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Geocoder;
import android.location.Address;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import android.location.Location;
import android.Manifest;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;



public class PetListActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    PetListAdapter adapter;
    List<PetPosts> petList;
    FirebaseFirestore db;

    private String filterStatus = "missing";  // Default filter is "missing"
    private String currentLocation = "Kampar, Perak";  // Example location (can be dynamic)

    private FusedLocationProviderClient fusedLocationClient; // For getting the location
    private TextView locationText; // TextView to display location
    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener;
    private Location currentUserLocation;
    private static final int REQUEST_LOCATION_PERMISSION = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pet_list);


        // Initialize BottomNavigationView
        BottomNavigationView navigation = findViewById(R.id.bottom_navigation);
        // Set the "Community" item as selected by default, or when you return to this page
        navigation.setSelectedItemId(R.id.navigation_community);
        mOnNavigationItemSelectedListener = new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                // Use if-else instead of switch for resource IDs
                if (item.getItemId() == R.id.navigation_home) {
                    Intent intent = new Intent(PetListActivity.this, PetActivity.class);
                    startActivity(intent);
                    finish();
                    return true;
                } else if (item.getItemId() == R.id.navigation_calendar) {
                    Intent intent = new Intent(PetListActivity.this, CalendarPageActivity.class);
                    startActivity(intent);
                    finish();
                    return true;
                } else if (item.getItemId() == R.id.navigation_community) {
                    // Navigate to PetListActivity when "Community" is clicked
                    Intent intent = new Intent(PetListActivity.this, PetListActivity.class);
                    startActivity(intent);
                    finish();
                    return true;
                } else if (item.getItemId() == R.id.navigation_profile) {
                    Intent intent = new Intent(PetListActivity.this, ProfileActivity.class);
                    startActivity(intent);
                    return true;
                }
                return false;
            }
        };
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        recyclerView = findViewById(R.id.recyclerViewPets);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        petList = new ArrayList<>();
        adapter = new PetListAdapter(this, petList, post -> showPetDetailsDialog(post));
        recyclerView.setAdapter(adapter);

        recyclerView.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();

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

        // 🚨 Permission Check before getting location
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission not granted, request it
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION_PERMISSION);
        } else {
            // Permission granted
            checkGpsAndGetLocation(); // 🔥 Call a function that checks GPS availability
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        checkGpsAndGetLocation(); // 🔥 Re-check GPS status every time Activity comes back to foreground
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
                getCurrentLocation();
            } else {
                // Permission denied
                Toast.makeText(this, "Location permission is required to display nearby pets.", Toast.LENGTH_SHORT).show();
            }
        }
    }


    private void checkGpsAndGetLocation() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        boolean isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        if (!isGpsEnabled && !isNetworkEnabled) {
            Toast.makeText(this, "Please enable location services", Toast.LENGTH_LONG).show();
            startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
        } else {
            getCurrentLocation();
        }
    }

    private void showPetDetailsDialog(PetPosts post) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_pet_details, null);

        ImageView petImage = dialogView.findViewById(R.id.petImage);
        TextView petName = dialogView.findViewById(R.id.petName);
        ImageView speciesIcon = dialogView.findViewById(R.id.speciesIcon);
        TextView petBreed = dialogView.findViewById(R.id.petBreed);
        TextView petDOB = dialogView.findViewById(R.id.petDOB);
        TextView petAge = dialogView.findViewById(R.id.petAge);
        ImageView genderIcon = dialogView.findViewById(R.id.genderIcon);
        TextView petStatus = dialogView.findViewById(R.id.petStatus);
        TextView petContact = dialogView.findViewById(R.id.petContact);
        TextView petDescription = dialogView.findViewById(R.id.petDescription);

        petName.setText(post.getPetName());
        String petType = post.getPetType();


        // Set icon based on pet type
        int speciesIconRes;
        switch (post.getPetType().toLowerCase()) {
            case "dog":
                speciesIconRes = R.drawable.ic_dog_icon;
                break;
            case "rabbit":
                speciesIconRes = R.drawable.ic_rabbit_icon;
                break;
            case "cat":
                speciesIconRes = R.drawable.ic_cat_icon;
                break;
            case "bird":
                speciesIconRes = R.drawable.ic_bird_icon;
                break;
            default:
                speciesIconRes = R.drawable.ic_default_icon;
                break;
        }

        speciesIcon.setImageResource(speciesIconRes);

        petBreed.setText(post.getBreed());
        petDOB.setText(post.getDob());

        String age = "";
        int years = post.getAgeInYears();
        int months = post.getAgeInMonths();

        if (years > 0) {
            age = years + " years";
            if (months > 0) {
                age += " " + months + " months";
            }
        } else {
            age = months + " months";
        }

        petAge.setText(age);


        // Set gender icon only if male or female
        String gender = post.getGender();
        if (gender.equalsIgnoreCase("Male")) {
            genderIcon.setVisibility(View.VISIBLE);
            genderIcon.setImageResource(R.drawable.ic_male_icon); // Replace with your male icon
        } else if (gender.equalsIgnoreCase("Female")) {
            genderIcon.setVisibility(View.VISIBLE);
            genderIcon.setImageResource(R.drawable.ic_female_icon); // Replace with your female icon
        } else {
            genderIcon.setVisibility(View.GONE); // Hide icon if gender is unknown or neutral
        }



        petStatus.setText(post.getStatus());
        petContact.setText(post.getContactNumber());
        petDescription.setText(post.getDescription());

        Glide.with(this)
                .load(post.getPhotoUrl())
                .placeholder(R.drawable.placeholder_image)
                .into(petImage);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView)
                .setPositiveButton("Close", (dialog, which) -> dialog.dismiss());

        builder.create().show();
    }




    private void fetchPetPostsByStatusAndLocation(@Nullable String status, boolean isFilterClicked) {
        if (currentUserLocation == null) {
            Toast.makeText(PetListActivity.this, "Current location not available.", Toast.LENGTH_SHORT).show();
            return;
        }

        double userLat = currentUserLocation.getLatitude();
        double userLon = currentUserLocation.getLongitude();
        double maxDistanceKm = 10.0; // Search radius

        db.collection("users")
                .get()
                .addOnSuccessListener(usersSnapshot -> {
                    petList.clear();

                    for (DocumentSnapshot userDoc : usersSnapshot) {
                        String userId = userDoc.getId();

                        db.collection("users")
                                .document(userId)
                                .collection("petPosts")
                                .get()
                                .addOnSuccessListener(petPostsSnapshot -> {
                                    for (DocumentSnapshot petPostDoc : petPostsSnapshot) {
                                        PetPosts post = petPostDoc.toObject(PetPosts.class);
                                        if (post != null) {
                                            double[] coords = extractCoordinates(post.getLastSeenLocation());
                                            if (coords != null && coords.length == 2) {
                                                double petLat = coords[0];
                                                double petLon = coords[1];

                                                double distance = calculateDistance(userLat, userLon, petLat, petLon);

                                                // If distance is within limit
                                                if (distance <= maxDistanceKm) {
                                                    boolean shouldAdd = false;

                                                    if (isFilterClicked) {
                                                        // If user clicked filter, only show posts with matching status
                                                        if (post.getStatus().equalsIgnoreCase(status)) {
                                                            shouldAdd = true;
                                                        }
                                                    } else {
                                                        // Default load: show both "missing" and "adoption" posts
                                                        if (post.getStatus().equalsIgnoreCase("missing") || post.getStatus().equalsIgnoreCase("adoption")) {
                                                            shouldAdd = true;
                                                        }
                                                    }

                                                    if (shouldAdd) {
                                                        String locationString = getLocationFromCoordinates(petLat, petLon);
                                                        post.setLastSeenLocation(locationString);
                                                        petList.add(post);
                                                    }
                                                }
                                            }
                                        }
                                    }
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
                        fetchPetPostsByStatusAndLocation(filterStatus, true);
                    }
                })
                .show();
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Permissions not granted, don't proceed
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            currentUserLocation = location; // save the location to global variable

                            double latitude = location.getLatitude();
                            double longitude = location.getLongitude();

                            // Convert latitude and longitude to a human-readable address
                            String address = getLocationFromCoordinates(latitude, longitude);

                            // Display the readable location in the TextView
                            locationText.setText(address);  // e.g., "Kampar, Perak"

                            // ⚡ Now location is ready, fetch posts
                            fetchPetPostsByStatusAndLocation(filterStatus, false);

                        } else {
                            // If location is null, request fresh location
                            requestNewLocationData();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    // Handle failure (optional)
                    Toast.makeText(PetListActivity.this, "Failed to get location.", Toast.LENGTH_SHORT).show();
                });
    }
    private void requestNewLocationData() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(1000); // 1 second
        locationRequest.setFastestInterval(500);
        locationRequest.setNumUpdates(1); // Get only one fresh update

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    currentUserLocation = location;

                    double latitude = location.getLatitude();
                    double longitude = location.getLongitude();

                    String address = getLocationFromCoordinates(latitude, longitude);
                    locationText.setText(address);

                    fetchPetPostsByStatusAndLocation(filterStatus, false);
                } else {
                    locationText.setText("Unable to get location.");
                }
            }
        }, Looper.getMainLooper());
    }




    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Radius of the Earth in kilometers
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c; // distance in km
    }


}