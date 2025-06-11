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
import com.example.cooking.utils.MySharedPreferences;
import com.example.cooking.R;
import com.example.cooking.Recipe.Ingredient;
import com.example.cooking.Recipe.Recipe;
import com.example.cooking.Recipe.Step;
import com.example.cooking.ui.adapters.EditIngredientsAdapter;
import com.example.cooking.ui.adapters.EditStepsAdapter;
import com.example.cooking.ui.viewmodels.EditRecipeViewModel;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;

public class EditRecipeActivity extends AppCompatActivity {
    private static final String TAG = "EditRecipeActivity";
    private static final int REQUEST_STORAGE_PERMISSION = 1001;
    private static final int REQUEST_PICK_IMAGE = 1002;
    public static final String EXTRA_EDIT_RECIPE = "EDIT_RECIPE";
    
    private TextInputEditText titleEditText;
    private Button addIngredientButton;
    private Button addStepButton;
    private RecyclerView ingredientsRecyclerView;
    private RecyclerView stepsRecyclerView;
    private EditIngredientsAdapter ingredientsAdapter;
    private EditStepsAdapter stepsAdapter;
    private Button saveButton;
    private ProgressBar progressBar;
    private ImageView recipeImageView;
    private TextView textImage;
    private TextInputLayout titleInputLayout;
    
    private EditRecipeViewModel viewModel;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate: Activity создается");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_recipe);
        
