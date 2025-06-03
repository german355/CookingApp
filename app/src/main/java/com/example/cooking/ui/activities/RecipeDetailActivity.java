package com.example.cooking.ui.activities;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.cooking.Recipe.Ingredient;
import com.example.cooking.Recipe.Step;
import com.example.cooking.R;
import com.example.cooking.Recipe.Recipe;
import com.example.cooking.ui.adapters.StepAdapter;
import com.example.cooking.ui.adapters.IngredientViewAdapter;
import com.example.cooking.ui.viewmodels.RecipeDetailViewModel;
import com.example.cooking.utils.ThemeUtils;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Активность для отображения подробной информации о рецепте.
 * Показывает полное описание, ингредиенты и инструкцию.
 */
public class RecipeDetailActivity extends AppCompatActivity {
    // Константы для передачи данных между активностями
    public static final String EXTRA_RECIPE_TITLE = "recipe_title";
    public static final String EXTRA_RECIPE_CREATOR = "recipe_creator";
    public static final String EXTRA_RECIPE_INSTRUCTOR = "recipe_instructor";
    public static final String EXTRA_RECIPE_FOOD = "recipe_food";
    public static final String EXTRA_RECIPE_PHOTO_URL = "photo_url";
    public static final String EXTRA_SELECTED_RECIPE = "SELECTED_RECIPE"; // <-- Ключ для Parcelable
    private static final String TAG = "RecipeDetailActivity";
    private static final int EDIT_RECIPE_REQUEST = 1001;
    
    // UI-компоненты
    private FloatingActionButton fabLike;
    private TextView titleTextView;
    private ShapeableImageView recipeImageView;
    private TextView createdAtTextView;
    private Button decreasePortionButton;
    private Button increasePortionButton;
    private TextView portionCountTextView;
    private Button toListButton;
    private Button toCartButton;
    private RecyclerView stepsRecyclerView;
    private StepAdapter stepAdapter;
    private RecyclerView ingredientsRecyclerView;
    private IngredientViewAdapter ingredientAdapter;
    
    // Данные рецепта
    private Recipe currentRecipe; // Будем хранить весь объект
    private int recipeId;
    private String userId;
    private int currentPortionCount = 1;
    private List<Step> steps = new ArrayList<>();
    private List<Ingredient> ingredients = new ArrayList<>();
    
    // ViewModel
    private RecipeDetailViewModel viewModel;
    
