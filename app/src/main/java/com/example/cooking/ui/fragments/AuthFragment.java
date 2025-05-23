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
        // Получаем NavController
        navController = Navigation.findNavController(view);

        // Инициализация Toolbar
        MaterialToolbar toolbar = view.findViewById(R.id.toolbar_auth);
        
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Используем NavController для возврата назад, если он доступен
                if (navController != null) {
                    navController.navigateUp();
                } else if (getActivity() != null) {
                    // Как запасной вариант, если NavController почему-то не инициализирован
                    getActivity().getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
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
                // Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show();
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
                Log.d(TAG, "Пользователь успешно авторизовался, обновляем MainViewModel");
                // Получаем ViewModel активности
                MainViewModel mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
                // Обновляем состояние авторизации в MainViewModel, что автоматически обновит UI
                mainViewModel.checkAuthState();

                Toast.makeText(requireContext(), "Вход выполнен успешно", Toast.LENGTH_SHORT).show();

                // Возвращаемся назад с помощью Navigation Component
                navController.navigateUp();
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
                Log.e(TAG, "Ошибка при запуске Google Sign-In: " + e.getMessage());
                Toast.makeText(requireContext(), "Ошибка при входе через Google: " + e.getMessage(),
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