package com.example.cooking.ui.viewmodels;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.cooking.auth.FirebaseAuthManager;
import com.example.cooking.utils.MySharedPreferences;

/**
 * ViewModel для MainActivity, управляет навигацией и общим состоянием
 * приложения
 */
public class MainViewModel extends AndroidViewModel {
    private static final String TAG = "MainViewModel";

    // Зависимости
    private final FirebaseAuthManager authManager;
    private final MySharedPreferences preferences;

    // Состояния UI
    private final MutableLiveData<Boolean> isUserLoggedIn = new MutableLiveData<>(false);
    private final MutableLiveData<Integer> selectedNavigationItem = new MutableLiveData<>();
    private final MutableLiveData<Boolean> showAddButton = new MutableLiveData<>(true);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Void> logoutEvent = new MutableLiveData<>();

    /**
     * Создает новый MainViewModel
     * 
     * @param application Контекст приложения
     */
    public MainViewModel(@NonNull Application application) {
        super(application);
        authManager = FirebaseAuthManager.getInstance();
        preferences = new MySharedPreferences(application);

        // Инициализируем начальное состояние
        checkAuthState();
    }

    /**
     * Проверяет состояние авторизации пользователя
     */
    public void checkAuthState() {
        String userId = preferences.getString("userId", "0");
        isUserLoggedIn.setValue(!userId.equals("0"));
    }

    /**
     * Возвращает LiveData с состоянием авторизации
     */
    public LiveData<Boolean> getIsUserLoggedIn() {
        return isUserLoggedIn;
    }

    /**
     * Возвращает LiveData с выбранным пунктом навигации
     */
    public LiveData<Integer> getSelectedNavigationItem() {
        return selectedNavigationItem;
    }

    /**
     * Устанавливает выбранный пункт навигации
     */
    public void setSelectedNavigationItem(int itemId) {
        selectedNavigationItem.setValue(itemId);

        // Обновляем видимость кнопки добавления в зависимости от выбранного пункта
        updateAddButtonVisibility(itemId);
    }

    /**
     * Обновляет видимость кнопки добавления рецепта
     */
    private void updateAddButtonVisibility(int itemId) {
        // Показываем кнопку только на главном экране
        showAddButton.setValue(itemId == com.example.cooking.R.id.nav_home);
    }

    /**
     * Устанавливает видимость кнопки добавления
     * 
     * @param show true - показать, false - скрыть
     */
    public void setShowAddButton(boolean show) {
        showAddButton.setValue(show);
    }

    /**
     * Возвращает LiveData с видимостью кнопки добавления
     */
    public LiveData<Boolean> getShowAddButton() {
        return showAddButton;
    }

    /**
     * Возвращает LiveData с сообщением об ошибке
     */
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    /**
     * Проверяет, авторизован ли пользователь
     * 
     * @return true, если пользователь авторизован
     */
    public boolean isUserLoggedIn() {
        Boolean value = isUserLoggedIn.getValue();
        return value != null && value;
    }

    /**
     * Инициализирует Google Sign In
     * 
     * @param webClientId ID клиента для Google Sign In
     */
    public void initGoogleSignIn(String webClientId) {
        try {
            authManager.initGoogleSignIn(getApplication(), webClientId);
        } catch (Exception e) {
            Log.e(TAG, "Ошибка инициализации Google Sign In", e);
            errorMessage.setValue("Ошибка инициализации входа через Google");
        }
    }

    /**
     * Обрабатывает результат активности
     * 
     * @param requestCode Код запроса
     * @param resultCode  Код результата
     */
    public void handleActivityResult(int requestCode, int resultCode) {
        // Обновляем состояние авторизации после возвращения из других активностей
        checkAuthState();
    }

    public LiveData<Void> getLogoutEvent() {
        return logoutEvent;
    }

    public void triggerLogoutEvent() {
        Log.d(TAG, "Событие выхода инициировано");
        // Убеждаемся, что статус авторизации соответствует выходу пользователя
        isUserLoggedIn.postValue(false);
        // Используем postValue для безопасности потоков
        logoutEvent.postValue(null);
    }
}