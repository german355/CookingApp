package com.example.cooking.ui.fragments;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.cooking.R;
import com.example.cooking.domain.entities.Recipe;
import com.example.cooking.ui.viewmodels.Recipe.FilteredRecipesViewModel;
import com.example.cooking.ui.adapters.Recipe.RecipeListAdapter;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.material.progressindicator.CircularProgressIndicator;

/**
 * Фрагмент для отображения списка отфильтрованных рецептов.
 * Получает параметры фильтрации через аргументы навигации.
 */
public class FilteredRecipesFragment extends Fragment implements RecipeListAdapter.OnRecipeLikeListener {

    private static final String TAG = "FilteredRecipesFragment";
    public static final String ARG_FILTER_KEY = "filter_key";
    public static final String ARG_FILTER_TYPE = "filter_type";

    private FilteredRecipesViewModel viewModel;
    private RecipeListAdapter adapter;
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private CircularProgressIndicator progressIndicator;
    private TextView emptyView;

    private String categoryName;
    private String filterKey;
    private String filterType;

    // Конструктор по умолчанию, обязателен для фрагментов
    public FilteredRecipesFragment() {}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(FilteredRecipesViewModel.class);
        // Получение аргументов, переданных при навигации
        if (getArguments() != null) {
            categoryName = getArguments().getString("categoryName");
            filterKey = getArguments().getString(ARG_FILTER_KEY);
            filterType = getArguments().getString(ARG_FILTER_TYPE);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_filtered_recipes, container, false);
        
        recyclerView = view.findViewById(R.id.filtered_recipes_recycler_view);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_filtered);
        progressIndicator = view.findViewById(R.id.progress_bar_filtered);
        emptyView = view.findViewById(R.id.empty_view_filtered_recipes);
        
        setupRecyclerView();
        setupObservers();

        // Устанавливаем заголовок ActionBar в название категории, если оно передано
        if (categoryName != null && !categoryName.isEmpty()) {
            androidx.appcompat.app.AppCompatActivity act = (androidx.appcompat.app.AppCompatActivity) requireActivity();
            if (act.getSupportActionBar() != null) {
                act.getSupportActionBar().setTitle(categoryName);
            }
        }

        // Pull-to-refresh
        swipeRefreshLayout.setOnRefreshListener(() -> {
            if (getArguments() != null) {
                String filterKey = getArguments().getString(ARG_FILTER_KEY);
                String filterType = getArguments().getString(ARG_FILTER_TYPE);
                viewModel.loadFilteredRecipes(filterKey, filterType);
            }
        });

        // Инициация фильтрации
        if (categoryName != null && filterKey != null && filterType != null) {
            viewModel.loadFilteredRecipes(filterKey, filterType);
        }

        return view;
    }

    private void setupRecyclerView() {
        adapter = new RecipeListAdapter(this);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        recyclerView.setAdapter(adapter);
    }

    private void setupObservers() {
        viewModel.filteredRecipes.observe(getViewLifecycleOwner(), recipes -> {
            adapter.submitList(recipes);
            showEmptyView(recipes == null || recipes.isEmpty());
        });

        viewModel.isRefreshing.observe(getViewLifecycleOwner(), isRefreshing -> {
            swipeRefreshLayout.setRefreshing(isRefreshing);
            progressIndicator.setVisibility(isRefreshing && adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
            emptyView.setVisibility(View.GONE);
        });

        viewModel.errorMessage.observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showEmptyView(boolean show) {
        if (emptyView != null) {
            emptyView.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (recyclerView != null) {
            recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    /**
     * Обрабатывает событие лайка/дизлайка рецепта из адаптера.
     * @param recipe Объект Recipe, статус лайка которого изменился.
     * @param isLiked Новое состояние лайка (true - лайкнут, false - дизлайкнут).
     */
    @Override
    public void onRecipeLike(Recipe recipe, boolean isLiked) {
        viewModel.toggleLikeStatus(recipe, isLiked);
    }
} 