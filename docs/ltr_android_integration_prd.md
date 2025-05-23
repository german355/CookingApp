# PRD: Клиентская интеграция Learning to Rank (LTR) в Android-приложение рецептов

**Версия:** 1.0  
**Дата:** 28.04.2025  
**Статус:** Черновик

## 1. Обзор продукта

### 1.1 Цель проекта

Интеграция клиентской части системы Learning to Rank (LTR) в Android-приложение для поиска рецептов. Android-приложение выступает ИСКЛЮЧИТЕЛЬНО в роли КЛИЕНТА, отправляя данные о взаимодействии пользователя на сервер и отображая полученные результаты. Вся обработка данных, обучение моделей и ранжирование выполняются на серверной стороне.

### 1.2 Бизнес-обоснование

- **Повышение удержания пользователей** за счет более точных результатов поиска, предоставляемых сервером
- **Увеличение вовлеченности** пользователей при работе с персонализированными рецептами
- **Отображение персонализированных результатов** для каждого пользователя на основе данных, обработанных на сервере
- **Конкурентное преимущество** перед другими кулинарными приложениями

### 1.3 Ключевые метрики успеха

- Увеличение среднего времени, проведенного в приложении, на 15%
- Рост CTR (Click-Through Rate) в результатах поиска на 20%
- Увеличение количества сохраненных рецептов на 25%
- Повышение оценки удовлетворенности пользователей на 0.5 балла (по шкале 1-5)

## 2. Функциональные требования

### 2.1 Основные компоненты клиентской части LTR в Android-приложении

#### 2.1.1 Сбор и отправка данных о взаимодействии пользователя

- Отслеживание кликов по результатам поиска для отправки на сервер
- Сбор данных о времени просмотра каждого рецепта
- Передача информации о действиях пользователя (сохранение, добавление в избранное, лайк)
- Отправка данных о прокрутке списка результатов поиска

#### 2.1.2 Компонент взаимодействия с сервером

- Передача пользовательских предпочтений на сервер
- Отправка истории поиска и взаимодействий
- Синхронизация избранных рецептов с сервером
- Получение персонализированных результатов с сервера

#### 2.1.3 Интерфейс отображения полученных результатов

- Визуальное отображение ранжированных на сервере результатов
- Индикация персонализированного ранжирования
- Возможность переключения между персонализированными и обычными результатами

### 2.2 Пользовательские сценарии

#### Сценарий 1: Запрос и отображение персонализированного поиска

1. Пользователь вводит запрос в строку поиска (например, "куриный суп с грибами")
2. Приложение отправляет запрос на сервер со всеми необходимыми данными пользователя
3. Сервер применяет LTR-ранжирование и возвращает готовые результаты
4. Приложение просто отображает полученные результаты
5. Система собирает данные о действиях пользователя с результатами для последующей отправки

#### Сценарий 2: Сбор данных о взаимодействии с результатами поиска

1. Пользователь просматривает результаты поиска
2. При каждом клике на рецепт приложение собирает данные о взаимодействии
3. Система фиксирует время, проведенное на странице рецепта
4. Все собранные данные отправляются на сервер в реальном времени или пакетно
5. Сервер обрабатывает полученные данные для улучшения будущих результатов

#### Сценарий 3: Отправка обратной связи для улучшения ранжирования

1. Пользователь может выставить явную оценку релевантности результата
2. Приложение собирает и отправляет эту информацию на сервер
3. Сервер использует полученные данные для улучшения модели ранжирования

#### Сценарий 4: Синхронизация избранного с сервером

1. Пользователь добавляет рецепт в избранное
2. Приложение отправляет данные об этом действии на сервер
3. Сервер использует эту информацию как сильный положительный сигнал для своих алгоритмов
4. При следующих запросах сервер может учитывать эти данные при ранжировании результатов

## 3. Технические требования

### 3.1 Архитектура клиентской части

```
[Пользовательский интерфейс]
       │
       ▼
[LTR Android Client SDK]
       │
       ├─► [Модуль сбора данных] ─► [Локальная очередь событий]
       │
       ├─► [Модуль передачи данных] ─► [Retrofit/OkHttp]
       │
       ├─► [Модуль отображения] ─► [RecyclerView/Adapters]
       │
       └─► [Кеш полученных результатов] ─► [Room DB]
```

