package com.example.newmyapp;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CalendarPageActivity extends AppCompatActivity {

    // 日历相关组件
    private RecyclerView calendarRecyclerView;
    private CalendarAdapter calendarAdapter;
    private TextView monthText;
    private Button prevMonthButton, nextMonthButton;

    // 日期相关变量
    private LocalDate currentMonth;
    private LocalDate selectedDate = LocalDate.now(); // 新增：记录选中的日期
    private Set<LocalDate> highlightDays = new HashSet<>();

    // Firebase相关
    private FirebaseFirestore db;
    private String userId;

    // 宠物数据
    private List<String> petIds = new ArrayList<>();
    private List<PetVaccineInfo> petVaccineInfoList = new ArrayList<>();

    // 底部导航
    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar_page);

        // 初始化底部导航
        setupBottomNavigation();

        // 初始化视图组件
        initViews();

        // 设置日历数据
        currentMonth = LocalDate.now().withDayOfMonth(1);
        highlightDays.add(LocalDate.now());

        // 初始化Firebase
        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // 加载数据
        updateCalendar();
        loadPetsAndEvents();

        // 设置按钮点击事件
        setupButtonListeners();
    }

    private void setupBottomNavigation() {
        BottomNavigationView navigation = findViewById(R.id.bottom_navigation);
        mOnNavigationItemSelectedListener = item -> {
            if (item.getItemId() == R.id.navigation_home) {
                startActivity(new Intent(this, PetActivity.class));
                return true;
            } else if (item.getItemId() == R.id.navigation_calendar) {
                return true; // 已经在日历页面
            } else if (item.getItemId() == R.id.navigation_community) {
                startActivity(new Intent(this, PetListActivity.class));
                return true;
            } else if (item.getItemId() == R.id.navigation_profile) {
                // 处理个人资料页面
                return true;
            }
            return false;
        };
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
    }

    private void initViews() {
        calendarRecyclerView = findViewById(R.id.calendarRecyclerView);
        monthText = findViewById(R.id.monthText);
        prevMonthButton = findViewById(R.id.prevMonthButton);
        nextMonthButton = findViewById(R.id.nextMonthButton);

        calendarRecyclerView.setLayoutManager(new GridLayoutManager(this, 7));
    }

    private void setupButtonListeners() {
        prevMonthButton.setOnClickListener(v -> {
            currentMonth = currentMonth.minusMonths(1);
            updateCalendar();
            loadPetsAndEvents();
        });

        nextMonthButton.setOnClickListener(v -> {
            currentMonth = currentMonth.plusMonths(1);
            updateCalendar();
            loadPetsAndEvents();
        });
    }

    // 更新日历显示
    private void updateCalendar() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM yyyy");
        monthText.setText(currentMonth.format(formatter));

        List<LocalDate> days = new ArrayList<>();
        LocalDate firstDayOfCalendar = currentMonth.withDayOfMonth(1);
        int dayOfWeek = firstDayOfCalendar.getDayOfWeek().getValue();

        // 添加空白格直到当月第一天
        for (int i = 1; i < dayOfWeek; i++) {
            days.add(null);
        }

        // 添加当月所有日期
        int lengthOfMonth = currentMonth.lengthOfMonth();
        for (int i = 1; i <= lengthOfMonth; i++) {
            days.add(currentMonth.withDayOfMonth(i));
        }

        // 补齐6行7列共42格
        while (days.size() < 42) {
            days.add(null);
        }

        // 初始化或更新日历适配器
        if (calendarAdapter == null) {
            calendarAdapter = new CalendarAdapter(days, highlightDays);
            calendarAdapter.setOnDateClickListener(date -> {
                selectedDate = date;
                TextView selectedDateText = findViewById(R.id.selectedDateText);
                selectedDateText.setText("Vaccine Date: " + date.format(DateTimeFormatter.ofPattern("yyyy/MM/dd")));
                highlightEventsForDate(date);
            });
            calendarRecyclerView.setAdapter(calendarAdapter);
        } else {
            calendarAdapter.updateDays(days);
        }
    }

    // 加载宠物和疫苗事件
    private void loadPetsAndEvents() {
        if (calendarAdapter == null) return;

        calendarAdapter.clearEventDays();
        petVaccineInfoList.clear();

        db.collection("users")
                .document(userId)
                .collection("pets")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    petIds.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        petIds.add(document.getId());
                    }
                    loadEventsForPets();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "加载宠物失败", Toast.LENGTH_SHORT).show();
                });
    }

    // 加载宠物疫苗事件
    private void loadEventsForPets() {
        TableLayout tableLayout = findViewById(R.id.petInfoTable);

        // 清除现有行（保留标题行）
        int childCount = tableLayout.getChildCount();
        for (int i = 1; i < childCount; i++) {
            tableLayout.removeViewAt(1);
        }

        if (petIds.isEmpty()) {
            return;
        }

        final int[] petsProcessed = {0};
        for (String petId : petIds) {
            db.collection("users")
                    .document(userId)
                    .collection("pets")
                    .document(petId)
                    .get()
                    .addOnSuccessListener(petDocumentSnapshot -> {
                        if (petDocumentSnapshot.exists()) {
                            String petName = (String) petDocumentSnapshot.get("name");

                            db.collection("users")
                                    .document(userId)
                                    .collection("pets")
                                    .document(petId)
                                    .collection("Log record")
                                    .document("info")
                                    .get()
                                    .addOnSuccessListener(logRecordSnapshot -> {
                                        if (logRecordSnapshot.exists()) {
                                            Map<String, Object> data = logRecordSnapshot.getData();
                                            if (data != null) {
                                                String nextVaccineDate = (String) data.get("nextVaccine");
                                                String note = (String) data.get("note");

                                                if (nextVaccineDate != null) {
                                                    try {
                                                        LocalDate eventDate = parseDate(nextVaccineDate);
                                                        if (eventDate != null) {
                                                            // 如果是当前月份的日期，添加到高亮
                                                            if (eventDate.getYear() == currentMonth.getYear() &&
                                                                    eventDate.getMonth() == currentMonth.getMonth()) {
                                                                calendarAdapter.addEventDay(eventDate);
                                                            }

                                                            // 添加到疫苗信息列表
                                                            petVaccineInfoList.add(new PetVaccineInfo(
                                                                    petName,
                                                                    nextVaccineDate,
                                                                    note != null ? note : "无备注",
                                                                    eventDate
                                                            ));
                                                        }
                                                    } catch (Exception e) {
                                                        e.printStackTrace();
                                                    }
                                                }
                                            }
                                        }

                                        petsProcessed[0]++;
                                        if (petsProcessed[0] == petIds.size()) {
                                            sortAndDisplayPetInfo();
                                            // 刷新高亮显示
                                            highlightEventsForDate(selectedDate);
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        e.printStackTrace();
                                        petsProcessed[0]++;
                                        if (petsProcessed[0] == petIds.size()) {
                                            sortAndDisplayPetInfo();
                                        }
                                    });
                        }
                    })
                    .addOnFailureListener(e -> {
                        e.printStackTrace();
                        petsProcessed[0]++;
                        if (petsProcessed[0] == petIds.size()) {
                            sortAndDisplayPetInfo();
                        }
                    });
        }
    }

    // 按日期排序并显示宠物信息
    private void sortAndDisplayPetInfo() {
        // 按疫苗日期排序（从早到晚）
        Collections.sort(petVaccineInfoList, Comparator.comparing(o -> o.eventDate));

        // 按排序结果添加到表格
        TableLayout tableLayout = findViewById(R.id.petInfoTable);
        for (PetVaccineInfo info : petVaccineInfoList) {
            addPetInfoToTable(info.petName, info.nextVaccineDate, info.note, info.eventDate);
        }
    }

    // 高亮显示特定日期的疫苗事件
    private void highlightEventsForDate(LocalDate date) {
        TableLayout tableLayout = findViewById(R.id.petInfoTable);

        // 跳过标题行（索引0）
        for (int i = 1; i < tableLayout.getChildCount(); i++) {
            TableRow row = (TableRow) tableLayout.getChildAt(i);
            LocalDate rowDate = (LocalDate) row.getTag();

            if (rowDate != null && rowDate.equals(date)) {
                // 高亮匹配的行
                row.setBackgroundColor(getResources().getColor(R.color.highlight_color));
                // 设置文字颜色
                for (int j = 0; j < row.getChildCount(); j++) {
                    TextView tv = (TextView) row.getChildAt(j);
                    tv.setTextColor(getResources().getColor(R.color.highlight_text_color));
                }
            } else {
                // 重置不匹配的行
                row.setBackgroundColor(getResources().getColor(android.R.color.white));
                for (int j = 0; j < row.getChildCount(); j++) {
                    TextView tv = (TextView) row.getChildAt(j);
                    tv.setTextColor(getResources().getColor(android.R.color.black));
                }
            }
        }
    }

    // 添加宠物信息到表格
    private void addPetInfoToTable(String petName, String nextVaccineDate, String note, LocalDate date) {
        TableLayout tableLayout = findViewById(R.id.petInfoTable);
        TableRow tableRow = new TableRow(this);

        // 存储日期用于后续高亮
        tableRow.setTag(date);

        // 宠物名字
        TextView nameTextView = new TextView(this);
        nameTextView.setText(petName);
        nameTextView.setPadding(16, 8, 16, 8);
        tableRow.addView(nameTextView);

        // 疫苗日期
        TextView vaccineDateTextView = new TextView(this);
        vaccineDateTextView.setText(nextVaccineDate);
        vaccineDateTextView.setPadding(16, 8, 16, 8);
        tableRow.addView(vaccineDateTextView);

        // 备注
        TextView noteTextView = new TextView(this);
        noteTextView.setText(note);
        noteTextView.setPadding(16, 8, 16, 8);
        tableRow.addView(noteTextView);

        tableLayout.addView(tableRow);

        // 如果是选中的日期，立即高亮显示
        if (date != null && date.equals(selectedDate)) {
            tableRow.setBackgroundColor(getResources().getColor(R.color.highlight_color));
            nameTextView.setTextColor(getResources().getColor(R.color.highlight_text_color));
            vaccineDateTextView.setTextColor(getResources().getColor(R.color.highlight_text_color));
            noteTextView.setTextColor(getResources().getColor(R.color.highlight_text_color));
        }
    }

    // 解析日期字符串
    private LocalDate parseDate(String dateStr) {
        try {
            if (dateStr.contains("/")) {
                String[] parts = dateStr.split("/");
                int day = Integer.parseInt(parts[0]);
                int month = Integer.parseInt(parts[1]);
                int year = Integer.parseInt(parts[2]);
                return LocalDate.of(year, month, day);
            } else {
                return LocalDate.parse(dateStr);
            }
        } catch (Exception e) {
            return null;
        }
    }

    // 宠物疫苗信息辅助类
    private static class PetVaccineInfo {
        String petName;
        String nextVaccineDate;
        String note;
        LocalDate eventDate;

        PetVaccineInfo(String petName, String nextVaccineDate, String note, LocalDate eventDate) {
            this.petName = petName;
            this.nextVaccineDate = nextVaccineDate;
            this.note = note;
            this.eventDate = eventDate;
        }
    }
}