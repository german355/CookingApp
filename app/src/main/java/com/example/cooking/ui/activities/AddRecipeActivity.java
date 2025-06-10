package com.example.cooking.ui.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cooking.R;
import com.example.cooking.Recipe.Ingredient;
import com.example.cooking.Recipe.Step;
import com.example.cooking.ui.adapters.IngredientAdapter;
import com.example.cooking.ui.adapters.StepAdapter;
import com.example.cooking.ui.viewmodels.AddRecipeViewModel;
import com.example.cooking.utils.MySharedPreferences;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import com.example.cooking.auth.FirebaseAuthManager;

/**
 * Активность для добавления нового рецепта
 * Использует AddRecipeViewModel для управления бизнес-логикой
 */
public class AddRecipeActivity extends AppCompatActivity implements
        IngredientAdapter.IngredientUpdateListener,
        StepAdapter.StepUpdateListener {
    private static final String TAG = "AddRecipeActivity";
    private static final int REQUEST_STORAGE_PERMISSION = 1001;
    private static final int REQUEST_PICK_IMAGE = 1002;
    
    // UI компоненты
    private TextInputEditText titleEditText;
    private Button saveButton;
    private ProgressBar progressBar;
    private ImageView recipeImageView;
    private TextInputLayout titleInputLayout;
    private TextView textImageView;
    private TextView ingredientsErrorTextView;
    private TextView stepsErrorTextView;

    // Новые UI компоненты для RecyclerView
    private RecyclerView ingredientsRecyclerView;
    private RecyclerView stepsRecyclerView;
    private Button addIngredientButton;
    private Button addStepButton;

    // Адаптеры
    private IngredientAdapter ingredientAdapter;
    private StepAdapter stepAdapter;

    // ViewModel
    private AddRecipeViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_recipe);
        // Сброс прокрутки NestedScrollView к началу
        NestedScrollView scrollView = findViewById(R.id.main_scroll_view);
        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_UP));

        // Настраиваем toolbar
        setupToolbar();

        // Инициализируем ViewModel
        viewModel = new ViewModelProvider(this).get(AddRecipeViewModel.class);
        
        // Инициализируем UI компоненты
        initViews();
        
        // Настраиваем RecyclerView
        setupRecyclerViews();
        
        // Настраиваем наблюдателей LiveData
        setupObservers();
        
        // Настраиваем обработчики событий
        setupEventListeners();
    }
    
    /**
     * Настраивает toolbar
     */
    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Добавить рецепт");
    }
    
    /**
     * Инициализирует UI компоненты
     */
    private void initViews() {
        titleEditText = findViewById(R.id.recipe_title);
        saveButton = findViewById(R.id.save_button);
        progressBar = findViewById(R.id.progress_bar);
        recipeImageView = findViewById(R.id.recipe_image);
        titleInputLayout = findViewById(R.id.recipe_title_layout);
        textImageView = findViewById(R.id.textImage);

        // Инициализация RecyclerView и кнопок добавления
        ingredientsRecyclerView = findViewById(R.id.ingredients_recyclerview);
        stepsRecyclerView = findViewById(R.id.steps_recyclerview);
        addIngredientButton = findViewById(R.id.add_ingredient_button);
        addStepButton = findViewById(R.id.add_step_button);

        recipeImageView.setImageResource(R.drawable.select_recipe_view);
    }
    
    /**
     * Настраивает RecyclerView
     */
    private void setupRecyclerViews() {
        ingredientAdapter = new IngredientAdapter(this);
        stepAdapter = new StepAdapter(this, this);

        // Настраиваем LinearLayoutManager с автоизмерением для корректного отображения всех элементов
        LinearLayoutManager ingredientsLayoutManager = new LinearLayoutManager(this);
        ingredientsLayoutManager.setAutoMeasureEnabled(true);
        ingredientsRecyclerView.setLayoutManager(ingredientsLayoutManager);
        ingredientsRecyclerView.setAdapter(ingredientAdapter);
        ingredientsRecyclerView.setNestedScrollingEnabled(false);
        ingredientsRecyclerView.setHasFixedSize(false);

        // Аналогично для RecyclerView шагов
        LinearLayoutManager stepsLayoutManager = new LinearLayoutManager(this);
        stepsLayoutManager.setAutoMeasureEnabled(true);
        stepsRecyclerView.setLayoutManager(stepsLayoutManager);
        stepsRecyclerView.setAdapter(stepAdapter);
        stepsRecyclerView.setNestedScrollingEnabled(false);
        stepsRecyclerView.setHasFixedSize(false);
        stepsRecyclerView.setItemViewCacheSize(20);
        
        stepsRecyclerView.setItemAnimator(null);


    }
    
    /**
     * Настраивает наблюдателей LiveData из ViewModel
     */
    private void setupObservers() {
        // Наблюдаем за статусом загрузки
        viewModel.getIsLoading().observe(this, isLoading -> {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            saveButton.setEnabled(!isLoading);
            if (isLoading) {
                saveButton.setText("Сохранение...");
            } else {
                saveButton.setText("Сохранить рецепт");
            }
        });
        
        // Наблюдаем за сообщениями об ошибках
        viewModel.getErrorMessage().observe(this, errorMsg -> {
            if (errorMsg != null && !errorMsg.isEmpty()) {
                Toast.makeText(this, "Ой, что-то пошло не так", Toast.LENGTH_LONG).show();
            }
        });
        
        // Наблюдаем за успешным сохранением
        viewModel.getSaveSuccess().observe(this, success -> {
            if (success != null && success) {
                Toast.makeText(this, "Рецепт успешно сохранен", Toast.LENGTH_LONG).show();
                setResult(RESULT_OK);
                finish();
            }
        });
        
        // Наблюдаем за ошибками валидации Title
        viewModel.getTitleError().observe(this, error -> {
            titleInputLayout.setError(error);
            titleInputLayout.setErrorTextColor(error != null ? ColorStateList.valueOf(Color.RED) : null);
        });
        
        // Наблюдаем за ошибками валидации списка Ингредиентов
        viewModel.getIngredientsListError().observe(this, error -> {
            if (ingredientsErrorTextView != null) {
                ingredientsErrorTextView.setVisibility(error != null ? View.VISIBLE : View.GONE);
                ingredientsErrorTextView.setText(error);
            } else if (error != null) {
                Toast.makeText(this, "Ой, что-то пошло не так", Toast.LENGTH_SHORT).show();
            }
        });
        
        // Наблюдаем за ошибками валидации списка Шагов
        viewModel.getStepsListError().observe(this, error -> {
            if (stepsErrorTextView != null) {
                stepsErrorTextView.setVisibility(error != null ? View.VISIBLE : View.GONE);
                stepsErrorTextView.setText(error);
            } else if (error != null) {
                Toast.makeText(this, "Ой, что-то пошло не так", Toast.LENGTH_SHORT).show();
            }
        });
        
        // Наблюдаем за ошибками изображения
        viewModel.getImageError().observe(this, error -> {
            if (error != null) {
                textImageView.setTextColor(Color.RED);
                textImageView.setText(error);
            } else {
                textImageView.setTextColor(ContextCompat.getColor(this, android.R.color.black));
                if (viewModel.hasImage()) {
                    textImageView.setText("Изображение выбрано");
                } else {
                    textImageView.setText("Выберите изображение*");
                }
            }
        });
        
        // Наблюдаем за списком ингредиентов
        viewModel.getIngredients().observe(this, ingredients -> {
            // Создаем новый список, чтобы DiffUtil в ListAdapter корректно обработал изменения
            ingredientAdapter.submitList(ingredients != null ? new ArrayList<>(ingredients) : null);
            // Прокручиваем к последнему элементу, если он был добавлен
            if (ingredients != null && !ingredients.isEmpty()) {
                ingredientsRecyclerView.post(() -> {
                    int position = ingredients.size() - 1;
                    if (position >= 0) {
                        ingredientsRecyclerView.smoothScrollToPosition(position);
                    }
                });
            }
        });
        
        // Наблюдаем за списком шагов
        viewModel.getSteps().observe(this, steps -> {
            // Обновляем список шагов
            stepAdapter.submitList(steps != null ? new ArrayList<>(steps) : null);
        });
    }
    
    /**
     * Настраивает обработчики событий для UI компонентов
     */
    private void setupEventListeners() {
        // Обработчик изменения текста для названия рецепта
        titleEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                viewModel.setTitle(s.toString());
            }
        });
        
        // Обработчик клика по изображению
        recipeImageView.setOnClickListener(view -> {
            checkStoragePermissionAndPickImage();
        });
        
        // Обработчик клика по кнопке "Добавить ингредиент"
        addIngredientButton.setOnClickListener(v -> {
            viewModel.addEmptyIngredient();
        });
        
        // Обработчик клика по кнопке "Добавить шаг"
        addStepButton.setOnClickListener(v -> {
            viewModel.addEmptyStep();
        });
        
        // Обработчик клика по кнопке "Сохранить"
        saveButton.setOnClickListener(v -> {
            MySharedPreferences prefs = new MySharedPreferences(AddRecipeActivity.this);
            String userId = prefs.getUserId();
            Log.d(TAG, userId);
            if (!FirebaseAuthManager.getInstance().isUserSignedIn()) {
                Toast.makeText(AddRecipeActivity.this, "Войдите в систему, чтобы добавлять рецепты", Toast.LENGTH_LONG).show();
                return;
            }
            viewModel.saveRecipe();
        });
    }
    
    /**
     * Проверяет разрешение на доступ к хранилищу и открывает галерею
     */
    private void checkStoragePermissionAndPickImage() {
        String permission;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permission = Manifest.permission.READ_MEDIA_IMAGES;
        } else {
            permission = Manifest.permission.READ_EXTERNAL_STORAGE;
        }
        
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{permission}, REQUEST_STORAGE_PERMISSION);
            Toast.makeText(this, "Для выбора фото необходимо предоставить разрешение на доступ к галерее",
                    Toast.LENGTH_LONG).show();
        } else {
            openGallery();
        }
    }
    
    /**
     * Открывает галерею для выбора изображения
     */
    private void openGallery() {
        try {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, REQUEST_PICK_IMAGE);
        } catch (Exception e) {
            Toast.makeText(this, "Не удалось открыть галерею",
                    Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openGallery();
            } else {
                Toast.makeText(this, "Для выбора изображения необходим доступ к хранилищу",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            Uri selectedImageUri = data.getData();
            if (selectedImageUri != null) {
                Log.d(TAG, "onActivityResult: Изображение выбрано: " + selectedImageUri);
                
                recipeImageView.setImageURI(selectedImageUri);
                textImageView.setText("Изображение выбрано");
                
                viewModel.processSelectedImage(selectedImageUri);
            }
        }
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            checkForUnsavedChangesAndExit();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    public void onBackPressed() {
        checkForUnsavedChangesAndExit();
    }
    
    /**
     * Проверяет наличие несохраненных изменений и предлагает выйти
     */
    private void checkForUnsavedChangesAndExit() {
        boolean titleChanged = titleEditText.getText() != null && !titleEditText.getText().toString().isEmpty();
        boolean imageSelected = viewModel.hasImage();
        boolean ingredientsExist = viewModel.getIngredients().getValue() != null && !viewModel.getIngredients().getValue().isEmpty();
        boolean stepsExist = viewModel.getSteps().getValue() != null && !viewModel.getSteps().getValue().isEmpty();

        if (titleChanged || imageSelected || ingredientsExist || stepsExist) {
            showExitConfirmDialog();
        } else {
            finish();
        }
    }
    
    /**
     * Показывает диалог подтверждения выхода
     */
    private void showExitConfirmDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Отменить создание рецепта?")
            .setMessage("Введенные данные будут потеряны")
            .setPositiveButton("Да", (dialog, which) -> {
                dialog.dismiss();
                finish();
            })
            .setNegativeButton("Нет", (dialog, which) -> dialog.dismiss())
            .show();
    }

    // --- Реализация слушателей адаптеров ---

    @Override
    public void onIngredientUpdated(int position, Ingredient ingredient) {
        viewModel.updateIngredient(position, ingredient);
    }

    @Override
    public void onIngredientRemoved(int position) {
        viewModel.removeIngredient(position);
    }

    @Override
    public void onStepUpdated(int position, Step step) {
        viewModel.updateStep(position, step);
    }

    @Override
    public void onStepRemoved(int position) {
        viewModel.removeStep(position);
    }
}