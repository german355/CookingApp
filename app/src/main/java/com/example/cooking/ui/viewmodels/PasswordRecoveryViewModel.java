package com.example.cooking.ui.viewmodels;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import android.util.Patterns;

import com.example.cooking.data.models.ApiResponse;
import com.example.cooking.data.models.PasswordResetRequest;
import com.example.cooking.data.models.PasswordResetResponse;
import com.example.cooking.network.api.ApiService;
import com.example.cooking.network.services.NetworkService;
import com.example.cooking.network.utils.ApiCallHandler;

import retrofit2.Call;

// import com.example.cooking.data.repository.UserRepository; // Пример
// import com.example.cooking.domain.usecase.PasswordRecoveryUseCase; // Пример

public class PasswordRecoveryViewModel extends AndroidViewModel {

    private final MutableLiveData<String> _email = new MutableLiveData<>();
    public LiveData<String> email = _email;

    private final MutableLiveData<RecoveryStatus> _recoveryStatus = new MutableLiveData<>();
    public LiveData<RecoveryStatus> recoveryStatus = _recoveryStatus;

    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>();
    public LiveData<Boolean> isLoading = _isLoading;

    private ApiService apiService;

    // Конструктор
    public PasswordRecoveryViewModel(Application application) {
        super(application);
        // Получаем экземпляр сервиса.
        apiService = NetworkService.getApiService(application.getApplicationContext());
    }

    // public PasswordRecoveryViewModel(AuthApiService apiService) { // Вариант с DI
    //     this.authApiService = apiService;
    // }

    public void onEmailChanged(String newEmail) {
        _email.setValue(newEmail);
    }

    public void requestPasswordRecovery() {
        _isLoading.setValue(true);
        String currentEmail = _email.getValue();

        if (currentEmail == null || currentEmail.trim().isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(currentEmail).matches()) {
            _recoveryStatus.setValue(new RecoveryStatus.Error("Введите корректный email"));
            _isLoading.setValue(false);
            return;
        }

        // Сетевой запрос
        PasswordResetRequest request = new PasswordResetRequest(currentEmail);
        Call<ApiResponse> call = apiService.requestPasswordReset(request);
        
        ApiCallHandler.execute(call, new ApiCallHandler.ApiCallback<ApiResponse>() {
            @Override
            public void onSuccess(ApiResponse response) {
                _isLoading.postValue(false);
                _recoveryStatus.postValue(new RecoveryStatus.Success(response.getMessage()));
            }
            
            @Override
            public void onError(String errorMessage) {
                _isLoading.postValue(false);
                _recoveryStatus.postValue(new RecoveryStatus.Error(errorMessage));
            }
        });
    }

    // Запечатанный класс для состояния восстановления (эквивалент sealed class в Kotlin)
    public static abstract class RecoveryStatus {
        private RecoveryStatus() {}

        public static final class Success extends RecoveryStatus {
            public final String message;
            public Success(String message) {
                this.message = message;
            }
        }

        public static final class Error extends RecoveryStatus {
            public final String errorMessage;
            public Error(String errorMessage) {
                this.errorMessage = errorMessage;
            }
        }
    }
} 