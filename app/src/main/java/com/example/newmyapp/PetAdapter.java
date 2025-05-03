package com.example.newmyapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class PetAdapter extends RecyclerView.Adapter<PetAdapter.PetViewHolder> {

    private Context context;
    private List<PetPosts> petList;

    public PetAdapter(Context context, List<PetPosts> petList) {
        this.context = context;
        this.petList = petList;
    }

    @NonNull
    @Override
    public PetViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_pet, parent, false);
        return new PetViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PetViewHolder holder, int position) {
        PetPosts pet = petList.get(position);

        // Set pet's name, type, gender, and age
        holder.txtPetName.setText(pet.getPetName());
        holder.txtAge.setText(formatAge(pet.getAgeInYears(), pet.getAgeInMonths()));
        holder.txtContactNumber.setText(pet.getContactNumber());
        holder.txtLocation.setText(pet.getLastSeenLocation());

        // Set the breed and display the species icon
        holder.txtBreed.setText(pet.getBreed());

        // Show the correct species icon based on pet type (Dog, Rabbit, etc.)
        if (pet.getPetType().equalsIgnoreCase("Dog")) {
            holder.imgSpeciesIcon.setImageResource(R.drawable.ic_dog_icon);  // Dog icon
        } else if (pet.getPetType().equalsIgnoreCase("Rabbit")) {
            holder.imgSpeciesIcon.setImageResource(R.drawable.ic_rabbit_icon);  // Rabbit icon
        } else if (pet.getPetType().equalsIgnoreCase("Cat")) {
            holder.imgSpeciesIcon.setImageResource(R.drawable.ic_cat_icon);  // Rabbit icon
        } else if (pet.getPetType().equalsIgnoreCase("Bird")) {
            holder.imgSpeciesIcon.setImageResource(R.drawable.ic_bird_icon);  // Rabbit icon
        } else {
            holder.imgSpeciesIcon.setImageResource(R.drawable.ic_default_icon);  // Default icon
        }

        // Set the gender icon (Female/Male)
        if (pet.getGender().equalsIgnoreCase("Female")) {
            holder.imgGenderIcon.setImageResource(R.drawable.ic_female_icon);  // Female icon
        } else if (pet.getGender().equalsIgnoreCase("Male")) {
            holder.imgGenderIcon.setImageResource(R.drawable.ic_male_icon);  // Male icon
        }

        // Load the pet's image using Glide
        Glide.with(context)
                .load(pet.getPhotoUrl())
                .placeholder(R.drawable.placeholder_image)
                .into(holder.imagePet);
    }


    @Override
    public int getItemCount() {
        return petList.size();
    }

    public static class PetViewHolder extends RecyclerView.ViewHolder {
        TextView txtPetName, txtBreed, txtAge, txtLocation, txtContactNumber; // Removed txtPetType and txtGender
        ImageView imagePet, imgGenderIcon, imgSpeciesIcon; // Retained the ImageView references

        public PetViewHolder(@NonNull View itemView) {
            super(itemView);
            txtPetName = itemView.findViewById(R.id.txtPetName);
            txtBreed = itemView.findViewById(R.id.txtBreed);
            txtAge = itemView.findViewById(R.id.txtAge);
            txtContactNumber = itemView.findViewById(R.id.txtContactNumber);
            txtLocation = itemView.findViewById(R.id.txtLocation);
            imagePet = itemView.findViewById(R.id.imagePet);
            imgGenderIcon = itemView.findViewById(R.id.imgGenderIcon);
            imgSpeciesIcon = itemView.findViewById(R.id.imgSpeciesIcon); // Initialize the species icon ImageView
        }
    }



    private String formatAge(int years, int months) {
        StringBuilder sb = new StringBuilder();
        if (years > 0) {
            sb.append(years).append(" year").append(years > 1 ? "s" : "");
        }
        if (months > 0) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(months).append(" month").append(months > 1 ? "s" : "");
        }
        if (sb.length() == 0) {
            sb.append("Less than a month");
        }
        return sb.toString();
    }
}
