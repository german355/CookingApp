package com.example.cooking.domain.usecases;

import android.app.Application;
import android.util.Log;
import android.widget.Toast;
import androidx.lifecycle.MutableLiveData;
import com.example.cooking.data.repositories.UnifiedRecipeRepository;

import java.util.Set;

/**
 * Use Case для операций с лайками рецептов
 * Отвечает за установку, переключение и получение статуса лайков
 */
public class RecipeLikeUseCase {
    private static final String TAG = "RecipeLikeUseCase";
    
    private final UnifiedRecipeRepository repository;
    private final Application application;
    private final android.net.ConnectivityManager connectivityManager;
    
    public RecipeLikeUseCase(Application application) {
        this.application = application;
        this.repository = UnifiedRecipeRepository.getInstance(application);
        this.connectivityManager = (android.net.ConnectivityManager) 
            application.getSystemService(android.content.Context.CONNECTIVITY_SERVICE);
    }
    
    /**
     * Устанавливает статус лайка для рецепта
     */
    public void setLikeStatus(String userId, int recipeId, boolean newLikeStatus, 
                             MutableLiveData<String> errorMessageLiveData) {
        Log.d(TAG, "setLikeStatus called: id=" + recipeId + " liked=" + newLikeStatus + " networkAvailable=" + isNetworkAvailable());
        
        // Проверяем доступность сети перед установкой статуса лайка
        if (!isNetworkAvailable()) {
            String errorMsg = "Вы в офлайн режиме и не можете ставить лайки";
            if (errorMessageLiveData != null) {
                errorMessageLiveData.setValue(errorMsg);
            }
            Toast.makeText(application, errorMsg, Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (userId == null || userId.equals("0") || userId.isEmpty()) {
            String errorMsg = "Войдите, чтобы установить статус лайка";
            if (errorMessageLiveData != null) {
                errorMessageLiveData.setValue(errorMsg);
            }
            return;
        }
        
        repository.setLikeStatus(recipeId, newLikeStatus);
    }
    
    /**
     * Переключает статус лайка рецепта
     */
    public void toggleLike(int recipeId) {
        // Логика переключения теперь полностью в setLikeStatus
    }
    
    /**
     * Получает список ID лайкнутых рецептов
     */
    public Set<Integer> getLikedRecipeIds() {
        return repository.getLikedRecipeIds();
    }
    
    /**
     * Проверяет доступность сети
     */
    public boolean isNetworkAvailable() {
        if (connectivityManager != null) {
            android.net.NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        }
        return false;
    }
    
    /**
     * Очищает ресурсы
     */
    public void clearResources() {
        repository.clearDisposables();
    }
} 