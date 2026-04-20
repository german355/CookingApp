package com.example.cooking.ui.activities;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
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
import com.example.cooking.R;
import com.example.cooking.domain.entities.Recipe;
import com.example.cooking.domain.units.MeasurementSystem;
import com.example.cooking.network.services.UserService;
import com.example.cooking.ui.adapters.Recipe.IngredientViewAdapter;
import com.example.cooking.ui.adapters.Recipe.StepAdapter;
import com.example.cooking.ui.viewmodels.Recipe.RecipeDetailUIState;
import com.example.cooking.ui.viewmodels.Recipe.RecipeDetailViewModel;
import com.example.cooking.utils.MySharedPreferences;
import com.example.cooking.utils.ThemeUtils;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.ArrayList;

/**
 * РђРєС‚РёРІРЅРѕСЃС‚СЊ РґР»СЏ РѕС‚РѕР±СЂР°Р¶РµРЅРёСЏ РїРѕРґСЂРѕР±РЅРѕР№ РёРЅС„РѕСЂРјР°С†РёРё Рѕ СЂРµС†РµРїС‚Рµ.
 * РџРѕРєР°Р·С‹РІР°РµС‚ РїРѕР»РЅРѕРµ РѕРїРёСЃР°РЅРёРµ, РёРЅРіСЂРµРґРёРµРЅС‚С‹ Рё РёРЅСЃС‚СЂСѓРєС†РёСЋ.
 */
public class RecipeDetailActivity extends AppCompatActivity {

    public static final String EXTRA_SELECTED_RECIPE = "SELECTED_RECIPE";
    private static final int EDIT_RECIPE_REQUEST = 1001;

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
    private RecipeDetailViewModel viewModel;
    private int recipeId;
    private MySharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences sharedPreferences = getSharedPreferences("acs", MODE_PRIVATE);
        ThemeUtils.applyTheme(sharedPreferences.getString("theme", "system"));
        setContentView(R.layout.activity_recipe_detail);

        Recipe currentRecipe = getIntent().getParcelableExtra(EXTRA_SELECTED_RECIPE);
        if (currentRecipe == null) {
            Toast.makeText(this, getString(R.string.error_recipe_data_load_failed), Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        setResult(Activity.RESULT_OK);
        recipeId = currentRecipe.getId();
        preferences = new MySharedPreferences(this);

        initializeUI();
        initializeViewModel();
        displayInitialData(currentRecipe);
    }

    private void initializeUI() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        toolbar.setNavigationOnClickListener(v -> finish());
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
        }

        titleTextView = findViewById(R.id.recipe_title);
        recipeImageView = findViewById(R.id.recipe_image);
        fabLike = findViewById(R.id.like_button);
        decreasePortionButton = findViewById(R.id.decrease_portion);
        increasePortionButton = findViewById(R.id.increase_portion);
        portionCountTextView = findViewById(R.id.portion_count);
        stepsRecyclerView = findViewById(R.id.steps_recyclerview);
        ingredientsRecyclerView = findViewById(R.id.ingredients_recyclerview);

        setupRecyclerView(stepsRecyclerView, true);
        setupRecyclerView(ingredientsRecyclerView, false);

        stepAdapter = new StepAdapter(this);
        stepsRecyclerView.setAdapter(stepAdapter);

        ingredientAdapter = new IngredientViewAdapter(this, new ArrayList<>(), getMeasurementSystem());
        ingredientsRecyclerView.setAdapter(ingredientAdapter);

        fabLike.setOnClickListener(v -> {
            if (!UserService.isUserLoggedIn()) {
                Toast.makeText(this, getString(R.string.recipe_detail_login_to_like), Toast.LENGTH_LONG).show();
            } else {
                viewModel.toggleLike();
            }
        });

