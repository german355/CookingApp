package com.example.cooking.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.cooking.auth.FirebaseAuthManager;
import com.example.cooking.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.example.cooking.ui.viewmodels.profile.AuthViewModel;
import java.util.Locale;

/**
 * Активность регистрации пользователя
 * Использует AuthViewModel для бизнес-логики аутентификации
 */
public class Regist extends AppCompatActivity {
    private static final String TAG = "RegistActivity";
    private static final int RC_SIGN_IN = FirebaseAuthManager.RC_SIGN_IN;
    
    // UI компоненты
    private TextInputEditText nameEditText;
    private TextInputEditText emailEditText;
    private TextInputEditText passwordEditText;
    private TextInputEditText confirmPasswordEditText;
    private Button registerButton;
    private Button googleSignupButton;
    private TextView loginPromptTextView;
    private TextView orTextView;
    private ProgressBar progressBar;
    
    // Layouts для отображения ошибок
    private TextInputLayout nameInputLayout;
    private TextInputLayout emailInputLayout;
    private TextInputLayout passwordInputLayout;
    private TextInputLayout confirmPasswordInputLayout;
    
    // ViewModel
    private AuthViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        
        // Инициализация Toolbar
        MaterialToolbar toolbar = findViewById(R.id.toolbar_register);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
        }
        
        // Инициализируем ViewModel
        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        
        // Инициализируем UI компоненты
        initViews();
        
        // Настраиваем наблюдателей LiveData
        setupObservers();
        
        // Настраиваем обработчики ввода
        setupInputListeners();
        
        // Настраиваем обработчики нажатий
        setupClickListeners();
        
        // Инициализация Google Sign-In
        String webClientId = getString(R.string.default_web_client_id);
        viewModel.initGoogleSignIn(webClientId);
    }
    
    /**
     * Инициализирует UI компоненты
     */
    private void initViews() {
        // Текстовые поля
        nameEditText = findViewById(R.id.NameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        confirmPasswordEditText = findViewById(R.id.passwordEditText2);
        
        // Layouts для отображения ошибок
        nameInputLayout = (TextInputLayout) nameEditText.getParent().getParent();
        emailInputLayout = (TextInputLayout) emailEditText.getParent().getParent();
        passwordInputLayout = (TextInputLayout) passwordEditText.getParent().getParent();
        confirmPasswordInputLayout = (TextInputLayout) confirmPasswordEditText.getParent().getParent();
        
        // Кнопки и другие UI элементы
        registerButton = findViewById(R.id.firebaseRegisterButton);
        googleSignupButton = findViewById(R.id.googleSignupButton);
        loginPromptTextView = findViewById(R.id.loginPromptTextView);
        orTextView = findViewById(R.id.orTextView);
        progressBar = findViewById(R.id.progressBar);

        if (progressBar == null) {
            Log.w(TAG, "ProgressBar не найден в layout, необходимо добавить его в activity_register.xml");
        }
    }
    
    /**
     * Настраивает наблюдателей LiveData
     */
    private void setupObservers() {
        // Наблюдаем за состоянием загрузки
        viewModel.getIsLoading().observe(this, isLoading -> {
            if (progressBar != null) {
                progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            }
            
            // Блокируем кнопки во время загрузки
            registerButton.setEnabled(!isLoading);
            googleSignupButton.setEnabled(!isLoading);
            
            // Изменяем текст кнопки регистрации
            registerButton.setText(isLoading ? getString(R.string.auth_register_loading) : getString(R.string.register_button));
        });
        
        // Наблюдаем за сообщениями об ошибках
        viewModel.getErrorMessage().observe(this, errorMsg -> {
            if (errorMsg != null && !errorMsg.isEmpty()) {
                String normalized = errorMsg.toLowerCase(Locale.ROOT);
                if (normalized.contains("429") || normalized.contains("too many requests")) {
                    Toast.makeText(this, getString(R.string.auth_too_many_requests), Toast.LENGTH_LONG).show();
                    viewModel.clearErrorMessage();
                    return;
                }
                if (normalized.contains("network")
                        || normalized.contains("unable to resolve host")
                        || normalized.contains("unknown host")
                        || normalized.contains("failed to connect")
                        || normalized.contains("timeout")
                        || normalized.contains("status code 7")
                        || normalized.contains(" 7:")) {
                    Toast.makeText(this, getString(R.string.error_no_internet_connection), Toast.LENGTH_LONG).show();
                    viewModel.clearErrorMessage();
                    return;
                }

                String nameEmpty = getString(R.string.auth_name_cannot_be_empty);
                String nameShort = getString(R.string.auth_name_too_short);
                String emailEmpty = getString(R.string.auth_email_cannot_be_empty);
                String emailInvalid = getString(R.string.auth_invalid_email_format);
                String passwordEmpty = getString(R.string.auth_password_cannot_be_empty);
                String passwordShort = getString(R.string.auth_password_too_short);
                String confirmEmpty = getString(R.string.auth_confirm_password_cannot_be_empty);
                String passwordsMismatch = getString(R.string.auth_passwords_do_not_match);

                if (errorMsg.equals(nameEmpty) || errorMsg.equals(nameShort)) {
                    nameInputLayout.setError(errorMsg);
                    viewModel.clearErrorMessage();
                    return;
                } else if (errorMsg.equals(emailEmpty) || errorMsg.equals(emailInvalid)) {
                    emailInputLayout.setError(errorMsg);
                    viewModel.clearErrorMessage();
                    return;
                } else if (errorMsg.equals(passwordEmpty) || errorMsg.equals(passwordShort)) {
                    passwordInputLayout.setError(errorMsg);
                    viewModel.clearErrorMessage();
                    return;
                } else if (errorMsg.equals(confirmEmpty) || errorMsg.equals(passwordsMismatch)) {
                    confirmPasswordInputLayout.setError(errorMsg);
                    viewModel.clearErrorMessage();
                    return;
                } else if (normalized.contains("badly formatted") || normalized.contains("invalid email")) {
                    emailInputLayout.setError(emailInvalid);
                    viewModel.clearErrorMessage();
                    return;
                } else if (normalized.contains("already in use") || normalized.contains("email address is already in use")) {
                    emailInputLayout.setError(getString(R.string.auth_email_already_in_use));
                    viewModel.clearErrorMessage();
                    return;
                } else if (normalized.contains("weak password") || normalized.contains("password should be at least") || normalized.contains("at least 6")) {
                    passwordInputLayout.setError(passwordShort);
                    viewModel.clearErrorMessage();
                    return;
                } else {
                    Toast.makeText(this, getString(R.string.auth_register_failed), Toast.LENGTH_LONG).show();
                }

                viewModel.clearErrorMessage();
            }
        });
        
        // Наблюдаем за состоянием аутентификации
        viewModel.getIsAuthenticated().observe(this, isAuthenticated -> {
            if (isAuthenticated) {
                // Уведомляем пользователя о подтверждении почты
                Toast.makeText(this, getString(R.string.register_verification_email_sent), Toast.LENGTH_LONG).show();
                // Переходим на главный экран
                navigateToMainActivity();
            }
        });
    }
    
    /**
     * Настраивает обработчики ввода
     */
    private void setupInputListeners() {
        // Настраиваем валидацию имени
        nameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            
            @Override
            public void afterTextChanged(Editable s) {
                validateName(s.toString());
            }
        });
        
        // Настраиваем валидацию email
        emailEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            
            @Override
            public void afterTextChanged(Editable s) {
                validateEmail(s.toString());
            }
        });
        
        // Настраиваем валидацию пароля
        passwordEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            
            @Override
            public void afterTextChanged(Editable s) {
                String password = s.toString();
                validatePassword(password);
                
                // Проверяем подтверждение пароля, если оно не пустое
                String confirmPassword = confirmPasswordEditText.getText().toString();
                if (!confirmPassword.isEmpty()) {
                    validatePasswordConfirmation(password, confirmPassword);
                }
            }
        });
        
        // Настраиваем валидацию подтверждения пароля
        confirmPasswordEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            
            @Override
            public void afterTextChanged(Editable s) {
                validatePasswordConfirmation(passwordEditText.getText().toString(), s.toString());
            }
        });
    }
    
    /**
     * Настраивает обработчики нажатий
     */
    private void setupClickListeners() {
        // Обработчик нажатия кнопки регистрации
        registerButton.setOnClickListener(v -> {
            // Получаем введенные данные
            String name = nameEditText.getText().toString().trim();
            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString();
            String confirmPassword = confirmPasswordEditText.getText().toString();
            
            // Проверяем все поля на валидность
            if (validateAllInputs(name, email, password, confirmPassword)) {
                // Сразу регистрируем пользователя без reCAPTCHA
                viewModel.registerUser(email, password, name);
            }
        });
        
        // Обработчик нажатия кнопки регистрации через Google
        googleSignupButton.setOnClickListener(v -> {
            try {
                viewModel.signInWithGoogle(this);
            } catch (Exception e) {
                Toast.makeText(this, getString(R.string.auth_google_login_error), Toast.LENGTH_SHORT).show();
            }
        });
        
        // Обработчик нажатия ссылки для перехода на экран входа
        loginPromptTextView.setOnClickListener(v -> {
            Intent intent = new Intent(Regist.this, MainActivity.class);
            intent.putExtra("show_auth_fragment", true);
            startActivity(intent);
            finish();
        });
    }
    
    /**
     * Проверяет валидность всех полей ввода
     */
    private boolean validateAllInputs(String name, String email, String password, String confirmPassword) {
        boolean isNameValid = validateName(name);
        boolean isEmailValid = validateEmail(email);
        boolean isPasswordValid = validatePassword(password);
        boolean isPasswordConfirmValid = validatePasswordConfirmation(password, confirmPassword);
        
        return isNameValid && isEmailValid && isPasswordValid && isPasswordConfirmValid;
    }
    
    /**
     * Валидация имени пользователя
     */
    private boolean validateName(String name) {
        boolean isValid = viewModel.validateName(name);
        
        if (!isValid) {
            nameInputLayout.setError(getString(R.string.auth_name_too_short));
        } else {
            nameInputLayout.setError(null);
        }
        
        return isValid;
    }
    
    /**
     * Валидация email
     */
    private boolean validateEmail(String email) {
        boolean isValid = viewModel.validateEmail(email);
        
        if (!isValid) {
            emailInputLayout.setError(getString(R.string.auth_invalid_email_format));
        } else {
            emailInputLayout.setError(null);
        }
        
        return isValid;
    }
    
    /**
     * Валидация пароля
     */
    private boolean validatePassword(String password) {
        boolean isValid = viewModel.validatePassword(password);
        
        if (!isValid) {
            passwordInputLayout.setError(getString(R.string.auth_password_too_short));
        } else {
            passwordInputLayout.setError(null);
        }
        
        return isValid;
    }
    
    /**
     * Валидация подтверждения пароля
     */
    private boolean validatePasswordConfirmation(String password, String confirmPassword) {
        boolean isValid = viewModel.doPasswordsMatch(password, confirmPassword);
        
        if (!isValid) {
            confirmPasswordInputLayout.setError(getString(R.string.auth_passwords_do_not_match));
        } else {
            confirmPasswordInputLayout.setError(null);
        }
        
        return isValid;
    }
    
    /**
     * Переход на главный экран после успешной регистрации
     */
    private void navigateToMainActivity() {
        Intent intent = new Intent(Regist.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        // Обработка результата входа через Google
        if (requestCode == RC_SIGN_IN) {
            viewModel.handleGoogleSignInResult(requestCode, resultCode, data);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        getOnBackPressedDispatcher().onBackPressed();
        return true;
    }
}
