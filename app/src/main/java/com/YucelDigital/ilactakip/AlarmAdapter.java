package com.YucelDigital.ilactakip;

import android.content.Context;

import java.util.HashMap;
import java.util.Map;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;

import java.util.List;

public class AlarmAdapter extends BaseAdapter {

    public interface OnMedicineInteractionListener {
        void onScheduleAlarm(Medicine medicine);
        void onSaveData();
        void onCheckEmptyState();
        void onOpenEditPage(Medicine medicine, int position);
    }

    private final Context context;
    private List<Medicine> medicineList;
    private final LayoutInflater inflater;
    private final OnMedicineInteractionListener listener;

    public AlarmAdapter(Context context, List<Medicine> medicineList, OnMedicineInteractionListener listener) {
        this.context      = context;
        this.medicineList = medicineList;
        this.inflater     = LayoutInflater.from(context);
        this.listener     = listener;
    }

    /** Adapter verilerini güncelle ve listeyi yenile (scroll pozisyonu korunsun) */
    public void updateData(List<Medicine> newList) {
        this.medicineList = newList;
        notifyDataSetChanged();
    }

    @Override public int    getCount()                { return medicineList.size(); }
    @Override public Object getItem(int position)     { return medicineList.get(position); }
    @Override public long   getItemId(int position)   { return position; }

    private static class ViewHolder {
        TextView nameTxt, timeTxt, daysTxt, noteTxt, statusTxt;
        LinearLayout daysContainer, noteContainer;
        SwitchCompat activeSwitch;
        CheckBox takenCheck;
        ImageView deleteBtn;
        View statusIndicator;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.list_item_alarm, parent, false);
            holder = new ViewHolder();
            holder.nameTxt = convertView.findViewById(R.id.medicineNameTextView);
            holder.timeTxt = convertView.findViewById(R.id.timeTextView);
            holder.daysTxt = convertView.findViewById(R.id.daysTextView);
            holder.noteTxt = convertView.findViewById(R.id.noteTextView);
            holder.statusTxt = convertView.findViewById(R.id.statusTextView);
            holder.daysContainer = convertView.findViewById(R.id.daysContainer);
            holder.noteContainer = convertView.findViewById(R.id.noteContainer);
            holder.activeSwitch = convertView.findViewById(R.id.activeSwitch);
            holder.takenCheck = convertView.findViewById(R.id.medicineCheckBox);
            holder.deleteBtn = convertView.findViewById(R.id.deleteButton);
            holder.statusIndicator = convertView.findViewById(R.id.statusIndicator);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Medicine medicine = medicineList.get(position);

        holder.nameTxt.setText(medicine.getName());

        if (medicine.isUseCustomDays() && medicine.getCustomDayTimes() != null) {
            holder.timeTxt.setText(formatCustomDayTimes(medicine.getCustomDayTimes()));
        } else {
            holder.timeTxt.setText(medicine.getTime());
        }
        holder.timeTxt.setVisibility(View.VISIBLE);

        int colorTaken   = ContextCompat.getColor(context, R.color.status_taken);
        int colorMissed  = ContextCompat.getColor(context, R.color.status_missed);
        int colorActive  = ContextCompat.getColor(context, R.color.indicator_active);
        int colorExpired = ContextCompat.getColor(context, R.color.indicator_expired);

        // Check expiry
        boolean isExpired = false;
        long endDate = medicine.getEndDate();
        if (endDate != 0) {
            java.util.Calendar endCal = java.util.Calendar.getInstance();
            endCal.setTimeInMillis(endDate);
            endCal.set(java.util.Calendar.HOUR_OF_DAY, 23);
            endCal.set(java.util.Calendar.MINUTE, 59);
            if (System.currentTimeMillis() > endCal.getTimeInMillis()) isExpired = true;
        }
        final boolean expired = isExpired;

        if (expired) {
            holder.statusTxt.setText("Tarihi Geçti");
            holder.statusTxt.setTextColor(colorExpired);
            if (holder.statusIndicator != null) holder.statusIndicator.setBackgroundColor(colorExpired);
            holder.activeSwitch.setEnabled(false);
            holder.takenCheck.setEnabled(false);
        } else if (medicine.isTaken()) {
            holder.statusTxt.setText("İlacını Aldın ✓");
            holder.statusTxt.setTextColor(colorTaken);
            if (holder.statusIndicator != null)
                holder.statusIndicator.setBackgroundColor(ContextCompat.getColor(context, R.color.indicator_taken));
            holder.activeSwitch.setEnabled(true);
            holder.takenCheck.setEnabled(true);
        } else {
            holder.statusTxt.setText("İlacını Almadın");
            holder.statusTxt.setTextColor(colorMissed);
            if (holder.statusIndicator != null)
                holder.statusIndicator.setBackgroundColor(ContextCompat.getColor(context, R.color.indicator_missed));
            holder.activeSwitch.setEnabled(true);
            holder.takenCheck.setEnabled(true);
        }

