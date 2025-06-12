package com.example.cooking.ui.activities;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import android.widget.AdapterView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.NavOptions;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;

import com.example.cooking.R;
import com.example.cooking.ui.fragments.HomeFragment;
import com.example.cooking.ui.fragments.AuthFragment;
import com.example.cooking.ui.viewmodels.MainViewModel;
import com.example.cooking.ui.viewmodels.SharedRecipeViewModel;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.appbar.MaterialToolbar;
import com.example.cooking.ui.activities.AddRecipeActivity;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.content.res.ColorStateList;

/**
 * Главная активность приложения.
 * Отвечает за управление фрагментами и нижней навигацией.
 */
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int REQUEST_ADD_RECIPE = 100;

    // UI компоненты
    public BottomNavigationView bottomNavigationView;
    private FloatingActionButton addButton;
    private FloatingActionButton fabAddRecipe;
    private FloatingActionButton fabChat;
    private MaterialToolbar toolbar;

    // Навигация
    private NavController navController;

    // Данные и состояние
    private MainViewModel viewModel;
    private SharedRecipeViewModel sharedRecipeViewModel;

    // Поля для истории поиска
    private SharedPreferences searchPrefs;
    private ArrayAdapter<String> suggestionAdapter;
    private SearchView.SearchAutoComplete searchAutoComplete;

    // Анимации для FAB
    private Animation fabShowAnim;
    private Animation fabHideAnim;

    // Оригинальные цвет и иконка главной FAB
    private ColorStateList fabOriginalTint;
    private int fabOriginalIcon;

    /**
     * Вызывается при создании активности.
     * Инициализирует интерфейс и настраивает навигацию.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewModel = new ViewModelProvider(this).get(MainViewModel.class);
        // Инициализация SharedRecipeViewModel
        sharedRecipeViewModel = new ViewModelProvider(this).get(SharedRecipeViewModel.class);

        initViews();
        setSupportActionBar(toolbar);
        setupNavigation(savedInstanceState);
        setupEventHandlers();
        setupObservers();

        // Обновление меню при смене фрагмента
        navController.addOnDestinationChangedListener((controller, destination, args) -> invalidateOptionsMenu());

        // Загрузка данных о рецептах уже происходит в MainViewModel
        // sharedRecipeViewModel.loadInitialRecipes(); // Убираем дублирование

        // Устанавливаем цвет статус-бара программно
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            getWindow().setStatusBarColor(getColor(R.color.md_theme_surfaceContainer));
        }
    }

    /**
     * Инициализирует UI компоненты
     */
    private void initViews() {
        // Инициализируем кнопку добавления рецепта
        addButton = findViewById(R.id.fab_add);
        // Сохраняем оригинальные цвет и иконку
        fabOriginalTint = addButton.getBackgroundTintList();
        fabOriginalIcon = R.drawable.more_vert;
        fabAddRecipe = findViewById(R.id.fab_add_recipe);
        fabChat = findViewById(R.id.fab_chat);
        toolbar = findViewById(R.id.toolbar);
        
        // Инициализируем нижнюю навигацию
        bottomNavigationView = findViewById(R.id.bottom_navigation);

        // Загрузка анимаций FAB
        fabShowAnim = AnimationUtils.loadAnimation(this, R.anim.fab_show);
        fabHideAnim = AnimationUtils.loadAnimation(this, R.anim.fab_hide);
    }

    /**
     * Настраивает навигацию с использованием Navigation Component
     */
    private void setupNavigation(Bundle savedInstanceState) {
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
            toolbar.setNavigationOnClickListener(v -> navController.navigateUp());
            // Следим за изменениями пункта назначения для обновления UI
            navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                int id = destination.getId();
                // Показываем FAB кнопку только на HomeFragment
                boolean showAddButton = id == R.id.nav_home;
                viewModel.setShowAddButton(showAddButton);
                
                // Показываем поиск только на главном экране, в каталоге и в избранном
                boolean showSearch = id == R.id.nav_home || id == R.id.nav_catalog || id == R.id.nav_favorites;
                if (showSearch) {
                    if (getSupportActionBar() != null) {
                        CharSequence label = destination.getLabel();
                        getSupportActionBar().setTitle(label != null ? label : "");
                    }
                } else {
                    if (getSupportActionBar() != null) {
                        CharSequence label = destination.getLabel();
                        getSupportActionBar().setTitle(label != null ? label : "");
                    }
                }

                // Показываем кнопку назад только в экране авторизации
                if (id == R.id.destination_auth || id == R.id.destination_settings ||   id == R.id.destination_profile) {
                    if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                } else {
                    if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(false);
                }
            });

            // Добавляем умный ручной обработчик
            bottomNavigationView.setOnItemSelectedListener(item -> {
                int destinationId = item.getItemId();

                // Опции навигации для сохранения состояния
                NavOptions options = new NavOptions.Builder()
                        .setLaunchSingleTop(true)
                        .setRestoreState(true)
                        .setPopUpTo(navController.getGraph().getStartDestinationId(), false, true)
                        .build();

                try {
                    navController.navigate(destinationId, null, options);
                    return true;
                } catch (IllegalArgumentException e) {
                    Log.e(TAG, "Не удалось найти пункт назначения: " + item.getTitle(), e);
                    return false;
                }
            });

            if (savedInstanceState == null) {
                bottomNavigationView.setSelectedItemId(navController.getGraph().getStartDestinationId());
            }
        }
    }

    /**
     * Настраивает обработчики событий
     */
    private void setupEventHandlers() {
        // Настраиваем кнопку добавления рецепта
        addButton.setOnClickListener(v -> viewModel.toggleFabMenu());
        fabAddRecipe.setOnClickListener(v -> {
            if (viewModel.isUserLoggedIn()) {
                startActivity(new Intent(this, AddRecipeActivity.class));
            } else {
                Toast.makeText(this, R.string.please_login_to_continue, Toast.LENGTH_SHORT).show();
            }
            viewModel.toggleFabMenu();
        });
        fabChat.setOnClickListener(v -> {
            if (viewModel.isUserLoggedIn()) {
                startActivity(new Intent(this, AiChatActivity.class));
            } else {
                Toast.makeText(this, R.string.please_login_to_continue, Toast.LENGTH_SHORT).show();
            }
            viewModel.toggleFabMenu();
        });
    }

    /**
     * Настраивает наблюдателей LiveData из ViewModel
     */
    private void setupObservers() {
        viewModel.getShowAddButton().observe(this, show -> {
            if (show) {
                addButton.show();
            } else {
                addButton.hide();
            }
        });

        viewModel.getIsUserLoggedIn().observe(this, isLoggedIn -> {
            Log.d(TAG, "Состояние авторизации изменилось: " + isLoggedIn);

            if (navController.getCurrentDestination() != null) {
                int currentDestId = navController.getCurrentDestination().getId();

                if (currentDestId == R.id.nav_profile) {
                    navController.navigate(R.id.nav_profile);
                }
            }
        });

        // Наблюдатель для события выхода
        viewModel.getLogoutEvent().observe(this, ignored -> {
            Log.d(TAG, "Получено событие выхода");

            // При выходе перезагружаем экран профиля
            if (navController.getCurrentDestination() != null) {
                int currentDestId = navController.getCurrentDestination().getId();

                if (currentDestId == R.id.nav_profile ||
                        currentDestId == R.id.destination_profile ||
                        currentDestId == R.id.destination_settings) {
                    navController.navigate(R.id.nav_profile);
                }
            }
        });

        viewModel.getIsFabMenuExpanded().observe(this, expanded -> {
            if (expanded) {
                fabAddRecipe.setVisibility(View.VISIBLE);
                fabChat.setVisibility(View.VISIBLE);
                fabAddRecipe.startAnimation(fabShowAnim);
                fabChat.startAnimation(fabShowAnim);
                // Сменить иконку и цвет главной FAB
                addButton.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
                addButton.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(this, android.R.color.darker_gray)
                ));
            } else {
                fabAddRecipe.startAnimation(fabHideAnim);
                fabChat.startAnimation(fabHideAnim);
                // после анимации скрываем
                fabAddRecipe.postDelayed(() -> fabAddRecipe.setVisibility(View.GONE), fabHideAnim.getDuration());
                fabChat.postDelayed(() -> fabChat.setVisibility(View.GONE), fabHideAnim.getDuration());
                // Вернуть оригинальную иконку и цвет
                addButton.setImageResource(fabOriginalIcon);
                addButton.setBackgroundTintList(fabOriginalTint);
            }
        });
    }

    /**
     * Обрабатывает нажатие на кнопку добавления рецепта
     */
    private void handleAddButtonClick() {
        Intent intent = new Intent(this, AddRecipeActivity.class);
        startActivityForResult(intent, REQUEST_ADD_RECIPE);
    }

    /**
     * Управляет переходом к домашнему фрагменту
     */
    public void navigateToHomeFragment() {
        bottomNavigationView.setSelectedItemId(R.id.nav_home);
    }

    /**
     * Управляет переходом к экрану авторизации
     */
    public void navigateToAuthScreen() {
        bottomNavigationView.setSelectedItemId(R.id.nav_profile);
        if (navController.getCurrentDestination().getId() == R.id.nav_profile) {
            navController.navigate(R.id.action_sharedProfile_to_auth);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
    }

    /**
     * Обрабатывает результаты возврата из других Activity
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult: requestCode=" + requestCode + ", resultCode=" + resultCode);
        if (requestCode == REQUEST_ADD_RECIPE && resultCode == RESULT_OK) {
            if (data != null && data.hasExtra("recipeAdded") && data.getBooleanExtra("recipeAdded", false)) {
                refreshHomeFragment();
                Toast.makeText(this, "Рецепт успешно добавлен", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == 200) {
            if (resultCode == Activity.RESULT_OK) {
                NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
                if (navHostFragment != null) {
                    Fragment currentFragment = navHostFragment.getChildFragmentManager().getPrimaryNavigationFragment();
                    if (currentFragment instanceof HomeFragment) {
                        ((HomeFragment) currentFragment).restoreLastSearch();
                        Log.d(TAG, "Вызван метод restoreLastSearch() в HomeFragment");
                    }
                }
            }
        } else if (requestCode == com.example.cooking.auth.FirebaseAuthManager.RC_SIGN_IN) {
            NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
            if (navHostFragment != null) {
                Fragment currentFragment = navHostFragment.getChildFragmentManager().getPrimaryNavigationFragment();
                if (currentFragment instanceof AuthFragment) {
                    currentFragment.onActivityResult(requestCode, resultCode, data);
                }
            }
        }
    }

    /**
     * Обновляет домашний фрагмент при возврате из AddRecipeActivity
     */
    private void refreshHomeFragment() {
        sharedRecipeViewModel.refreshRecipes();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        MenuItem searchItem = menu.findItem(R.id.action_search);
        // Меняем цвет иконки поиска в зависимости от темы
        int nightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        if (nightMode == Configuration.UI_MODE_NIGHT_YES) {
            searchItem.getIcon().setTint(ContextCompat.getColor(this, android.R.color.white));
        } else {
            searchItem.getIcon().setTint(ContextCompat.getColor(this, android.R.color.black));
        }
        SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setQueryHint("Поиск по рецептам");
        int searchPlateId = searchView.getContext().getResources().getIdentifier(
                "android:id/search_plate", null, null);
        View searchPlate = searchView.findViewById(searchPlateId);
        if (searchPlate != null) {
            searchPlate.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            int searchSrcTextId = getResources().getIdentifier(
                    "android:id/search_src_text", null, null);
            android.widget.EditText searchEditText = searchView.findViewById(searchSrcTextId);
            if (searchEditText != null) {
                searchEditText.setBackground(null);
                searchEditText.setHintTextColor(getResources().getColor(
                        R.color.md_theme_onSurfaceVariant, null));
                searchEditText.setTextColor(getResources().getColor(
                        R.color.md_theme_onSurface, null));
            }
        }
        // Инициализация истории поиска
        searchPrefs = getSharedPreferences("search_prefs", MODE_PRIVATE);
        Set<String> historySet = searchPrefs.getStringSet("search_history", new LinkedHashSet<>());
        List<String> historyList = new ArrayList<>(historySet);
        suggestionAdapter = new ArrayAdapter<>(this,
            android.R.layout.simple_dropdown_item_1line, historyList);
        searchAutoComplete = searchView.findViewById(androidx.appcompat.R.id.search_src_text);
        searchAutoComplete.setAdapter(suggestionAdapter);
        searchAutoComplete.setThreshold(1);
        searchAutoComplete.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String suggestion = suggestionAdapter.getItem(position);
                if (suggestion != null) {
                    searchView.setQuery(suggestion, true);
                }
            }
        });

        // Логика поиска
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                performSearch(query);
                return true;
            }
            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        // Перехват клика по подсказкам
        searchView.setOnSuggestionListener(new SearchView.OnSuggestionListener() {
            @Override
            public boolean onSuggestionSelect(int position) {
                return false;
            }

            @Override
            public boolean onSuggestionClick(int position) {
                String suggestion = suggestionAdapter.getItem(position);
                if (suggestion != null) {
                    searchView.setQuery(suggestion, true);
                }
                return true;
            }
        });

        searchItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                // Скрыть заголовок при открытии поиска
                if (getSupportActionBar() != null) getSupportActionBar().setTitle("");
                return true;
            }
            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                // Восстановить заголовок при закрытии поиска
                if (getSupportActionBar() != null) {
                    int id = navController.getCurrentDestination().getId();
                    if (id == R.id.nav_catalog) {
                        getSupportActionBar().setTitle("Каталог");
                    } else if (navController.getCurrentDestination().getLabel() != null) {
                        getSupportActionBar().setTitle(navController.getCurrentDestination().getLabel());
                    } else {
                        getSupportActionBar().setTitle(" ");
                    }
                }
                // Сброс поиска — показать все рецепты
                performSearch("");
                return true;
            }
        });
        return true;
    }
    
    /**
     * Выполняет поиск по введенному запросу
     */
    private void performSearch(String query) {
        Log.d(TAG, "Выполняю поиск: " + query);
        
        // Сохраняем запрос в истории поиска
        if (query != null && !query.trim().isEmpty()) {
            Set<String> historySet = searchPrefs.getStringSet("search_history", new LinkedHashSet<>());
            Set<String> newHistorySet = new LinkedHashSet<>(historySet);
            newHistorySet.add(query);
            searchPrefs.edit().putStringSet("search_history", newHistorySet).apply();
            suggestionAdapter.clear();
            suggestionAdapter.addAll(newHistorySet);
            suggestionAdapter.notifyDataSetChanged();
        }
        
        // Убедимся, что мы на главном экране
NavDestination current = navController.getCurrentDestination();
if (current == null || current.getId() != R.id.nav_home) {
            navigateToHomeFragment();
        }
        
        // Получаем текущий HomeFragment и вызываем поиск
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment != null) {
            Fragment currentFragment = navHostFragment.getChildFragmentManager().getPrimaryNavigationFragment();
            if (currentFragment instanceof HomeFragment) {
                HomeFragment homeFragment = (HomeFragment) currentFragment;
                homeFragment.performSearch(query);
            }
        }
    }

    // Показываем или скрываем кнопку поиска в зависимости от фрагмента
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem searchItem = menu.findItem(R.id.action_search);
        int id = navController.getCurrentDestination() != null
            ? navController.getCurrentDestination().getId() : -1;
        boolean visible = id == R.id.nav_home;
        searchItem.setVisible(visible);

        if (!visible && searchItem.isActionViewExpanded()) {
            // Уходим с главного фрагмента — сворачиваем строку поиска
            searchItem.collapseActionView();
        }

        // Если вернулись на Home и строка поиска свернута, сбрасываем режим поиска и показываем все рецепты
        if (visible && !searchItem.isActionViewExpanded()) {
            if (sharedRecipeViewModel != null) {
                sharedRecipeViewModel.exitSearchMode();
            }
        }
        return super.onPrepareOptionsMenu(menu);
    }
}
