package com.example.cooking.domain.entities;

import java.util.List;
import java.util.UUID;

public class Message {
    public enum MessageType { USER, AI, LOADING, RECIPES, RECIPE_FLOW_LOADING, CREATED_RECIPE }

    private final String id;
    private MessageType type;
    private String text;
    private String secondaryText;
    private List<Recipe> attachedRecipes;
    private Recipe attachedRecipe;

    public Message(String text, boolean isUser) {
        this.id = UUID.randomUUID().toString();
        this.text = text;
        this.type = isUser ? MessageType.USER : MessageType.AI;
    }

    public Message(MessageType type) {
        this.id = UUID.randomUUID().toString();
        this.type = type;
    }

    public Message(List<Recipe> recipes) {
        this.id = UUID.randomUUID().toString();
        this.attachedRecipes = recipes;
        this.type = MessageType.RECIPES;
    }

    public Message(Recipe recipe, String secondaryText) {
        this.id = UUID.randomUUID().toString();
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

    public String getId() {
        return id;
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
