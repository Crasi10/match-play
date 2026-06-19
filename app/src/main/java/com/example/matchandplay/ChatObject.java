package com.example.matchandplay;

public class ChatObject {
    private String message;
    private String currentUserId; // Who sent this message?

    // Required empty constructor for Firebase
    public ChatObject() {
    }

    public ChatObject(String message, String currentUserId) {
        this.message = message;
        this.currentUserId = currentUserId;
    }

    public String getMessage() { return message; }
    public String getCurrentUserId() { return currentUserId; }
}