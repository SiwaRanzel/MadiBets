package com.bloodline.madibets.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Owner: Pieter (D-series). TODO: add fields from the Group table + getters/setters. */
public class Group {
    private int groupID;
    private String groupName;
    private String description;
    private String password;   // optional join password; plaintext inbound, stored as a bcrypt HASH
    private boolean hasPassword; // transient flag for the client (never exposes the hash)
    private Integer maxMembers; // max joining members (EXCLUDES owner); null = unlimited
    private int createdBy;
    private java.sql.Timestamp createdDate;
    private boolean createdByAdmin;
    private String createdByType;  // STUDENT | LECTURER | ADMIN
    private String creatorName;    // Full name of the creator (e.g. "Jason Sauls")

    public Group() {}

    public Group(int groupID, String groupName, String description, int createdBy, java.sql.Timestamp createdDate) {
        this.groupID = groupID;
        this.groupName = groupName;
        this.description = description;
        this.createdBy = createdBy;
        this.createdDate = createdDate;
    }

    public int getGroupID() {
        return groupID;
    }

    public void setGroupID(int groupID) {
        this.groupID = groupID;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    // WRITE_ONLY: accept an inbound password on create, but never serialize the
    // (hashed) value back out to any client.
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    public String getPassword() {
        return password;
    }

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isHasPassword() {
        return hasPassword;
    }

    public void setHasPassword(boolean hasPassword) {
        this.hasPassword = hasPassword;
    }

    public Integer getMaxMembers() {
        return maxMembers;
    }

    public void setMaxMembers(Integer maxMembers) {
        this.maxMembers = maxMembers;
    }

    public int getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(int createdBy) {
        this.createdBy = createdBy;
    }

    public java.sql.Timestamp getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(java.sql.Timestamp createdDate) {
        this.createdDate = createdDate;
    }

    public boolean isCreatedByAdmin() {
        return createdByAdmin;
    }

    public void setCreatedByAdmin(boolean createdByAdmin) {
        this.createdByAdmin = createdByAdmin;
    }

    public String getCreatedByType() {
        return createdByType;
    }

    public void setCreatedByType(String createdByType) {
        this.createdByType = createdByType;
    }

    public String getCreatorName() {
        return creatorName;
    }

    public void setCreatorName(String creatorName) {
        this.creatorName = creatorName;
    }

    public boolean isCreatedByLecturer() {
        return "LECTURER".equalsIgnoreCase(createdByType);
    }

    @Override
    public String toString() {
        return "Group{" +
                "groupID=" + groupID +
                ", groupName='" + groupName + '\'' +
                ", description='" + description + '\'' +
                ", createdBy=" + createdBy +
                ", createdDate=" + createdDate +
                '}';
    }
}
