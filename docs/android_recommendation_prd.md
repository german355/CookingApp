# Требования к внедрению клиентской части системы рекомендаций для Android-приложения

## Обзор продукта

### Цель проекта
Интеграция клиентской части системы персонализированных рекомендаций рецептов в Android-приложение. Android-приложение выступает ИСКЛЮЧИТЕЛЬНО в роли КЛИЕНТА, все вычисления и обработка данных происходят на сервере с использованием компонентов Learning-to-Rank (LTR) и переранжирования. Клиентская часть отвечает за отображение результатов и сбор данных о взаимодействии пользователя для отправки на сервер.

### Бизнес-обоснование
- Увеличение вовлеченности пользователей за счет персонализированной выдачи, предоставляемой сервером
- Повышение коэффициента конверсии в приготовление блюд по рецептам
- Увеличение среднего времени использования приложения
- Рост числа повторных возвращений в приложение

### Ключевые метрики успеха
- Рост CTR в разделе рекомендаций на 25%
- Увеличение среднего времени сессии на 15%
- Повышение показателя DAU/MAU на 10%
- Увеличение числа просмотренных рецептов на одного пользователя на 20%

## Функциональные требования

### Основные компоненты на стороне Android-клиента

#### 1. Компонент сбора и отправки данных о взаимодействии пользователя
- Трекинг просмотров рецептов (время просмотра, глубина прокрутки)
- Отслеживание кликов по результатам поиска
- Сбор данных о добавлении в избранное и оценках
- Регистрация контекста выполнения действий (время суток, день недели, др.)
- Отправка событий на сервер в реальном времени или пакетная передача при восстановлении соединения

#### 2. Компонент получения и кеширования данных
- Кеширование полученных от сервера результатов для офлайн-доступа
- Хранение минимального набора пользовательских предпочтений для передачи серверу
- Интеграция с серверным API для получения персонализированных результатов
- Синхронизация избранных рецептов с сервером
- Передача параметров пользовательских настроек на сервер для учета в рекомендациях

#### 3. Интерфейс для отображения полученных результатов
- Компонент отображения персонализированной выдачи поиска
- Блок рекомендаций на главном экране, получаемых с сервера
- Секция "Вам может понравиться" после просмотра рецепта
- Карусель "Похожие рецепты" на странице рецепта
- Сезонные рекомендации, получаемые с учетом текущего времени года и локации

## Пользовательские сценарии

### Сценарий 1: Персонализированный поиск через сервер
1. Пользователь вводит поисковый запрос "суп"
2. Приложение отправляет запрос на сервер, передавая только необходимые данные:
   - Текст запроса
   - ID пользователя для персонализации
   - Список ID избранных рецептов (или указание, что нужно запросить с сервера)
   - Историю недавних просмотров (опционально)
3. Сервер выполняет всю обработку, ранжирование и возвращает готовые результаты
4. Приложение просто отображает полученные результаты в компоненте RecyclerView
5. При клике на рецепт приложение собирает данные о событии и отправляет их на сервер:
   - ID запроса
   - ID рецепта
   - Позицию в выдаче
   - Время от показа до клика

### Сценарий 2: Отправка данных об избранном для уточнения рекомендаций
1. Пользователь добавляет рецепт "Куриный суп" в избранное
2. Приложение отправляет только информацию о добавлении в избранное на сервер
3. При следующем запросе рекомендаций сервер учитывает обновленные данные и:
   - Возвращает рецепты с похожими ингредиентами и тегами с более высоким рангом
   - Включает рецепты, похожие на добавленные в избранное, в блок рекомендаций
4. Приложение отображает полученные результаты с индикатором "Рекомендовано на основе ваших предпочтений"

### Сценарий 3: Работа с кешированными данными при отсутствии сети
1. Пользователь открывает приложение без доступа к интернету
2. Приложение отображает кешированные результаты последнего запроса к серверу
3. Отображаются ранее полученные рекомендации на основе последних данных с сервера
4. После восстановления соединения приложение отправляет накопленные данные о действиях пользователя и запрашивает обновленные рекомендации

## Технические требования

### Архитектура Android-компонентов клиентской части

