package com.example.newmyapp;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.Map;

public class LogRecordActivity extends AppCompatActivity {

    private TextView vaccinatedText, nextVaccineText, eatRoutineText, noteText, emptyMessage;
    private Button addOrEditButton;
    private FirebaseFirestore db;
    private String petId;
    private String petName; // <-- 新增
    private static final int REQUEST_CODE = 1001;
    private boolean hasLog = false;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_record);
        // Initialize BottomNavigationView
        BottomNavigationView navigation = findViewById(R.id.bottom_navigation);
        // Set the "Community" item as selected by default, or when you return to this page
        navigation.setSelectedItemId(R.id.navigation_home);
        mOnNavigationItemSelectedListener = new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                // Use if-else instead of switch for resource IDs
                if (item.getItemId() == R.id.navigation_home) {
                    Intent intent = new Intent(LogRecordActivity.this, PetActivity.class);
                    startActivity(intent);
                    finish();
                    return true;
                } else if (item.getItemId() == R.id.navigation_calendar) {
                    Intent intent = new Intent(LogRecordActivity.this, CalendarPageActivity.class);
                    startActivity(intent);
                    finish();
                    return true;
                } else if (item.getItemId() == R.id.navigation_community) {
                    // Navigate to PetListActivity when "Community" is clicked
                    Intent intent = new Intent(LogRecordActivity.this, PetListActivity.class);
                    startActivity(intent);
                    finish();
                    return true;
                } else if (item.getItemId() == R.id.navigation_profile) {
                    Intent intent = new Intent(LogRecordActivity.this, ProfileActivity.class);
                    startActivity(intent);
                    return true;
                }
                return false;
            }
        };
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);


        // 绑定控件
        vaccinatedText = findViewById(R.id.vaccinatedText);
        nextVaccineText = findViewById(R.id.nextVaccineText);
        eatRoutineText = findViewById(R.id.eatRoutineText);
        noteText = findViewById(R.id.noteText);
        emptyMessage = findViewById(R.id.noLogMessage);
        addOrEditButton = findViewById(R.id.addLogButton);
        Button testNotificationButton = findViewById(R.id.testNotificationButton);

        db = FirebaseFirestore.getInstance();

        petId = getIntent().getStringExtra("petId"); // 拿到宠物ID
        petName = getIntent().getStringExtra("petName"); // 拿到宠物名字 <-- 新增

        // 权限请求
        requestNotificationPermissionIfNeeded();
        requestExactAlarmPermissionIfNeeded();

        // 测试按钮（直接发广播）
        testNotificationButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, ReminderReceiver.class);
            sendBroadcast(intent);
        });

        // 加载日志记录
        loadLogRecord();

        // 添加或编辑日志
        addOrEditButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddLogRecordActivity.class);
            intent.putExtra("petId", petId);
            intent.putExtra("petName", petName);
            intent.putExtra("isEdit", hasLog);
            startActivityForResult(intent, REQUEST_CODE);  // <-- 改这里
        });

    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
            }
        }
    }

    private void requestExactAlarmPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!isExactAlarmPermissionGranted()) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                startActivityForResult(intent, REQUEST_CODE);
            }
        }
    }

    private boolean isExactAlarmPermissionGranted() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return true;
        }
        return true; // 默认允许
    }

    private void setReminder(long reminderTime, String petName, String vaccineDate, String vaccineTime) {
        Intent intent = new Intent(this, ReminderReceiver.class);
        intent.putExtra("petName", petName);
        intent.putExtra("vaccineDate", vaccineDate);
        intent.putExtra("vaccineTime", vaccineTime);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            long reminderTimeBefore30Min = reminderTime - (30 * 60 * 1000);
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderTimeBefore30Min, pendingIntent);
        }
    }

    private void loadLogRecord() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        db.collection("users")
                .document(userId)
                .collection("pets")
                .document(petId)
                .collection("Log record")
                .document("info")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // 有日志记录
                        Map<String, Object> data = documentSnapshot.getData();

                        vaccinatedText.setText(" " + data.get("vaccinated"));
                        eatRoutineText.setText("Eat Routine: " + data.get("eatRoutine"));
                        noteText.setText("Note: " + data.get("note"));

                        String nextVaccineDate = (String) data.get("nextVaccine");
                        String nextVaccineTime = (String) data.get("vaccinatedTime");

                        if (nextVaccineDate != null && nextVaccineTime != null) {
                            nextVaccineText.setVisibility(View.VISIBLE);
                            nextVaccineText.setText(" " + nextVaccineDate + " & " + nextVaccineTime);
                        } else {
                            nextVaccineText.setVisibility(View.GONE);
                        }

                        hasLog = true; // 有日志，后面跳转是编辑模式

                        // 设置按钮文本为 "Edit"
                        addOrEditButton.setText("Edit Log");

                        Boolean reminder = (Boolean) data.get("reminder");
                        if (Boolean.TRUE.equals(reminder) && nextVaccineDate != null && nextVaccineTime != null) {
                            try {
                                String[] dateParts = nextVaccineDate.split("/");
                                String[] timeParts = nextVaccineTime.split(":");

                                int day = Integer.parseInt(dateParts[0]);
                                int month = Integer.parseInt(dateParts[1]) - 1;
                                int year = Integer.parseInt(dateParts[2]);

                                int hour = Integer.parseInt(timeParts[0]);
                                int minute = Integer.parseInt(timeParts[1]);

                                Calendar calendar = Calendar.getInstance();
                                calendar.set(year, month, day, hour, minute, 0);

                                setReminder(calendar.getTimeInMillis(), petName, nextVaccineDate, nextVaccineTime);
                            } catch (Exception e) {
                                Log.e("ReminderParseError", "Failed to parse reminder time", e);
                            }
                        }
                    } else {
                        // 没有日志记录
                        emptyMessage.setVisibility(View.VISIBLE);
                        addOrEditButton.setText("Add Log");
                        hasLog = false;
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading log.", Toast.LENGTH_SHORT).show();
                });
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1 && grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show();
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK) {
            loadLogRecord(); // <-- 回来后刷新
        }
    }

}
