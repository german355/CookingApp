package com.example.cooking.ui.fragments;

import android.os.Bundle;
import android.os.Parcelable;
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
import androidx.recyclerview.widget.LinearLayoutManager;
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
    
    private HomeViewModel homeViewModel;
    private RecyclerView.LayoutManager layoutManager;
    private Parcelable recyclerViewState;
    // Флаг, что происходит восстановление состояния списка
    private boolean isRestoring = false;
    // Сохранённая позиция и смещение прокрутки
    private int savedScrollPosition = -1;
    private int savedScrollOffset = 0;
    
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
        
        // Инициализация HomeViewModel
        homeViewModel = new ViewModelProvider(requireActivity()).get(HomeViewModel.class);
        
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
            isRestoring = true;
            RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
            if (layoutManager != null) {
                layoutManager.onRestoreInstanceState(savedInstanceState.getParcelable("recycler_state"));
            }
        }
        
        // Настраиваем swipe-to-refresh
        swipeRefreshLayout.setOnRefreshListener(() -> homeViewModel.refreshRecipes());
        
        // Подписываемся на LiveData из HomeViewModel
        observeHomeViewModel(homeViewModel);
        
        return view;
    }
    
    /**
     * Сохраняем состояние RecyclerView при уничтожении фрагмента
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (recyclerView != null && recyclerView.getLayoutManager() != null) {
            outState.putParcelable("recycler_state", recyclerView.getLayoutManager().onSaveInstanceState());
        }
    }
    
    /**
     * Настраиваем наблюдение за LiveData из HomeViewModel
     */
    private void observeHomeViewModel(HomeViewModel viewModel) {
        viewModel.getRecipes().observe(getViewLifecycleOwner(), recipes -> {
            if (!viewModel.isInSearchMode()) {
                adapter.submitList(recipes);
                showEmptyView(recipes == null || recipes.isEmpty());
            } else {
                Log.d(TAG, "getRecipes observer: пропускаем обновление, так как в режиме поиска");
            }
        });
        viewModel.getIsRefreshing().observe(getViewLifecycleOwner(), isRefreshing -> {
            swipeRefreshLayout.setRefreshing(isRefreshing);
        });
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                showErrorMessage(error);
            }
        });
        
        // Наблюдаем за результатами поиска
        viewModel.getSearchResults().observe(getViewLifecycleOwner(), searchResults -> {
            swipeRefreshLayout.setRefreshing(false);
            if (searchResults != null && !searchResults.isEmpty()) {
                Log.d(TAG, "searchResultsObserver: получены результаты поиска: " + searchResults.size());
                Toast.makeText(requireContext(), "Найдено " + searchResults.size() + " рецептов", Toast.LENGTH_SHORT).show();

                // При обычном поиске прокручиваем в начало после обновления списка
                if (!isRestoring) {
                    adapter.submitList(searchResults, () -> {
                        recyclerView.scrollToPosition(0);
                        Log.d(TAG, "searchResultsObserver: прокрутка к началу списка (post-submit)");
                    });
                } else {
                    adapter.submitList(searchResults);
                    Log.d(TAG, "searchResultsObserver: пропускаем прокрутку, восстанавливаем позицию");
                    isRestoring = false;
                }
                showEmptyView(false);
            } else {
                Log.d(TAG, "searchResultsObserver: результаты поиска пусты или null");
                showEmptyView(true);
                swipeRefreshLayout.setRefreshing(false);
            }
        });
    }
    
    /**
     * Обработка нажатия на кнопку лайка
     */
    @Override
    public void onRecipeLike(Recipe recipe, boolean isLiked) {
        if (recipe != null) {
            homeViewModel.updateLikedRepositoryStatus(recipe.getId(), isLiked);
        }
    }
    
    /**
     * Показывает или скрывает сообщение об отсутствии рецептов
     */
    private void showEmptyView(boolean show) {
        Log.d(TAG, "showEmptyView: " + (show ? "показываю пустое представление" : "показываю список рецептов"));
        
        // Устанавливаем текст для пустого представления
        emptyView.setText("Рецепты не найдены");
        
        if (show) {
            Log.d(TAG, "Устанавливаю GONE для RecyclerView и VISIBLE для emptyView");
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
            // Показываем Toast для подтверждения отсутствия результатов
            Toast.makeText(requireContext(), "Рецепты не найдены", Toast.LENGTH_SHORT).show();
        } else {
            Log.d(TAG, "Устанавливаю VISIBLE для RecyclerView и GONE для emptyView");
            recyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
            
            // Проверка видимости после установки
            Log.d(TAG, "После установки: RecyclerView.visibility = " + 
                  (recyclerView.getVisibility() == View.VISIBLE ? "VISIBLE" : "GONE/INVISIBLE") + ", " + 
                  "emptyView.visibility = " + (emptyView.getVisibility() == View.VISIBLE ? "VISIBLE" : "GONE/INVISIBLE"));
            
            // Если после установки RecyclerView все еще не виден, принудительно устанавливаем VISIBLE
            if (recyclerView.getVisibility() != View.VISIBLE) {
                Log.d(TAG, "Принудительно устанавливаю VISIBLE для RecyclerView");
                recyclerView.setVisibility(View.VISIBLE);
            }
        }
    }
    
    /**
     * Показывает сообщение об ошибке
     */
    private void showErrorMessage(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Обновление списка рецептов при возврате к фрагменту,
        // но только если мы не в режиме поиска
        if (!homeViewModel.isInSearchMode()) {
            Log.d(TAG, "onResume: Обновляем список рецептов (не в режиме поиска)");
            homeViewModel.refreshRecipes();
        } else {
            Log.d(TAG, "onResume: Пропускаем обновление рецептов, так как мы в режиме поиска");
        }
    }
    
    @Override
    public void onPause() {
        super.onPause();
        if (recyclerView != null && recyclerView.getLayoutManager() instanceof LinearLayoutManager) {
            LinearLayoutManager lm = (LinearLayoutManager) recyclerView.getLayoutManager();
            savedScrollPosition = lm.findFirstVisibleItemPosition();
            View firstView = lm.findViewByPosition(savedScrollPosition);
            savedScrollOffset = (firstView != null) ? (firstView.getTop() - recyclerView.getPaddingTop()) : 0;
            Log.d(TAG, "onPause: savedScrollPosition=" + savedScrollPosition + ", offset=" + savedScrollOffset);
        }
        if (recyclerView != null && recyclerView.getLayoutManager() != null) {
            recyclerViewState = recyclerView.getLayoutManager().onSaveInstanceState();
            Log.d(TAG, "onPause: сохранено состояние RecyclerView");
        }
    }
    
    /**
     * Выполняет поиск рецептов
     * @param query строка поиска
     */
    public void performSearch(String query) {
        Log.d(TAG, "HomeFragment: выполняю поиск: '" + query + "'");
        
        // Показываем индикатор загрузки
        swipeRefreshLayout.setRefreshing(true);
        
        // Вызываем поиск в HomeViewModel
        homeViewModel.searchRecipes(query);
        
        // Логируем действие для отладки
        Log.d(TAG, "Запрос поиска отправлен в HomeViewModel");
    }
    
    /**
     * Восстанавливает последний поисковый запрос при возврате из деталей рецепта
     */
    public void restoreLastSearch() {
        Log.d(TAG, "restoreLastSearch: начинаем восстановление списка");
        isRestoring = true;
         
        if (homeViewModel.isInSearchMode() && homeViewModel.getLastSearchQuery() != null) {
            List<Recipe> lastResults = homeViewModel.getSearchResults().getValue();
            if (lastResults != null) {
                adapter.submitList(lastResults, () -> {
                    // Восстанавливаем позицию через сохранённые значения
                    if (savedScrollPosition >= 0 && recyclerView.getLayoutManager() instanceof LinearLayoutManager) {
                        ((LinearLayoutManager) recyclerView.getLayoutManager())
                            .scrollToPositionWithOffset(savedScrollPosition, savedScrollOffset);
                        Log.d(TAG, "restoreLastSearch: scrollToPositionWithOffset pos=" + savedScrollPosition + " offset=" + savedScrollOffset);
                    } else if (recyclerViewState != null) {
                        recyclerView.getLayoutManager().onRestoreInstanceState(recyclerViewState);
                        Log.d(TAG, "restoreLastSearch: положение списка восстановлено через state");
                    }
                    // Сброс флага и сохранённых значений
                    isRestoring = false;
                    savedScrollPosition = -1;
                    savedScrollOffset = 0;
                });
                showEmptyView(lastResults.isEmpty());
                Toast.makeText(requireContext(), "Восстановлены результаты поиска", Toast.LENGTH_SHORT).show();
            } else {
                Log.d(TAG, "restoreLastSearch: нет сохраненных результатов поиска");
                isRestoring = false;
            }
        } else {
            Log.d(TAG, "restoreLastSearch: нет активного поиска для восстановления");
            isRestoring = false;
        }
    }
}