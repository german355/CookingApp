package com.example.cooking.ui.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.NavOptions;
import androidx.appcompat.widget.SearchView;

import com.example.cooking.R;
import com.example.cooking.ui.viewmodels.MainViewModel;
import com.example.cooking.ui.viewmodels.SharedRecipeViewModel;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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

        // Загрузка данных о рецептах при старте приложения
        sharedRecipeViewModel.loadInitialRecipes();

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
        toolbar = findViewById(R.id.toolbar);
        
        // Инициализируем нижнюю навигацию
        bottomNavigationView = findViewById(R.id.bottom_navigation);
    }

    /**
     * Настраивает навигацию с использованием Navigation Component
     */
    private void setupNavigation(Bundle savedInstanceState) {
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();

            // Убираем стандартную привязку
            // NavigationUI.setupWithNavController(bottomNavigationView, navController);
            // NavigationUI.setupActionBarWithNavController(this, navController);

            // Следим за изменениями пункта назначения для обновления UI
            navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                int id = destination.getId();
                boolean showAddButton = id != R.id.nav_profile &&
                        id != R.id.destination_profile &&
                        id != R.id.destination_auth &&
                        id != R.id.destination_settings;
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
            });

            // Добавляем умный ручной обработчик
            bottomNavigationView.setOnItemSelectedListener(item -> {
                int destinationId = item.getItemId();

                // Опции навигации для сохранения состояния
                NavOptions options = new NavOptions.Builder()
                        .setLaunchSingleTop(true) // Не перезапускать, если уже наверху стека
                        .setRestoreState(true) // Восстановить состояние при возврате
                        // Перейти к началу *основного* графа, не включая его, сохранив стек текущей
                        // вкладки
                        .setPopUpTo(navController.getGraph().getStartDestinationId(), false, true)
                        .build();

                try {
                    navController.navigate(destinationId, null, options);
                    return true; // Успешно перешли
                } catch (IllegalArgumentException e) {
                    // Если destinationId не найден в текущем графе (редко, но возможно)
                    Log.e(TAG, "Не удалось найти пункт назначения: " + item.getTitle(), e);
                    return false;
                }
            });

            // Устанавливаем начальный выбранный элемент (например, Главная)
            // Это может быть не нужно, если startDestination графа совпадает с элементом
            // меню
            if (savedInstanceState == null) { // Только при первом запуске
                bottomNavigationView.setSelectedItemId(navController.getGraph().getStartDestinationId());
            }
        }
    }

    /**
     * Настраивает обработчики событий
     */
    private void setupEventHandlers() {
        // Настраиваем кнопку добавления рецепта
        addButton.setOnClickListener(view -> {
            handleAddButtonClick();
        });
    }

    /**
     * Настраивает наблюдателей LiveData из ViewModel
     */
    private void setupObservers() {
        // Наблюдатель для видимости кнопки добавления
        viewModel.getShowAddButton().observe(this, show -> {
            if (show) {
                addButton.show();
            } else {
                addButton.hide();
            }
        });

        // Наблюдатель для состояния авторизации
        viewModel.getIsUserLoggedIn().observe(this, isLoggedIn -> {
            Log.d(TAG, "Состояние авторизации изменилось: " + isLoggedIn);

            // Если мы сейчас на экранах профиля и статус авторизации изменился
            if (navController.getCurrentDestination() != null) {
                int currentDestId = navController.getCurrentDestination().getId();

                if (currentDestId == R.id.nav_profile) {
                    // Перезагружаем текущий фрагмент профиля
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
                    // Возвращаемся к корневому экрану профиля
                    navController.navigate(R.id.nav_profile);
                }
            }
        });
    }

    /**
     * Обрабатывает нажатие на кнопку добавления рецепта
     */
    private void handleAddButtonClick() {
        Log.d(TAG, "Нажата кнопка добавления рецепта");
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment != null) {
            Fragment currentFragment = navHostFragment.getChildFragmentManager().getPrimaryNavigationFragment();
            if (currentFragment != null) {
                currentFragment.onActivityResult(requestCode, resultCode, data);
            }
        }
        if (requestCode == REQUEST_ADD_RECIPE) {
            if (resultCode == RESULT_OK) {
                Log.d(TAG, "Получен результат: Рецепт добавлен");
                Toast.makeText(this, "Рецепт успешно добавлен", Toast.LENGTH_SHORT).show();
                refreshHomeFragment();
            } else {
                Log.d(TAG, "Получен результат: Отмена добавления рецепта");
            }
        }
    }

    /**
     * Обновляет домашний фрагмент при возврате из AddRecipeActivity
     */
    private void refreshHomeFragment() {
        // Обновляем рецепты через SharedRecipeViewModel при возврате из AddRecipeActivity
        sharedRecipeViewModel.refreshRecipes();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setQueryHint("Поиск по рецептам");
        // Стилизация SearchView (по желанию)
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

        // Логика поиска
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                performSearch(query);
                return true;
            }
            @Override
            public boolean onQueryTextChange(String newText) {
                // Можно реализовать live-поиск
                return false;
            }
        });

        // Показывать/скрывать заголовок Toolbar в зависимости от состояния поиска
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
        // Очищаем фокус с SearchView после отправки

        
        // Убедимся, что мы на главном экране или каталоге
        int currentDestId = navController.getCurrentDestination().getId();
        if (currentDestId != R.id.nav_home && currentDestId != R.id.nav_catalog) {
            // Если мы не на главном экране или в каталоге, переходим на главный экран
            bottomNavigationView.setSelectedItemId(R.id.nav_home);
        }
        
        // Выполняем поиск через SharedViewModel
        sharedRecipeViewModel.searchRecipes(query);
        
        // Сохранение в историю поиска
        Set<String> savedSet = searchPrefs.getStringSet("search_history", new LinkedHashSet<>());
        List<String> list = new ArrayList<>(savedSet);
        list.remove(query);
        list.add(0, query);
        if (list.size() > 10) list.remove(list.size() - 1);
        LinkedHashSet<String> newSet = new LinkedHashSet<>(list);
        searchPrefs.edit().putStringSet("search_history", newSet).apply();
        suggestionAdapter.clear();
        suggestionAdapter.addAll(list);
        suggestionAdapter.notifyDataSetChanged();

        // Логируем поисковый запрос
        Log.d(TAG, "Выполнен поиск по запросу: " + query);

    }

    // Показываем или скрываем кнопку поиска в зависимости от фрагмента
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem searchItem = menu.findItem(R.id.action_search);
        int id = navController.getCurrentDestination() != null
            ? navController.getCurrentDestination().getId() : -1;
        boolean visible = id == R.id.nav_home || id == R.id.nav_favorites;
        searchItem.setVisible(visible);
        if (!visible && searchItem.isActionViewExpanded()) {
            searchItem.collapseActionView();
        }
        return super.onPrepareOptionsMenu(menu);
    }
}
