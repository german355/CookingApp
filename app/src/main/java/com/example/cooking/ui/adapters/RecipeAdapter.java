package com.example.cooking.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cooking.R;
import com.example.cooking.Recipe.Recipe;
import com.squareup.picasso.Picasso;

import java.util.List;

/**
 * Адаптер для отображения списка рецептов в RecyclerView
 */
public class RecipeAdapter extends RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder> {

    private List<Recipe> recipes;
    private final OnRecipeClickListener listener;

    /**
     * Интерфейс для обработки нажатий на рецепт
     */
    public interface OnRecipeClickListener {
        void onRecipeClick(Recipe recipe);
    }

    /**
     * Конструктор адаптера
     * recipes список рецептов
     *  listener слушатель кликов
     */
    public RecipeAdapter(List<Recipe> recipes, OnRecipeClickListener listener) {
        this.recipes = recipes;
        this.listener = listener;
    }

    /**
     * Обновляет список рецептов и уведомляет об изменениях
     * recipes новый список рецептов
     */
    public void updateRecipes(List<Recipe> recipes) {
        this.recipes = recipes;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecipeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recipe, parent, false);
        return new RecipeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecipeViewHolder holder, int position) {
        Recipe recipe = recipes.get(position);
        holder.bind(recipe, listener);
    }

    @Override
    public int getItemCount() {
        return recipes != null ? recipes.size() : 0;
    }

    /**
     * ViewHolder для рецепта
     */
    public static class RecipeViewHolder extends RecyclerView.ViewHolder {
        private final TextView titleTextView;
        private final ImageView recipeImageView;
        private final TextView ingredientsCountTextView;

        public RecipeViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.recipe_title);
            recipeImageView = itemView.findViewById(R.id.recipe_image);
            ingredientsCountTextView = itemView.findViewById(R.id.ingredients_count);
        }

        /**
         * Привязывает данные рецепта к элементам интерфейса
         *  recipe рецепт для отображения
         *  listener слушатель кликов
         */
        public void bind(Recipe recipe, OnRecipeClickListener listener) {
            titleTextView.setText(recipe.getTitle());
            
            // Отображение количества ингредиентов
            int ingredientsCount = recipe.getIngredients() != null ? recipe.getIngredients().size() : 0;
            ingredientsCountTextView.setText(String.format("Ингредиентов: %d", ingredientsCount));
            
            // Загрузка изображения, если есть URL
            if (recipe.getPhoto_url() != null && !recipe.getPhoto_url().isEmpty()) {
                Picasso.get()
                        .load(recipe.getPhoto_url())
                        .placeholder(R.drawable.placeholder_food)
                        .error(R.drawable.placeholder_food)
                        .into(recipeImageView);
            } else {
                recipeImageView.setImageResource(R.drawable.placeholder_food);
            }
            
            // Установка слушателя кликов
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onRecipeClick(recipe);
                }
            });
        }
    }
} 