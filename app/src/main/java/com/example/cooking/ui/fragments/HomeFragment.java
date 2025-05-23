package com.example.cooking.ui.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import android.content.Intent;
import android.widget.Toast;

import com.example.cooking.ui.activities.MainActivity;
import com.example.cooking.utils.MySharedPreferences;
import com.example.cooking.R;
import com.example.cooking.Recipe.Recipe;
import com.example.cooking.ui.adapters.RecipeListAdapter;
import com.example.cooking.ui.activities.AddRecipeActivity;
import com.example.cooking.utils.RecipeSearchService;
import com.example.cooking.ui.viewmodels.HomeViewModel;
import com.example.cooking.ui.viewmodels.SharedRecipeViewModel;
import com.example.cooking.network.utils.Resource;

import android.widget.SearchView;

/**
 * Фрагмент главного экрана.
 * Отображает сетку рецептов в виде карточек.
 */
public class HomeFragment extends Fragment implements RecipeListAdapter.OnRecipeLikeListener {
    private static final String TAG = "HomeFragment";
    
    private RecyclerView recyclerView;
    private RecipeListAdapter adapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar progressBar;
    private TextView emptyView;
    private MySharedPreferences preferences;
    private String userId;
    
    // Заменяем HomeViewModel на SharedRecipeViewModel
    private SharedRecipeViewModel sharedViewModel;
    
    // Добавляем поля для автоматического обновления
    private Handler autoRefreshHandler;
    private Runnable autoRefreshRunnable;
    private static final long AUTO_REFRESH_INTERVAL = 30000; // 30 секунд
    private boolean autoRefreshEnabled = true;
    
    // Добавляем флаг для отслеживания инициированного пользователем обновления
    private boolean userInitiatedRefresh = false;
    
    // Сохранение состояния RecyclerView
    private static final String KEY_RECYCLER_STATE = "recycler_state";
    private RecyclerView.LayoutManager layoutManager;
    
    /**
     * Создает и настраивает представление фрагмента.
     * Инициализирует RecyclerView и загружает рецепты.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        
        // Получаем ID пользователя
        preferences = new MySharedPreferences(requireContext());
        userId = preferences.getString("userId", "0");
        
        // Инициализация SharedViewModel из Activity
        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedRecipeViewModel.class);
        
        // Инициализация views
        recyclerView = view.findViewById(R.id.recycler_view);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh);
        progressBar = view.findViewById(R.id.progress_bar);
        emptyView = view.findViewById(R.id.empty_view);
        
        // Настройка RecyclerView
        layoutManager = new GridLayoutManager(getContext(), 2);
        recyclerView.setLayoutManager(layoutManager);
        
        // Инициализируем адаптер
        adapter = new RecipeListAdapter(this);
        recyclerView.setAdapter(adapter);
        
        // Восстановление состояния RecyclerView
        if (savedInstanceState != null) {
            RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
            if (layoutManager != null) {
                layoutManager.onRestoreInstanceState(savedInstanceState.getParcelable(KEY_RECYCLER_STATE));
            }
        }
        
        // Настраиваем swipe-to-refresh
        swipeRefreshLayout.setOnRefreshListener(() -> {
            userInitiatedRefresh = true;
            sharedViewModel.refreshRecipes();
        });
        
        // Наблюдаем за данными из SharedViewModel
        observeViewModel();
        
        // Наблюдаем за результатами поиска
        sharedViewModel.getSearchResults().observe(getViewLifecycleOwner(), recipes -> {
            if (recipes != null) {
                adapter.submitList(recipes);
                showEmptyView(recipes.isEmpty());
            }
        });
        
        // Инициализируем обработчик для автоматического обновления
        autoRefreshHandler = new Handler(Looper.getMainLooper());
        autoRefreshRunnable = () -> {
            if (autoRefreshEnabled) {
                Log.d(TAG, "Выполняется автоматическое обновление рецептов");
                userInitiatedRefresh = false; // Это не пользовательское обновление
                sharedViewModel.refreshRecipes();
                // Запланировать следующее обновление
                autoRefreshHandler.postDelayed(autoRefreshRunnable, AUTO_REFRESH_INTERVAL);
            }
        };
        
        // Запускаем автоматическое обновление
        startAutoRefresh();
        
        return view;
    }
    
    /**
     * Сохраняем состояние RecyclerView при уничтожении фрагмента
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (recyclerView != null && recyclerView.getLayoutManager() != null) {
            outState.putParcelable(KEY_RECYCLER_STATE, recyclerView.getLayoutManager().onSaveInstanceState());
        }
    }
    
    /**
     * Настраиваем наблюдение за LiveData из ViewModel
     */
    private void observeViewModel() {
        // Наблюдаем за списком рецептов
        sharedViewModel.getRecipes().observe(getViewLifecycleOwner(), resource -> {
            if (resource.isSuccess()) {
                List<Recipe> recipes = resource.getData();
                if (recipes != null && !recipes.isEmpty()) {
                    adapter.submitList(recipes);
                    showEmptyView(false);
                } else {
                    showEmptyView(true);
                }
            } else if (resource.isError()) {
                showErrorMessage(resource.getMessage());
                showEmptyView(true);
            }
        });
        
        // Наблюдаем за состоянием загрузки
        sharedViewModel.getIsRefreshing().observe(getViewLifecycleOwner(), isRefreshing -> {
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
                    if (adapter.getItemCount() == 0) {
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
        
        // Наблюдаем за сообщениями об ошибках
        sharedViewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                showErrorMessage(error);
            }
        });
    }
    
