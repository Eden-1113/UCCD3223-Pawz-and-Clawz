package com.example.newmyapp;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class AddPetActivity extends AppCompatActivity {

    FirebaseFirestore db;
    EditText petNameEditText, petBreedEditText, petAgeEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_pet_acticity);

        db = FirebaseFirestore.getInstance();
        petNameEditText = findViewById(R.id.petNameEditText);
        petBreedEditText = findViewById(R.id.petBreedEditText);
        petAgeEditText = findViewById(R.id.petAgeEditText);

        // 设置保存按钮的点击监听器
        findViewById(R.id.savePetButton).setOnClickListener(v -> savePet());
    }

    private void savePet() {
        String petName = petNameEditText.getText().toString().trim();
        String petBreed = petBreedEditText.getText().toString().trim();
        String petAgeStr = petAgeEditText.getText().toString().trim();

        // 检查输入字段是否有效
        if (petName.isEmpty() || petBreed.isEmpty() || petAgeStr.isEmpty()) {
            Toast.makeText(this, "All fields must be filled!", Toast.LENGTH_SHORT).show();
            return;
        }

        // 尝试将 petAge 转换为整数
        int petAge;
        try {
            petAge = Integer.parseInt(petAgeStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Age must be a valid number!", Toast.LENGTH_SHORT).show();
            return;
        }

        // 获取当前用户的 UID
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // 创建一个新的宠物对象
        Pet newPet = new Pet(petName, petBreed, petAge);

        // 获取 Firestore 实例
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // 获取用户的宠物集合路径
        CollectionReference petsRef = db.collection("users").document(userId).collection("pets");

        // 添加新宠物到该子集合
        petsRef.add(newPet)
                .addOnSuccessListener(documentReference -> {
                    // 获取生成的 petId
                    String petId = documentReference.getId();
                    Toast.makeText(AddPetActivity.this, "New pet added with ID: " + petId, Toast.LENGTH_SHORT).show();
                    // 返回到主页面
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(AddPetActivity.this, "Error adding pet", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                });
    }
}
