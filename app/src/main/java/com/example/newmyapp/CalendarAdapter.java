package com.example.newmyapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.DayViewHolder> {

    private List<LocalDate> days;
    private Set<LocalDate> highlightDays;
    private Set<LocalDate> eventDays = new HashSet<>();
    private LocalDate selectedDate = null;
    private OnDateClickListener onDateClickListener;

    // 新增：日期点击监听接口
    public interface OnDateClickListener {
        void onDateClick(LocalDate date);
    }

    // 新增：设置日期点击监听器
    public void setOnDateClickListener(OnDateClickListener listener) {
        this.onDateClickListener = listener;
    }

    public CalendarAdapter(List<LocalDate> days, Set<LocalDate> highlightDays) {
        this.days = days;
        this.highlightDays = highlightDays;
    }

    public void updateDays(List<LocalDate> newDays) {
        this.days = newDays;
        notifyDataSetChanged();
    }

    public void addEventDay(LocalDate date) {
        eventDays.add(date);
        notifyDataSetChanged();
    }

    public void clearEventDays() {
        eventDays.clear();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.calendardayitem, parent, false);
        return new DayViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DayViewHolder holder, int position) {
        LocalDate date = days.get(position);

        if (date == null) {
            holder.dayText.setText("");
            holder.dayOfWeekText.setText("");
            holder.dayText.setBackgroundResource(R.drawable.day_normal_bg);
        } else {
            holder.dayText.setText(String.valueOf(date.getDayOfMonth()));

            DayOfWeek dayOfWeek = date.getDayOfWeek();
            String dayOfWeekStr = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault());
            holder.dayOfWeekText.setText(dayOfWeekStr);

            if (date.equals(selectedDate)) {
                holder.dayText.setBackgroundResource(R.drawable.day_selected_bg);
            } else if (eventDays.contains(date)) {
                holder.dayText.setBackgroundResource(R.drawable.day_event_bg);
            } else if (highlightDays.contains(date)) {
                holder.dayText.setBackgroundResource(R.drawable.day_highlight_bg);
            } else {
                holder.dayText.setBackgroundResource(R.drawable.day_normal_bg);
            }
        }

        holder.itemView.setOnClickListener(v -> {
            if (date != null) {
                selectedDate = date;
                notifyDataSetChanged();
                // 新增：触发日期点击事件
                if (onDateClickListener != null) {
                    onDateClickListener.onDateClick(date);
                }
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (date != null) {
                if (eventDays.contains(date)) {
                    eventDays.remove(date);
                } else {
                    eventDays.add(date);
                }
                notifyDataSetChanged();
            }
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return days.size();
    }

    static class DayViewHolder extends RecyclerView.ViewHolder {
        TextView dayText;
        TextView dayOfWeekText;

        DayViewHolder(View itemView) {
            super(itemView);
            dayText = itemView.findViewById(R.id.dayText);
            dayOfWeekText = itemView.findViewById(R.id.dayOfWeekText);
        }
    }
}