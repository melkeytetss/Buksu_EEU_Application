package com.example.buksu_eeu;

public class NotificationModel {
    private String id;
    private String recipientId; // "admin" or the specific userId
    private String title;
    private String message;
    private long timestamp;
    private boolean read;

    public NotificationModel() {}

    public NotificationModel(String recipientId, String title, String message) {
        this.recipientId = recipientId;
        this.title = title;
        this.message = message;
        this.timestamp = System.currentTimeMillis();
        this.read = false;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getRecipientId() { return recipientId; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public long getTimestamp() { return timestamp; }
    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }
}