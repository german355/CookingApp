# Система работы с рецептами в приложении "Cooking"

## Обзор системы рецептов

Система рецептов является основной функциональной частью приложения "Cooking". Она обеспечивает следующие возможности:

- Отображение списка доступных рецептов
- Детальный просмотр отдельного рецепта
- Добавление рецепта в избранное
- Создание и редактирование собственных рецептов
- Поиск и фильтрация рецептов по различным критериям

## Архитектура системы рецептов

### Основные компоненты

1. **Модели данных**:
   - `Recipe` - основная модель рецепта
   - `Ingredient` - модель ингредиента
   - `Step` - модель шага приготовления

2. **UI компоненты**:
   - `HomeFragment` - отображение списка рецептов
   - `FavoritesFragment` - отображение избранных рецептов
   - `RecipeDetailActivity` - детальный просмотр рецепта
   - `AddRecipeActivity` - создание/редактирование рецепта

3. **ViewModel компоненты**:
   - `RecipesViewModel` - управление данными рецептов на главном экране
   - `FavoritesViewModel` - управление избранными рецептами
   - `RecipeDetailViewModel` - управление данными отдельного рецепта
   - `AddRecipeViewModel` - управление процессом создания/редактирования рецепта

4. **Адаптеры**:
   - `RecipesAdapter` - адаптер для списка рецептов
   - `IngredientsAdapter` - адаптер для списка ингредиентов
   - `StepsAdapter` - адаптер для списка шагов приготовления

5. **Сервисы**:
   - `RecipeService` - сервис для работы с API рецептов
   - `RecipeRepository` - репозиторий для доступа к данным рецептов
   - `FavoriteManager` - управление избранными рецептами

## Модель данных рецепта

### Основные поля модели Recipe

```java
public class Recipe {
    private String id;           // Уникальный идентификатор рецепта
    private String title;        // Название рецепта
    private String description;  // Краткое описание
    private String imageUrl;     // URL изображения рецепта
    private int cookingTime;     // Время приготовления в минутах
    private int servings;        // Количество порций
    private String difficulty;   // Сложность приготовления
    private String userId;       // ID пользователя, создавшего рецепт
    private List<Ingredient> ingredients; // Список ингредиентов
    private List<Step> steps;    // Список шагов приготовления
    private boolean isFavorite;  // Флаг избранного
    private Date createdAt;      // Дата создания
    private Date updatedAt;      // Дата обновления
    
    // Геттеры, сеттеры и другие методы
}
```

### Поля модели Ingredient

```java
public class Ingredient {
    private String id;
    private String name;        // Название ингредиента
    private double quantity;    // Количество
    private String unit;        // Единица измерения
    
    // Геттеры, сеттеры и другие методы
}
```

### Поля модели Step

```java
public class Step {
    private int order;          // Порядковый номер шага
    private String description; // Описание шага
    private String imageUrl;    // Изображение (опционально)
    
    // Геттеры, сеттеры и другие методы
}
```

## Жизненный цикл рецепта

### Получение списка рецептов

1. `HomeFragment` запрашивает список рецептов у `RecipesViewModel`
2. `RecipesViewModel` делает запрос к `RecipeRepository`
3. `RecipeRepository` проверяет наличие кэшированных данных в локальной БД
4. Если данные актуальны, возвращает их; иначе делает API-запрос через `RecipeService`
5. Полученные данные сохраняются в Room и возвращаются во ViewModel
6. ViewModel обновляет LiveData, которая наблюдается фрагментом
7. `HomeFragment` отображает полученный список с помощью `RecipesAdapter`

### Добавление рецепта в избранное

1. Пользователь нажимает кнопку "Добавить в избранное" в UI
2. `RecipesAdapter` (или другой UI-компонент) вызывает метод в ViewModel
3. ViewModel делегирует запрос в `FavoriteManager`
4. `FavoriteManager` обновляет данные в локальной БД и синхронизирует с сервером
5. Обновленный статус избранного передается обратно в UI

### Создание нового рецепта

1. Пользователь заполняет форму в `AddRecipeActivity`
2. `AddRecipeViewModel` валидирует введенные данные
3. При сохранении вызывается метод в `RecipeRepository`
4. `RecipeRepository` создает запись в локальной БД и отправляет данные на сервер
5. Пользователь перенаправляется на экран деталей нового рецепта

## Взаимодействие с API

### Актуальные эндпоинты основного API (частично Retrofit, частично OkHttp)

*   `POST /recipes/add`
    *   **Описание**: Добавление/обновление рецепта (multipart/form-data или JSON).
    *   **Клиент**: `AddRecipeViewModel` / `EditRecipeViewModel` (через OkHttp).