    /**
     * Обработка нажатия на кнопку лайка
     */
    @Override
    public void onRecipeLike(Recipe recipe, boolean isLiked) {
        if (recipe != null) {
            // Используем общую модель
            sharedViewModel.updateLikeStatus(recipe, isLiked);
        }
    }
    
    /**
     * Показывает или скрывает сообщение об отсутствии рецептов
     */
    private void showEmptyView(boolean show) {
        if (show) {
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
        }
    }
    
    /**
     * Выполняет поиск по заданному запросу через SharedViewModel
     */
    public void performSearch(String query) {
        // Метод вызывается из MainActivity
        userInitiatedRefresh = false; // Поиск не считаем инициированным через swipe refresh
        sharedViewModel.searchRecipes(query);
    }
    
    /**
     * Показывает сообщение об ошибке
     */
    private void showErrorMessage(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }

    /**
     * Запустить автоматическое обновление списка рецептов
     */
    private void startAutoRefresh() {
        // Отменяем предыдущий callback, если он был
        stopAutoRefresh();
        
        if (autoRefreshEnabled) {
            // Запускаем новый callback
            autoRefreshHandler.postDelayed(autoRefreshRunnable, AUTO_REFRESH_INTERVAL);
            Log.d(TAG, "Запущено автоматическое обновление рецептов");
        }
    }
    
    /**
     * Остановить автоматическое обновление списка рецептов
     */
    private void stopAutoRefresh() {
        autoRefreshHandler.removeCallbacks(autoRefreshRunnable);
        Log.d(TAG, "Остановлено автоматическое обновление рецептов");
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // Обновляем данные оптимальным способом - с сервера или локально
        // Обеспечиваем, что userInitiatedRefresh = false, чтобы автоматическое обновление
        // использовало правильный индикатор загрузки
        userInitiatedRefresh = false;
        sharedViewModel.refreshIfNeeded();
        startAutoRefresh();
    }
    
    @Override
    public void onPause() {
        super.onPause();
        stopAutoRefresh();
    }

    /**
     * Загружает начальные данные при первом открытии фрагмента
     */
    private void loadInitialData() {
        // Устанавливаем флаг в false, чтобы показать правильный индикатор загрузки
        userInitiatedRefresh = false;
        sharedViewModel.loadInitialRecipes();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // Загружаем начальные данные
        loadInitialData();
    }
}