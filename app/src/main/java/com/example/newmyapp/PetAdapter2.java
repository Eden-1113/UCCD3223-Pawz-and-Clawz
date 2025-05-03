package com.example.newmyapp;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PetAdapter2 extends RecyclerView.Adapter<PetAdapter2.PetViewHolder> {

    private Context context;
    private List<Pet> petList;
    private FirebaseFirestore db;

    public PetAdapter2(Context context, List<Pet> petList) {
        this.context = context;
        this.petList = petList;
        this.db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public PetViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.pet_item, parent, false);
        return new PetViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PetViewHolder holder, int position) {
        Pet pet = petList.get(position);

        holder.petNameTextView.setText(pet.getName());
        holder.petBreedTextView.setText("Breed: " + pet.getBreed());
        holder.petAgeTextView.setText("Age: " + calculateAgeFromBirthday(pet.getBirthday()));

        Glide.with(context)
                .load(pet.getImageUrl())
                .placeholder(R.drawable.rounded_image_background)
                .error(R.drawable.placeholder_image)
                .circleCrop()
                .into(holder.petImageView);

        holder.editButton.setOnClickListener(v -> {
            Intent intent = new Intent(context, EditPetActivity.class);
            intent.putExtra("petId", pet.getPetId());
            intent.putExtra("userId", FirebaseAuth.getInstance().getCurrentUser().getUid());
            intent.putExtra("petName", pet.getName());
            intent.putExtra("petBreed", pet.getBreed());
            intent.putExtra("petBirthday", pet.getBirthday());
            intent.putExtra("petInfo", pet.getInfo());
            intent.putExtra("petImageUrl", pet.getImageUrl());
            context.startActivity(intent);
        });

        holder.logButton.setOnClickListener(v -> {
            Intent intent = new Intent(context, LogRecordActivity.class);
            intent.putExtra("petId", pet.getPetId());
            context.startActivity(intent);
        });

        holder.deleteButton.setOnClickListener(v -> {
            new androidx.appcompat.app.AlertDialog.Builder(context)
                    .setTitle("Delete Pet")
                    .setMessage("Are you sure you want to delete this pet?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        Pet toRemove = petList.get(holder.getBindingAdapterPosition());
                        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                        db.collection("users").document(uid)
                                .collection("pets").document(toRemove.getPetId())
                                .delete()
                                .addOnSuccessListener(aVoid ->
                                        Toast.makeText(context, "Pet deleted", Toast.LENGTH_SHORT).show()
                                )
                                .addOnFailureListener(e ->
                                        Toast.makeText(context, "Delete failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                                );
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    @Override
    public int getItemCount() {
        return petList.size();
    }

    public static class PetViewHolder extends RecyclerView.ViewHolder {
        ImageView petImageView;
        TextView petNameTextView, petBreedTextView, petAgeTextView;
        Button editButton, logButton;
        ImageButton deleteButton;

        public PetViewHolder(@NonNull View itemView) {
            super(itemView);
            petImageView = itemView.findViewById(R.id.petImageView);
            petNameTextView = itemView.findViewById(R.id.petNameTextView);
            petBreedTextView = itemView.findViewById(R.id.petBreedTextView);
            petAgeTextView = itemView.findViewById(R.id.petAgeTextView);
            editButton = itemView.findViewById(R.id.editButton);
            logButton = itemView.findViewById(R.id.logRecordButton);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }
    }

    private String calculateAgeFromBirthday(String birthdayStr) {
        if (birthdayStr == null || birthdayStr.isEmpty()) return "Unknown";
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        try {
            Date birthDate = sdf.parse(birthdayStr);
            if (birthDate == null) return "Unknown";
            Calendar birthCal = Calendar.getInstance();
            birthCal.setTime(birthDate);
            Calendar today = Calendar.getInstance();
            int years = today.get(Calendar.YEAR) - birthCal.get(Calendar.YEAR);
            int months = today.get(Calendar.MONTH) - birthCal.get(Calendar.MONTH);
            if (months < 0) {
                years--;
                months += 12;
            }
            return years + "y " + months + "m";
        } catch (ParseException e) {
            return "Invalid";
        }
    }
}
