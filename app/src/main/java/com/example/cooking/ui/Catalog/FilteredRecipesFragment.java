package com.example.cooking.ui.Catalog;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
// import androidx.navigation.fragment.NavHostFragment; // Не используется здесь явно
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.cooking.R;
import com.example.cooking.Recipe.Recipe;
import com.example.cooking.ui.adapters.RecipeListAdapter;
import java.util.List; // ArrayList не используется напрямую, достаточно List

/**
 * Фрагмент для отображения списка отфильтрованных рецептов.
 * Получает параметры фильтрации через аргументы навигации.
 */
public class FilteredRecipesFragment extends Fragment implements RecipeListAdapter.OnRecipeLikeListener {

    private FilteredRecipesViewModel viewModel;
    private RecyclerView recyclerView;
    private TextView emptyView;
    private RecipeListAdapter recipeListAdapter;

    private String categoryName;
    private String filterKey;
    private String filterType;

    // Конструктор по умолчанию, обязателен для фрагментов
    public FilteredRecipesFragment() {}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Получение аргументов, переданных при навигации
        if (getArguments() != null) {
            FilteredRecipesFragmentArgs args = FilteredRecipesFragmentArgs.fromBundle(getArguments());
            categoryName = args.getCategoryName();
            filterKey = args.getFilterKey();
            filterType = args.getFilterType();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_filtered_recipes, container, false);
        
        recyclerView = view.findViewById(R.id.filtered_recipes_recycler_view);
        emptyView = view.findViewById(R.id.empty_view_filtered_recipes);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        
        recipeListAdapter = new RecipeListAdapter(this);
        recyclerView.setAdapter(recipeListAdapter);
        
        // Отладочный Toast для проверки полученных аргументов
        // Toast.makeText(getContext(), "Категория: " + categoryName + " (Ключ: " + filterKey + ", Тип: " + filterType + ")", Toast.LENGTH_LONG).show();

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(FilteredRecipesViewModel.class);

        // Инициация загрузки отфильтрованных рецептов
        viewModel.loadFilteredRecipes(filterKey, filterType);

        // Наблюдение за списком отфильтрованных рецептов
        viewModel.getFilteredRecipes().observe(getViewLifecycleOwner(), recipes -> {
            if (recipes != null && !recipes.isEmpty()) {
                recipeListAdapter.submitList(recipes);
                recyclerView.setVisibility(View.VISIBLE);
                emptyView.setVisibility(View.GONE);
                android.util.Log.d("FilteredRecipes", "Отображено рецептов: " + recipes.size());
                // Toast "Загружено рецептов" убран, чтобы не появлялся при каждом обновлении списка
            } else {
                recyclerView.setVisibility(View.GONE);
                emptyView.setVisibility(View.VISIBLE);
                android.util.Log.d("FilteredRecipes", "Рецепты не найдены или список пуст для текущих фильтров.");
                Toast.makeText(getContext(), "Рецепты не найдены", Toast.LENGTH_SHORT).show(); 
            }
        });

        // Наблюдение за сообщениями об ошибках
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(getContext(), "Ошибка: " + error, Toast.LENGTH_LONG).show();
                recyclerView.setVisibility(View.GONE);
                emptyView.setVisibility(View.VISIBLE);
                emptyView.setText("Ошибка загрузки: " + error); // Отображаем ошибку в emptyView
                android.util.Log.e("FilteredRecipes", "Ошибка загрузки: " + error);
            }
        });
    }

    /**
     * Обрабатывает событие лайка/дизлайка рецепта из адаптера.
     * Выполняет оптимистичное обновление UI и передает действие в ViewModel.
     * @param recipe Объект Recipe, статус лайка которого изменился.
     * @param isLiked Новое состояние лайка (true - лайкнут, false - дизлайкнут).
     */
    @Override
    public void onRecipeLike(Recipe recipe, boolean isLiked) {
        String action = isLiked ? "лайкнут" : "дизлайкнут";
        Toast.makeText(getContext(), "Рецепт '" + recipe.getTitle() + "' " + action, Toast.LENGTH_SHORT).show();

        // 1. Оптимистичное обновление: изменяем состояние лайка у локального объекта
        // и немедленно обновляем соответствующий элемент в RecyclerView.
        recipe.setLiked(isLiked);

        List<Recipe> currentList = recipeListAdapter.getCurrentList();
        int position = -1;
        for (int i = 0; i < currentList.size(); i++) {
            // Поиск по ID, так как объекты могут быть разными экземплярами
            if (currentList.get(i).getId() == recipe.getId()) { 
                position = i;
                break;
            }
        }

        if (position != -1) {
            recipeListAdapter.notifyItemChanged(position);
        } else {
            // Если элемент не найден в списке адаптера, логируем это.
            // Это маловероятно, но полезно для отладки.
            android.util.Log.w("FilteredRecipesFragment", "Рецепт с ID " + recipe.getId() + " не найден в списке для оптимистичного обновления UI.");
        }

        // 2. Сообщаем ViewModel об изменении для персистентного сохранения состояния лайка.
        if (viewModel != null) {
            viewModel.toggleLikeStatus(recipe, isLiked);
        }
    }
} 