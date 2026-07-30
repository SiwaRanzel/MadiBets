package com.bloodline.madibets.model;

public class User {
    private int userID;
    private String name;
    private String surname;
    private String email;
    private String password;
    private String userType;
    private String studentNo;
    private String staffNo;
    private String avatarPath;

    public User() {}

    public User(int userID, String name, String surname, String email, String password, String userType) {
        this.userID = userID;
        this.name = name;
        this.surname = surname;
        this.email = email;
        this.password = password;
        this.userType = userType;
    }

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
    public String getStudentNo()        { return studentNo; }
    public void setStudentNo(String v)  { this.studentNo = v; }
    public String getStaffNo()          { return staffNo; }
    public void setStaffNo(String v)    { this.staffNo = v; }
    public String getAvatarPath()       { return avatarPath; }
    public void setAvatarPath(String v) { this.avatarPath = v; }
}