        if (!medicine.isActive() && !expired) {
            if (holder.statusIndicator != null) holder.statusIndicator.setBackgroundColor(colorExpired);
        }

        // Date & Note
        if (medicine.isUseCustomDays() && medicine.getCustomDayTimes() != null) {
            holder.daysContainer.setVisibility(View.GONE);
        } else if (medicine.getDateRange() != null && !medicine.getDateRange().isEmpty()) {
            holder.daysContainer.setVisibility(View.VISIBLE);
            holder.daysTxt.setText(medicine.getDateRange());
        } else {
            holder.daysContainer.setVisibility(View.GONE);
        }

        if (medicine.getNote() != null && !medicine.getNote().isEmpty()) {
            holder.noteContainer.setVisibility(View.VISIBLE);
            holder.noteTxt.setText(medicine.getNote());
        } else {
            holder.noteContainer.setVisibility(View.GONE);
        }

        // Remove listeners first to prevent recycling bugs
        holder.activeSwitch.setOnCheckedChangeListener(null);
        holder.takenCheck.setOnCheckedChangeListener(null);

        holder.activeSwitch.setChecked(medicine.isActive());
        holder.takenCheck.setChecked(medicine.isTaken());

        // Switch — Active/Inactive
        holder.activeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            medicine.setActive(isChecked);
            if (!isChecked) {
                cancelAlarm(medicine);
                Toast.makeText(context, "Alarm pasif edildi", Toast.LENGTH_SHORT).show();
            } else {
                if (listener != null) {
                    listener.onScheduleAlarm(medicine);
                    Toast.makeText(context, "Alarm aktif edildi", Toast.LENGTH_SHORT).show();
                }
            }
            triggerSave();
            notifyDataSetChanged();
        });

        // Taken checkbox
        holder.takenCheck.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (expired) return;
            medicine.setTaken(isChecked);
            triggerSave();
            notifyDataSetChanged();
        });

        // Delete with confirmation
        holder.deleteBtn.setOnClickListener(v -> {
            new AlertDialog.Builder(context)
                .setTitle("İlacı Sil")
                .setMessage(medicine.getName() + " ilacını silmek istediğinize emin misiniz?")
                .setPositiveButton("Sil", (dialog, which) -> {
                    cancelAlarm(medicine);
                    medicineList.remove(medicine);
                    notifyDataSetChanged();
                    if (listener != null) {
                        listener.onCheckEmptyState();
                        listener.onSaveData();
                    }
                    Toast.makeText(context, "İlaç silindi", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("İptal", null)
                .show();
        });

        // Edit on click
        convertView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onOpenEditPage(medicine, position);
            }
        });

        return convertView;
    }

    private void triggerSave() {
        if (listener != null) {
            listener.onSaveData();
        }
    }

    private void cancelAlarm(Medicine medicine) {
        AlarmHelper.cancelAlarm(context, medicine);
    }

    /** HashMap<calDay, times> → "Pzt 09:00, Sal 10:30" formatına dönüştür */
    private String formatCustomDayTimes(HashMap<Integer, String> dayTimes) {
        String[] shortNames = {"", "Paz", "Pzt", "Sal", "Çar", "Per", "Cum", "Cmt"};
        int[] ordered = {2, 3, 4, 5, 6, 7, 1}; // Pzt-Paz sırası

        StringBuilder sb = new StringBuilder();
        for (int day : ordered) {
            if (dayTimes.containsKey(day)) {
                String times = dayTimes.get(day);
                // Sadece ilk saati göster (frekans otomatik hesaplanmış olabilir)
                String firstTime = times;
                if (times.contains(",")) {
                    firstTime = times.split(",")[0].trim();
                }
                if (sb.length() > 0) sb.append(", ");
                sb.append(shortNames[day]).append(" ").append(firstTime);
            }
        }
        return sb.toString();
    }
}