package com.bloodline.madibets.model;

/** Owner: Pieter (D-series). Task = a True/False question assigned to a Group. */
public class Task {
    private int taskID;
    private int groupID;
    private String question;
    private boolean correctAnswer;
    private double amount;
    private int createdBy;
    private java.sql.Timestamp createdDate;

    // Transient fields populated for the UI (not stored in DB)
    private Boolean answered;      // null = not answered, true/false = student's submitted answer
    private Boolean isCorrect;     // null = not answered, true/false = whether the answer was correct

    public Task() {}

    public Task(int taskID, int groupID, String question, boolean correctAnswer, double amount, int createdBy, java.sql.Timestamp createdDate) {
        this.taskID = taskID;
        this.groupID = groupID;
        this.question = question;
        this.correctAnswer = correctAnswer;
        this.amount = amount;
        this.createdBy = createdBy;
        this.createdDate = createdDate;
    }

    public int getTaskID() { return taskID; }
    public void setTaskID(int taskID) { this.taskID = taskID; }

    public int getGroupID() { return groupID; }
    public void setGroupID(int groupID) { this.groupID = groupID; }

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }

    public boolean isCorrectAnswer() { return correctAnswer; }
    public void setCorrectAnswer(boolean correctAnswer) { this.correctAnswer = correctAnswer; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public int getCreatedBy() { return createdBy; }
    public void setCreatedBy(int createdBy) { this.createdBy = createdBy; }

    public java.sql.Timestamp getCreatedDate() { return createdDate; }
    public void setCreatedDate(java.sql.Timestamp createdDate) { this.createdDate = createdDate; }

    public Boolean getAnswered() { return answered; }
    public void setAnswered(Boolean answered) { this.answered = answered; }

    public Boolean getIsCorrect() { return isCorrect; }
    public void setIsCorrect(Boolean isCorrect) { this.isCorrect = isCorrect; }

    @Override
    public String toString() {
        return "Task{" +
                "taskID=" + taskID +
                ", groupID=" + groupID +
                ", question='" + question + '\'' +
                ", correctAnswer=" + correctAnswer +
                ", amount=" + amount +
                ", createdBy=" + createdBy +
                ", createdDate=" + createdDate +
                '}';
    }
}