### 3.2 Требования к Android клиентскому SDK

- **Минимальная версия Android:** API 21 (Android 5.0 Lollipop)
- **Целевая версия Android:** API 33 (Android 13)
- **Поддерживаемые языки программирования:** Java, Kotlin (опционально)
- **Размер SDK:** не более 1MB
- **Использование памяти:** <30MB при работе

### 3.3 Компоненты Java LTR клиентского SDK

```java
// Основной класс клиентского SDK
public class LTRClient {
    private static LTRClient instance;
    private final Context context;
    private final LTRDataCollector dataCollector;
    private final LTRApiClient apiClient;
    private final LTRResultsRenderer renderer;
    private final LTRCacheManager cacheManager;
    
    // Синглтон, инициализация
    public static synchronized LTRClient getInstance(Context context) {
        if (instance == null) {
            instance = new LTRClient(context.getApplicationContext());
        }
        return instance;
    }
    
    // Сбор данных о взаимодействии для отправки на сервер
    public void collectClickEvent(SearchResult result, int position) {...}
    public void collectViewDuration(Recipe recipe, long durationMs) {...}
    public void collectSaveAction(Recipe recipe) {...}
    public void collectFavoriteAction(Recipe recipe, boolean isFavorite) {...}
    
    // Запрос данных с сервера
    public void requestPersonalizedResults(String query, SearchRequestCallback callback) {...}
    public void requestSimilarRecipes(Recipe recipe, SimilarRecipesCallback callback) {...}
    
    // Методы работы с кешем
    public void clearResultsCache() {...}
    public void enablePersonalization(boolean enabled) {...}
    
    // Методы синхронизации с сервером
    public void syncFavoritesWithServer(List<Recipe> favorites) {...}
}

// Модуль сбора данных о взаимодействиях
public class LTRDataCollector {
    // Методы сбора разных типов взаимодействий
    // Очередь событий для отправки на сервер
}

// Модуль взаимодействия с API сервера
public class LTRApiClient {
    // Отправка собранных данных о взаимодействиях на сервер
    // Получение персонализированных результатов с сервера
}

// Модуль отображения результатов
public class LTRResultsRenderer {
    // Отображение полученных с сервера результатов
    // Визуализация индикаторов персонализации
}

// Модуль кеширования
public class LTRCacheManager {
    // Кеширование полученных с сервера результатов
    // Использование Room DB для хранения
}
```

### 3.4 Интеграция с серверным API

#### 3.4.1 Эндпоинты API для взаимодействия с LTR сервером

- `POST /search/click` - отправка данных о клике на результат поиска
- `GET /search` - запрос поиска с персонализированным ранжированием
- `POST /search/feedback` - отправка явной обратной связи
- `GET /user/preferences` - получение обработанных сервером персональных настроек
- `POST /user/favorites/sync` - синхронизация списка избранного с сервером

#### 3.4.2 Формат данных для отправки информации о взаимодействиях

```json
{
  "event_type": "click|view|save|favorite|unfavorite",
  "query": "текст поискового запроса",
  "recipe_id": 12345,
  "position": 3,
  "timestamp": 1682628583000,
  "duration_ms": 45000,
  "user_id": "user123",
  "session_id": "session456",
  "device_info": {
    "model": "Samsung Galaxy S21",
    "os_version": "Android 12",
    "app_version": "2.1.0"
  }
}
```

## 4. Реализация клиентской части в Android-приложении

### 4.1 Интеграция клиентского SDK в проект

```java
// В классе Application
public class CookingApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        
        // Инициализация LTR клиентского SDK
        LTRClient.getInstance(this)
                .setServerUrl("http://api.cooking-server.com")
                .setApiKey("YOUR_API_KEY")
                .enablePersonalization(true)
                .initialize();
        
        // Подготовка данных для синхронизации при старте
        recipesRepository.getFavorites().observe(lifecycleOwner, favorites -> {
            if (favorites != null && !favorites.isEmpty()) {
                LTRClient.getInstance(this).syncFavoritesWithServer(favorites);
            }
        });
    }
}
```

### 4.2 Реализация в поисковом активити

