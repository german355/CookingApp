package com.example.cooking.domain.usecases;

import android.app.Application;
import android.util.Log;

import com.example.cooking.utils.MySharedPreferences;
import com.example.cooking.network.services.UserService;

/**
 * Use Case для управления правами доступа пользователя
 * Централизует логику проверки прав на редактирование и управление рецептами
 */
public class UserPermissionUseCase {
    private static final String TAG = "UserPermissionUseCase";
    
    // Константы уровней доступа
    public static final int PERMISSION_USER = 1;    // Обычный пользователь
    public static final int PERMISSION_ADMIN = 2;   // Администратор
    
    private final MySharedPreferences preferences;
    
    public UserPermissionUseCase(Application application) {
        this.preferences = new MySharedPreferences(application);
    }
    
    /**
     * Результат проверки прав доступа
     */
    public static class PermissionResult {
        private final boolean hasPermission;
        private final String reason;
        
        private PermissionResult(boolean hasPermission, String reason) {
            this.hasPermission = hasPermission;
            this.reason = reason;
        }
        
        public boolean hasPermission() {
            return hasPermission;
        }
        
        public String getReason() {
            return reason;
        }
        
        public static PermissionResult allowed() {
            return new PermissionResult(true, null);
        }
        
        public static PermissionResult denied(String reason) {
            return new PermissionResult(false, reason);
        }
    }
    
    /**
     * Проверяет, является ли пользователь автором рецепта
     * @param currentUserId ID текущего пользователя
     * @param recipeOwnerId ID владельца рецепта
     * @return true если пользователь является автором
     */
    public boolean isRecipeOwner(String currentUserId, String recipeOwnerId) {
        if (currentUserId == null || recipeOwnerId == null) {
            Log.d(TAG, "isRecipeOwner: один из ID равен null - currentUserId=" + currentUserId + ", recipeOwnerId=" + recipeOwnerId);
            return false;
        }
        
        boolean isOwner = currentUserId.equals(recipeOwnerId);
        Log.d(TAG, "isRecipeOwner: currentUserId=" + currentUserId + ", recipeOwnerId=" + recipeOwnerId + ", result=" + isOwner);
        return isOwner;
    }
    
    /**
     * Проверяет, имеет ли пользователь права администратора
     * @return true если пользователь администратор
     */
    public boolean isAdmin() {
        int permission = preferences.getUserPermission();
        boolean isAdmin = permission >= PERMISSION_ADMIN;
        Log.d(TAG, "isAdmin: permission=" + permission + ", result=" + isAdmin);
        return isAdmin;
    }
    
    /**
     * Проверяет, авторизован ли пользователь
     * @return true если пользователь авторизован
     */
    public boolean isUserLoggedIn() {
        boolean isLoggedIn = UserService.isUserLoggedIn();
        Log.d(TAG, "isUserLoggedIn: result=" + isLoggedIn);
        return isLoggedIn;
    }
    
    /**
     * Получает ID текущего пользователя
     * @return ID текущего пользователя или null если не авторизован
     */
    public String getCurrentUserId() {
        String userId = preferences.getUserId();
        Log.d(TAG, "getCurrentUserId: result=" + userId);
        return userId;
    }
    
    /**
     * Получает уровень прав текущего пользователя
     * @return уровень прав (1 - пользователь, 2 - администратор)
     */
    public int getCurrentUserPermission() {
        int permission = preferences.getUserPermission();
        Log.d(TAG, "getCurrentUserPermission: result=" + permission);
        return permission;
    }
    
    /**
     * Проверяет, может ли пользователь редактировать рецепт
     * @param recipeOwnerId ID владельца рецепта
     * @return PermissionResult с результатом проверки
     */
    public PermissionResult canEditRecipe(String recipeOwnerId) {
        // Проверяем авторизацию
        if (!isUserLoggedIn()) {
            return PermissionResult.denied("Пользователь не авторизован");
        }
        
        String currentUserId = getCurrentUserId();
        
        // Проверяем валидность ID пользователя
        if (currentUserId == null || currentUserId.equals("0")) {
            return PermissionResult.denied("Некорректный ID пользователя");
        }
        
        // Проверяем права администратора
        if (isAdmin()) {
            Log.d(TAG, "canEditRecipe: доступ разрешен - пользователь администратор");
            return PermissionResult.allowed();
        }
        
        // Проверяем, является ли пользователь автором рецепта
        if (isRecipeOwner(currentUserId, recipeOwnerId)) {
            Log.d(TAG, "canEditRecipe: доступ разрешен - пользователь автор рецепта");
            return PermissionResult.allowed();
        }
        
        Log.d(TAG, "canEditRecipe: доступ запрещен - пользователь не автор и не администратор");
        return PermissionResult.denied("Вы не являетесь автором этого рецепта или модератором");
    }
    
    /**
     * Проверяет, может ли пользователь удалить рецепт
     * @param recipeOwnerId ID владельца рецепта
     * @return PermissionResult с результатом проверки
     */
    public PermissionResult canDeleteRecipe(String recipeOwnerId) {
        // Проверяем авторизацию
        if (!isUserLoggedIn()) {
            return PermissionResult.denied("Пользователь не авторизован");
        }
        
        String currentUserId = getCurrentUserId();
        
        // Проверяем валидность ID пользователя
        if (currentUserId == null || currentUserId.equals("0")) {
            return PermissionResult.denied("Некорректный ID пользователя");
        }
        
        // Проверяем права администратора
        if (isAdmin()) {
            Log.d(TAG, "canDeleteRecipe: доступ разрешен - пользователь администратор");
            return PermissionResult.allowed();
        }
        
        // Проверяем, является ли пользователь автором рецепта
        if (isRecipeOwner(currentUserId, recipeOwnerId)) {
            Log.d(TAG, "canDeleteRecipe: доступ разрешен - пользователь автор рецепта");
            return PermissionResult.allowed();
        }
        
        Log.d(TAG, "canDeleteRecipe: доступ запрещен - пользователь не автор и не администратор");
        return PermissionResult.denied("Вы не являетесь автором этого рецепта или модератором");
    }

    
    /**
     * Получает подробную информацию о правах пользователя для отладки
     * @return строка с информацией о правах
     */
    public String getUserPermissionInfo() {
        String currentUserId = getCurrentUserId();
        int permission = getCurrentUserPermission();
        boolean isLoggedIn = isUserLoggedIn();
        boolean isAdmin = isAdmin();
        
        return String.format("UserPermission [userId=%s, permission=%d, isLoggedIn=%b, isAdmin=%b]", 
                            currentUserId, permission, isLoggedIn, isAdmin);
    }
} 