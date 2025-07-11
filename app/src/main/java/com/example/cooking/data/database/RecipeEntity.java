package com.example.cooking.data.database;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.example.cooking.domain.entities.Ingredient;
import com.example.cooking.domain.entities.Recipe;
import com.example.cooking.domain.entities.Step;
import com.example.cooking.data.database.converters.DataConverters;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Представляет сущность рецепта для хранения в базе данных Room.
 * Использует {@link TypeConverters} для сохранения списков ингредиентов и шагов.
 * Поддерживает ленивую сериализацию для оптимизации производительности.
 */
@Entity(tableName = "recipes", 
        indices = {
            @androidx.room.Index(value = "mealType", name = "index_mealType"),
            @androidx.room.Index(value = "foodType", name = "index_foodType"),
            @androidx.room.Index(value = "title", name = "index_title")
        })
@TypeConverters(DataConverters.class)
public class RecipeEntity {
    @PrimaryKey
    private int id;
    private String title;        // Название рецепта
    private List<Ingredient> ingredients; // Список ингредиентов
    private List<Step> instructions;   // Список шагов приготовления
    private String created_at;
    private String userId;
    private String mealType;
    private String foodType;
    private String photo_url;
    private boolean isLiked;

    @Ignore
    private String cachedIngredientsJson;
    @Ignore  
    private String cachedInstructionsJson;
    @Ignore
    private boolean ingredientsCacheValid = false;
    @Ignore
    private boolean instructionsCacheValid = false;

    /**
     * Конструктор по умолчанию.
     * Необходим для Room и процессов десериализации.
     */
    public RecipeEntity() {
    }

    /**
     * Создает сущность рецепта на основе доменной модели а.
     */
    public RecipeEntity(Recipe recipe) {
        this.id = recipe.getId();
        this.title = recipe.getTitle();

        this.ingredients = recipe.getIngredients() == null ? new ArrayList<>() : new ArrayList<>(recipe.getIngredients());
        this.instructions = recipe.getSteps() == null ? new ArrayList<>() : new ArrayList<>(recipe.getSteps());
        this.mealType = recipe.getMealType();
        this.foodType = recipe.getFoodType();
        this.created_at = recipe.getCreated_at();
        this.userId = recipe.getUserId();
        this.photo_url = recipe.getPhoto_url();
        this.isLiked = recipe.isLiked();
        // Кэш инициализируется как невалидный
        invalidateCache();
    }

    /**
     * Преобразует текущий {@link RecipeEntity} обратно в доменную модель {@link Recipe}.
     * @return Объект {@link Recipe}.
     */
    public Recipe toRecipe() {
        Recipe recipe = new Recipe();
        recipe.setId(id);
        recipe.setTitle(title);
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

    // --- Методы для ленивой сериализации ---

    /**
     * Получает JSON представление списка ингредиентов с кэшированием.
     * @return JSON строка или null если список пуст
     */
    public String getIngredientsJson() {
        if (!ingredientsCacheValid || cachedIngredientsJson == null) {
            cachedIngredientsJson = DataConverters.fromIngredientList(ingredients);
            ingredientsCacheValid = true;
        }
        return cachedIngredientsJson;
    }

    /**
     * Получает JSON представление списка инструкций с кэшированием.
     * @return JSON строка или null если список пуст
     */
    public String getInstructionsJson() {
        if (!instructionsCacheValid || cachedInstructionsJson == null) {
            cachedInstructionsJson = DataConverters.fromStepList(instructions);
            instructionsCacheValid = true;
        }
        return cachedInstructionsJson;
    }

    /**
     * Инвалидирует весь кэш сериализации.
     */
    private void invalidateCache() {
        invalidateIngredientsCache();
        invalidateInstructionsCache();
    }

    /**
     * Инвалидирует кэш ингредиентов.
     */
    private void invalidateIngredientsCache() {
        ingredientsCacheValid = false;
        cachedIngredientsJson = null;
    }

    /**
     * Инвалидирует кэш инструкций.
     */
    private void invalidateInstructionsCache() {
        instructionsCacheValid = false;
        cachedInstructionsJson = null;
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
        // Инвалидируем кэш при изменении данных
        invalidateIngredientsCache();
    }

    public List<Step> getInstructions() {
        return instructions;
    }

    public void setInstructions(List<Step> instructions) {
        this.instructions = instructions;
        // Инвалидируем кэш при изменении данных
        invalidateInstructionsCache();
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

    // --- Дополнительные утилитные методы ---

    /**
     * Проверяет, валиден ли кэш ингредиентов.
     * @return true если кэш валиден
     */
    @Ignore
    public boolean isIngredientsCacheValid() {
        return ingredientsCacheValid;
    }

    /**
     * Проверяет, валиден ли кэш инструкций.
     * @return true если кэш валиден
     */
    @Ignore
    public boolean isInstructionsCacheValid() {
        return instructionsCacheValid;
    }

    /**
     * Принудительно обновляет кэш сериализации.
     */
    @Ignore
    public void refreshCache() {
        invalidateCache();
        // Заранее загружаем кэш
        getIngredientsJson();
        getInstructionsJson();
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
        return "RecipeEntity{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", isLiked=" + isLiked +
                ", mealType='" + mealType + '\'' +
                ", foodType='" + foodType + '\'' +
                ", cacheValid=[ingredients:" + ingredientsCacheValid + ", instructions:" + instructionsCacheValid + "]" +
                '}';
    }
} 