*   `GET /recipes`
    *   **Описание**: Получение списка всех рецептов (с учетом лайков для `userId`).
    *   **Параметр**: `userId`.
    *   **Клиент**: `RecipeApi` (Retrofit) -> `RecipeRemoteRepository`.
*   `GET /recipes/{recipe_id}`
    *   **Описание**: Получение деталей одного рецепта.
    *   **Клиент**: `RecipeRepository` (вероятно, OkHttp или Retrofit при кэш-промахе).
*   `DELETE /recipes/{recipe_id}`
    *   **Описание**: Удаление рецепта.
    *   **Заголовки**: `Authorization`, `X-User-ID`, `X-User-Permission`.
    *   **Клиент**: `RecipeDeleter` (OkHttp).
*   `POST /recipes/{recipe_id}/like`
    *   **Описание**: Добавление/удаление лайка (переключение).
    *   **Тело запроса**: JSON с `userId`.
    *   **Клиент**: `RecipeRemoteRepository`, `RecipeDetailViewModel` (OkHttp).
*   `GET /recipes/liked`
    *   **Описание**: Получение списка лайкнутых рецептов.
    *   **Параметр**: `userId`.
    *   **Клиент**: `ApiService` (Retrofit) -> `LikedRecipesRepository`.

## Кэширование и локальное хранение

### Room Database

Для локального хранения рецептов и избранного используется Room Database со следующими сущностями:

*   `RecipeEntity`: Хранит основные данные рецептов (`id`, `title`, `ingredients` (строка), `instructions` (строка), `photo_url`, `user_id` и т.д.).
*   `LikedRecipeEntity`: Хранит связь между пользователем (`userId`) и лайкнутым рецептом (`recipeId`), а также дату добавления (`likedAt`).


### Стратегия кэширования

1. **Сохранение данных**:
   - Все загруженные с сервера рецепты сохраняются в базе данных
   - Избранные рецепты отмечаются в отдельной таблице

2. **Инвалидация кэша**:
   - Автоматическое обновление при запуске приложения
   - Pull-to-refresh для принудительного обновления
   - Тайм-аут кэша: 1 час для общего списка, 5 минут для избранных

3. **Офлайн-режим**:
   - Приложение работает с локальными данными при отсутствии сети
   - При восстановлении соединения производится синхронизация

## Отображение рецептов

### Основные макеты

1. **recipe_list_item.xml** - элемент списка рецептов:
   - Миниатюра изображения
   - Название рецепта
   - Краткое описание
   - Время приготовления
   - Иконка "Избранное"

2. **fragment_home.xml** - домашний экран со списком рецептов:
   - SearchView для поиска
   - Фильтры категорий
   - RecyclerView со списком рецептов
   - FAB для добавления нового рецепта

3. **activity_recipe_detail.xml** - экран детального просмотра:
   - Изображение рецепта
   - Название и описание
   - Информация о времени и порциях
   - Список ингредиентов
   - Пошаговая инструкция
   - Кнопка "Добавить в избранное"

### Реализация RecipesAdapter

```java
public class RecipesAdapter extends RecyclerView.Adapter<RecipesAdapter.RecipeViewHolder> {

    private List<Recipe> recipes = new ArrayList<>();
    private OnRecipeClickListener listener;
    private OnFavoriteClickListener favoriteListener;
    
    // Интерфейсы для обработки событий
    public interface OnRecipeClickListener {
        void onRecipeClick(Recipe recipe);
    }
    
    public interface OnFavoriteClickListener {
        void onFavoriteClick(Recipe recipe, boolean isFavorite);
    }
    
    // Метод обновления данных
    public void updateRecipes(List<Recipe> newRecipes) {
        this.recipes = newRecipes;
        notifyDataSetChanged();
    }
    
    // ViewHolder и другие методы адаптера
}
```

## Рекомендации по расширению

1. **Добавление новых фильтров**:
   - Добавьте новые поля в модель `Recipe`
   - Расширьте запросы в DAO и SQL-запросы
   - Добавьте UI-элементы для фильтрации

2. **Внедрение категорий**:
   - Создайте новые сущности в Room: `Category` и `RecipeCategory`
   - Расширьте API для получения и фильтрации по категориям
   - Добавьте UI для выбора категорий

3. **Улучшение поиска**:
   - Внедрите полнотекстовый поиск в Room
   - Добавьте поиск по ингредиентам
   - Реализуйте прогнозирующий ввод и подсказки

4. **Модерация контента**:
   - Добавьте статус модерации в модель
   - Расширьте API для модерации рецептов
   - Реализуйте функцию жалоб на некачественный контент 