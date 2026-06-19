package com.example.matchandplay;

public class User {
    private String uid;
    private String name;
    private String age;
    private String position;
    private String profileImageUrl;

    // NEW: The algorithmic score (0 to 100)
    private int matchScore;

    // Constructor required for Firebase
    public User() {
    }

    // Updated Constructor with Image URL and Match Score initialization
    public User(String uid, String name, String age, String position, String profileImageUrl) {
        this.uid = uid;
        this.name = name;
        this.age = age;
        this.position = position;
        this.profileImageUrl = profileImageUrl;
        this.matchScore = 0; // Default score is 0 when a card is first created
    }

    // --- GETTERS ---
    public String getUid() {
        return uid;
    }

    public String getName() {
        return name;
    }

    public String getAge() {
        return age;
    }

    public String getPosition() {
        return position;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    // NEW: Getter for Match Score
    public int getMatchScore() {
        return matchScore;
    }

    // --- SETTERS ---
    public void setUid(String uid) {
        this.uid = uid;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setAge(String age) {
        this.age = age;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    // NEW: Setter for Match Score
    public void setMatchScore(int matchScore) {
        this.matchScore = matchScore;
    }
}