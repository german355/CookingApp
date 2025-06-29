package com.example.cooking.ui.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.example.cooking.R;
import com.example.cooking.domain.entities.Recipe;
import com.example.cooking.network.utils.Resource;
import com.example.cooking.ui.adapters.Recipe.RecipeListAdapter;
import com.example.cooking.ui.viewmodels.HomeViewModel;
import com.example.cooking.utils.MySharedPreferences;

import java.util.List;

/**
 * Фрагмент главного экрана.
 * Отображает сетку рецептов в виде карточек.
 */
public class HomeFragment extends Fragment implements RecipeListAdapter.OnRecipeLikeListener {
    private static final String TAG = "HomeFragment";
    
    private RecyclerView recyclerView;
    private RecipeListAdapter adapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private View emptyView;
    
    private HomeViewModel homeViewModel;
    
    /**
     * Создает и настраивает представление фрагмента.
     * Инициализирует RecyclerView и загружает рецепты.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        
        // Получаем ID пользователя и SharedPreferences
        MySharedPreferences preferences = new MySharedPreferences(requireContext());
        String userId = preferences.getString("userId", "0");

        // Показ Toast с подсказкой при первом запуске приложения
        boolean hintShown = preferences.getBoolean("swipe_hint_shown", false);
        if (!hintShown) {
            Toast.makeText(requireContext(), "Сделайте свайп вверх для получения рецептов", Toast.LENGTH_LONG).show();
            preferences.putBoolean("swipe_hint_shown", true);
        }
        
        // Инициализация ViewModel, привязанной к жизненному циклу фрагмента
        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        
        // Инициализация views
        recyclerView = view.findViewById(R.id.recycler_view);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh);
        emptyView = view.findViewById(R.id.empty_view);
        
        // Настройка RecyclerView
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 2);
        recyclerView.setLayoutManager(layoutManager);
        
        // Оптимизации для производительности
        recyclerView.setHasFixedSize(true); // Размер RecyclerView не изменяется
        recyclerView.setItemViewCacheSize(20); // Увеличиваем кэш View
        recyclerView.setDrawingCacheEnabled(true);
        recyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
        
        // Инициализируем адаптер
        adapter = new RecipeListAdapter(this);
        recyclerView.setAdapter(adapter);
        
        // Настраиваем swipe-to-refresh
        swipeRefreshLayout.setOnRefreshListener(() -> homeViewModel.refreshRecipes());
        
        // Подписываемся на LiveData из HomeViewModel
        setupObservers();
        
        return view;
    }
    
    private void setupObservers() {
        // Наблюдение за основным списком рецептов
        homeViewModel.recipes.observe(getViewLifecycleOwner(), resource -> {
            Log.d(TAG, "setupObservers: Получен Resource: " + (resource != null ? resource.getStatus() : "null"));
            if (resource == null) {
                Log.w(TAG, "setupObservers: resource is null");
                return;
            }

            Log.d(TAG, "setupObservers: Status = " + resource.getStatus() + 
                      ", Data size = " + (resource.getData() != null ? resource.getData().size() : "null") +
                      ", Message = " + resource.getMessage());

            // Управляем видимостью ProgressBar
            swipeRefreshLayout.setRefreshing(resource.getStatus() == Resource.Status.LOADING);
            
            if (resource.getStatus() == Resource.Status.SUCCESS) {
                List<Recipe> recipes = resource.getData();
                Log.d(TAG, "setupObservers: SUCCESS - обновляем adapter с " + (recipes != null ? recipes.size() : 0) + " рецептами");
                if (recipes != null && !recipes.isEmpty()) {
                    Log.d(TAG, "setupObservers: Первый рецепт: " + recipes.get(0).getTitle());
                }
                adapter.submitList(recipes);
                showEmptyView(recipes == null || recipes.isEmpty());
            } else if (resource.getStatus() == Resource.Status.ERROR) {
                Log.e(TAG, "setupObservers: ERROR - " + resource.getMessage());
                // Показываем ошибку и пустой вид, если данных нет
                if (resource.getData() == null || resource.getData().isEmpty()) {
                    showEmptyView(true);
                    showErrorMessage(resource.getMessage());
                }
            } else if (resource.getStatus() == Resource.Status.LOADING) {
                Log.d(TAG, "setupObservers: LOADING...");
            }
        });
            
        // Наблюдение за ошибками (для Toast/Snackbar)
        homeViewModel.errorMessage.observe(getViewLifecycleOwner(), this::showErrorMessage);
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
     * Обработка нажатия на кнопку лайка
     */
    @Override
    public void onRecipeLike(Recipe recipe, boolean isLiked) {
        if (recipe != null) {
            homeViewModel.updateLikeStatus(recipe, isLiked);
        }
    }
    
    /**
     * Показывает или скрывает сообщение об отсутствии рецептов
     */
    private void showEmptyView(boolean show) {
        emptyView.setVisibility(show ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void showErrorMessage(String message) {
        //Toast.makeText(getContext(), "Ой, что-то пошло не так", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Убрали автоматическое обновление, чтобы не было запроса на сервер
    }
    
    @Override
    public void onPause() {
        super.onPause();
    }
    
    /**
     * Выполняет поиск рецептов
     * @param query строка поиска
     */
    public void performSearch(String query) {
        Log.d(TAG, "HomeFragment: выполняю поиск: '" + query + "'");
        homeViewModel.performSearch(query);
    }
    
    /**
     * Сбрасывает поиск и возвращается к показу всех рецептов
     */
    public void clearSearch() {
        Log.d(TAG, "HomeFragment: сбрасываю поиск");
        homeViewModel.performSearch("");
    }
}