package com.example.cooking.domain.entities;

import java.util.List;

public class Message {
    public enum MessageType { USER, AI, LOADING, RECIPES, RECIPE_FLOW_LOADING, CREATED_RECIPE }

    private MessageType type;
    private String text;
    private String secondaryText;
    private List<Recipe> attachedRecipes;
    private Recipe attachedRecipe;

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

    public Message(Recipe recipe, String secondaryText) {
        this.attachedRecipe = recipe;
        this.secondaryText = secondaryText;
        this.type = MessageType.CREATED_RECIPE;
    }

    public static Message recipeFlowLoading(String title, String secondaryText) {
        Message message = new Message(MessageType.RECIPE_FLOW_LOADING);
        message.text = title;
        message.secondaryText = secondaryText;
        return message;
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

    public Recipe getAttachedRecipe() {
        return attachedRecipe;
    }

    public String getSecondaryText() {
        return secondaryText;
    }
}
