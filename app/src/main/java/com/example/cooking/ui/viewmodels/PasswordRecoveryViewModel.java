package com.example.cooking.ui.viewmodels;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import android.util.Patterns;

import com.example.cooking.domain.usecases.PasswordRecoveryUseCase;
import com.example.cooking.R;

public class PasswordRecoveryViewModel extends AndroidViewModel {

    private final MutableLiveData<String> _email = new MutableLiveData<>();
    public LiveData<String> email = _email;

    private final MutableLiveData<RecoveryStatus> _recoveryStatus = new MutableLiveData<>();
    public LiveData<RecoveryStatus> recoveryStatus = _recoveryStatus;

    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>();
    public LiveData<Boolean> isLoading = _isLoading;

    private final PasswordRecoveryUseCase recoveryUseCase;

    // Конструктор
    public PasswordRecoveryViewModel(Application application) {
        super(application);
        recoveryUseCase = new PasswordRecoveryUseCase(application);
    }


    public void onEmailChanged(String newEmail) {
        _email.setValue(newEmail);
    }

    public void requestPasswordRecovery() {
        String currentEmail = _email.getValue();
        if (currentEmail == null || currentEmail.trim().isEmpty() ||
            !Patterns.EMAIL_ADDRESS.matcher(currentEmail).matches()) {
            _recoveryStatus.setValue(new RecoveryStatus.Error(getApplication().getString(R.string.password_recovery_enter_correct_email)));
            _isLoading.setValue(false);
            return;
        }
        recoveryUseCase.requestPasswordRecovery(currentEmail, _isLoading, _recoveryStatus);
    }

    @Override
    protected void onCleared() {
        recoveryUseCase.clear();
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