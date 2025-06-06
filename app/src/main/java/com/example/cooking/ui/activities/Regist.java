package com.example.cooking.ui.activities;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
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
import com.example.cooking.utils.MySharedPreferences;
import com.example.cooking.R;
import com.example.cooking.data.models.ApiResponse;
import com.example.cooking.network.services.UserService;
import com.google.android.gms.common.api.ApiException;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseUser;
import com.example.cooking.ui.viewmodels.AuthViewModel;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;

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
        //resendVerificationButton = findViewById(R.id.resendVerificationButton);
        
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
            registerButton.setText(isLoading ? "Подождите..." : "Зарегистрироваться");
        });
        
        // Наблюдаем за сообщениями об ошибках
        viewModel.getErrorMessage().observe(this, errorMsg -> {
            if (errorMsg != null && !errorMsg.isEmpty()) {
                emailInputLayout.setError(errorMsg);
                viewModel.clearErrorMessage();
            }
        });
        
        // Наблюдаем за состоянием аутентификации
        viewModel.getIsAuthenticated().observe(this, isAuthenticated -> {
            if (isAuthenticated) {
                // Уведомляем пользователя о подтверждении почты
                Toast.makeText(this, "На вашу почту отправлено письмо для подтверждения", Toast.LENGTH_LONG).show();
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
                Toast.makeText(this, "Ошибка при входе через Google" , Toast.LENGTH_SHORT).show();
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
            nameInputLayout.setError("Имя должно содержать не менее 2 символов");
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
            emailInputLayout.setError("Введите корректный email");
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
            passwordInputLayout.setError("Пароль должен содержать не менее 6 символов");
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
            confirmPasswordInputLayout.setError("Пароли не совпадают");
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
