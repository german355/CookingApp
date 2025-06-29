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
import com.example.cooking.domain.usecases.UserPermissionUseCase;
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

    public static final String EXTRA_SELECTED_RECIPE = "SELECTED_RECIPE"; // <-- Ключ для Parcelable
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
    
    // Данные рецепта
    private Recipe currentRecipe;
    private int recipeId;
    private String userId;
    private int currentPortionCount = 1;
    private List<Step> steps = new ArrayList<>();
    private List<Ingredient> ingredients = new ArrayList<>();
    
    // ViewModel и Use Cases
    private RecipeDetailViewModel viewModel;
    private UserPermissionUseCase userPermissionUseCase;
    
    /**
     * Инициализирует активность и заполняет её данными о рецепте
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Применяем сохраненную тему перед setContentView
        // Используем тот же файл SharedPreferences, что и в SettingsFragment и MyApplication
        SharedPreferences sharedPreferences = getSharedPreferences("acs", MODE_PRIVATE);
        String themeValue = sharedPreferences.getString("theme", "system"); // Используем тот же ключ и значение по умолчанию
        ThemeUtils.applyTheme(themeValue);

        setContentView(R.layout.activity_recipe_detail);

        // Настраиваем toolbar
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        
        // Настраиваем обработчик нажатия на кнопку "назад"
        toolbar.setNavigationOnClickListener(v -> finish());

        // Получаем данные о рецепте из Intent
        currentRecipe = getIntent().getParcelableExtra(EXTRA_SELECTED_RECIPE); // Используем константу

        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
        }

        // Проверяем, что данные получены
        if (currentRecipe == null) {
            Log.e(TAG, "Объект Recipe не найден в Intent extras. Ключ: " + EXTRA_SELECTED_RECIPE);
            Toast.makeText(this, "Ошибка: Не удалось загрузить данные рецепта.", Toast.LENGTH_LONG).show();
            finish(); // Закрываем активность, если данных нет
            return;
        }
        setResult(Activity.RESULT_OK);

        // Извлекаем данные из объекта Recipe
        recipeId = currentRecipe.getId();
        userId = currentRecipe.getUserId();
        // Списки берем напрямую из объекта
        this.ingredients = currentRecipe.getIngredients() != null ? new ArrayList<>(currentRecipe.getIngredients()) : new ArrayList<>();
        this.steps = currentRecipe.getSteps() != null ? new ArrayList<>(currentRecipe.getSteps()) : new ArrayList<>();

        // Логируем полученные данные для отладки
        Log.d(TAG, "Получен рецепт: ID = " + recipeId + ", Название = " + currentRecipe.getTitle());

        
        // Инициализируем UI-компоненты
        titleTextView = findViewById(R.id.recipe_title);
        recipeImageView = findViewById(R.id.recipe_image);
        fabLike = findViewById(R.id.like_button);
        decreasePortionButton = findViewById(R.id.decrease_portion);
        increasePortionButton = findViewById(R.id.increase_portion);
        portionCountTextView = findViewById(R.id.portion_count);
        stepsRecyclerView = findViewById(R.id.steps_recyclerview);
        ingredientsRecyclerView = findViewById(R.id.ingredients_recyclerview);
        
        // Настраиваем RecyclerView для шагов
        setupStepsRecyclerView();
        
        // Настраиваем RecyclerView для ингредиентов
        setupIngredientsRecyclerView();

        // Инициализируем ViewModel и Use Cases
        viewModel = new ViewModelProvider(this).get(RecipeDetailViewModel.class);
        userPermissionUseCase = new UserPermissionUseCase(getApplication());
        
        // Получаем уровень доступа пользователя
        com.example.cooking.utils.MySharedPreferences preferences = new com.example.cooking.utils.MySharedPreferences(this);
        int permissionLevel = preferences.getInt("permission", 1);
        
        // Вместо loadRecipe используем init 
        viewModel.init(recipeId, permissionLevel);
        
        // Настраиваем наблюдателей LiveData
        setupObservers();
        
        // Настраиваем обработчики событий
        setupEventListeners();

        // Первичное отображение данных из Intent (пока ViewModel загружает)
        Log.d(TAG, "onCreate: Установка начального UI...");
        if (titleTextView != null && currentRecipe != null && currentRecipe.getTitle() != null) {
            Log.d(TAG, "onCreate: Установка заголовка: " + currentRecipe.getTitle());
            titleTextView.setText(currentRecipe.getTitle());
        } else {
            Log.e(TAG, "onCreate: titleTextView is null или currentRecipe/title is null перед установкой заголовка");
            if (titleTextView != null) titleTextView.setText("Название не найдено");
        }
        updateLikeButton(currentRecipe.isLiked());
        if (recipeImageView != null && currentRecipe != null && currentRecipe.getPhoto_url() != null && !currentRecipe.getPhoto_url().isEmpty()) {
            Log.d(TAG, "onCreate: Загрузка изображения Glide: " + currentRecipe.getPhoto_url());
            Glide.with(this)
                 .load(currentRecipe.getPhoto_url())
                 .placeholder(R.drawable.placeholder_image)
                 .error(R.drawable.error_image)
                 .into(recipeImageView);
        } else {
            Log.w(TAG, "onCreate: Не удалось загрузить изображение Glide (ImageView null, Recipe null, URL null/пустой)");
             if(recipeImageView != null) recipeImageView.setImageResource(R.drawable.default_recipe_image); // Установим дефолтное изображение
        }
        updatePortionCount();
    }
    
    /**
     * Настраивает RecyclerView для шагов рецепта
     */
    private void setupStepsRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        stepsRecyclerView.setLayoutManager(layoutManager);
        stepsRecyclerView.setNestedScrollingEnabled(false);
        
        // Оптимизации производительности для RecyclerView
        stepsRecyclerView.setHasFixedSize(true);
        stepsRecyclerView.setItemViewCacheSize(10); // Увеличиваем кэш
        stepsRecyclerView.setDrawingCacheEnabled(true);
        stepsRecyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
        
        // Создаем адаптер
        stepAdapter = new StepAdapter(this); 
        stepsRecyclerView.setAdapter(stepAdapter);
        // Сразу передаем шаги, полученные из Intent
        stepAdapter.submitList(this.steps); 
        Log.d(TAG, "setupStepsRecyclerView: Передано шагов в адаптер: " + this.steps.size());
    }
    
    /**
     * Настраивает RecyclerView для ингредиентов
     */
    private void setupIngredientsRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        ingredientsRecyclerView.setLayoutManager(layoutManager);
        ingredientsRecyclerView.setNestedScrollingEnabled(false);
        
        // Оптимизации производительности для RecyclerView
        ingredientsRecyclerView.setHasFixedSize(true);
        ingredientsRecyclerView.setItemViewCacheSize(15); // Больше кэш для ингредиентов
        ingredientsRecyclerView.setDrawingCacheEnabled(true);
        ingredientsRecyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
        
        // Инициализируем адаптер списком, полученным из Parcelable
        ingredientAdapter = new IngredientViewAdapter(this, this.ingredients);
        ingredientsRecyclerView.setAdapter(ingredientAdapter);
    }
    
    /**
     * Настраивает обработчики событий
     */
    private void setupEventListeners() {
        // Настраиваем клик по кнопке "лайк"
        fabLike.setOnClickListener(v -> {
            if (!com.example.cooking.network.services.UserService.isUserLoggedIn()) {
                Toast.makeText(this, "Войдите в аккаунт, чтобы поставить лайк", Toast.LENGTH_LONG).show();
            } else {
                viewModel.toggleLike();
            }
        });
        
        // Настраиваем кнопки изменения порции
        decreasePortionButton.setOnClickListener(v -> {
            if (currentPortionCount > 1) {
                currentPortionCount--;
                updatePortionCount();
            }
        });
        
        increasePortionButton.setOnClickListener(v -> {
            currentPortionCount++;
            updatePortionCount();
        });
            }
    
    /**
     * Обновляет отображение количества порций
     */
    private void updatePortionCount() {
        portionCountTextView.setText(String.valueOf(currentPortionCount));
        // Эффективное обновление только при наличии адаптера
        if (ingredientAdapter != null) { 
            // Теперь используется DiffUtil вместо notifyDataSetChanged()
            ingredientAdapter.updatePortionCount(currentPortionCount);
        }
    }
    
    /**
     * Настраивает наблюдение за данными из ViewModel
     */
    private void setupObservers() {
        // Наблюдаем за данными рецепта из ViewModel
        viewModel.getRecipe().observe(this, recipeFromVm -> {
            if (recipeFromVm != null && (currentRecipe == null || !currentRecipe.equals(recipeFromVm))) {
                currentRecipe = recipeFromVm;
                updateUI(recipeFromVm);
            }
        });
        
        // Наблюдаем за статусом "лайк" - только если рецепт уже загружен
        viewModel.getIsLiked().observe(this, isLiked -> {
            if (isLiked != null && currentRecipe != null && currentRecipe.isLiked() != isLiked) {
                // Обновляем только кнопку лайка, не весь UI
                updateLikeButton(isLiked);
            }
        });

        
        // Наблюдаем за ошибками
        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
                viewModel.clearErrorMessage();
            }
        });
        
        // Наблюдаем за результатом удаления
        viewModel.getDeleteSuccess().observe(this, success -> {
            if (success != null && success) {
                Toast.makeText(this, "Рецепт успешно удален", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }
    
    /**
     * Обновляет UI элементы данными из рецепта (полученного из ViewModel)
     */
    private void updateUI(Recipe recipe) {
        if (recipe == null) {
            Log.w(TAG, "updateUI вызван с null Recipe объектом.");
            return;
        }
        Log.d(TAG, "updateUI: Обновление UI для рецепта: " + recipe.getTitle());
        
        if (titleTextView != null && recipe.getTitle() != null) {
             Log.d(TAG, "updateUI: Установка заголовка: " + recipe.getTitle());
            titleTextView.setText(recipe.getTitle());
        } else {
             Log.e(TAG, "updateUI: titleTextView is null или recipe.getTitle() is null");
             if (titleTextView != null) titleTextView.setText("Название не найдено");
        }
        updateLikeButton(recipe.isLiked());

        // Обновляем адаптеры новыми данными эффективно
        this.ingredients = recipe.getIngredients() != null ? new ArrayList<>(recipe.getIngredients()) : new ArrayList<>();
        if (ingredientAdapter != null) {
            // Используем DiffUtil для эффективного обновления
            ingredientAdapter.updateIngredients(this.ingredients);
            ingredientAdapter.updatePortionCount(currentPortionCount); 
        } else {
             Log.e(TAG, "updateUI: ingredientAdapter is null");
        }
        
        this.steps = recipe.getSteps() != null ? new ArrayList<>(recipe.getSteps()) : new ArrayList<>();
        if (stepAdapter != null) {
             stepAdapter.submitList(this.steps);
        } else {
             Log.e(TAG, "updateUI: stepAdapter is null");
        }

        if (recipeImageView != null && recipe.getPhoto_url() != null && !recipe.getPhoto_url().isEmpty()) {
             Log.d(TAG, "updateUI: Загрузка изображения Glide: " + recipe.getPhoto_url());
             Glide.with(this)
                  .load(recipe.getPhoto_url())
                  .placeholder(R.drawable.placeholder_image)
                  .error(R.drawable.error_image)
                  .into(recipeImageView);
        } else {
             Log.w(TAG, "updateUI: Не удалось загрузить изображение Glide (ImageView null, URL null/пустой)");
             if(recipeImageView != null) recipeImageView.setImageResource(R.drawable.default_recipe_image);
        }
        
        // Отложенное обновление меню для предотвращения блокировки UI
        findViewById(android.R.id.content).post(() -> {
            Log.d(TAG, "updateUI: Отложенное обновление меню");
            invalidateOptionsMenu(); 
        });
    }
    
    /**
     * Обновляет состояние кнопки лайка
     */
    private void updateLikeButton(boolean isLiked) {
        if (isLiked) {
            fabLike.setImageResource(R.drawable.ic_favorite); // Закрашенный лайк
        } else {
            fabLike.setImageResource(R.drawable.ic_favorite_border); // Пустой лайк
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(TAG, "onCreateOptionsMenu: Начало создания меню");
        getMenuInflater().inflate(R.menu.menu_recipe_detail, menu);
        
        // Получаем ID создателя рецепта
        String recipeCreatorId = currentRecipe != null ? currentRecipe.getUserId() : null;
        
        // Используем UserPermissionUseCase для проверки прав
        boolean canEdit = userPermissionUseCase.canEditRecipe(recipeCreatorId).hasPermission();
        boolean canDelete = userPermissionUseCase.canDeleteRecipe(recipeCreatorId).hasPermission();
        
        Log.d(TAG, "onCreateOptionsMenu: canEdit = " + canEdit + ", canDelete = " + canDelete);
        Log.d(TAG, "onCreateOptionsMenu: " + userPermissionUseCase.getUserPermissionInfo());

        // Находим пункты меню
        MenuItem moreItem = menu.findItem(R.id.action_more);
        Log.d(TAG, "onCreateOptionsMenu: moreItem найден? " + (moreItem != null));
        
        if (moreItem != null && moreItem.hasSubMenu()) {
            Log.d(TAG, "onCreateOptionsMenu: moreItem имеет субменю");
            Menu subMenu = moreItem.getSubMenu();
            
            MenuItem editItem = subMenu.findItem(R.id.action_edit);
            MenuItem deleteItem = subMenu.findItem(R.id.action_delete);

            Log.d(TAG, "onCreateOptionsMenu: editItem найден? " + (editItem != null));
            Log.d(TAG, "onCreateOptionsMenu: deleteItem найден? " + (deleteItem != null));

            // Управляем видимостью пунктов меню
            if (editItem != null) {
                editItem.setVisible(canEdit);
                Log.d(TAG, "onCreateOptionsMenu: editItem.setVisible(" + canEdit + ")");
            }
            if (deleteItem != null) {
                deleteItem.setVisible(canDelete);
                Log.d(TAG, "onCreateOptionsMenu: deleteItem.setVisible(" + canDelete + ")");
            }
        } else {
            Log.w(TAG, "onCreateOptionsMenu: moreItem is null или не имеет субменю");
        }

        Log.d(TAG, "onCreateOptionsMenu: Завершение создания меню");
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Recipe recipe = currentRecipe;
        if (recipe == null) {
            return super.onOptionsItemSelected(item);
        }
        
        int id = item.getItemId();
                                   
        if (id == R.id.action_share) {
            shareRecipe(recipe);
            return true;
        } else if (id == R.id.action_edit) {
            // Используем UserPermissionUseCase для проверки прав
            UserPermissionUseCase.PermissionResult editResult = userPermissionUseCase.canEditRecipe(recipe.getUserId());
            if (editResult.hasPermission()) {
                editRecipe(recipe);
            } else {
                Toast.makeText(this, editResult.getReason(), Toast.LENGTH_SHORT).show();
            }
            return true;
        } else if (id == R.id.action_delete) {
            // Используем UserPermissionUseCase для проверки прав
            UserPermissionUseCase.PermissionResult deleteResult = userPermissionUseCase.canDeleteRecipe(recipe.getUserId());
            if (deleteResult.hasPermission()) {
                showDeleteConfirmationDialog();
            } else {
                Toast.makeText(this, deleteResult.getReason(), Toast.LENGTH_SHORT).show();
            }
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }
    
    /**
     * Показывает диалог подтверждения удаления рецепта
     */
    private void showDeleteConfirmationDialog() {
        Recipe recipe = currentRecipe;
        if (recipe == null) return;
        
        new AlertDialog.Builder(this)
                .setTitle("Удаление рецепта")
                .setMessage("Вы уверены, что хотите удалить рецепт? Это действие нельзя отменить.")
                .setPositiveButton("Удалить", (dialog, which) -> {
                    // Вызываем метод удаления в ViewModel
                    viewModel.deleteRecipe();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }
    
    /**
     * Делится рецептом через другие приложения
     */
    private void shareRecipe(Recipe recipe) {
        if (recipe == null) return;

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        String shareBody = "Посмотри рецепт: " + recipe.getTitle() + "\nПодробнее: [ссылка на приложение или веб-версию]"; // TODO: Добавить ссылку
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Рецепт: " + recipe.getTitle());
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareBody);

        startActivity(Intent.createChooser(shareIntent, "Поделиться рецептом через"));
    }
    
    /**
     * Открывает активность редактирования рецепта
     */
    private void editRecipe(Recipe recipe) {
        if (recipe == null) return;
        Intent intent = new Intent(this, EditRecipeActivity.class);
        intent.putExtra(EditRecipeActivity.EXTRA_EDIT_RECIPE, recipe);
        startActivityForResult(intent, EDIT_RECIPE_REQUEST);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == EDIT_RECIPE_REQUEST && resultCode == RESULT_OK) {
            // Получаем свежий рецепт после редактирования
            Toast.makeText(this, "Рецепт успешно обновлен", Toast.LENGTH_SHORT).show();
            
            // Получаем уровень доступа пользователя
            com.example.cooking.utils.MySharedPreferences preferences = new com.example.cooking.utils.MySharedPreferences(this);
            int permissionLevel = preferences.getUserPermission();
            
            // Перезагружаем рецепт
            viewModel.init(recipeId, permissionLevel);
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