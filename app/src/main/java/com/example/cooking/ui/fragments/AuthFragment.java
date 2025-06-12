package com.example.cooking.ui.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.cooking.R;
import com.example.cooking.auth.FirebaseAuthManager;
import com.example.cooking.ui.activities.Regist;
import com.example.cooking.ui.activities.PasswordRecoveryActivity;
import com.example.cooking.ui.viewmodels.AuthViewModel;
import com.example.cooking.ui.viewmodels.MainViewModel;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseUser;

/**
 * Фрагмент авторизации, который показывается вместо ProfileFragment,
 * если пользователь не авторизован.
 */
public class AuthFragment extends Fragment {
    private static final String TAG = "AuthFragment";
    private static final int RC_SIGN_IN = FirebaseAuthManager.RC_SIGN_IN;

    private TextInputEditText emailEditText;
    private TextInputEditText passwordEditText;
    private TextInputLayout emailInputLayout;
    private TextInputLayout passwordInputLayout;
    private Button loginButton;
    private Button googleLoginButton;
    private TextView registerTextView;
    private ProgressBar progressBar;
    private TextView forgotPasswordTextView;

    private AuthViewModel viewModel;
    private NavController navController;
    private boolean isInitialAuthState = true;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_auth, container, false);

        // Инициализация ViewModel
        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        // Инициализация UI элементов
        emailEditText = view.findViewById(R.id.email_edit_text);
        passwordEditText = view.findViewById(R.id.password_edit_text);
        emailInputLayout = view.findViewById(R.id.email_input_layout);
        passwordInputLayout = view.findViewById(R.id.password_input_layout);
        loginButton = view.findViewById(R.id.login_button);
        googleLoginButton = view.findViewById(R.id.google_login_button);
        registerTextView = view.findViewById(R.id.register_text_view);
        progressBar = view.findViewById(R.id.login_progress_bar);
        forgotPasswordTextView = view.findViewById(R.id.forgotPasswordTextView);

        // Настройка обработчиков ввода
        setupTextWatchers();

        // Настройка обработчиков нажатий
        setupClickListeners();

        // Настройка наблюдателей LiveData
        setupObservers();

        // Инициализация Google Sign-In
        String webClientId = getString(R.string.default_web_client_id);
        viewModel.initGoogleSignIn(webClientId);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);
    }

    private void setupObservers() {
        // Наблюдатель для состояния загрузки
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            loginButton.setEnabled(!isLoading);
            googleLoginButton.setEnabled(!isLoading);

            if (isLoading) {
                loginButton.setText("Входим...");
            } else {
                loginButton.setText("Войти");
            }
        });

        // Наблюдатель для сообщений об ошибках
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                // Сброс предыдущих ошибок
                emailInputLayout.setError(null);
                passwordInputLayout.setError(null);
                // Обработка ошибок
                switch (errorMessage) {
                    case "Email не может быть пустым":
                    case "Неверный формат email":
                        emailInputLayout.setError(errorMessage);
                        break;
                    case "Пароль не может быть пустым":
                    case "Пароль должен содержать не менее 6 символов":
                        passwordInputLayout.setError(errorMessage);
                        break;
                    default: {
                        // Преобразуем Firebase-сообщения и технические тексты в понятный формат
                        String msg = errorMessage != null ? errorMessage.trim() : "";
                        if (msg.startsWith("Ошибка авторизации:")) {
                            msg = msg.substring("Ошибка авторизации:".length()).trim();
                        }

                        String low = msg.toLowerCase();

                        // 1) Ошибки формата e-mail
                        if (low.contains("badly formatted")) {
                            emailInputLayout.setError("Неверный формат email");
                            break;
                        }

                        // 2) Ошибки пароля / учётных данных
                        if (low.contains("wrong password") || low.contains("wrong-password") || low.contains("invalid password") || low.contains("invalid-credentials")) {
                            passwordInputLayout.setError("Неверная почта или пароль");
                            break;
                        }

                        // 3) Пользователь не найден
                        if (low.contains("user not found") || low.contains("no user record")) {
                            Toast.makeText(requireContext(), "Пользователь с такой почтой не найден", Toast.LENGTH_LONG).show();
                            break;
                        }

                        // 4) Сетевые проблемы
                        if (low.contains("network") || low.contains("unable to resolve host") || low.contains("failed to connect") || low.contains("timeout") || low.contains(" 7:") || low.contains("status code 7")) {
                            Toast.makeText(requireContext(), "Проблемы с соединением. Проверьте интернет и попробуйте снова", Toast.LENGTH_LONG).show();
                            break;
                        }

                        // 5) Блокировка из-за большого количества попыток
                        if (low.contains("blocked") || low.contains("too many requests")) {
                            Toast.makeText(requireContext(), "Слишком много попыток. Попробуйте позже", Toast.LENGTH_LONG).show();
                            break;
                        }

                        // 6) Остальные случаи – универсальное сообщение
                        Toast.makeText(requireContext(), "Не удалось войти. Попробуйте позже", Toast.LENGTH_LONG).show();

                        // Иногда сообщение может быть пустым при статусе 7, обрабатываем как сетевую ошибку
                        if (msg.isEmpty()) {
                            Toast.makeText(requireContext(), "Проблемы с соединением. Проверьте интернет и попробуйте снова", Toast.LENGTH_LONG).show();
                            break;
                        }

                        break;
                    }
                }
                viewModel.clearErrorMessage();
            }
        });

        // Наблюдатель для состояния аутентификации
        viewModel.getIsAuthenticated().observe(getViewLifecycleOwner(), isAuthenticated -> {
            if (isInitialAuthState) {
                isInitialAuthState = false;
                return;
            }
            if (isAuthenticated) {
                // Событие успешного входа
                MainViewModel mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
                mainViewModel.triggerLoginEvent();
                Toast.makeText(requireContext(), "Вход выполнен успешно", Toast.LENGTH_SHORT).show();
                navController.navigate(R.id.nav_profile);
            }
        });
    }

    private void setupTextWatchers() {
        // Email TextWatcher
        emailEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                boolean isEmailValid = viewModel.validateEmail(s.toString());
                if (!isEmailValid) {
                    emailInputLayout.setError("Введите корректный email");
                } else {
                    emailInputLayout.setError(null);
                }
            }
        });

        // Password TextWatcher
        passwordEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                boolean isPasswordValid = viewModel.validatePassword(s.toString());
                if (!isPasswordValid) {
                    passwordInputLayout.setError("Пароль должен содержать не менее 6 символов");
                } else {
                    passwordInputLayout.setError(null);
                }
            }
        });
    }

    private void setupClickListeners() {
        // Обработчик нажатия кнопки входа
        loginButton.setOnClickListener(v -> {
            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString();

            if (viewModel.validateAllLoginInputs(email, password)) {
                viewModel.signInWithEmailPassword(email, password);
            }
        });

        // Обработчик нажатия кнопки входа через Google
        googleLoginButton.setOnClickListener(v -> {
            try {
                viewModel.signInWithGoogle(requireActivity());
            } catch (Exception e) {
                Toast.makeText(requireContext(), "Ошибка при входе через Google",
                        Toast.LENGTH_SHORT).show();
            }
        });

        // Обработчик нажатия текста регистрации
        registerTextView.setOnClickListener(v -> {
            Intent intent = new Intent(requireActivity(), Regist.class);
            startActivity(intent);
        });

        // Обработчик для "Забыли пароль?"
        if (forgotPasswordTextView != null) {
            forgotPasswordTextView.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), PasswordRecoveryActivity.class);
                startActivity(intent);
            });
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Обработка результата входа через Google
        if (requestCode == RC_SIGN_IN) {
            viewModel.handleGoogleSignInResult(requestCode, resultCode, data);
        }
    }
}