#### RecipeRecommendationClient (Java)
```java
public class RecipeRecommendationClient {
    private static RecipeRecommendationClient instance;
    private final Context appContext;
    private final RecommendationApiService apiService;
    private final ResultsCacheManager cacheManager;
    private final UserInteractionCollector interactionCollector;
    
    // Singleton pattern
    public static synchronized RecipeRecommendationClient getInstance(Context context) {
        if (instance == null) {
            instance = new RecipeRecommendationClient(context.getApplicationContext());
        }
        return instance;
    }
    
    private RecipeRecommendationClient(Context context) {
        this.appContext = context;
        this.apiService = new RecommendationApiService();
        this.cacheManager = new ResultsCacheManager(context);
        this.interactionCollector = new UserInteractionCollector(context);
    }
    
    // Методы для получения данных с сервера
    public void requestPersonalizedSearchResults(String query, int limit, SearchResultCallback callback);
    public void requestRecommendedRecipes(int limit, RecommendationCallback callback);
    public void requestSimilarRecipes(long recipeId, int limit, RecommendationCallback callback);
    
    // Сбор данных о взаимодействии для отправки на сервер
    public void collectRecipeView(long recipeId);
    public void collectSearchClick(String query, long recipeId, int position);
    public void collectFavoriteAction(long recipeId, boolean isFavorite);
    public void collectRatingAction(long recipeId, float rating);
    
    // Синхронизация и передача настроек
    public void syncDataWithServer();
    public void sendUserPreferences(UserPreferences preferences);
}
```

#### UserInteractionCollector (Java)
```java
public class UserInteractionCollector {
    private final Context context;
    private final DatabaseHelper dbHelper;
    private final ApiService apiService;
    
    // Конструктор, инициализация
    
    // Сбор событий для последующей отправки на сервер
    public void collectEvent(UserEvent event);
    
    // Пакетная отправка событий на сервер
    public void sendCollectedEvents();
    
    // Обработка соединения
    public void handleConnectivityChange(boolean isConnected);
}
```

#### ResultsCacheManager (Java)
```java
public class ResultsCacheManager {
    private final Context context;
    private final SharedPreferences preferences;
    private final RecipeDatabase database;
    
    // Кеширование результатов, полученных с сервера
    public void cacheSearchResults(String query, List<Recipe> results);
    public void cacheRecommendations(String recommendationType, List<Recipe> recommendations);
    
    // Получение кешированных результатов при отсутствии соединения
    public List<Recipe> getCachedSearchResults(String query);
    public List<Recipe> getCachedRecommendations(String recommendationType);
    
    // Управление устаревшими данными
    public void invalidateOldCache();
}
```

### SDK требования
- Минимальная версия Android API: 21 (Android 5.0)
- Целевая версия Android API: 33 (Android 13)
- Библиотеки:
  - Retrofit для API запросов к серверу
  - Room для локального кеширования данных, полученных с сервера
  - WorkManager для фоновой отправки данных на сервер
  - Dagger/Hilt для внедрения зависимостей

## Интеграция с серверным API

### Основные эндпоинты для запросов от клиента

#### 1. Получение готовых персонализированных результатов
```
GET /api/search
```
Параметры:
- `query` (string): Поисковый запрос
- `user_id` (string): Идентификатор пользователя
- `size` (int, optional): Количество результатов (по умолчанию 20)
- `favorites` (array, optional): Массив ID избранных рецептов

Ответ (готовые результаты с сервера):
```json
{
  "success": true,
  "query": "суп",
  "processing_time_ms": 235,
  "results": [
    {
      "id": "123",
      "title": "Куриный суп с лапшой",
      "photo_url": "http://example.com/images/123.jpg",
      "score": 0.89,
      "ingredients": ["курица", "лапша", "морковь"],
      "time": 45,
      "is_recommended_based_on_favorites": true
    },
    // ...
  ]
}
```

#### 2. Отправка данных о клике для обработки на сервере
```
POST /search/click
```
Тело запроса:
```json
{
  "query": "суп",
  "doc_id": 123,
  "position": 2,
  "user_id": "user_uuid",
  "timestamp": "2023-05-23T14:35:42Z",
  "session_id": "session_uuid",
  "click_context": {
    "device": "Android",
    "view_time_ms": 1250,
    "app_version": "2.3.1"
  }
}
```