    /**
     * Инициализирует активность и заполняет её данными о рецепте
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Применяем сохраненную тему перед setContentView
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
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
        Log.d(TAG, "Кол-во ингредиентов: " + this.ingredients.size());
        Log.d(TAG, "Кол-во шагов: " + this.steps.size());
        Log.d(TAG, "Дата создания: " + currentRecipe.getCreated_at());
        Log.d(TAG, "ID пользователя: " + userId);
        Log.d(TAG, "URL фото: " + currentRecipe.getPhoto_url());
        Log.d(TAG, "Лайкнут: " + currentRecipe.isLiked());
        
        // Инициализируем UI-компоненты
        titleTextView = findViewById(R.id.recipe_title);
        recipeImageView = findViewById(R.id.recipe_image);
        createdAtTextView = findViewById(R.id.recipe_date);
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

        // Инициализируем и настраиваем ViewModel
        viewModel = new ViewModelProvider(this).get(RecipeDetailViewModel.class);
        
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
        createdAtTextView.setText(String.format(Locale.getDefault(), "Создано: %s", currentRecipe.getCreated_at()));
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
        // Создаем адаптер
        stepAdapter = new StepAdapter(this); 
        stepsRecyclerView.setAdapter(stepAdapter);
        // Сразу передаем шаги, полученные из Intent
        stepAdapter.submitList(this.steps); 
        Log.d(TAG, "setupStepsRecyclerView: Передано шагов в адаптер: " + this.steps.size()); // Добавим лог
    }
    
    /**
     * Настраивает RecyclerView для ингредиентов
     */
    private void setupIngredientsRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        ingredientsRecyclerView.setLayoutManager(layoutManager);
        ingredientsRecyclerView.setNestedScrollingEnabled(false);
        // Инициализируем адаптер списком, полученным из Parcelable
        ingredientAdapter = new IngredientViewAdapter(this, this.ingredients);
        ingredientsRecyclerView.setAdapter(ingredientAdapter);
    }
    
    /**
     * Настраивает обработчики событий
     */
    private void setupEventListeners() {
        // Настраиваем клик по кнопке "лайк"
        fabLike.setOnClickListener(v -> viewModel.toggleLike());
        
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
        // Убедимся, что адаптер не null перед вызовом
        if (ingredientAdapter != null) { 
            ingredientAdapter.updatePortionCount(currentPortionCount);
        }
    }
    
    /**
     * Настраивает наблюдение за данными из ViewModel
     */
    private void setupObservers() {
        // Наблюдаем за данными рецепта из ViewModel
        viewModel.getRecipe().observe(this, recipeFromVm -> {
            if (recipeFromVm != null) {
                currentRecipe = recipeFromVm;
                updateUI(recipeFromVm);
            }
        });
        
        // Наблюдаем за статусом "лайк"
        viewModel.getIsLiked().observe(this, this::updateLikeButton);
        
        // Наблюдаем за статусом индикатора загрузки
        viewModel.getIsLoading().observe(this, isLoading -> {
            // Здесь можно показать/скрыть индикатор загрузки
            if (isLoading) {
                // Показать прогрессбар или другой индикатор
            } else {
                // Скрыть индикатор
            }
        });
        
        // Наблюдаем за ошибками
        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(this, "Ой что-то пошло не так", Toast.LENGTH_LONG).show();
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
        createdAtTextView.setText(String.format(Locale.getDefault(), "Создано: %s", recipe.getCreated_at()));
        updateLikeButton(recipe.isLiked());

        // Обновляем адаптеры новыми данными
        this.ingredients = recipe.getIngredients() != null ? new ArrayList<>(recipe.getIngredients()) : new ArrayList<>();
        if (ingredientAdapter != null) {
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
             if(recipeImageView != null) recipeImageView.setImageResource(R.drawable.default_recipe_image); // Установим дефолтное изображение
        }
        invalidateOptionsMenu(); 
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
        getMenuInflater().inflate(R.menu.menu_recipe_detail, menu);
        // Получаем SharedPreferences для доступа к данным пользователя
        com.example.cooking.utils.MySharedPreferences prefs = new com.example.cooking.utils.MySharedPreferences(this);
        String currentUserId = prefs.getUserId();
        int permission = prefs.getUserPermission(); // 1 - обычный пользователь, 2 - админ

        // Получаем ID создателя рецепта
        String recipeCreatorId = currentRecipe != null ? currentRecipe.getUserId() : null;

        // Логирование для отладки
        Log.d(TAG, "onCreateOptionsMenu: currentUserId = " + currentUserId);
        Log.d(TAG, "onCreateOptionsMenu: permission = " + permission);
        Log.d(TAG, "onCreateOptionsMenu: recipeCreatorId = " + recipeCreatorId);

        // Проверяем, является ли текущий пользователь автором рецепта или администратором
        boolean isAuthor = currentUserId != null && recipeCreatorId != null && currentUserId.equals(recipeCreatorId);
        boolean isAdmin = permission == 2;

        // Логирование для отладки
        Log.d(TAG, "onCreateOptionsMenu: isAuthor = " + isAuthor);
        Log.d(TAG, "onCreateOptionsMenu: isAdmin = " + isAdmin);

        // Находим пункты меню
        MenuItem moreItem = menu.findItem(R.id.action_more);
        if (moreItem != null && moreItem.hasSubMenu()) {
            Menu subMenu = moreItem.getSubMenu();
            MenuItem editItem = subMenu.findItem(R.id.action_edit);
            MenuItem deleteItem = subMenu.findItem(R.id.action_delete);

            // Управляем видимостью пунктов меню
            if (editItem != null) {
                editItem.setVisible(isAuthor || isAdmin);
                Log.d(TAG, "onCreateOptionsMenu: editItem.isVisible() = " + editItem.isVisible());
            } else {
                Log.d(TAG, "onCreateOptionsMenu: editItem is null");
            }
            if (deleteItem != null) {
                deleteItem.setVisible(isAuthor || isAdmin);
                Log.d(TAG, "onCreateOptionsMenu: deleteItem.isVisible() = " + deleteItem.isVisible());
            } else {
                Log.d(TAG, "onCreateOptionsMenu: deleteItem is null");
            }
        } else {
            Log.d(TAG, "onCreateOptionsMenu: moreItem is null or has no SubMenu");
            // Если moreItem нет, то и edit/delete точно не будет. 
            // Убираем попытку найти старые ID, так как их больше нет и это вызывает ошибку компиляции.
        }

        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Recipe recipe = currentRecipe;
        if (recipe == null) {
            return super.onOptionsItemSelected(item);
        }
        
        int id = item.getItemId();
        
        // Получаем ID текущего пользователя и его права доступа
        com.example.cooking.utils.MySharedPreferences prefs = new com.example.cooking.utils.MySharedPreferences(this);
        String currentUserId = prefs.getUserId();
        int permissionLevel = prefs.getUserPermission();
        
        // Имеет право редактировать, если пользователь - автор рецепта или админ
        boolean hasEditPermission = (recipe.getUserId() != null && recipe.getUserId().equals(currentUserId)) 
                                   || permissionLevel == 2;
                                   
        if (id == R.id.action_share) {
            shareRecipe(recipe);
            return true;
        } else if (id == R.id.action_edit) {
            if (hasEditPermission) {
                editRecipe(recipe);
            } else {
                Toast.makeText(this, "У вас нет прав на редактирование этого рецепта", Toast.LENGTH_SHORT).show();
            }
            return true;
        } else if (id == R.id.action_delete) {
            if (hasEditPermission) {
                showDeleteConfirmationDialog();
            } else {
                Toast.makeText(this, "У вас нет прав на удаление этого рецепта", Toast.LENGTH_SHORT).show();
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
            // Используем RecipeDetailViewModel для перезагрузки
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