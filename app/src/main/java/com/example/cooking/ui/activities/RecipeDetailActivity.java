package com.example.cooking.ui.activities;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.cooking.domain.entities.Ingredient;
import com.example.cooking.domain.entities.Step;
import com.example.cooking.R;
import com.example.cooking.domain.entities.Recipe;
import com.example.cooking.ui.adapters.Recipe.StepAdapter;
import com.example.cooking.ui.adapters.Recipe.IngredientViewAdapter;
import com.example.cooking.ui.viewmodels.Recipe.RecipeDetailViewModel;
import com.example.cooking.ui.viewmodels.Recipe.RecipeDetailUIState;
import com.example.cooking.utils.ThemeUtils;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.ArrayList;
import java.util.List;

/**
 * Активность для отображения подробной информации о рецепте.
 * Показывает полное описание, ингредиенты и инструкцию.
 */
public class RecipeDetailActivity extends AppCompatActivity {

    public static final String EXTRA_SELECTED_RECIPE = "SELECTED_RECIPE";
    private static final String TAG = "RecipeDetailActivity";
    private static final int EDIT_RECIPE_REQUEST = 1001;
    
    // UI-компоненты
    private FloatingActionButton fabLike;
    private TextView titleTextView;
    private ShapeableImageView recipeImageView;
    private Button decreasePortionButton;
    private Button increasePortionButton;
    private TextView portionCountTextView;
    private RecyclerView stepsRecyclerView;
    private StepAdapter stepAdapter;
    private RecyclerView ingredientsRecyclerView;
    private IngredientViewAdapter ingredientAdapter;
    
    // ViewModel
    private RecipeDetailViewModel viewModel;
    private int recipeId;
    

    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Применяем тему и устанавливаем layout
        SharedPreferences sharedPreferences = getSharedPreferences("acs", MODE_PRIVATE);
        ThemeUtils.applyTheme(sharedPreferences.getString("theme", "system"));
        setContentView(R.layout.activity_recipe_detail);

        // Получаем рецепт из Intent
        Recipe currentRecipe = getIntent().getParcelableExtra(EXTRA_SELECTED_RECIPE);
        if (currentRecipe == null) {
            Toast.makeText(this, "Ошибка: Не удалось загрузить данные рецепта.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        setResult(Activity.RESULT_OK);
        recipeId = currentRecipe.getId();

        // Инициализация
        initializeUI();
        initializeViewModel();
        displayInitialData(currentRecipe);
    }
    
    /**
     * Инициализирует весь UI: toolbar, views, RecyclerViews и обработчики
     */
    private void initializeUI() {
        // Настройка toolbar
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        toolbar.setNavigationOnClickListener(v -> finish());
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
        }
        
        // Инициализация UI компонентов
        titleTextView = findViewById(R.id.recipe_title);
        recipeImageView = findViewById(R.id.recipe_image);
        fabLike = findViewById(R.id.like_button);
        decreasePortionButton = findViewById(R.id.decrease_portion);
        increasePortionButton = findViewById(R.id.increase_portion);
        portionCountTextView = findViewById(R.id.portion_count);
        stepsRecyclerView = findViewById(R.id.steps_recyclerview);
        ingredientsRecyclerView = findViewById(R.id.ingredients_recyclerview);
        
        // Настройка RecyclerViews
        setupRecyclerView(stepsRecyclerView, true);
        setupRecyclerView(ingredientsRecyclerView, false);
        
        stepAdapter = new StepAdapter(this);
        stepsRecyclerView.setAdapter(stepAdapter);
        
        ingredientAdapter = new IngredientViewAdapter(this, new ArrayList<>());
        ingredientsRecyclerView.setAdapter(ingredientAdapter);
        
        // Обработчики событий
        fabLike.setOnClickListener(v -> {
            if (!com.example.cooking.network.services.UserService.isUserLoggedIn()) {
                Toast.makeText(this, "Войдите в аккаунт, чтобы поставить лайк", Toast.LENGTH_LONG).show();
            } else {
                viewModel.toggleLike();
            }
        });
        
        decreasePortionButton.setOnClickListener(v -> viewModel.decrementPortion());
        increasePortionButton.setOnClickListener(v -> viewModel.incrementPortion());
    }
    
    /**
     * Универсальная настройка RecyclerView
     */
    private void setupRecyclerView(RecyclerView recyclerView, boolean hasFixedSize) {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setNestedScrollingEnabled(false);
        recyclerView.setHasFixedSize(hasFixedSize);
    }
    
    /**
     * Инициализирует ViewModel
     */
    private void initializeViewModel() {
        viewModel = new ViewModelProvider(this).get(RecipeDetailViewModel.class);
        
        com.example.cooking.utils.MySharedPreferences preferences = new com.example.cooking.utils.MySharedPreferences(this);
        viewModel.init(recipeId, preferences.getInt("permission", 1));
        viewModel.getUIState().observe(this, this::renderUIState);
    }
    
    /**
     * Отображает начальные данные из Intent
     */
    private void displayInitialData(Recipe recipe) {
        if (recipe != null) {
            titleTextView.setText(recipe.getTitle());
            updateLikeButton(recipe.isLiked());
            loadImage(recipe.getPhoto_url());
            
            // Отображаем ингредиенты и шаги
            if (recipe.getIngredients() != null) {
                ingredientAdapter.updateIngredients(recipe.getIngredients());
            }
            if (recipe.getSteps() != null) {
                stepAdapter.submitList(recipe.getSteps());
            }
        }
    }
    
