package com.example.cooking.data.database;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.example.cooking.Recipe.Ingredient;
import com.example.cooking.Recipe.Recipe;
import com.example.cooking.Recipe.Step;
import com.example.cooking.data.database.converters.DataConverters;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Представляет сущность рецепта для хранения в базе данных Room.
 * Использует {@link TypeConverters} для сохранения списков ингредиентов и шагов.
 */
@Entity(tableName = "recipes")
@TypeConverters(DataConverters.class)
public class RecipeEntity {
    @PrimaryKey // Первичный ключ для таблицы recipes
    private int id;
    private String title;        // Название рецепта
    private List<Ingredient> ingredients; // Список ингредиентов
    private List<Step> instructions;   // Список шагов приготовления
    private String created_at;   // Дата создания (или последнего обновления) рецепта
    private String userId;       // Идентификатор пользователя, создавшего рецепт (если применимо)
    private String mealType;     // Тип приема пищи (напр., Завтрак, Обед)
    private String foodType;     // Тип блюда (напр., Суп, Салат)
    private String photo_url;    // URL основного изображения рецепта
    private boolean isLiked;     // Статус "лайк" от пользователя для данного рецепта

    /**
     * Конструктор по умолчанию.
     * Необходим для Room и процессов десериализации.
     */
    public RecipeEntity() {
    }

    /**
     * Создает {@link RecipeEntity} на основе доменной модели {@link Recipe}.
     * @param recipe Доменный объект рецепта.
     */
    public RecipeEntity(Recipe recipe) {
        this.id = recipe.getId();
        this.title = recipe.getTitle();
        // Копируем списки, чтобы избежать модификации оригинальных списков в объекте Recipe
        this.ingredients = recipe.getIngredients() == null ? new ArrayList<>() : new ArrayList<>(recipe.getIngredients());
        this.instructions = recipe.getSteps() == null ? new ArrayList<>() : new ArrayList<>(recipe.getSteps());
        this.mealType = recipe.getMealType();
        this.foodType = recipe.getFoodType();
        this.created_at = recipe.getCreated_at();
        this.userId = recipe.getUserId();
        this.photo_url = recipe.getPhoto_url();
        this.isLiked = recipe.isLiked();
    }

    /**
     * Преобразует текущий {@link RecipeEntity} обратно в доменную модель {@link Recipe}.
     * @return Объект {@link Recipe}.
     */
    public Recipe toRecipe() {
        Recipe recipe = new Recipe();
        recipe.setId(id);
        recipe.setTitle(title);
        // Создаем новые копии списков для объекта Recipe
        recipe.setIngredients(ingredients == null ? new ArrayList<>() : new ArrayList<>(ingredients));
        recipe.setSteps(instructions == null ? new ArrayList<>() : new ArrayList<>(instructions));
        recipe.setMealType(mealType);
        recipe.setFoodType(foodType);
        recipe.setCreated_at(created_at);
        recipe.setUserId(userId);
        recipe.setPhoto_url(photo_url);
        recipe.setLiked(isLiked);
        return recipe;
    }

    // Геттеры и сеттеры
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<Ingredient> getIngredients() {
        return ingredients;
    }

    public void setIngredients(List<Ingredient> ingredients) {
        this.ingredients = ingredients;
    }

    public List<Step> getInstructions() {
        return instructions;
    }

    public void setInstructions(List<Step> instructions) {
        this.instructions = instructions;
    }

    public String getMealType() {
        return mealType;
    }

    public void setMealType(String mealType) {
        this.mealType = mealType;
    }
    public String getFoodType() {
        return foodType;
    }

    public void setFoodType(String foodType) {
        this.foodType = foodType;
    }
    public String getCreated_at() {
        return created_at;
    }

    public void setCreated_at(String created_at) {
        this.created_at = created_at;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getPhoto_url() {
        return photo_url;
    }

    public void setPhoto_url(String photo_url) {
        this.photo_url = photo_url;
    }

    public boolean isLiked() {
        return isLiked;
    }

    public void setLiked(boolean liked) {
        isLiked = liked;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RecipeEntity that = (RecipeEntity) o;
        return id == that.id &&
               isLiked == that.isLiked &&
               Objects.equals(title, that.title) &&
               Objects.equals(ingredients, that.ingredients) &&
               Objects.equals(instructions, that.instructions) &&
               Objects.equals(created_at, that.created_at) &&
               Objects.equals(userId, that.userId) &&
               Objects.equals(mealType, that.mealType) &&
               Objects.equals(foodType, that.foodType) &&
               Objects.equals(photo_url, that.photo_url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, ingredients, instructions, created_at, userId, mealType, foodType, photo_url, isLiked);
    }

    @NonNull
    @Override
    public String toString() {
        // Более краткий toString для лучшей читаемости логов, можно добавить больше полей при необходимости
        return "RecipeEntity{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", isLiked=" + isLiked +
                ", mealType='" + mealType + '\'' +
                ", foodType='" + foodType + '\'' +
                '}';
    }
} 