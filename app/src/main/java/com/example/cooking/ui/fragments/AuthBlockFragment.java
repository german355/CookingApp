package com.example.cooking.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.example.cooking.R;
import com.example.cooking.Recipe.Recipe;
import com.example.cooking.ui.activities.MainActivity;

/**
 * Фрагмент, отображаемый неавторизованным пользователям вместо избранного.
 * Предлагает пользователям авторизоваться для доступа к функциям избранного.
 */
public class AuthBlockFragment extends Fragment {
    
    private static final String TAG = "AuthBlockFragment";
    
    public AuthBlockFragment() {
        // Пустой конструктор
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_auth_block, container, false);
        
        // Инициализация кнопки входа
        Button loginButton = view.findViewById(R.id.btn_login);
        
        // Устанавливаем обработчик нажатия на кнопку
        loginButton.setOnClickListener(v -> {
            // Переходим на экран авторизации
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).bottomNavigationView.setSelectedItemId(R.id.nav_profile);
            }
        });
        
        return view;
    }
    
    /**
     * Метод, который будет вызываться при попытке лайкнуть рецепт неавторизованным пользователем.
     * Показывает уведомление (Toast) с сообщением.
     *
     * @param recipe рецепт, который пытаются лайкнуть
     * @return всегда false, так как операция не может быть выполнена
     */
    public static boolean onUnauthorizedLike(Recipe recipe) {
        // Вернуть false и ничего не делать, так как пользователь не авторизован
        return false;
    }
    
    /**
     * Метод для отображения Toast сообщения о необходимости авторизации.
     * Позволяет вызывать Toast из любого места, где нужно уведомить 
     * неавторизованного пользователя о необходимости войти в систему.
     *
     * @param view текущий View (необходим для получения контекста)
     */
    public static void showAuthRequiredToast(View view) {
        if (view != null && view.getContext() != null) {
            Toast.makeText(
                view.getContext(), 
                "Войдите в систему, чтобы добавлять рецепты в избранное", 
                Toast.LENGTH_SHORT
            ).show();
        }
    }
} 