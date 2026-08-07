package com.bloodline.madibets.model;

public class Query {
    private int queryID;
    private String title;
    private String description;
    private String queryDate;
    private int userID;
    private String resolvedStatus;

    public Query() {}

    public int getQueryID() { return queryID; }
    public void setQueryID(int queryID) { this.queryID = queryID; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getQueryDate() { return queryDate; }
    public void setQueryDate(String queryDate) { this.queryDate = queryDate; }

    public int getUserID() { return userID; }
    public void setUserID(int userID) { this.userID = userID; }

    public String getResolvedStatus() { return resolvedStatus; }
    public void setResolvedStatus(String resolvedStatus) { this.resolvedStatus = resolvedStatus; }
}
