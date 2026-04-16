package com.example.cooking.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.cooking.R;
import com.example.cooking.domain.entities.Recipe;
import com.example.cooking.ui.adapters.Recipe.RecipeListAdapter;
import com.example.cooking.ui.viewmodels.Recipe.MyRecipesViewModel;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.util.List;

public class MyRecipesFragment extends Fragment implements RecipeListAdapter.OnRecipeLikeListener {
    private MyRecipesViewModel viewModel;
    private RecipeListAdapter adapter;
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private CircularProgressIndicator progressIndicator;
    private TextView emptyView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_filtered_recipes, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(MyRecipesViewModel.class);

        recyclerView = view.findViewById(R.id.filtered_recipes_recycler_view);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_filtered);
        progressIndicator = view.findViewById(R.id.progress_bar_filtered);
        emptyView = view.findViewById(R.id.empty_view_filtered_recipes);

        adapter = new RecipeListAdapter(this);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        recyclerView.setAdapter(adapter);

        swipeRefreshLayout.setOnRefreshListener(() -> viewModel.loadMyRecipes());

        viewModel.getRecipes().observe(getViewLifecycleOwner(), this::renderRecipes);
        viewModel.getIsRefreshing().observe(getViewLifecycleOwner(), refreshing -> {
            swipeRefreshLayout.setRefreshing(Boolean.TRUE.equals(refreshing));
            progressIndicator.setVisibility(Boolean.TRUE.equals(refreshing) && adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
        });
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
            }
        });

        viewModel.loadMyRecipes();
    }

    private void renderRecipes(List<Recipe> recipes) {
        adapter.submitList(recipes);
        boolean isEmpty = recipes == null || recipes.isEmpty();
        emptyView.setText(R.string.my_recipes_empty);
        emptyView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onRecipeLike(Recipe recipe, boolean isLiked) {
        viewModel.toggleLikeStatus(recipe, isLiked);
    }
}
