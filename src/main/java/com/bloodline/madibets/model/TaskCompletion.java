package com.bloodline.madibets.model;

import java.sql.Timestamp;

public class TaskCompletion {
    private int completionID;
    private int taskID;
    private int userID;
    private String completionStatus; // 'PENDING', 'COMPLETED', 'REJECTED'
    private Timestamp completionDate;

    public TaskCompletion() {}

    public TaskCompletion(int completionID, int taskID, int userID, String completionStatus, Timestamp completionDate) {
        this.completionID = completionID;
        this.taskID = taskID;
        this.userID = userID;
        this.completionStatus = completionStatus;
        this.completionDate = completionDate;
    }

    public int getCompletionID() {
        return completionID;
    }

    public void setCompletionID(int completionID) {
        this.completionID = completionID;
    }

    public int getTaskID() {
        return taskID;
    }

    public void setTaskID(int taskID) {
        this.taskID = taskID;
    }

    public int getUserID() {
        return userID;
    }

    public void setUserID(int userID) {
        this.userID = userID;
    }

    public String getCompletionStatus() {
        return completionStatus;
    }

    public void setCompletionStatus(String completionStatus) {
        this.completionStatus = completionStatus;
    }

    public Timestamp getCompletionDate() {
        return completionDate;
    }

    public void setCompletionDate(Timestamp completionDate) {
        this.completionDate = completionDate;
    }
}
