package com.example.cooking.ui.viewmodels.Recipe;

import android.net.Uri;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.cooking.domain.entities.Ingredient;
import com.example.cooking.domain.entities.Step;

import java.util.ArrayList;
import java.util.List;

/**
 * Класс для управления состоянием экрана редактирования рецепта.
 * Консолидирует все LiveData поля для упрощения EditRecipeViewModel.
 */
public class EditRecipeState {
    
    // === UI STATE ===
    private final MutableLiveData<Boolean> _isSaving = new MutableLiveData<>(false);
    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> _saveResult = new MutableLiveData<>(null);
    
    // === RECIPE DATA ===
    private final MutableLiveData<Integer> _recipeId = new MutableLiveData<>();
    private final MutableLiveData<String> _recipeOwnerId = new MutableLiveData<>();
    private final MutableLiveData<String> _title = new MutableLiveData<>("");
    private final MutableLiveData<List<Ingredient>> _ingredients = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Step>> _steps = new MutableLiveData<>(new ArrayList<>());
    
    // === IMAGE STATE ===
    private final MutableLiveData<String> _photoUrl = new MutableLiveData<>(null);
    private final MutableLiveData<byte[]> _imageBytes = new MutableLiveData<>(null);
    private boolean imageChanged = false;
    private Uri selectedImageUri = null;
    
    // === PUBLIC GETTERS (LIVE DATA) ===
    
    // UI State
    public LiveData<Boolean> getIsSaving() { return _isSaving; }
    public LiveData<String> getErrorMessage() { return _errorMessage; }
    public LiveData<Boolean> getSaveResult() { return _saveResult; }
    
    // Recipe Data
    public LiveData<Integer> getRecipeId() { return _recipeId; }
    public LiveData<String> getRecipeOwnerId() { return _recipeOwnerId; }
    public LiveData<String> getTitle() { return _title; }
    public LiveData<List<Ingredient>> getIngredients() { return _ingredients; }
    public LiveData<List<Step>> getSteps() { return _steps; }
    
    // Image State
    public LiveData<String> getPhotoUrl() { return _photoUrl; }
    public LiveData<byte[]> getImageBytes() { return _imageBytes; }
    
    // === SETTERS (MUTABLE ACCESS) ===
    
    // UI State
    public void setIsSaving(boolean isSaving) { _isSaving.setValue(isSaving); }
    public void setErrorMessage(String errorMessage) { _errorMessage.setValue(errorMessage); }
    public void setSaveResult(Boolean saveResult) { _saveResult.setValue(saveResult); }
    public void clearErrorMessage() { _errorMessage.setValue(null); }
    public void clearSaveResult() { _saveResult.setValue(null); }
    
    // Recipe Data
    public void setRecipeId(Integer recipeId) { _recipeId.setValue(recipeId); }
    public void setRecipeOwnerId(String recipeOwnerId) { _recipeOwnerId.setValue(recipeOwnerId); }
    public void setTitle(String title) { _title.setValue(title); }
    public void setIngredients(List<Ingredient> ingredients) { _ingredients.setValue(ingredients); }
    public void setSteps(List<Step> steps) { _steps.setValue(steps); }
    
    // Image State
    public void setPhotoUrl(String photoUrl) { _photoUrl.setValue(photoUrl); }
    public void setImageBytes(byte[] imageBytes) { _imageBytes.setValue(imageBytes); }
    public void setImageChanged(boolean imageChanged) { this.imageChanged = imageChanged; }
    public void setSelectedImageUri(Uri selectedImageUri) { this.selectedImageUri = selectedImageUri; }
    
    // === VALUE GETTERS (CURRENT VALUES) ===
    
    // UI State
    public Boolean getIsSavingValue() { return _isSaving.getValue(); }
    public String getErrorMessageValue() { return _errorMessage.getValue(); }
    public Boolean getSaveResultValue() { return _saveResult.getValue(); }
    
    // Recipe Data
    public Integer getRecipeIdValue() { return _recipeId.getValue(); }
    public String getRecipeOwnerIdValue() { return _recipeOwnerId.getValue(); }
    public String getTitleValue() { return _title.getValue(); }
    public List<Ingredient> getIngredientsValue() { return _ingredients.getValue(); }
    public List<Step> getStepsValue() { return _steps.getValue(); }
    
    // Image State
    public String getPhotoUrlValue() { return _photoUrl.getValue(); }
    public byte[] getImageBytesValue() { return _imageBytes.getValue(); }
    public boolean isImageChanged() { return imageChanged; }
    public Uri getSelectedImageUri() { return selectedImageUri; }
    
    // === CONVENIENCE METHODS ===
    

    /**
     * Возвращает количество ингредиентов
     */
    public int getIngredientsCount() {
        List<Ingredient> ingredients = _ingredients.getValue();
        return ingredients != null ? ingredients.size() : 0;
    }
    
    /**
     * Возвращает количество шагов
     */
    public int getStepsCount() {
        List<Step> steps = _steps.getValue();
        return steps != null ? steps.size() : 0;
    }

    
    /**
     * Сбрасывает UI состояние
     */
    public void resetUIState() {
        _isSaving.setValue(false);
        _errorMessage.setValue(null);
        _saveResult.setValue(null);
    }
} 