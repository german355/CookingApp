package com.example.cooking.data.models;

public class ChatMessageNoUsageNow {
    private String id;
    private String text;
    private String sender;
    private long timestamp;

    public ChatMessageNoUsageNow() {}

    public ChatMessageNoUsageNow(String id, String text, String sender, long timestamp) {
        this.id = id;
        this.text = text;
        this.sender = sender;
        this.timestamp = timestamp;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
