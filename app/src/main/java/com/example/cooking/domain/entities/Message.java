package com.example.cooking.domain.entities;

import java.util.List;

public class Message {
    public enum MessageType { USER, AI, LOADING, RECIPES }

    private MessageType type;
    private String text;
    private List<Recipe> attachedRecipes;

    public Message(String text, boolean isUser) {
        this.text = text;
        this.type = isUser ? MessageType.USER : MessageType.AI;
    }

    public Message(MessageType type) {
        this.type = type;
    }

    public Message(List<Recipe> recipes) {
        this.attachedRecipes = recipes;
        this.type = MessageType.RECIPES;
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

    public List<Recipe> getAttachedRecipes() {
        return attachedRecipes;
    }
}
