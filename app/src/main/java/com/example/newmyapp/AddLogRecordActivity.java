package com.example.newmyapp;

import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.auth.FirebaseAuth;

import com.google.android.material.chip.Chip;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.button.MaterialButton;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class AddLogRecordActivity extends AppCompatActivity {

    private ImageButton backButton;
    private TextView titleText;
    private Chip petNameChip;
    private RadioGroup vaccinatedGroup;
    private RadioButton vaccinatedYes, vaccinatedNo;
    private TextInputEditText nextVaccineDateInput, vaccinatedTimeInput, eatRoutineInput, noteInput;
    private SwitchMaterial reminderToggle;
    private MaterialButton submitLogButton;

    // Initialize Firebase Firestore
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private String userId = FirebaseAuth.getInstance().getCurrentUser().getUid(); // Get current user ID
    private String petId;  // Pet ID will be passed via Intent

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_log_record);

        // Retrieve petId from Intent
        petId = getIntent().getStringExtra("petId");

        // Initialize the views by ID
        backButton = findViewById(R.id.backButton);
        titleText = findViewById(R.id.titleText);
        petNameChip = findViewById(R.id.petNameChip);
        vaccinatedGroup = findViewById(R.id.vaccinatedGroup);
        vaccinatedYes = findViewById(R.id.vaccinatedYes);
        vaccinatedNo = findViewById(R.id.vaccinatedNo);
        nextVaccineDateInput = findViewById(R.id.nextVaccineDateInput);
        vaccinatedTimeInput = findViewById(R.id.vaccinatedTimeInput);
        eatRoutineInput = findViewById(R.id.eatRoutineInput);
        noteInput = findViewById(R.id.noteInput);
        reminderToggle = findViewById(R.id.reminderToggle);
        submitLogButton = findViewById(R.id.submitLogButton);

        // Set up listeners or other logic here
        backButton.setOnClickListener(v -> {
            onBackPressed();  // Go back to the previous screen
        });

        submitLogButton.setOnClickListener(v -> {
            submitLog();  // Logic for submitting the log record
        });

        // Set up listener for vaccination status selection
        vaccinatedGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.vaccinatedYes) {
                toggleVaccinationFields(true);
            } else {
                toggleVaccinationFields(false);
            }
        });

        // Set up click listeners for the date and time input fields
        nextVaccineDateInput.setOnClickListener(v -> showDatePicker());
        vaccinatedTimeInput.setOnClickListener(v -> showTimePicker());

        // Load pet data when the activity starts
        getPetDataFromDatabase(petNameChip);
    }

    private void toggleVaccinationFields(boolean show) {
        int visibility = show ? View.VISIBLE : View.GONE;
        nextVaccineDateInput.setVisibility(visibility);
        vaccinatedTimeInput.setVisibility(visibility);
        reminderToggle.setVisibility(visibility);
    }

    private void getPetDataFromDatabase(Chip petNameChip) {
        db.collection("users")
                .document(userId)  // Use current user ID
                .collection("pets")
                .document(petId)  // Use pet ID
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String petName = documentSnapshot.getString("name");
                        String vaccinated = documentSnapshot.getString("vaccinated");
                        String nextVaccine = documentSnapshot.getString("nextVaccine");
                        String vaccinatedTime = documentSnapshot.getString("vaccinatedTime");
                        String eatRoutine = documentSnapshot.getString("eatRoutine");
                        String note = documentSnapshot.getString("note");
                        Boolean reminder = documentSnapshot.getBoolean("reminder");

                        petNameChip.setText(petName);

                        if ("Yes".equals(vaccinated)) {
                            vaccinatedYes.setChecked(true);
                            nextVaccineDateInput.setText(nextVaccine);
                            vaccinatedTimeInput.setText(vaccinatedTime);
                            reminderToggle.setChecked(reminder != null && reminder);
                            toggleVaccinationFields(true);
                        } else {
                            vaccinatedNo.setChecked(true);
                            toggleVaccinationFields(false);
                        }

                        eatRoutineInput.setText(eatRoutine);
                        noteInput.setText(note);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load pet data.", Toast.LENGTH_SHORT).show();
                });
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    String dateStr = dayOfMonth + "/" + (month + 1) + "/" + year;
                    nextVaccineDateInput.setText(dateStr);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        dialog.show();
    }

    private void showTimePicker() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                (view, hourOfDay, minuteOfHour) -> {
                    String timeStr = String.format("%02d:%02d", hourOfDay, minuteOfHour);
                    vaccinatedTimeInput.setText(timeStr);
                }, hour, minute, true);
        timePickerDialog.show();
    }

    private void submitLog() {
        String vaccinated = vaccinatedYes.isChecked() ? "Yes" : "No";
        String nextVaccine = nextVaccineDateInput.getText().toString();
        String vaccinatedTime = vaccinatedTimeInput.getText().toString();
        String eatRoutine = eatRoutineInput.getText().toString();
        String note = noteInput.getText().toString();
        boolean reminder = reminderToggle.isChecked();

        Map<String, Object> log = new HashMap<>();
        log.put("vaccinated", vaccinated);
        if (vaccinated.equals("Yes")) {
            log.put("nextVaccine", nextVaccine);
            log.put("vaccinatedTime", vaccinatedTime);
            log.put("reminder", reminder);
        }
        log.put("eatRoutine", eatRoutine);
        log.put("note", note);
        log.put("timestamp", System.currentTimeMillis());

        // Save log with fixed document ID "info" to ensure only one log per pet
        db.collection("users")
                .document(userId)  // Use current user ID
                .collection("pets")
                .document(petId)  // Use pet ID
                .collection("Log record")
                .document("info")  // Use fixed document ID "info"
                .set(log)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Log added!", Toast.LENGTH_SHORT).show();

                    // If reminder is enabled and next vaccine and time are set, schedule a reminder
                    if (reminder && !nextVaccine.isEmpty() && !vaccinatedTime.isEmpty()) {
                        String[] dateParts = nextVaccine.split("/");
                        String[] timeParts = vaccinatedTime.split(":");

                        int day = Integer.parseInt(dateParts[0]);
                        int month = Integer.parseInt(dateParts[1]) - 1;
                        int year = Integer.parseInt(dateParts[2]);
                        int hour = Integer.parseInt(timeParts[0]);
                        int minute = Integer.parseInt(timeParts[1]);

                        Calendar calendar = Calendar.getInstance();
                        calendar.set(year, month, day, hour, minute, 0);

                        long reminderTime = calendar.getTimeInMillis();
                        setReminder(reminderTime, petNameChip.getText().toString());
                    }

                    finish();  // Close the activity after successful log submission
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error adding log.", Toast.LENGTH_SHORT).show();
                });
    }

    private void setReminder(long reminderTime, String petName) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        if (alarmManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                startActivity(intent);
                return;
            }

            long reminderTimeBefore30Min = reminderTime - (30 * 60 * 1000);
            Intent intent = new Intent(this, ReminderReceiver.class);
            intent.putExtra("petName", petName);

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderTimeBefore30Min, pendingIntent);
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, reminderTimeBefore30Min, pendingIntent);
                }
            } catch (SecurityException e) {
                Toast.makeText(this, "Error scheduling alarm: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }
}