```java
public class SearchActivity extends AppCompatActivity {
    private LTRClient ltrClient;
    private RecipeAdapter adapter;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        
        ltrClient = LTRClient.getInstance(this);
        
        // Настройка поискового интерфейса
        setupSearchView();
        setupRecyclerView();
    }
    
    // Метод запроса персонализированного поиска с сервера
    private void requestSearch(String query) {
        // Отображение индикатора загрузки
        showLoading(true);
        
        // Запрос персонализированных результатов с сервера
        ltrClient.requestPersonalizedResults(query, new SearchRequestCallback() {
            @Override
            public void onSuccess(List<Recipe> recipes) {
                // Отображение результатов
                adapter.setRecipes(recipes);
                showLoading(false);
            }
            
            @Override
            public void onError(Exception e) {
                // Обработка ошибки
                showError(e.getMessage());
                showLoading(false);
            }
        });
    }
    
    // Обработка кликов на результаты для отправки данных на сервер
    private void setupRecyclerView() {
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        adapter = new RecipeAdapter();
        
        adapter.setOnItemClickListener((recipe, position) -> {
            // Сбор данных о клике для отправки на сервер
            ltrClient.collectClickEvent(recipe, position);
            
            // Переход на страницу рецепта
            navigateToRecipeDetails(recipe);
        });
        
        recyclerView.setAdapter(adapter);
    }
    
    // Переключатель персонализации
    private void setupPersonalizationToggle() {
        SwitchCompat personalizationSwitch = findViewById(R.id.personalizationSwitch);
        personalizationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            ltrClient.enablePersonalization(isChecked);
            // Повторный запрос для обновления результатов
            if (currentQuery != null) {
                requestSearch(currentQuery);
            }
        });
    }
}
```

### 4.3 Реализация на странице деталей рецепта

```java
public class RecipeDetailsActivity extends AppCompatActivity {
    private LTRClient ltrClient;
    private Recipe recipe;
    private long startViewTime;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe_details);
        
        ltrClient = LTRClient.getInstance(this);
        recipe = getIntent().getParcelableExtra("recipe");
        
        // Фиксируем время начала просмотра
        startViewTime = System.currentTimeMillis();
        
        // Настройка отображения
        setupRecipeView();
        setupSimilarRecipes();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        
        // Отправляем данные о длительности просмотра
        long viewDuration = System.currentTimeMillis() - startViewTime;
        ltrClient.collectViewDuration(recipe, viewDuration);
    }
    
    // Запрос похожих рецептов с сервера
    private void setupSimilarRecipes() {
        ltrClient.requestSimilarRecipes(recipe, new SimilarRecipesCallback() {
            @Override
            public void onSuccess(List<Recipe> similarRecipes) {
                // Отображение похожих рецептов
                showSimilarRecipes(similarRecipes);
            }
            
            @Override
            public void onError(Exception e) {
                // Скрытие секции с похожими рецептами при ошибке
                hideSimilarRecipesSection();
            }
        });
    }
    
    // Обработка добавления в избранное
    public void onFavoriteClick(View view) {
        boolean isFavorite = toggleFavoriteStatus(recipe);
        
        // Отправка данных на сервер
        ltrClient.collectFavoriteAction(recipe, isFavorite);
    }
}
```

## 5. Ограничения и риски

### 5.1 Ограничения клиентской части
- Все вычисления выполняются только на сервере
- Клиентская часть не содержит логики ранжирования
- Требуется подключение к интернету для получения персонализированных результатов
- Кеширование используется только для отображения последних полученных результатов

### 5.2 Риски
- Задержки в работе сервера могут привести к ухудшению пользовательского опыта
- Отсутствие подключения к интернету ограничивает персонализацию
- Несвоевременная отправка данных о взаимодействии может снизить качество персонализации

## 6. Метрики и мониторинг

### 6.1 Клиентские метрики для отслеживания
- Скорость отображения результатов, полученных с сервера
- Частота сбоев при отправке данных о взаимодействии
- Время ответа сервера на запросы персонализации
- Объем передаваемых данных
- Процент успешно отправленных событий взаимодействия

### 6.2 Инструменты мониторинга
- Firebase Performance для отслеживания скорости работы клиентской части
- Crashlytics для отслеживания сбоев
- Custom Events для анализа пользовательского поведения
- Network Profiler для анализа сетевого взаимодействия с сервером 