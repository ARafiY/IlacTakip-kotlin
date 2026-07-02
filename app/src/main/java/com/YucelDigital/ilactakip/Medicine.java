package com.YucelDigital.ilactakip;

import java.io.Serializable;
import java.util.HashMap;

public class Medicine implements Serializable {
    private static final long serialVersionUID = 1L;
    private int intervalDays;
    private String name;
    private String time; // Standart mod: "08:00, 14:00"
    private String dateRange;
    private String note;
    private boolean isActive;
    private boolean isTaken;
    private long startDate;
    private long endDate;
    private String soundUri;

    // Güne özel zamanlama
    private boolean useCustomDays;
    // Key: Calendar gün sabiti (1=Pazar, 2=Pazartesi, ... 7=Cumartesi)
    // Value: Başlangıç saati "09:00"
    private HashMap<Integer, String> customDayTimes;

    public Medicine(String name, String time, String dateRange, String note) {
        this.name = name;
        this.time = time;
        this.dateRange = dateRange;
        this.note = note;
        this.isActive = true;
        this.isTaken = false;
        this.startDate = 0;
        this.endDate = 0;
        this.intervalDays = 1;
        this.soundUri = null;
        this.useCustomDays = false;
        this.customDayTimes = null;
    }

    // Getter ve Setter Metotları
    public String getName() { return name; }
    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }
    public String getDateRange() { return dateRange; }
    public String getNote() { return note; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    public boolean isTaken() { return isTaken; }
    public void setTaken(boolean taken) { isTaken = taken; }
    public long getStartDate() { return startDate; }
    public void setStartDate(long startDate) { this.startDate = startDate; }
    public long getEndDate() { return endDate; }
    public void setEndDate(long endDate) { this.endDate = endDate; }
    public int getIntervalDays() { return intervalDays; }
    public void setIntervalDays(int intervalDays) { this.intervalDays = intervalDays; }
    public String getSoundUri() { return soundUri; }
    public void setSoundUri(String soundUri) { this.soundUri = soundUri; }

    // Güne özel zamanlama
    public boolean isUseCustomDays() { return useCustomDays; }
    public void setUseCustomDays(boolean useCustomDays) { this.useCustomDays = useCustomDays; }
    public HashMap<Integer, String> getCustomDayTimes() { return customDayTimes; }
    public void setCustomDayTimes(HashMap<Integer, String> customDayTimes) { this.customDayTimes = customDayTimes; }
}