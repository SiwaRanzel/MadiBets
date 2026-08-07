package com.bloodline.madibets.model;

import java.math.BigDecimal;

public class Task {
    private int taskID;
    private String title;
    private String description;
    private int groupID;
    private Integer userID; // Can be null if assigned to the whole group
    private BigDecimal amount;
    private int createdBy;

    // Constructors
    public Task() {}

    public Task(int taskID, String title, String description, int groupID, Integer userID, BigDecimal amount, int createdBy) {
        this.taskID = taskID;
        this.title = title;
        this.description = description;
        this.groupID = groupID;
        this.userID = userID;
        this.amount = amount;
        this.createdBy = createdBy;
    }

    // Getters and Setters
    public int getTaskID() {
        return taskID;
    }

    public void setTaskID(int taskID) {
        this.taskID = taskID;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getGroupID() {
        return groupID;
    }

    public void setGroupID(int groupID) {
        this.groupID = groupID;
    }

    public Integer getUserID() {
        return userID;
    }

    public void setUserID(Integer userID) {
        this.userID = userID;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public int getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(int createdBy) {
        this.createdBy = createdBy;
    }
}
