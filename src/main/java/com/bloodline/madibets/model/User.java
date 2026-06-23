package com.bloodline.madibets.model;

/** Mirrors one row of the User table. Owner: Siwapiwe (A-series). */
public class User {
    private int userID;
    private String name;
    private String surname;
    private String email;
    private String password;   // stored as a bcrypt hash
    private String userType;   // STUDENT | LECTURER | ADMIN

    public User() {}

    public int getUserID()              { return userID; }
    public void setUserID(int v)        { this.userID = v; }
    public String getName()             { return name; }
    public void setName(String v)       { this.name = v; }
    public String getSurname()          { return surname; }
    public void setSurname(String v)    { this.surname = v; }
    public String getEmail()            { return email; }
    public void setEmail(String v)      { this.email = v; }
    public String getPassword()         { return password; }
    public void setPassword(String v)   { this.password = v; }
    public String getUserType()         { return userType; }
    public void setUserType(String v)   { this.userType = v; }
}