    /**
     * Обновляет UI на основе состояния ViewModel
     */
    private void renderUIState(RecipeDetailUIState state) {
        if (state == null) return;
        
        // Обновляем рецепт
        if (state.hasRecipe()) {
            updateRecipeUI(state);
        }
        
        // Обновляем статусы
        updateLikeButton(state.isLiked());
        portionCountTextView.setText(String.valueOf(state.getPortionCount()));
        
        // Обрабатываем состояния
        if (state.hasError()) {
            Toast.makeText(this, state.getErrorMessage(), Toast.LENGTH_LONG).show();
            viewModel.clearError();
        }
        
        if (state.isDeleteSuccess()) {
            Toast.makeText(this, "Рецепт успешно удален", Toast.LENGTH_SHORT).show();
            finish();
        }
        
        invalidateOptionsMenu();
    }
    
    /**
     * Обновляет UI элементы рецепта
     */
    private void updateRecipeUI(RecipeDetailUIState state) {
        Recipe recipe = state.getRecipe();
        
        // Обновляем заголовок только если изменился
        if (!titleTextView.getText().toString().equals(recipe.getTitle())) {
            titleTextView.setText(recipe.getTitle());
        }
        
        loadImage(state.getRecipeImageUrl());
        
        // Обновляем адаптеры
        if (state.getIngredients() != null) {
            ingredientAdapter.updateIngredients(state.getIngredients());
            ingredientAdapter.updatePortionCount(state.getPortionCount());
        }
        
        if (state.getSteps() != null) {
            stepAdapter.submitList(state.getSteps());
        }
    }
    
    /**
     * Загружает изображение в ImageView
     */
    private void loadImage(String imageUrl) {
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(this)
                 .load(imageUrl)
                 .placeholder(R.drawable.placeholder_image)
                 .error(R.drawable.error_image)
                 .into(recipeImageView);
        } else {
            recipeImageView.setImageResource(R.drawable.default_recipe_image);
        }
    }
    
    /**
     * Обновляет внешний вид кнопки лайка
     */
    private void updateLikeButton(boolean isLiked) {
        if (isLiked) {
            fabLike.setImageResource(R.drawable.ic_favorite);
        } else {
            fabLike.setImageResource(R.drawable.ic_favorite_border);
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_recipe_detail, menu);
        return true;
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        
        RecipeDetailUIState state = viewModel.getUIState().getValue();
        if (state != null) {
            setMenuItemVisible(menu, R.id.action_edit, state.canEdit());
            setMenuItemVisible(menu, R.id.action_delete, state.canDelete());
        }
        
        return true;
    }
    
    private void setMenuItemVisible(Menu menu, int itemId, boolean visible) {
        MenuItem item = menu.findItem(itemId);
        if (item != null) {
            item.setVisible(visible);
        }
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        RecipeDetailUIState state = viewModel.getUIState().getValue();
        if (state == null || !state.hasRecipe()) {
            return super.onOptionsItemSelected(item);
        }
        
        int id = item.getItemId();
        if (id == R.id.action_share) {
            shareRecipe(state.getRecipe());
        } else if (id == R.id.action_edit) {
            editRecipe(state.getRecipe());
        } else if (id == R.id.action_delete) {
            showDeleteDialog();
        } else {
            return super.onOptionsItemSelected(item);
        }
        
        return true;
    }
    
    /**
     * Показывает диалог подтверждения удаления
     */
    private void showDeleteDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Удаление рецепта")
                .setMessage("Вы уверены, что хотите удалить рецепт? Это действие нельзя отменить.")
                .setPositiveButton("Удалить", (dialog, which) -> viewModel.deleteRecipe())
                .setNegativeButton("Отмена", null)
                .show();
    }
    
    /**
     * Делится рецептом через другие приложения
     */
    private void shareRecipe(Recipe recipe) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        String shareBody = "Посмотри рецепт: " + recipe.getTitle() + "\nПриложение для кулинарных рецептов";
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Рецепт: " + recipe.getTitle());
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
        startActivity(Intent.createChooser(shareIntent, "Поделиться рецептом через"));
    }
    
    /**
     * Открывает активность редактирования рецепта
     */
    private void editRecipe(Recipe recipe) {
        Intent intent = new Intent(this, EditRecipeActivity.class);
        intent.putExtra(EditRecipeActivity.EXTRA_EDIT_RECIPE, recipe);
        startActivityForResult(intent, EDIT_RECIPE_REQUEST);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == EDIT_RECIPE_REQUEST && resultCode == RESULT_OK) {
            Toast.makeText(this, "Рецепт успешно обновлен", Toast.LENGTH_SHORT).show();
            // Перезагружаем рецепт через ViewModel
            viewModel.refreshRecipe();
        }
    }

    @Override
    public void onBackPressed() {
        setResult(Activity.RESULT_OK);
        super.onBackPressed();
    }

    @Override
    public boolean onSupportNavigateUp() {
        setResult(Activity.RESULT_OK);
        finish();
        return true;
    }
    

}