        decreasePortionButton.setOnClickListener(v -> viewModel.decrementPortion());
        increasePortionButton.setOnClickListener(v -> viewModel.incrementPortion());
    }

    private void setupRecyclerView(RecyclerView recyclerView, boolean hasFixedSize) {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setNestedScrollingEnabled(false);
        recyclerView.setHasFixedSize(hasFixedSize);
    }

    private void initializeViewModel() {
        viewModel = new ViewModelProvider(this).get(RecipeDetailViewModel.class);
        viewModel.init(recipeId, preferences.getInt("permission", 1));
        viewModel.getUIState().observe(this, this::renderUIState);
    }

    private void displayInitialData(Recipe recipe) {
        titleTextView.setText(recipe.getTitle());
        updateLikeButton(recipe.isLiked());
        loadImage(recipe.getPhoto_url());

        if (recipe.getIngredients() != null) {
            ingredientAdapter.updateMeasurementSystem(getMeasurementSystem());
            ingredientAdapter.updateIngredients(recipe.getIngredients());
        }
        if (recipe.getSteps() != null) {
            stepAdapter.submitList(recipe.getSteps());
        }
    }

    private void renderUIState(RecipeDetailUIState state) {
        if (state == null) {
            return;
        }

        if (state.hasRecipe()) {
            updateRecipeUI(state);
        }

        updateLikeButton(state.isLiked());
        portionCountTextView.setText(String.valueOf(state.getPortionCount()));

        if (state.hasError()) {
            Toast.makeText(this, state.getErrorMessage(), Toast.LENGTH_LONG).show();
            viewModel.clearError();
        }

        if (state.isDeleteSuccess()) {
            Toast.makeText(this, getString(R.string.recipe_deleted_success), Toast.LENGTH_SHORT).show();
            finish();
        }

        invalidateOptionsMenu();
    }

    private void updateRecipeUI(RecipeDetailUIState state) {
        Recipe recipe = state.getRecipe();
        if (!titleTextView.getText().toString().equals(recipe.getTitle())) {
            titleTextView.setText(recipe.getTitle());
        }

        loadImage(state.getRecipeImageUrl());

        if (state.getIngredients() != null) {
            ingredientAdapter.updateMeasurementSystem(getMeasurementSystem());
            ingredientAdapter.updateIngredients(state.getIngredients());
            ingredientAdapter.updatePortionCount(state.getPortionCount());
        }

        if (state.getSteps() != null) {
            stepAdapter.submitList(state.getSteps());
        }
    }

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

    private void updateLikeButton(boolean isLiked) {
        fabLike.setImageResource(isLiked ? R.drawable.ic_favorite : R.drawable.ic_favorite_border);
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
        } else if (id == R.id.action_chat_recipe) {
            openRecipeChat();
        } else if (id == R.id.action_edit) {
            editRecipe(state.getRecipe());
        } else if (id == R.id.action_delete) {
            showDeleteDialog();
        } else {
            return super.onOptionsItemSelected(item);
        }

        return true;
    }

    private void showDeleteDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.recipe_delete_title)
                .setMessage(R.string.recipe_delete_message)
                .setPositiveButton(R.string.recipe_delete_confirm, (dialog, which) -> viewModel.deleteRecipe())
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void openRecipeChat() {
        if (!UserService.isUserLoggedIn()) {
            Toast.makeText(this, R.string.please_login_to_continue, Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, AiChatActivity.class);
        intent.putExtra(AiChatActivity.EXTRA_CONTEXT_RECIPE_ID, recipeId);
        startActivity(intent);
    }

    private void shareRecipe(Recipe recipe) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        String shareBody = getString(R.string.recipe_share_body, recipe.getTitle());
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.recipe_share_subject, recipe.getTitle()));
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
        startActivity(Intent.createChooser(shareIntent, getString(R.string.recipe_share_chooser)));
    }

    private void editRecipe(Recipe recipe) {
        Intent intent = new Intent(this, EditRecipeActivity.class);
        intent.putExtra(EditRecipeActivity.EXTRA_EDIT_RECIPE, recipe);
        startActivityForResult(intent, EDIT_RECIPE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == EDIT_RECIPE_REQUEST && resultCode == RESULT_OK) {
            Toast.makeText(this, getString(R.string.recipe_updated_success), Toast.LENGTH_SHORT).show();
            viewModel.refreshRecipe();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        ingredientAdapter.updateMeasurementSystem(getMeasurementSystem());
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

    private MeasurementSystem getMeasurementSystem() {
        return MeasurementSystem.fromPreferenceValue(preferences.getMeasurementSystem());
    }
}
