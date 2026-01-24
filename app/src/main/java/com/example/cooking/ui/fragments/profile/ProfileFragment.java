package com.example.cooking.ui.fragments.profile;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.cooking.R;
import com.example.cooking.ui.viewmodels.profile.ProfileViewModel;
import com.example.cooking.ui.viewmodels.MainViewModel;
import com.example.cooking.ui.activities.AiChatActivity;

/**
 * Фрагмент для отображения профиля пользователя и настроек приложения
 *
 * Позволяет пользователю управлять своим аккаунтом и персональными данными
 */
public class ProfileFragment extends Fragment {
    private static final String TAG = "ProfileFragment";

    private TextView nameTextView;
    private TextView emailTextView;
    private Button editNameButton;
    private Button changePasswordButton;
    private Button logoutButton;
    private Button deleteAccountButton;
    private Button reportProblemButton;
    private ProgressBar progressBar;

    private ProfileViewModel viewModel;
    private MainViewModel mainViewModel;
    private NavController navController;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Инициализируем ViewModel фрагмента
        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);
        // Инициализируем ViewModel активности для доступа к общим событиям
        mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        // Инициализируем элементы UI
        nameTextView = view.findViewById(R.id.profile_name);
        emailTextView = view.findViewById(R.id.profile_email);
        editNameButton = view.findViewById(R.id.edit_name_button);
        changePasswordButton = view.findViewById(R.id.change_password_button);
        logoutButton = view.findViewById(R.id.logout_button);
        reportProblemButton = view.findViewById(R.id.report_problem_button);

        progressBar = view.findViewById(R.id.progress_bar);

        // Настраиваем наблюдателей LiveData
        setupObservers();

        // Настраиваем обработчики событий
        setupEventListeners();

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Получаем NavController
        navController = Navigation.findNavController(view);
    }

    /**
     * Настраивает наблюдателей для LiveData из ViewModel
     */
    private void setupObservers() {
        // Наблюдатель за состоянием загрузки
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (progressBar != null) {
                progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            }
        });

        // Наблюдатель за сообщениями об ошибках
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty() && isAdded()) {
                // Очищаем ошибку после показа
                viewModel.clearErrorMessage();
            }
        });

        // Наблюдатель за именем пользователя
        viewModel.getDisplayName().observe(getViewLifecycleOwner(), name -> {
            if (nameTextView != null) {
                nameTextView.setText(name != null ? name : getString(R.string.profile_name_not_set));
            }
        });

        // Наблюдатель за email пользователя
        viewModel.getEmail().observe(getViewLifecycleOwner(), email -> {
            if (emailTextView != null) {
                emailTextView.setText(email != null ? email : getString(R.string.profile_email_not_set));
            }
        });

        // Наблюдатель за состоянием аутентификации
        viewModel.getIsAuthenticated().observe(getViewLifecycleOwner(), isAuthenticated -> {
            // Здесь можно обновлять UI, если нужно (например, скрывать/показывать кнопки)
            // Но основное переключение на AuthFragment происходит в MainActivity
        });

        // Наблюдатель за успешным выполнением операции (выход, удаление, смена
        // имени/пароля)
        viewModel.getOperationSuccess().observe(getViewLifecycleOwner(), success -> {
            if (success != null && success) {
                Log.d(TAG, "Операция успешно завершена. Проверяем, был ли это выход.");
                // Проверяем, вышел ли пользователь (isAuthenticated стал false)
                Boolean isAuth = viewModel.getIsAuthenticated().getValue();
                Log.d(TAG, "Текущее значение isAuthenticated: " + isAuth);

                if (isAuth != null && !isAuth) {
                    // Инициируем событие выхода в MainViewModel
                    mainViewModel.triggerLogoutEvent();
                }
                // Сбрасываем флаг успеха, чтобы он не сработал снова
                viewModel.clearOperationSuccess();
            }
        });
    }

    /**
     * Настраивает обработчики событий UI
     */
    private void setupEventListeners() {
        // Обработчик кнопки редактирования имени
        editNameButton.setOnClickListener(v -> showEditNameDialog());

        // Обработчик кнопки изменения пароля
        changePasswordButton.setOnClickListener(v -> showChangePasswordDialog());

        // Обработчик кнопки выхода
        logoutButton.setOnClickListener(v -> confirmLogout());

        // Обработчик кнопки сообщения о проблеме
        reportProblemButton.setOnClickListener(v -> showBugReportDialog());

        // Обработчик кнопки удаления аккаунта (Пока не реализованно)
        if (deleteAccountButton != null) {
           // deleteAccountButton.setOnClickListener(v -> showDeleteAccountConfirmation());
        }
    }

    /**
     * Показывает диалог для редактирования имени пользователя
     */
    private void showEditNameDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(R.string.profile_edit_name_title);

        // Настраиваем поле ввода
        final EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(viewModel.getDisplayName().getValue());
        builder.setView(input);

        // Настраиваем кнопки
        builder.setPositiveButton(R.string.action_save, (dialog, which) -> {
            String newName = input.getText().toString().trim();
            if (!newName.isEmpty()) {
                viewModel.updateDisplayName(newName);
            } else {
                Toast.makeText(requireContext(), getString(R.string.profile_name_empty), Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.cancel());

        builder.show();
    }

    /**
     * Показывает диалог для изменения пароля
     */
    private void showChangePasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(R.string.change_password_dialog_title);

        // Создаем View для диалога с двумя полями ввода
        View viewInflated = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_change_password, (ViewGroup) getView(), false);

        final EditText currentPasswordInput = viewInflated.findViewById(R.id.current_password);
        final EditText newPasswordInput = viewInflated.findViewById(R.id.new_password);

        builder.setView(viewInflated);

        // Настраиваем кнопки
        builder.setPositiveButton(R.string.action_change, (dialog, which) -> {
            String currentPassword = currentPasswordInput.getText().toString();
            String newPassword = newPasswordInput.getText().toString();

            if (currentPassword.isEmpty() || newPassword.isEmpty()) {
                Toast.makeText(requireContext(), getString(R.string.error_all_fields_required), Toast.LENGTH_SHORT).show();
                return;
            }

            // Вызываем метод ViewModel для обновления пароля
            viewModel.updatePassword(currentPassword, newPassword);
        });
        builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.cancel());

        builder.show();
    }

    /**
     * Показывает диалог подтверждения выхода из аккаунта
     */
    private void confirmLogout() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.profile_logout_button)
                .setMessage(R.string.profile_logout_confirm_message)
                .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                    // Выполняем выход через ViewModel
                    viewModel.signOut();
                    // Наблюдатель за operationSuccess обработает переход
                })
                .setNegativeButton(android.R.string.no, (dialog, which) -> dialog.dismiss())
                .show();
    }

    /**
     * Показывает диалог подтверждения удаления аккаунта(пока не используется)
     */
    private void showDeleteAccountConfirmation() {
        View viewInflated = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_password_confirm, (ViewGroup) getView(), false);

        final EditText passwordInput = viewInflated.findViewById(R.id.password_input);

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.profile_delete_account_title)
                .setMessage(R.string.profile_delete_account_message)
                .setView(viewInflated)
                .setPositiveButton(R.string.recipe_delete_confirm, (dialog, which) -> {
                    String password = passwordInput.getText().toString();
                    if (!password.isEmpty()) {
                        viewModel.deleteAccount(password);
                    } else {
                        Toast.makeText(requireContext(), getString(R.string.profile_password_empty), Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
                .show();
    }

    /**
     * Показывает диалог информации о том, как сообщать о проблемах через AI ассистента
     */
    private void showBugReportDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.bug_report_title))
                .setMessage(getString(R.string.bug_report_description) + "\n\n" + 
                           getString(R.string.bug_report_examples))
                .setPositiveButton(R.string.action_open_chat, (dialog, which) -> {
                    // Открываем AI чат
                    Intent intent = new Intent(requireContext(), AiChatActivity.class);
                    startActivity(intent);
                })
                .setNegativeButton(R.string.action_close, (dialog, which) -> dialog.dismiss())
                .show();
    }
}
