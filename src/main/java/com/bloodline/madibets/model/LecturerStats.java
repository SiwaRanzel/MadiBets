package com.bloodline.madibets.model;

public class LecturerStats {
    private int totalGroups;
    private int totalStudents;
    private int newStudentsThisWeek;
    private int activeToday;

    public LecturerStats() {}

    public LecturerStats(int totalGroups, int totalStudents, int newStudentsThisWeek, int activeToday) {
        this.totalGroups = totalGroups;
        this.totalStudents = totalStudents;
        this.newStudentsThisWeek = newStudentsThisWeek;
        this.activeToday = activeToday;
    }

    public int getTotalGroups() {
        return totalGroups;
    }

    public void setTotalGroups(int totalGroups) {
        this.totalGroups = totalGroups;
    }

    public int getTotalStudents() {
        return totalStudents;
    }

    public void setTotalStudents(int totalStudents) {
        this.totalStudents = totalStudents;
    }

    public int getNewStudentsThisWeek() {
        return newStudentsThisWeek;
    }

    public void setNewStudentsThisWeek(int newStudentsThisWeek) {
        this.newStudentsThisWeek = newStudentsThisWeek;
    }

    public int getActiveToday() {
        return activeToday;
    }

    public void setActiveToday(int activeToday) {
        this.activeToday = activeToday;
    }
}
