package com.example.cooking.domain.usecases.Authicated;

import android.app.Application;
import androidx.lifecycle.MutableLiveData;
import android.content.Context;

import com.example.cooking.data.models.PasswordResetRequest;
import com.example.cooking.network.api.ApiService;
import com.example.cooking.network.services.NetworkService;
import com.example.cooking.ui.viewmodels.profile.PasswordRecoveryViewModel.RecoveryStatus;

import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;

/**
 * UseCase для восстановления пароля
 */
public class PasswordRecoveryUseCase {
    private final ApiService apiService;
    private final CompositeDisposable disposables = new CompositeDisposable();

    public PasswordRecoveryUseCase(Application application) {
        Context ctx = application.getApplicationContext();
        apiService = NetworkService.getApiService(ctx);
    }

    public void requestPasswordRecovery(String email,
            MutableLiveData<Boolean> isLoading,
            MutableLiveData<RecoveryStatus> recoveryStatus) {
        isLoading.postValue(true);
        disposables.add(
            apiService.requestPasswordReset(new PasswordResetRequest(email))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    response -> {
                        isLoading.postValue(false);
                        recoveryStatus.postValue(new RecoveryStatus.Success(response.getMessage()));
                    },
                    throwable -> {
                        isLoading.postValue(false);
                        recoveryStatus.postValue(new RecoveryStatus.Error(throwable.getMessage()));
                    }
                )
        );
    }

    public void clear() {
        disposables.clear();
    }
}
