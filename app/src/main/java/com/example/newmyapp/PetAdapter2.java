package com.example.newmyapp;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class PetAdapter2 extends RecyclerView.Adapter<PetAdapter2.PetViewHolder> {

    private List<Pet> petList;

    public PetAdapter2(List<Pet> petList) {
        this.petList = petList;
    }

    @NonNull
    @Override
    public PetViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.pet_item, parent, false);
        return new PetViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PetViewHolder holder, int position) {
        Pet pet = petList.get(position);

        holder.petNameTextView.setText(pet.getName());
        holder.petBreedTextView.setText("Breed: " + pet.getBreed());
        holder.petAgeTextView.setText("Age: " + pet.getAge());

        // Edit button click listener
        holder.editButton.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), EditPetActivity.class);
            intent.putExtra("petId", pet.getId());
            intent.putExtra("petName", pet.getName());
            intent.putExtra("petBreed", pet.getBreed());
            intent.putExtra("petAge", String.valueOf(pet.getAge()));
            v.getContext().startActivity(intent);
        });

        // Log Record button click listener
        holder.logRecordButton.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), LogRecordActivity.class);
            intent.putExtra("petId", pet.getId()); // 传 petId 给 LogRecordActivity
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return petList.size();
    }

    public static class PetViewHolder extends RecyclerView.ViewHolder {
        TextView petNameTextView, petBreedTextView, petAgeTextView;
        Button editButton, logRecordButton; // ← 添加 logRecordButton

        public PetViewHolder(@NonNull View itemView) {
            super(itemView);
            petNameTextView = itemView.findViewById(R.id.petNameTextView);
            petBreedTextView = itemView.findViewById(R.id.petBreedTextView);
            petAgeTextView = itemView.findViewById(R.id.petAgeTextView);
            editButton = itemView.findViewById(R.id.editButton);
            logRecordButton = itemView.findViewById(R.id.logRecordButton); // ← 初始化 logRecordButton
        }
    }
}
