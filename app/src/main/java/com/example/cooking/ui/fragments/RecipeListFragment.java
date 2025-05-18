package com.example.cooking.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SearchView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.cooking.R;
import com.example.cooking.Recipe.Recipe;
import com.example.cooking.ui.adapters.RecipeAdapter;
import com.example.cooking.ui.viewmodels.RecipeListViewModel;
import com.example.cooking.network.utils.Resource;

import java.util.ArrayList;
import java.util.List;

/**
 * Фрагмент для отображения списка рецептов
 */
public class RecipeListFragment extends Fragment {
    private RecipeListViewModel viewModel;
    private RecipeAdapter adapter;
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefresh;
    private SearchView searchView;
    private View emptyState;
    private View errorState;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_recipe_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Инициализация UI компонентов
        recyclerView = view.findViewById(R.id.recipe_recycler_view);
        swipeRefresh = view.findViewById(R.id.swipe_refresh);
        searchView = view.findViewById(R.id.search_view);
        emptyState = view.findViewById(R.id.empty_state);
        errorState = view.findViewById(R.id.error_state);
        
        // Настройка RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new RecipeAdapter(new ArrayList<>(), this::onRecipeClick);
        recyclerView.setAdapter(adapter);
        
        // Настройка SwipeRefreshLayout
        swipeRefresh.setOnRefreshListener(this::refreshRecipes);
        
        // Настройка SearchView
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                viewModel.searchRecipes(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // Опционально: можно реализовать поиск при вводе
                return false;
            }
        });
        
        // Получение ViewModel
        viewModel = new ViewModelProvider(this).get(RecipeListViewModel.class);
        observeViewModel();
    }
    
    /**
     * Настройка наблюдателей ViewModel
     */
    private void observeViewModel() {
        viewModel.getRecipes().observe(getViewLifecycleOwner(), resource -> {
            // Обработка статуса ресурса
            switch (resource.getStatus()) {
                case LOADING:
                    showLoading();
                    // Если есть данные, отображаем их, пока идет загрузка
                    if (resource.getData() != null) {
                        showRecipes(resource.getData());
                    }
                    break;
                    
                case SUCCESS:
                    hideLoading();
                    if (resource.getData() != null) {
                        showRecipes(resource.getData());
                    } else {
                        showEmptyState();
                    }
                    break;
                    
                case ERROR:
                    hideLoading();
                    // Если есть данные, отображаем их, несмотря на ошибку
                    if (resource.getData() != null && !resource.getData().isEmpty()) {
                        showRecipes(resource.getData());
                        showErrorToast(resource.getMessage());
                    } else {
                        showErrorState(resource.getMessage());
                    }
                    break;
            }
        });
        
        viewModel.isRefreshing().observe(getViewLifecycleOwner(), isRefreshing -> 
            swipeRefresh.setRefreshing(isRefreshing));
    }
    
    /**
     * Обработчик нажатия на рецепт
     */
    private void onRecipeClick(Recipe recipe) {
        // Здесь будет переход к деталям рецепта
        Toast.makeText(requireContext(), 
            "Выбран рецепт: " + recipe.getTitle(), 
            Toast.LENGTH_SHORT).show();
    }
    
    /**
     * Обновление списка рецептов
     */
    private void refreshRecipes() {
        viewModel.refreshRecipes();
    }
    
    /**
     * Отображение списка рецептов
     */
    private void showRecipes(List<Recipe> recipes) {
        adapter.updateRecipes(recipes);
        recyclerView.setVisibility(View.VISIBLE);
        emptyState.setVisibility(View.GONE);
        errorState.setVisibility(View.GONE);
    }
    
    /**
     * Показать состояние загрузки
     */
    private void showLoading() {
        swipeRefresh.setRefreshing(true);
    }
    
    /**
     * Скрыть состояние загрузки
     */
    private void hideLoading() {
        swipeRefresh.setRefreshing(false);
    }
    
    /**
     * Показать пустое состояние (нет рецептов)
     */
    private void showEmptyState() {
        recyclerView.setVisibility(View.GONE);
        emptyState.setVisibility(View.VISIBLE);
        errorState.setVisibility(View.GONE);
    }
    
    /**
     * Показать состояние ошибки
     */
    private void showErrorState(String errorMessage) {
        recyclerView.setVisibility(View.GONE);
        emptyState.setVisibility(View.GONE);
        errorState.setVisibility(View.VISIBLE);
        if (errorState.findViewById(R.id.error_message) != null) {
            // TODO: установить текст ошибки, если есть TextView
        }
    }
    
    /**
     * Показать всплывающее сообщение об ошибке
     */
    private void showErrorToast(String errorMessage) {
        Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show();
    }
} 