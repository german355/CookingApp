# Архитектура приложения "Cooking App"

## 1. Обзор архитектуры

Приложение "Cooking App" построено с использованием архитектурного паттерна **MVVM (Model-View-ViewModel)** и следует принципам чистой архитектуры, разделяя логику на слои:

*   **UI Layer (View)**: Отвечает за отображение данных и взаимодействие с пользователем.
*   **ViewModel Layer**: Содержит логику представления, управляет состоянием UI и взаимодействует со слоем данных.
*   **Data Layer**: Отвечает за предоставление и управление данными из различных источников (сеть, локальная база данных, SharedPreferences).

Эта архитектура обеспечивает четкое разделение обязанностей, улучшает тестируемость, поддерживаемость и масштабируемость кода.

## 2. Структура слоев и компонентов

### 2.1. UI Layer (View)

*   **Назначение**: Отображение информации пользователю, обработка пользовательского ввода и передача событий в ViewModel.
*   **Компоненты**:
    *   **Activities**: Основные контейнеры для UI (`MainActivity`, `RecipeDetailActivity`, `AddRecipeActivity`, `EditRecipeActivity`, `Regist`). `MainActivity` использует Navigation Component для управления фрагментами.
    *   **Fragments**: Представляют отдельные экраны или части экранов (`HomeFragment`, `FavoritesFragment`, `ProfileFragment`, `SettingsFragment`, `AuthFragment` и др.). Наблюдают за `LiveData` из соответствующих ViewModel для обновления UI.
    *   **Adapters**: (`RecipesAdapter`, `StepsAdapter`, `IngredientsAdapter` и др.) Используются для отображения списков данных в `RecyclerView`.
    *   **XML Layouts**: Определяют структуру и внешний вид пользовательского интерфейса.

### 2.2. ViewModel Layer

*   **Назначение**: Предоставление данных для UI Layer, обработка логики представления, управление состоянием UI и взаимодействие с Data Layer. Не содержит прямых ссылок на View.
*   **Компоненты**:
    *   **ViewModel классы**: (`HomeViewModel`, `RecipeDetailViewModel`, `FavoritesViewModel`, `ProfileViewModel`, `AuthViewModel`, `AddRecipeViewModel`, `EditRecipeViewModel`, `MainViewModel`). Каждый ViewModel соответствует определенному Activity или Fragment.
    *   **LiveData**: Используется для предоставления данных из ViewModel в UI Layer. UI подписывается на `LiveData` и автоматически обновляется при изменении данных. `LiveData` учитывает жизненный цикл компонентов View.
    *   **LikeSyncViewModel**: Shared ViewModel для синхронизации состояния лайков между различными фрагментами (`HomeFragment`, `FavoritesFragment`, `RecipeDetailActivity`).

### 2.3. Data Layer

*   **Назначение**: Абстрагирование источников данных и предоставление единого интерфейса для ViewModel Layer. Управляет получением, кэшированием и синхронизацией данных.
*   **Компоненты**:
    *   **Repositories**: Основные точки входа в Data Layer для ViewModel. Координируют работу с локальными и удаленными источниками данных.
        *   `RecipeRepository`: Фасад для получения данных о рецептах. Обращается к `RecipeLocalRepository` и `RecipeRemoteRepository`.
        *   `LikedRecipesRepository`: Управляет списком избранных рецептов, синхронизируя `LikedRecipeDao` (Room) с сервером (`ApiService` через Retrofit).
        *   `RecipeLocalRepository`: Предоставляет доступ к данным рецептов, кэшированным в локальной базе данных Room (`RecipeDao`).
        *   `RecipeRemoteRepository`: Обеспечивает взаимодействие с основным REST API сервера для CRUD операций с рецептами и лайками (использует `RecipeApi` через Retrofit и `OkHttp` напрямую).
        *   *Репозиторий для LTR* (внутри пакета `ltr`): Взаимодействует с LTR API через `LTRApiService` (Retrofit).
    *   **Data Sources**:
        *   **Remote**:
            *   **Основное API**: Взаимодействие через `Retrofit` (`ApiService`, `RecipeApi`) и `OkHttp`.
            *   **LTR API**: Взаимодействие через `Retrofit` (`LTRApiService`).
            *   **OkHttp**: HTTP-клиент, используемый Retrofit'ом и напрямую. Настроен с `AuthInterceptor` (добавление Bearer токена), `CacheInterceptor` (HTTP-кэширование), логированием.
        *   **Local**:
            *   **Room Database**: (`AppDatabase`, `RecipeDao`, `LikedRecipeDao`). Локальная персистентная база данных для кэширования `RecipeEntity` и `LikedRecipeEntity`. Используются `Converters` для сложных типов.
            *   **SharedPreferences**: (`MySharedPreferences`). Хранение простых данных: токен аутентификации, `userId`, `username`, `permission`, настройки.

## 3. Поток данных (Типичный сценарий)

1.  **Fragment/Activity (View)** инициирует действие (например, загрузка списка рецептов) или подписывается на `LiveData` в **ViewModel**.
2.  **ViewModel** запрашивает данные у соответствующего **Repository**.
3.  **Repository** решает, откуда предоставить данные:
    *   Проверяет локальный кэш (Room) через **Local Data Source** (`RecipeLocalRepository`/`LikedRecipesRepository`).
    *   Если данных нет или они устарели, обращается к **Remote Data Source** (`RecipeRemoteRepository` / `LTRApiService`) для запроса к сети.
4.  **Remote Data Source** выполняет сетевой запрос (Retrofit/OkHttp).
5.  Полученные данные (если запрос к сети был успешен) сохраняются в локальный кэш (Room) через **Local Data Source**.
6.  **Repository** возвращает данные (из кэша или сети) в **ViewModel**.
7.  **ViewModel** обрабатывает данные и обновляет соответствующий объект **LiveData**.
8.  **Fragment/Activity (View)**, подписанный на это **LiveData**, получает обновленные данные и перерисовывает UI.

## 4. Ключевые библиотеки и технологии

*   **Android Jetpack**: ViewModel, LiveData, Room, Navigation Component.
*   **Retrofit**: Типобезопасный HTTP-клиент для взаимодействия с REST API.
*   **OkHttp**: Основа для Retrofit, используется для настройки сети (кэширование, интерцепторы).
*   **Gson**: Сериализация/десериализация JSON.
*   **Room**: ORM для работы с локальной базой данных SQLite.
*   **ExecutorService**: Для выполнения фоновых задач.
*   **Glide / Picasso** (предположительно): Загрузка изображений.

## 5. Преимущества архитектуры

*   **Разделение ответственности**: Четкое разграничение UI, логики представления и работы с данными.
*   **Тестируемость**: Изоляция компонентов упрощает написание юнит-тестов (особенно для ViewModel и Data Layer).
*   **Поддерживаемость и масштабируемость**: Легкость внесения изменений и добавления нового функционала.
*   **Управление жизненным циклом**: Корректная работа с жизненным циклом Android-компонентов благодаря ViewModel и LiveData.
*   **Оффлайн-режим**: Возможность работы с кэшированными данными при отсутствии сети. 