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

import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

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

    private final CompositeDisposable disposables = new CompositeDisposable();

    // Конструктор
    public PasswordRecoveryViewModel(Application application) {
        super(application);
        // Получаем экземпляр сервиса.
        apiService = NetworkService.getApiService(application.getApplicationContext());
    }


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

        // Сетевой запрос через RxJava
        disposables.add(
            apiService.requestPasswordReset(new PasswordResetRequest(currentEmail))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    response -> {
                        _isLoading.postValue(false);
                        _recoveryStatus.postValue(new RecoveryStatus.Success(response.getMessage()));
                    },
                    throwable -> {
                        _isLoading.postValue(false);
                        _recoveryStatus.postValue(new RecoveryStatus.Error(throwable.getMessage()));
                    }
                )
        );
    }

    @Override
    protected void onCleared() {
        disposables.clear();
        super.onCleared();
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