package com.example.newmyapp;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class EditPetActivity extends AppCompatActivity {

    FirebaseFirestore db;
    EditText petNameEditText, petBreedEditText, petAgeEditText;
    String petId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_pet);

        db = FirebaseFirestore.getInstance();
        petNameEditText = findViewById(R.id.petNameEditText);
        petBreedEditText = findViewById(R.id.petBreedEditText);
        petAgeEditText = findViewById(R.id.petAgeEditText);

        // 获取传递过来的宠物信息
        petId = getIntent().getStringExtra("petId");
        String petName = getIntent().getStringExtra("petName");
        String petBreed = getIntent().getStringExtra("petBreed");
        String petAge = getIntent().getStringExtra("petAge");

        petNameEditText.setText(petName);
        petBreedEditText.setText(petBreed);
        petAgeEditText.setText(petAge);

        // 设置保存按钮的点击监听器
        findViewById(R.id.saveButton).setOnClickListener(v -> saveChanges());
    }

    private void saveChanges() {
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

        // 检查 petId 是否有效
        if (petId != null && !petId.isEmpty()) {
            // 更新数据库路径到 users/{userId}/pets/{petId}
            db.collection("users").document(userId)
                    .collection("pets").document(petId)
                    .update("name", petName, "breed", petBreed, "age", petAge)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(EditPetActivity.this, "Pet details updated!", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(EditPetActivity.this, "Failed to update pet details.", Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    });
        } else {
            Toast.makeText(EditPetActivity.this, "Pet ID is missing!", Toast.LENGTH_SHORT).show();
        }
    }
}
