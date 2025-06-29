package com.example.cooking.ui.fragments.favorite;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.cooking.ui.activities.MainActivity;
import com.example.cooking.auth.FirebaseAuthManager;
import com.example.cooking.R;
import com.example.cooking.domain.entities.Recipe;
import com.example.cooking.ui.adapters.Recipe.RecipeListAdapter;
import com.example.cooking.ui.viewmodels.FavoritesViewModel;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.util.List;


/**
 * Фрагмент для отображения избранных (лайкнутых) рецептов пользователя.
 */
public class FavoritesFragment extends Fragment implements RecipeListAdapter.OnRecipeLikeListener {
    private static final String TAG = "FavoritesFragment";
    
    private RecyclerView recyclerView;
    private RecipeListAdapter adapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView emptyView;
    private CircularProgressIndicator progressIndicator;
    private View emptyContainer; // Контейнер для EmptyFavoritesFragment
    
    private FavoritesViewModel viewModel;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        FirebaseAuthManager authManager = FirebaseAuthManager.getInstance();
        
        viewModel = new ViewModelProvider(this).get(FavoritesViewModel.class);

        
        if (!authManager.isUserSignedIn()) {
            View authBlockView = inflater.inflate(R.layout.fragment_auth_block, container, false);
            Button loginButton = authBlockView.findViewById(R.id.btn_login);
            
            loginButton.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).bottomNavigationView.setSelectedItemId(R.id.nav_profile);
                }
            });
            
            return authBlockView;
        }
        
        View view = inflater.inflate(R.layout.fragment_favorites, container, false);
        
        recyclerView = view.findViewById(R.id.recycler_view_favorites);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_favorites);
        emptyView = view.findViewById(R.id.empty_view_favorites);
        progressIndicator = view.findViewById(R.id.loading_view_favorites);
        emptyContainer = view.findViewById(R.id.empty_container_favorites);

        setupRecyclerView();
        setupSwipeRefresh();
        
        observeViewModel();
        
        return view;
    }
    
    private void setupRecyclerView() {
        adapter = new RecipeListAdapter(this);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
    }
    
    private void setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener(() -> {
            viewModel.refreshRecipes();
        });
    }
    
    /**
     * Настраивает наблюдение за данными из ViewModel
     */
    private void observeViewModel() {
        viewModel.favoriteRecipes.observe(getViewLifecycleOwner(), recipes -> {
            Log.d(TAG, "Получен обновленный список избранных: " + (recipes != null ? recipes.size() : "null"));
            if (recipes != null) {
                updateRecipesList(recipes);
            }
        });
        
        viewModel.isRefreshing.observe(getViewLifecycleOwner(), isRefreshing -> {
             swipeRefreshLayout.setRefreshing(isRefreshing);
        });
        
        viewModel.errorMessage.observe(getViewLifecycleOwner(), error -> {
             if (error != null && !error.isEmpty()) {
                 Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
             }
        });
    }
    
    /**
     * Обновляет список рецептов в UI и управляет видимостью заглушки.
     */
    private void updateRecipesList(List<Recipe> recipes) {
        adapter.submitList(recipes);
        if (recipes.isEmpty()) {
            showEmptyFavoritesFragment();
        } else {
            hideEmptyFavoritesFragment();
            recyclerView.setVisibility(View.VISIBLE);
        }
    }
    
    /**
     * Показывает фрагмент пустого состояния избранного (когда нет лайков ВООБЩЕ).
     */
    private void showEmptyFavoritesFragment() {
        Log.d(TAG, "Showing empty favorites fragment.");
        recyclerView.setVisibility(View.GONE);
        emptyView.setVisibility(View.GONE);
        if (emptyContainer != null) {
             emptyContainer.setVisibility(View.VISIBLE);
             if (getChildFragmentManager().findFragmentById(R.id.empty_container_favorites) == null) {
                 getChildFragmentManager().beginTransaction()
                         .replace(R.id.empty_container_favorites, new EmptyFavoritesFragment())
                         .commitAllowingStateLoss();
             }
        } else {
            Log.e(TAG, "Empty container view is null!");
            emptyView.setText(R.string.no_favorites_yet);
            emptyView.setVisibility(View.VISIBLE);
        }
        hideLoading();
        hideErrorState();
    }
    
    /**
     * Скрывает фрагмент пустого состояния избранного.
     */
    private void hideEmptyFavoritesFragment() {
        Log.d(TAG, "Hiding empty favorites fragment.");
        if (emptyContainer != null) {
            emptyContainer.setVisibility(View.GONE);
             Fragment emptyFragment = getChildFragmentManager().findFragmentById(R.id.empty_container_favorites);
            if (emptyFragment != null) {
                getChildFragmentManager().beginTransaction().remove(emptyFragment).commitAllowingStateLoss();
            }
        }
        recyclerView.setVisibility(View.VISIBLE);
    }
    
    /**
    * Показывает состояние ошибки (когда не удалось загрузить данные и список пуст).
    */
    private void showErrorState(String message) {
        recyclerView.setVisibility(View.GONE);
        if (emptyContainer != null) emptyContainer.setVisibility(View.GONE);
        emptyView.setText(message != null ? message : getString(R.string.error_loading_recipes));
        emptyView.setVisibility(View.VISIBLE);
        hideLoading();
    }
    
    /**
    * Скрывает состояние ошибки.
    */
    private void hideErrorState() {
        Log.d(TAG, "Hiding error state.");
        if (emptyView.getVisibility() == View.VISIBLE) {
             emptyView.setVisibility(View.GONE);
             recyclerView.setVisibility(View.VISIBLE);
        }
    }
    
    /**
     * Показывает индикатор загрузки.
     */
    private void showLoading() {
        Log.d(TAG, "Showing loading indicator.");
        if (progressIndicator != null && (adapter == null || adapter.getItemCount() == 0)) {
             recyclerView.setVisibility(View.GONE);
             if (emptyContainer != null) emptyContainer.setVisibility(View.GONE);
             emptyView.setVisibility(View.GONE);
             progressIndicator.setVisibility(View.VISIBLE);
        }
    }
    
    /**
     * Скрывает индикатор загрузки.
     */
    private void hideLoading() {
         Log.d(TAG, "Hiding loading indicator.");
        if (progressIndicator != null) {
            progressIndicator.setVisibility(View.GONE);
        }
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // Убираем автоматическое обновление при onResume, так как это может вызывать дублирование запросов
        // Данные обновляются автоматически через SharedRecipeViewModel
        // viewModel.onRefreshRequested();
    }
    
    @Override
    public void onRecipeLike(Recipe recipe, boolean isLiked) {
        // В избранном лайк можно только убрать
        if (!isLiked) {
            viewModel.toggleLikeStatus(recipe);
        }
    }
    
    /**
     * Метод для возможного обновления данных извне (например, после логина).
     * Вызывает соответствующий метод ViewModel.
     */
    public void refreshData() {
        viewModel.refreshRecipes();
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "onDestroyView called.");
    }
}