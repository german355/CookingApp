package com.example.cooking.model;

public class Message {
    public enum MessageType { USER, AI, LOADING }

    private MessageType type;
    private String text;

    // Сообщение пользователя или AI
    public Message(String text, boolean isUser) {
        this.text = text;
        this.type = isUser ? MessageType.USER : MessageType.AI;
    }

    // Индикатор загрузки
    public Message(MessageType type) {
        this.type = type;
    }

    public MessageType getType() {
        return type;
    }

    public String getText() {
        return text;
    }

    public boolean isUser() {
        return type == MessageType.USER;
    }
}
