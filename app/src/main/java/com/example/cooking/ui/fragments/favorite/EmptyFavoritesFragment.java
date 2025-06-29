package com.example.cooking.ui.fragments.favorite;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.cooking.ui.activities.MainActivity;
import com.example.cooking.R;

/**
 * Фрагмент для отображения пустого состояния избранных рецептов
 */
public class EmptyFavoritesFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_empty_favorites, container, false);

        // Установка изображения для пустого состояния
        ImageView imageEmptyFavorites = view.findViewById(R.id.image_empty_favorites);
        imageEmptyFavorites.setImageResource(R.drawable.emtyfavorites);
        
        // Настройка кнопки перехода к рецептам
        Button btnGoToRecipes = view.findViewById(R.id.btn_go_to_recipes);
        btnGoToRecipes.setOnClickListener(v -> navigateToRecipesFragment());
        
        return view;
    }
    
    /**
     * Переход к фрагменту с рецептами
     */
    private void navigateToRecipesFragment() {
        if (getActivity() instanceof MainActivity) {
            MainActivity activity = (MainActivity) getActivity();
            activity.navigateToHomeFragment();
        }
    }
} 