#### 3. Синхронизация избранных рецептов с сервером
```
POST /user/favorites/sync
```
Тело запроса:
```json
{
  "user_id": "user_uuid",
  "device_id": "device_uuid",
  "favorites": [
    {
      "recipe_id": 123,
      "added_at": "2023-05-20T10:15:30Z"
    },
    {
      "recipe_id": 456,
      "added_at": "2023-05-22T18:30:45Z"
    }
  ],
  "last_sync_timestamp": "2023-05-19T00:00:00Z"
}
```

#### 4. Получение обновлений для кеширования на клиенте
```
GET /api/recommendation/cached-data
```
Параметры:
- `user_id` (string): Идентификатор пользователя
- `device_id` (string): Идентификатор устройства
- `last_sync`: Временная метка последней синхронизации

Ответ: Данные для кеширования на клиенте на случай отсутствия сети

## Хранение данных на клиенте

### Локальная база данных для кеширования (Room)

#### CachedRecipeEntity
```java
@Entity(tableName = "cached_recipes")
public class CachedRecipeEntity {
    @PrimaryKey
    private long id;
    private String title;
    private String ingredients;
    private String instructions;
    private String photoUrl;
    private int preparationTime;
    private String cuisine;
    private boolean isFavorite;
    private long lastViewed;
    private float serverScore;
    private long cachedAt;
    
    // Геттеры и сеттеры
}
```

#### UserInteractionQueueEntity
```java
@Entity(tableName = "interaction_queue")
public class UserInteractionQueueEntity {
    @PrimaryKey(autoGenerate = true)
    private long id;
    private String eventType; // VIEW, SEARCH_CLICK, FAVORITE, RATING
    private long recipeId;
    private String query; // Для поисковых событий
    private int position; // Для поисковых событий
    private float rating; // Для рейтинговых событий
    private long timestamp;
    private boolean isSent;
    
    // Геттеры и сеттеры
}
```

## Дорожная карта внедрения клиентской части

### Фаза 1: Базовая интеграция с серверным API (4 недели)
- Создание компонентов сбора данных о взаимодействии пользователя
- Интеграция с API для получения готовых результатов поиска
- Реализация передачи контекста пользователя серверу
- Добавление компонентов отображения полученных результатов

### Фаза 2: Расширенное взаимодействие с сервером (3 недели)
- Реализация синхронизации избранного с сервером
- Добавление компонентов отображения различных типов рекомендаций
- Отправка данных о взаимодействии с рекомендациями на сервер
- Добавление индикаторов персонализации в интерфейсе

### Фаза 3: Кеширование и работа при отсутствии сети (3 недели)
- Реализация кеширования полученных с сервера данных
- Создание механизма отложенной отправки событий
- Оптимизация производительности и сетевого взаимодействия
- A/B тестирование различных способов отображения рекомендаций

### Фаза 4: Аналитика и улучшения (2 недели)
- Интеграция с системами аналитики для отслеживания эффективности
- Добавление расширенных метрик использования клиентской части
- Оптимизация на основе полученных данных
- Подготовка к релизу

## Ограничения и риски

### Ограничения
- Клиентская часть должна работать на устройствах с ограниченной памятью (минимум 2GB RAM)
- Объем кешируемых данных не должен превышать 50MB на устройстве
- Сбор данных о взаимодействии не должен увеличивать расход батареи более чем на 5%

### Риски
- Задержка ответа от сервера может негативно влиять на пользовательский опыт
- Несоответствие между кешированными и актуальными данными при длительном отсутствии сети
- Потенциальная потеря данных о взаимодействии при проблемах с отправкой на сервер

## Метрики и мониторинг клиентской части

### Ключевые метрики для отслеживания
- Скорость отображения полученных с сервера результатов
- Частота успешной отправки данных на сервер
- Объем данных, передаваемых между клиентом и сервером
- Время работы с кешированными данными при отсутствии сети
- Процент успешно синхронизированных взаимодействий

### Инструменты мониторинга клиентской части
- Firebase Analytics для отслеживания пользовательских взаимодействий
- Custom Events для метрик взаимодействия с рекомендациями
- Crashlytics для отслеживания ошибок и исключений
- Performance Monitoring для оценки скорости загрузки и отзывчивости UI 