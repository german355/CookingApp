package com.example.cooking.ui.Catalog;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
// import androidx.navigation.fragment.NavHostFragment; // Не используется здесь явно
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.cooking.R;
import com.example.cooking.Recipe.Recipe;
import com.example.cooking.ui.adapters.RecipeListAdapter;
import java.util.List; // ArrayList не используется напрямую, достаточно List
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

/**
 * Фрагмент для отображения списка отфильтрованных рецептов.
 * Получает параметры фильтрации через аргументы навигации.
 */
public class FilteredRecipesFragment extends Fragment implements RecipeListAdapter.OnRecipeLikeListener {

    private FilteredRecipesViewModel viewModel;
    private RecyclerView recyclerView;
    private TextView emptyView;
    private RecipeListAdapter recipeListAdapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar progressBar;

    private String categoryName;
    private String filterKey;
    private String filterType;

    // Флаг для отслеживания инициированного пользователем обновления
    private boolean userInitiatedRefresh = false;

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
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_filtered);
        progressBar = view.findViewById(R.id.progress_bar_filtered);
        
        // Настройка RecyclerView в виде сетки с двумя колонками, как в HomeFragment
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        
        recipeListAdapter = new RecipeListAdapter(this);
        recyclerView.setAdapter(recipeListAdapter);
        
        // Настраиваем SwipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener(() -> {
            userInitiatedRefresh = true;
            if (viewModel != null) {
                viewModel.refreshData();
            }
        });

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
            } else {
                recyclerView.setVisibility(View.GONE);
                emptyView.setVisibility(View.VISIBLE);
                android.util.Log.d("FilteredRecipes", "Рецепты не найдены или список пуст для текущих фильтров.");
                emptyView.setText("Рецепты для категории \"" + categoryName + "\" не найдены");
            }
        });

        // Наблюдение за состоянием загрузки
        viewModel.getIsRefreshing().observe(getViewLifecycleOwner(), isRefreshing -> {
            if (isRefreshing) {
                // Если обновление в процессе
                if (userInitiatedRefresh) {
                    // Если обновление было инициировано пользователем через swipe, 
                    // оставляем SwipeRefreshLayout видимым, скрываем progressBar
                    swipeRefreshLayout.setRefreshing(true);
                    progressBar.setVisibility(View.GONE);
                } else {
                    // Если обновление НЕ было инициировано пользователем, 
                    // показываем progressBar, не показываем SwipeRefreshLayout
                    swipeRefreshLayout.setRefreshing(false);
                    // Показываем прогресс бар только при первой загрузке, когда список пустой
                    if (recipeListAdapter.getItemCount() == 0) {
                        progressBar.setVisibility(View.VISIBLE);
                    }
                }
            } else {
                // Обновление закончилось, сбрасываем флаг и убираем оба индикатора
                userInitiatedRefresh = false;
                swipeRefreshLayout.setRefreshing(false);
                progressBar.setVisibility(View.GONE);
            }
        });

        // Наблюдение за сообщениями об ошибках
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
                
                // Проверяем, пустой ли список
                boolean isEmpty = recipeListAdapter.getItemCount() == 0;
                recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
                emptyView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
                
                if (isEmpty) {
                    emptyView.setText(error);
                }
                
                android.util.Log.e("FilteredRecipes", "Ошибка: " + error);
            }
        });
    }

    /**
     * Обрабатывает событие лайка/дизлайка рецепта из адаптера.
     * @param recipe Объект Recipe, статус лайка которого изменился.
     * @param isLiked Новое состояние лайка (true - лайкнут, false - дизлайкнут).
     */
    @Override
    public void onRecipeLike(Recipe recipe, boolean isLiked) {
        if (recipe != null && viewModel != null) {
            // Делегируем обработку в ViewModel
            viewModel.toggleLikeStatus(recipe, isLiked);
        }
    }
} 