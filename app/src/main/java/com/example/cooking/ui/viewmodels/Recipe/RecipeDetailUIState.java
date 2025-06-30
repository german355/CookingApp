package com.example.cooking.ui.viewmodels.Recipe;

import com.example.cooking.domain.entities.Recipe;
import com.example.cooking.domain.entities.Ingredient;
import com.example.cooking.domain.entities.Step;

import java.util.List;
import java.util.Objects;

/**
 * Единое состояние UI для экрана детального просмотра рецепта
 * Объединяет все данные и состояния в один immutable объект
 */
public class RecipeDetailUIState {
    
    // Основные данные
    private final Recipe recipe;
    private final boolean isLiked;
    private final int portionCount;
    
    // Состояния UI
    private final boolean isLoading;
    private final String errorMessage;
    private final boolean deleteSuccess;
    
    // Права доступа
    private final boolean canEdit;
    private final boolean canDelete;
    
    // Состояние по умолчанию
    public static final RecipeDetailUIState INITIAL = new RecipeDetailUIState(
        null, false, 1, false, null, false, false, false
    );
    
    private RecipeDetailUIState(Recipe recipe, boolean isLiked, int portionCount,
                               boolean isLoading, String errorMessage, boolean deleteSuccess,
                               boolean canEdit, boolean canDelete) {
        this.recipe = recipe;
        this.isLiked = isLiked;
        this.portionCount = portionCount;
        this.isLoading = isLoading;
        this.errorMessage = errorMessage;
        this.deleteSuccess = deleteSuccess;
        this.canEdit = canEdit;
        this.canDelete = canDelete;
    }
    
    // Геттеры
    public Recipe getRecipe() { return recipe; }
    public boolean isLiked() { return isLiked; }
    public int getPortionCount() { return portionCount; }
    public boolean isLoading() { return isLoading; }
    public String getErrorMessage() { return errorMessage; }
    public boolean isDeleteSuccess() { return deleteSuccess; }
    public boolean canEdit() { return canEdit; }
    public boolean canDelete() { return canDelete; }
    
    // Convenience методы
    public boolean hasError() {
        return errorMessage != null && !errorMessage.isEmpty();
    }
    
    public boolean hasRecipe() {
        return recipe != null;
    }
    
    public List<Ingredient> getIngredients() {
        return recipe != null ? recipe.getIngredients() : null;
    }
    
    public List<Step> getSteps() {
        return recipe != null ? recipe.getSteps() : null;
    }
    
    public String getRecipeTitle() {
        return recipe != null ? recipe.getTitle() : "Загрузка...";
    }
    
    public String getRecipeImageUrl() {
        return recipe != null ? recipe.getPhoto_url() : null;
    }
    
    // Factory методы для создания новых состояний
    public static RecipeDetailUIState loading() {
        return INITIAL.withLoading(true);
    }
    
    public static RecipeDetailUIState withRecipe(Recipe recipe, boolean canEdit, boolean canDelete) {
        return new RecipeDetailUIState(
            recipe, 
            recipe != null ? recipe.isLiked() : false,
            1, // default portion count
            false, // not loading
            null, // no error
            false, // not deleted
            canEdit,
            canDelete
        );
    }
    
    public static RecipeDetailUIState error(String errorMessage) {
        return INITIAL.withError(errorMessage);
    }
    
    // Методы для создания модифицированных копий (immutable pattern)
    public RecipeDetailUIState withRecipe(Recipe recipe) {
        return new RecipeDetailUIState(recipe, recipe != null ? recipe.isLiked() : this.isLiked, 
            portionCount, isLoading, errorMessage, deleteSuccess, canEdit, canDelete);
    }
    
    public RecipeDetailUIState withLikedStatus(boolean isLiked) {
        return new RecipeDetailUIState(recipe, isLiked, portionCount, isLoading, 
            errorMessage, deleteSuccess, canEdit, canDelete);
    }
    
    public RecipeDetailUIState withPortionCount(int portionCount) {
        return new RecipeDetailUIState(recipe, isLiked, portionCount, isLoading, 
            errorMessage, deleteSuccess, canEdit, canDelete);
    }
    
    public RecipeDetailUIState withLoading(boolean isLoading) {
        return new RecipeDetailUIState(recipe, isLiked, portionCount, isLoading, 
            errorMessage, deleteSuccess, canEdit, canDelete);
    }
    
    public RecipeDetailUIState withError(String errorMessage) {
        return new RecipeDetailUIState(recipe, isLiked, portionCount, false, 
            errorMessage, deleteSuccess, canEdit, canDelete);
    }
    
    public RecipeDetailUIState withoutError() {
        return new RecipeDetailUIState(recipe, isLiked, portionCount, isLoading, 
            null, deleteSuccess, canEdit, canDelete);
    }
    
    public RecipeDetailUIState withDeleteSuccess(boolean deleteSuccess) {
        return new RecipeDetailUIState(recipe, isLiked, portionCount, isLoading, 
            errorMessage, deleteSuccess, canEdit, canDelete);
    }
    
    public RecipeDetailUIState withPermissions(boolean canEdit, boolean canDelete) {
        return new RecipeDetailUIState(recipe, isLiked, portionCount, isLoading, 
            errorMessage, deleteSuccess, canEdit, canDelete);
    }
    
    // Методы для удобства управления порциями
    public RecipeDetailUIState incrementPortion() {
        return withPortionCount(portionCount + 1);
    }
    
    public RecipeDetailUIState decrementPortion() {
        return portionCount > 1 ? withPortionCount(portionCount - 1) : this;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RecipeDetailUIState that = (RecipeDetailUIState) o;
        return isLiked == that.isLiked &&
               portionCount == that.portionCount &&
               isLoading == that.isLoading &&
               deleteSuccess == that.deleteSuccess &&
               canEdit == that.canEdit &&
               canDelete == that.canDelete &&
               Objects.equals(recipe, that.recipe) &&
               Objects.equals(errorMessage, that.errorMessage);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(recipe, isLiked, portionCount, isLoading, errorMessage, 
                          deleteSuccess, canEdit, canDelete);
    }
    
    @Override
    public String toString() {
        return "RecipeDetailUIState{" +
                "recipeId=" + (recipe != null ? recipe.getId() : "null") +
                ", isLiked=" + isLiked +
                ", portionCount=" + portionCount +
                ", isLoading=" + isLoading +
                ", hasError=" + hasError() +
                ", canEdit=" + canEdit +
                ", canDelete=" + canDelete +
                '}';
    }
} 