        // Сброс прокрутки NestedScrollView к началу
        NestedScrollView scrollView = findViewById(R.id.main_scroll_view);
        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_UP));
        
        viewModel = new ViewModelProvider(this).get(EditRecipeViewModel.class);
        
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Редактировать рецепт");
        
        Intent intent = getIntent();
        Recipe recipeToEdit = intent.getParcelableExtra(EXTRA_EDIT_RECIPE);

        if (recipeToEdit == null) {
            Log.e(TAG, "Ошибка: Не удалось получить Recipe объект для редактирования. Ключ: " + EXTRA_EDIT_RECIPE);
            Toast.makeText(this, "Ошибка загрузки данных для редактирования.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        Log.d(TAG, "Получен Recipe для редактирования: ID = " + recipeToEdit.getId());
        
        // Валидация прав на редактирование
        MySharedPreferences prefs = new MySharedPreferences(this);
        String currentUserId = prefs.getUserId();
        int permission = prefs.getUserPermission();
        if (!recipeToEdit.getUserId().equals(currentUserId) && permission < 2) {
            Toast.makeText(this, "У вас нет прав для редактирования этого рецепта", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        
        titleEditText = findViewById(R.id.recipe_title);
        titleInputLayout = findViewById(R.id.recipe_title_layout);
        ingredientsRecyclerView = findViewById(R.id.ingredients_recyclerview);
        stepsRecyclerView = findViewById(R.id.steps_recyclerview);
        addIngredientButton = findViewById(R.id.add_ingredient_button);
        addStepButton = findViewById(R.id.add_step_button);
        saveButton = findViewById(R.id.save_button);
        progressBar = findViewById(R.id.progress_bar);
        recipeImageView = findViewById(R.id.recipe_image);
        textImage = findViewById(R.id.textImage);
        textImage.setText("Изображение рецепта* (нажмите, чтобы изменить)");
        
        setupRecyclerViews();
        
        viewModel.setRecipeData(recipeToEdit);
        
        if (recipeToEdit.getPhoto_url() != null && !recipeToEdit.getPhoto_url().isEmpty()) {
            viewModel.loadImageFromUrl(recipeToEdit.getPhoto_url());
        }
        
        setupObservers();
        setupEventListeners();
        saveButton.setText("Сохранить изменения");
    }
    
    private void setupRecyclerViews() {
        ingredientsAdapter = new EditIngredientsAdapter(this, 
            new EditIngredientsAdapter.IngredientInteractionListener() {
                @Override
                public void onIngredientUpdated(int position, Ingredient ingredient) {
                    viewModel.updateIngredient(position, ingredient);
                }
                
                @Override
                public void onIngredientRemoved(int position) {
                    viewModel.removeIngredient(position);
                }
            });
        ingredientsRecyclerView.setAdapter(ingredientsAdapter);
        ingredientsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        ingredientsRecyclerView.setNestedScrollingEnabled(false);
        
        stepsAdapter = new EditStepsAdapter(new ArrayList<>(),
            new EditStepsAdapter.StepInteractionListener() {
                @Override
                public void onStepChanged(int position, Step step) {
                    viewModel.updateStep(position, step);
                }
                
                @Override
                public void onStepRemove(int position) {
                    viewModel.removeStep(position);
                }
            });
        stepsRecyclerView.setAdapter(stepsAdapter);
        stepsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        stepsRecyclerView.setNestedScrollingEnabled(false);
    }
    
    private void setupObservers() {
        viewModel.getTitle().observe(this, title -> {
            if (!titleEditText.getText().toString().equals(title)) {
                titleEditText.setText(title);
            }
        });
        
        viewModel.getIngredients().observe(this, ingredients -> {
            if (ingredients != null) {
                ingredientsAdapter.setIngredients(new ArrayList<>(ingredients));
            }
        });
        
        viewModel.getSteps().observe(this, steps -> {
            if (steps != null) {
                stepsAdapter.updateSteps(new ArrayList<>(steps));
            }
        });
        
        viewModel.getPhotoUrl().observe(this, url -> {
            if (url != null && !url.isEmpty() && viewModel.getImageBytes().getValue() == null) {
                Glide.with(this).load(url).into(recipeImageView);
                textImage.setText("Изображение загружено");
            }
        });
        
        viewModel.getImageBytes().observe(this, bytes -> {
            if (bytes != null && bytes.length > 0) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                recipeImageView.setImageBitmap(bitmap);
                textImage.setText("Изображение загружено");
            } else if (viewModel.getPhotoUrl().getValue() == null || viewModel.getPhotoUrl().getValue().isEmpty()){
                recipeImageView.setImageResource(R.drawable.placeholder_image);
                textImage.setText("Нажмите чтобы загрузить изображение");
            }
        });
        
        viewModel.getIsSaving().observe(this, isSaving -> {
            progressBar.setVisibility(isSaving ? View.VISIBLE : View.GONE);
            saveButton.setEnabled(!isSaving);
            titleEditText.setEnabled(!isSaving);
            addIngredientButton.setEnabled(!isSaving);
            addStepButton.setEnabled(!isSaving);
            recipeImageView.setEnabled(!isSaving);
        });

        viewModel.getSaveResult().observe(this, result -> {
            if (result != null) {
                if (result) {
                    Toast.makeText(this, "Рецепт успешно сохранен", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                } else {
                }
                viewModel.clearSaveResult();
            }
        });

        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(this, "Ой что-то пошло не так", Toast.LENGTH_LONG).show();
                viewModel.clearErrorMessage();
            }
        });
    }
    
    private void setupEventListeners() {
        addIngredientButton.setOnClickListener(v -> viewModel.addEmptyIngredient());
        
        addStepButton.setOnClickListener(v -> viewModel.addEmptyStep());
        
        recipeImageView.setOnClickListener(v -> checkStoragePermissionAndPickImage());
        textImage.setOnClickListener(v -> checkStoragePermissionAndPickImage());

        saveButton.setOnClickListener(v -> viewModel.saveRecipe());
        
        titleEditText.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override public void afterTextChanged(android.text.Editable s) {
                viewModel.setTitle(s.toString());
                if (titleInputLayout.isErrorEnabled()) {
                    titleInputLayout.setError(null);
                    titleInputLayout.setErrorEnabled(false);
                }
            }
        });
    }
    
    private void checkStoragePermissionAndPickImage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_MEDIA_IMAGES}, REQUEST_STORAGE_PERMISSION);
            } else {
                openGallery();
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_STORAGE_PERMISSION);
            } else {
                openGallery();
            }
        }
    }
    
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_PICK_IMAGE);
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openGallery();
            } else {
                Toast.makeText(this, "Разрешение на доступ к хранилищу необходимо для выбора фото", Toast.LENGTH_SHORT).show();
            }
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
} 
