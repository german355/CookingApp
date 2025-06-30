package com.example.cooking.ui.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.cooking.R;
import com.example.cooking.domain.entities.Ingredient;
import com.example.cooking.domain.entities.Recipe;
import com.example.cooking.domain.entities.Step;
import com.example.cooking.ui.adapters.Recipe.IngredientAdapter;
import com.example.cooking.ui.adapters.Recipe.StepAdapter;
import com.example.cooking.ui.viewmodels.Recipe.EditRecipeViewModel;
import com.example.cooking.domain.usecases.UserPermissionUseCase;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;

/**
 * Активность для редактирования рецепта
 */
public class EditRecipeActivity extends AppCompatActivity implements
        IngredientAdapter.IngredientUpdateListener,
        StepAdapter.StepUpdateListener {
    
    private static final String TAG = "EditRecipeActivity";
    private static final int REQUEST_STORAGE_PERMISSION = 1001;
    private static final int REQUEST_PICK_IMAGE = 1002;
    public static final String EXTRA_EDIT_RECIPE = "EDIT_RECIPE";
    
    // UI компоненты
    private TextInputEditText titleEditText;
    private Button addIngredientButton, addStepButton, saveButton;
    private RecyclerView ingredientsRecyclerView, stepsRecyclerView;
    private IngredientAdapter ingredientsAdapter;
    private StepAdapter stepsAdapter;
    private ProgressBar progressBar;
    private ImageView recipeImageView;
    private TextView textImage;
    
    private EditRecipeViewModel viewModel;
    private UserPermissionUseCase userPermissionUseCase;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_recipe);
        
        // Сброс прокрутки к началу
        NestedScrollView scrollView = findViewById(R.id.main_scroll_view);
        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_UP));
        
        Recipe recipeToEdit = getIntent().getParcelableExtra(EXTRA_EDIT_RECIPE);
        if (!validateRecipeAndPermissions(recipeToEdit)) {
            return;
        }
        
        setupUI();
        setupViewModelAndObservers(recipeToEdit);
    }
    
    /**
     * Валидация рецепта и прав доступа
     */
    private boolean validateRecipeAndPermissions(Recipe recipeToEdit) {
        if (recipeToEdit == null) {
            Log.e(TAG, "Recipe объект не найден");
            Toast.makeText(this, "Ошибка загрузки данных для редактирования.", Toast.LENGTH_LONG).show();
            finish();
            return false;
        }
        
        userPermissionUseCase = new UserPermissionUseCase(getApplication());
        var permissionResult = userPermissionUseCase.canEditRecipe(recipeToEdit.getUserId());
        if (!permissionResult.hasPermission()) {
            Toast.makeText(this, permissionResult.getReason(), Toast.LENGTH_LONG).show();
            Log.w(TAG, "Доступ запрещен: " + permissionResult.getReason());
            finish();
            return false;
        }
        
        Log.d(TAG, "Валидация прошла успешно для рецепта ID: " + recipeToEdit.getId());
        return true;
    }
    
    /**
     * Настройка всего UI
     */
    private void setupUI() {
        // Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Редактировать рецепт");
        
        // Инициализация views
        titleEditText = findViewById(R.id.recipe_title);
        ingredientsRecyclerView = findViewById(R.id.ingredients_recyclerview);
        stepsRecyclerView = findViewById(R.id.steps_recyclerview);
        addIngredientButton = findViewById(R.id.add_ingredient_button);
        addStepButton = findViewById(R.id.add_step_button);
        saveButton = findViewById(R.id.save_button);
        progressBar = findViewById(R.id.progress_bar);
        recipeImageView = findViewById(R.id.recipe_image);
        textImage = findViewById(R.id.textImage);
        
        textImage.setText("Изображение рецепта* (нажмите, чтобы изменить)");
        saveButton.setText("Сохранить изменения");
        
        setupRecyclerViews();
        setupEventListeners();
    }
    
    /**
     * Настройка RecyclerViews
     */
    private void setupRecyclerViews() {
        ingredientsAdapter = new IngredientAdapter(this);
        stepsAdapter = new StepAdapter(this, this);
        
        setupRecyclerView(ingredientsRecyclerView, ingredientsAdapter);
        setupRecyclerView(stepsRecyclerView, stepsAdapter);
    }
    
    private void setupRecyclerView(RecyclerView recyclerView, RecyclerView.Adapter adapter) {
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setNestedScrollingEnabled(false);
    }
    
    /**
     * Настройка ViewModel и observers
     */
    private void setupViewModelAndObservers(Recipe recipeToEdit) {
        viewModel = new ViewModelProvider(this).get(EditRecipeViewModel.class);
        viewModel.setRecipeData(recipeToEdit);
        
        // Загружаем изображение если есть URL
        if (recipeToEdit.getPhoto_url() != null && !recipeToEdit.getPhoto_url().isEmpty()) {
            viewModel.loadImageFromUrl(recipeToEdit.getPhoto_url());
        }
        
        // Основные состояния
        viewModel.getIsSaving().observe(this, this::handleSavingState);
        viewModel.getErrorMessage().observe(this, this::handleErrorMessage);
        viewModel.getSaveResult().observe(this, this::handleSaveResult);
        
        // Данные рецепта
        viewModel.getTitle().observe(this, title -> {
            if (!titleEditText.getText().toString().equals(title)) {
                titleEditText.setText(title);
            }
        });
        
        viewModel.getIngredients().observe(this, ingredients -> {
            if (ingredients != null) {
                ingredientsAdapter.submitList(new ArrayList<>(ingredients));
            }
        });
        
        viewModel.getSteps().observe(this, steps -> {
            if (steps != null) {
                stepsAdapter.submitList(new ArrayList<>(steps));
            }
        });
        
        // Изображения
        viewModel.getPhotoUrl().observe(this, this::handlePhotoUrl);
        viewModel.getImageBytes().observe(this, this::handleImageBytes);
    }
    
    private void handleSavingState(Boolean isSaving) {
        progressBar.setVisibility(isSaving ? View.VISIBLE : View.GONE);
        boolean enabled = !isSaving;
        saveButton.setEnabled(enabled);
        titleEditText.setEnabled(enabled);
        addIngredientButton.setEnabled(enabled);
        addStepButton.setEnabled(enabled);
        recipeImageView.setEnabled(enabled);
        
        saveButton.setText(isSaving ? "Сохранение..." : "Сохранить изменения");
    }
    
    private void handleErrorMessage(String error) {
        if (error != null && !error.isEmpty()) {
            Toast.makeText(this, error, Toast.LENGTH_LONG).show();
            viewModel.clearErrorMessage();
        }
    }
    
    private void handleSaveResult(Boolean result) {
        if (result != null && result) {
            Toast.makeText(this, "Рецепт успешно сохранен", Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK);
            finish();
        }
        if (result != null) {
            viewModel.clearSaveResult();
        }
    }
    
    private void handlePhotoUrl(String url) {
        if (url != null && !url.isEmpty() && viewModel.getImageBytes().getValue() == null) {
            Glide.with(this).load(url).into(recipeImageView);
            textImage.setText("Изображение загружено");
        }
    }
    
    private void handleImageBytes(byte[] bytes) {
        if (bytes != null && bytes.length > 0) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            recipeImageView.setImageBitmap(bitmap);
            textImage.setText("Изображение загружено");
        } else if (viewModel.getPhotoUrl().getValue() == null || viewModel.getPhotoUrl().getValue().isEmpty()) {
            recipeImageView.setImageResource(R.drawable.placeholder_image);
            textImage.setText("Нажмите чтобы загрузить изображение");
        }
    }
    
    /**
     * Настройка обработчиков событий
     */
    private void setupEventListeners() {
        addIngredientButton.setOnClickListener(v -> viewModel.addEmptyIngredient());
        addStepButton.setOnClickListener(v -> viewModel.addEmptyStep());
        recipeImageView.setOnClickListener(v -> checkStoragePermissionAndPickImage());
        textImage.setOnClickListener(v -> checkStoragePermissionAndPickImage());
        saveButton.setOnClickListener(v -> viewModel.saveRecipe());
        
        titleEditText.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(android.text.Editable s) { viewModel.setTitle(s.toString()); }
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
        } else {
            openGallery();
        }
    }
    
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_PICK_IMAGE);
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_STORAGE_PERMISSION && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openGallery();
        } else {
            Toast.makeText(this, "Разрешение на доступ к хранилищу необходимо для выбора фото", Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PICK_IMAGE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            viewModel.processSelectedImage(imageUri);
        }
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Реализация слушателей адаптеров
    @Override public void onIngredientUpdated(int position, Ingredient ingredient) { viewModel.updateIngredient(position, ingredient); }
    @Override public void onIngredientRemoved(int position) { viewModel.removeIngredient(position); }
    @Override public void onStepUpdated(int position, Step step) { viewModel.updateStep(position, step); }
    @Override public void onStepRemoved(int position) { viewModel.removeStep(position); }
} 
