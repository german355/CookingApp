package com.example.cooking.ui.activities;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.example.cooking.R;
import com.example.cooking.ui.viewmodels.profile.PasswordRecoveryViewModel;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputLayout;

public class PasswordRecoveryActivity extends AppCompatActivity {

    private PasswordRecoveryViewModel viewModel;
    private EditText emailEditText;
    private TextInputLayout emailInputLayout;
    private Button sendRecoveryEmailButton;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_password_recovery);

        // Инициализация Toolbar
        MaterialToolbar toolbar = findViewById(R.id.toolbar_password_recovery);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
        }

        // Инициализация ViewModel
        // Для более сложного создания ViewModel (с фабрикой) используйте ViewModelProvider.Factory
        viewModel = new ViewModelProvider(this).get(PasswordRecoveryViewModel.class);

        // Привязка View
        emailInputLayout = findViewById(R.id.til_email_recovery);
        emailEditText = findViewById(R.id.et_email_recovery);
        sendRecoveryEmailButton = findViewById(R.id.btn_send_recovery_email);
        progressBar = findViewById(R.id.progressBarPasswordRecovery);

        // Наблюдение за LiveData
        viewModel.isLoading.observe(this, isLoading -> {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            sendRecoveryEmailButton.setEnabled(!isLoading);
            emailEditText.setEnabled(!isLoading);
        });

        viewModel.recoveryStatus.observe(this, recoveryStatus -> {
            if (recoveryStatus instanceof PasswordRecoveryViewModel.RecoveryStatus.Success) {
                Toast.makeText(this, ((PasswordRecoveryViewModel.RecoveryStatus.Success) recoveryStatus).message, Toast.LENGTH_LONG).show();
                // Можно добавить переход на другой экран или закрытие этого, например:
                // finish(); 
            } else if (recoveryStatus instanceof PasswordRecoveryViewModel.RecoveryStatus.Error) {
                emailInputLayout.setError(((PasswordRecoveryViewModel.RecoveryStatus.Error) recoveryStatus).errorMessage);
                // Toast.makeText(this, ((PasswordRecoveryViewModel.RecoveryStatus.Error) recoveryStatus).errorMessage, Toast.LENGTH_SHORT).show();
            }
        });

        // Обработчики событий
        emailEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.onEmailChanged(s.toString());
                if (emailInputLayout.getError() != null) {
                    emailInputLayout.setError(null); // Сбрасываем ошибку при вводе
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        sendRecoveryEmailButton.setOnClickListener(v -> {
            viewModel.requestPasswordRecovery();
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        getOnBackPressedDispatcher().onBackPressed();
        return true;
    }
} 