package com.example.cooking.ui.viewmodels;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;

import com.example.cooking.R;
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
    // Событие входа
    private final MutableLiveData<Void> loginEvent = new MutableLiveData<>();
    // Событие выхода
    private final MutableLiveData<Void> logoutEvent = new MutableLiveData<>();
    // Событие поиска
    private final MutableLiveData<String> searchQueryEvent = new MutableLiveData<>();
    // Событие добавления рецепта
    private final MutableLiveData<Void> recipeAddedEvent = new MutableLiveData<>();
    // Состояние раскрытого меню FAB
    private final MutableLiveData<Boolean> isFabMenuExpanded = new MutableLiveData<>(false);

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
        // Инициализация Google Sign In перенесена во ViewModel
        initGoogleSignIn(getApplication().getString(R.string.default_web_client_id));
    }

    /**
     * Проверяет состояние авторизации пользователя
     */
    public void checkAuthState() {
        // Используем проверку FirebaseAuth вместо SharedPreferences
        isUserLoggedIn.setValue(authManager.isUserSignedIn());
    }

    /**
     * Возвращает LiveData с состоянием авторизации
     */
    public LiveData<Boolean> getIsUserLoggedIn() {
        return isUserLoggedIn;
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
            errorMessage.setValue(getApplication().getString(R.string.main_view_model_google_signin_error));
        }
    }

    public LiveData<Void> getLoginEvent() {
        return loginEvent;
    }

    public void triggerLoginEvent() {
        // Событие успешного входа и обновление статуса авторизации
        loginEvent.setValue(null);
        isUserLoggedIn.setValue(true);
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



    // Получить статус меню FAB
    public LiveData<Boolean> getIsFabMenuExpanded() {
        return isFabMenuExpanded;
    }

    // Переключить состояние меню FAB
    public void toggleFabMenu() {
        Boolean current = isFabMenuExpanded.getValue();
        isFabMenuExpanded.setValue(current == null || !current);
    }
}