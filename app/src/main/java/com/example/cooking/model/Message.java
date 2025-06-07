package com.example.cooking.model;

import java.util.List;
import com.example.cooking.Recipe.Recipe;

public class Message {
    public enum MessageType { USER, AI, LOADING, RECIPES }

    private MessageType type;
    private String text;
    private List<Recipe> attachedRecipes;

    // Сообщение пользователя или AI
    public Message(String text, boolean isUser) {
        this.text = text;
        this.type = isUser ? MessageType.USER : MessageType.AI;
    }

    // Индикатор загрузки
    public Message(MessageType type) {
        this.type = type;
    }

    // Сообщение с прикрепленными рецептами
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
