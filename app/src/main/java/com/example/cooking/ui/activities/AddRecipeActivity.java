package com.example.cooking.ui.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

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
import com.example.cooking.domain.entities.Ingredient;
import com.example.cooking.domain.entities.Step;
import com.example.cooking.ui.adapters.Recipe.IngredientAdapter;
import com.example.cooking.ui.adapters.Recipe.StepAdapter;
import com.example.cooking.ui.viewmodels.Recipe.AddRecipeViewModel;
import com.example.cooking.utils.MySharedPreferences;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import com.example.cooking.auth.FirebaseAuthManager;

/**
 * Активность для добавления нового рецепта
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
    private TextView textImageView;
    private RecyclerView ingredientsRecyclerView, stepsRecyclerView;
    private Button addIngredientButton, addStepButton;

    // Новые UI элементы для улучшенного дизайна изображения
    private View imagePlaceholder;
    private View imageContainer;
    private ProgressBar imageProgress;

    // Адаптеры и ViewModel
    private IngredientAdapter ingredientAdapter;
    private StepAdapter stepAdapter;
    private AddRecipeViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_recipe);
        
        // Сброс прокрутки к началу
        NestedScrollView scrollView = findViewById(R.id.main_scroll_view);
        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_UP));

        setupUI();
        setupViewModelAndObservers();
    }
    
    /**
     * Настройка всего UI в одном методе
     */
    private void setupUI() {
        // Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Добавить рецепт");
        
        // Инициализация views
        titleEditText = findViewById(R.id.recipe_title);
        saveButton = findViewById(R.id.save_button);
        progressBar = findViewById(R.id.progress_bar);
        recipeImageView = findViewById(R.id.recipe_image);
        textImageView = findViewById(R.id.textImage);
        ingredientsRecyclerView = findViewById(R.id.ingredients_recyclerview);
        stepsRecyclerView = findViewById(R.id.steps_recyclerview);
        addIngredientButton = findViewById(R.id.add_ingredient_button);
        addStepButton = findViewById(R.id.add_step_button);

        // Новые UI элементы
        imagePlaceholder = findViewById(R.id.image_placeholder);
        imageContainer = findViewById(R.id.image_container);
        imageProgress = findViewById(R.id.image_progress);

        // Изначально показываем placeholder
        showImagePlaceholder();
        
        // Настройка RecyclerViews
        setupRecyclerViews();
        
        // Обработчики событий
        setupEventListeners();
    }
    
    /**
     * Настройка RecyclerViews
     */
    private void setupRecyclerViews() {
        ingredientAdapter = new IngredientAdapter(this);
        stepAdapter = new StepAdapter(this, this);

        setupRecyclerView(ingredientsRecyclerView, ingredientAdapter);
        setupRecyclerView(stepsRecyclerView, stepAdapter);
        stepsRecyclerView.setItemViewCacheSize(20);
        stepsRecyclerView.setItemAnimator(null);
    }
    
    private void setupRecyclerView(RecyclerView recyclerView, RecyclerView.Adapter adapter) {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setAutoMeasureEnabled(true);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);
        recyclerView.setNestedScrollingEnabled(false);
        recyclerView.setHasFixedSize(false);
    }
    
    /**
     * Настройка ViewModel и observers
     */
    private void setupViewModelAndObservers() {
        viewModel = new ViewModelProvider(this).get(AddRecipeViewModel.class);
        
        // Основные состояния
        viewModel.getIsLoading().observe(this, this::handleLoadingState);
        viewModel.getErrorMessage().observe(this, this::handleErrorMessage);
        viewModel.getSaveSuccess().observe(this, this::handleSaveSuccess);
        
        // Данные
        viewModel.getIngredients().observe(this, ingredients -> {
            ingredientAdapter.submitList(ingredients != null ? new ArrayList<>(ingredients) : null);
            if (ingredients != null && !ingredients.isEmpty()) {
                ingredientsRecyclerView.post(() -> 
                    ingredientsRecyclerView.smoothScrollToPosition(ingredients.size() - 1));
            }
        });
        
        viewModel.getSteps().observe(this, steps -> {
            stepAdapter.submitList(steps != null ? new ArrayList<>(steps) : null);
        });
    }
    
    private void handleLoadingState(Boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        saveButton.setEnabled(!isLoading);
        saveButton.setText(isLoading ? "Сохранение..." : "Сохранить рецепт");
    }
    
    private void handleErrorMessage(String errorMsg) {
        if (errorMsg != null && !errorMsg.isEmpty()) {
            Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
            viewModel.clearErrorMessage();
        }
    }
    
    private void handleSaveSuccess(Boolean success) {
        if (success != null && success) {
            Toast.makeText(this, "Рецепт успешно сохранен", Toast.LENGTH_LONG).show();
            setResult(RESULT_OK);
            finish();
        }
    }
    
    /**
     * Настройка обработчиков событий
     */
    private void setupEventListeners() {
        titleEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { viewModel.setTitle(s.toString()); }
        });
        
        // Обработчики для изображения
        imagePlaceholder.setOnClickListener(view -> checkStoragePermissionAndPickImage());
        imageContainer.setOnClickListener(view -> checkStoragePermissionAndPickImage());
        
        addIngredientButton.setOnClickListener(v -> viewModel.addEmptyIngredient());
        addStepButton.setOnClickListener(v -> viewModel.addEmptyStep());
        
        saveButton.setOnClickListener(v -> {
            if (!FirebaseAuthManager.getInstance().isUserSignedIn()) {
                Toast.makeText(this, "Войдите в систему, чтобы добавлять рецепты", Toast.LENGTH_LONG).show();
                return;
            }
            viewModel.saveRecipe();
        });
    }
    
    /**
     * Проверка разрешений и открытие галереи
     */
    private void checkStoragePermissionAndPickImage() {
        String permission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU 
            ? Manifest.permission.READ_MEDIA_IMAGES 
            : Manifest.permission.READ_EXTERNAL_STORAGE;
        
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{permission}, REQUEST_STORAGE_PERMISSION);
            Toast.makeText(this, "Для выбора фото необходимо предоставить разрешение", Toast.LENGTH_LONG).show();
        } else {
            openGallery();
        }
    }
    
    private void openGallery() {
        try {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, REQUEST_PICK_IMAGE);
        } catch (Exception e) {
            Toast.makeText(this, "Не удалось открыть галерею", Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_STORAGE_PERMISSION && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openGallery();
        } else {
            Toast.makeText(this, "Для выбора изображения необходим доступ к хранилищу", Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            Uri selectedImageUri = data.getData();
            if (selectedImageUri != null) {
                showImageLoading();
                recipeImageView.setImageURI(selectedImageUri);
                showSelectedImage();
                textImageView.setText("Изображение выбрано");
                viewModel.processSelectedImage(selectedImageUri);
            }
        }
    }
    
    /**
     * Показать placeholder для изображения
     */
    private void showImagePlaceholder() {
        imagePlaceholder.setVisibility(View.VISIBLE);
        imageContainer.setVisibility(View.GONE);
        imageProgress.setVisibility(View.GONE);
    }
    
    /**
     * Показать выбранное изображение
     */
    private void showSelectedImage() {
        imagePlaceholder.setVisibility(View.GONE);
        imageContainer.setVisibility(View.VISIBLE);
        imageProgress.setVisibility(View.GONE);
    }
    
    /**
     * Показать индикатор загрузки изображения
     */
    private void showImageLoading() {
        imagePlaceholder.setVisibility(View.GONE);
        imageContainer.setVisibility(View.GONE);
        imageProgress.setVisibility(View.VISIBLE);
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
    public void onBackPressed() { checkForUnsavedChangesAndExit(); }
    
    private void checkForUnsavedChangesAndExit() {
        boolean hasChanges = (titleEditText.getText() != null && !titleEditText.getText().toString().isEmpty())
            || viewModel.hasImage()
            || (viewModel.getIngredients().getValue() != null && !viewModel.getIngredients().getValue().isEmpty())
            || (viewModel.getSteps().getValue() != null && !viewModel.getSteps().getValue().isEmpty());

        if (hasChanges) {
            new AlertDialog.Builder(this)
                .setTitle("Отменить создание рецепта?")
                .setMessage("Введенные данные будут потеряны")
                .setPositiveButton("Да", (dialog, which) -> finish())
                .setNegativeButton("Нет", (dialog, which) -> dialog.dismiss())
                .show();
        } else {
            finish();
        }
    }

    // Реализация слушателей адаптеров
    @Override public void onIngredientUpdated(int position, Ingredient ingredient) { viewModel.updateIngredient(position, ingredient); }
    @Override public void onIngredientRemoved(int position) { viewModel.removeIngredient(position); }
    @Override public void onStepUpdated(int position, Step step) { viewModel.updateStep(position, step); }
    @Override public void onStepRemoved(int position) { viewModel.removeStep(position); }
}