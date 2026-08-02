package com.bloodline.madibets.model;

/**
 * Owner: Jason (C-series)
 * Maps to the Friendship table in the madibets database.
 * Represents a friend request between two users.
 */
public class Friendship {
    private int friendshipID;
    private int requesterID;
    private int addresseID;
    private String status; // PENDING, ACCEPTED, REJECTED

    // Optional: store the friend's name for display purposes (not in DB, populated by joins)
    private String requesterName;
    private String addresseName;

    public Friendship() {}

    public Friendship(int friendshipID, int requesterID, int addresseID, String status) {
        this.friendshipID = friendshipID;
        this.requesterID = requesterID;
        this.addresseID = addresseID;
        this.status = status;
    }

    public int getFriendshipID()              { return friendshipID; }
    public void setFriendshipID(int v)        { this.friendshipID = v; }
    public int getRequesterID()               { return requesterID; }
    public void setRequesterID(int v)         { this.requesterID = v; }
    public int getAddresseID()                { return addresseID; }
    public void setAddresseID(int v)          { this.addresseID = v; }
    public String getStatus()                 { return status; }
    public void setStatus(String v)           { this.status = v; }
    public String getRequesterName()          { return requesterName; }
    public void setRequesterName(String v)    { this.requesterName = v; }
    public String getAddresseName()           { return addresseName; }
    public void setAddresseName(String v)     { this.addresseName = v; }
}
