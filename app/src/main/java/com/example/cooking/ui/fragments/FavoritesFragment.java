package com.example.cooking.ui.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.cooking.ui.activities.MainActivity;
import com.example.cooking.utils.MySharedPreferences;
import com.example.cooking.R;
import com.example.cooking.Recipe.Recipe;
import com.example.cooking.ui.adapters.RecipeListAdapter;
import com.example.cooking.ui.viewmodels.FavoritesViewModel;
import com.google.android.material.progressindicator.CircularProgressIndicator;


import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
    
    private String userId;
    
    private FavoritesViewModel viewModel;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        MySharedPreferences preferences = new MySharedPreferences(requireContext());
        userId = preferences.getString("userId", "0");
        
        viewModel = new ViewModelProvider(this).get(FavoritesViewModel.class);
        
        if (!viewModel.isUserLoggedIn()) {
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
        
        if (getActivity() != null && viewModel.isUserLoggedIn()) {
            viewModel.observeLikeChanges(getViewLifecycleOwner(), getActivity());
        }

        return view;
    }
    
    private void setupRecyclerView() {
        adapter = new RecipeListAdapter(this);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
    }
    
    private void setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener(() -> {
            Log.d(TAG, "Swipe to refresh triggered.");
            viewModel.refreshLikedRecipes();
        });
    }
    
    /**
     * Настраивает наблюдение за данными из ViewModel
     */
    private void observeViewModel() {
        viewModel.getFilteredLikedRecipes().observe(getViewLifecycleOwner(), recipes -> {
            Log.d(TAG, "Observer received update: " + (recipes != null ? recipes.size() : "null") + " recipes");
            swipeRefreshLayout.setRefreshing(false);
            hideLoading();
            if (recipes != null) {
                updateRecipesList(recipes);
            } else {
                Log.w(TAG, "Received null recipe list from observer.");
                updateRecipesList(new ArrayList<>());
            }
        });
        
        viewModel.getIsRefreshing().observe(getViewLifecycleOwner(), isRefreshing -> {
             Log.d(TAG, "Observer received isRefreshing update: " + isRefreshing);
             if (isRefreshing && (adapter == null || adapter.getItemCount() == 0)) {
                 showLoading();
             }
             swipeRefreshLayout.setRefreshing(isRefreshing);
        });
        
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
             Log.d(TAG, "Observer received error message: " + error);
             swipeRefreshLayout.setRefreshing(false);
             hideLoading();
             if (error != null && !error.isEmpty()) {
                 if (adapter == null || adapter.getItemCount() == 0) {
                    showErrorState(error);
                 } else {
                    hideErrorState();
                 }
             } else {
                 hideErrorState();
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
        Log.d(TAG, "Showing error state: " + message);
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
        String currentUserId = new MySharedPreferences(requireContext()).getString("userId", "0");
        if (!Objects.equals(userId, currentUserId)) {
            userId = currentUserId;
            viewModel.updateUser(userId);
        } else if (viewModel.isUserLoggedIn()) {
             Log.d(TAG, "onResume: User is logged in.");
        }
    }
    
    @Override
    public void onRecipeLike(Recipe recipe, boolean isLiked) {
        Log.d(TAG, "Recipe like status changed: " + recipe.getTitle() + ", isLiked: " + isLiked);
        if (!viewModel.isUserLoggedIn()) {
            Toast.makeText(getContext(), "Войдите, чтобы изменить статус лайка", Toast.LENGTH_SHORT).show();
            // Обновляем UI, чтобы отразить старое состояние
            adapter.notifyDataSetChanged();
            return;
        }
        viewModel.toggleLikeStatus(recipe, isLiked);
    }
    
    /**
     * Метод для возможного обновления данных извне (например, после логина).
     * Вызывает соответствующий метод ViewModel.
     */
    public void refreshData() {
        if (viewModel != null) {
             Log.d(TAG, "refreshData called.");
             String currentUserId = new MySharedPreferences(requireContext()).getString("userId", "0");
              if (!Objects.equals(userId, currentUserId)) {
                 userId = currentUserId;
                 viewModel.updateUser(userId);
             } else {
                 viewModel.refreshLikedRecipes();
             }
        }
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "onDestroyView called.");
    }
}