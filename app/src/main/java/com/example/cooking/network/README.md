# Сетевой слой приложения Cooking

Этот документ описывает архитектуру сетевого слоя приложения Cooking.

## Структура каталогов

```
network/
  ├── api/            - Интерфейсы API для Retrofit
  ├── interceptors/   - Перехватчики OkHttp для обработки запросов
  ├── models/         - Модели данных для сетевого слоя
  ├── responses/      - Классы ответов от API
  ├── services/       - Сервисы для работы с API
  └── utils/          - Утилиты для работы с сетью
```

## Основные компоненты

### NetworkService

Центральный класс для управления сетевыми запросами. Предоставляет:
- Настроенный OkHttpClient с перехватчиками
- Настроенный Retrofit клиент
- Метод для получения ApiService

### ApiService

Единый интерфейс API для взаимодействия с сервером. Содержит методы для:
- Аутентификации и управления пользователями
- Получения и поиска рецептов
- Создания, обновления и удаления рецептов

### BaseApiResponse

Базовый класс для всех ответов от API. Содержит общие поля:
- success - флаг успешности запроса
- message - сообщение от сервера
- status - статус ответа

### ApiCallHandler

Утилитный класс для обработки API-запросов. Предоставляет:
- Единый интерфейс для обработки ответов
- Обработку ошибок
- Преобразование ответов в удобный формат

### Resource

Обобщенный класс для представления состояний ответов API. Используется для передачи данных между слоями приложения с учетом статуса:
- SUCCESS - успешное выполнение запроса
- ERROR - ошибка при выполнении запроса
- LOADING - запрос выполняется

### RetryInterceptor

Перехватчик для повторных попыток запросов при сетевых ошибках или серверных ошибках (5xx).

## Репозитории

### NetworkRepository

Базовый класс для всех репозиториев, работающих с сетью. Предоставляет:
- Общую функциональность для сетевых запросов
- Проверку наличия интернет-соединения
- Выполнение операций в фоновом потоке

### RecipeRepository

Репозиторий для управления данными рецептов. Предоставляет:
- Получение списка рецептов с кэшированием
- Поиск рецептов
- Получение списка лайкнутых рецептов
- Загрузка конкретного рецепта по ID

### UserRepository

Репозиторий для управления данными пользователей. Предоставляет:
- Регистрация новых пользователей
- Вход пользователей
- Восстановление пароля
- Управление данными текущего пользователя

## Использование

### Получение API-сервиса

```java
// Получение API-сервиса
ApiService apiService = NetworkService.getApiService(context);
```

### Использование репозиториев

```java
// Использование RecipeRepository
RecipeRepository recipeRepository = new RecipeRepository(context);
recipeRepository.getRecipes(userId).observe(this, resource -> {
    switch (resource.getStatus()) {
        case LOADING:
            // Отображаем индикатор загрузки
            showLoading();
            break;
        case SUCCESS:
            // Отображаем данные
            hideLoading();
            showRecipes(resource.getData());
            break;
        case ERROR:
            // Отображаем ошибку
            hideLoading();
            showError(resource.getMessage());
            break;
    }
});

// Использование UserRepository
UserRepository userRepository = new UserRepository(context);
userRepository.loginUser(email, password).observe(this, resource -> {
    switch (resource.getStatus()) {
        case LOADING:
            // Отображаем индикатор загрузки
            showLoginProgress();
            break;
        case SUCCESS:
            // Сохраняем данные пользователя и переходим к главному экрану
            hideLoginProgress();
            ApiResponse response = resource.getData();
            userRepository.saveUserData(response);
            navigateToHome();
            break;
        case ERROR:
            // Отображаем ошибку
            hideLoginProgress();
            showLoginError(resource.getMessage());
            break;
    }
});
```

### Выполнение запроса с ApiCallHandler

```java
// Выполнение запроса с обработкой ответа
ApiCallHandler.execute(call, new ApiCallHandler.ApiCallback<RecipesResponse>() {
    @Override
    public void onSuccess(RecipesResponse response) {
        // Обработка успешного ответа
        List<Recipe> recipes = response.getRecipes();
    }
    
    @Override
    public void onError(String errorMessage) {
        // Обработка ошибки
        showErrorMessage(errorMessage);
    }
});

// Выполнение запроса с LiveData
LiveData<Resource<RecipesResponse>> recipesLiveData = ApiCallHandler.asLiveData(call);
recipesLiveData.observe(this, resource -> {
    // Обработка Resource
});
```

## Рекомендации по использованию

1. Используйте `NetworkService` для получения ApiService.
2. Используйте репозитории для доступа к данным из разных источников.
3. Используйте `Resource` для передачи данных с учетом статуса запроса.
4. Все новые модели ответов должны наследоваться от `BaseApiResponse`.
5. Не создавайте новые интерфейсы API, а добавляйте методы в существующий `ApiService`.
6. Для обработки специфических ошибок используйте `ApiCallHandler.ApiCallback`.

## Обработка ошибок

Все ошибки обрабатываются централизованно в `ApiCallHandler`. Типы ошибок:
- Ошибки сервера (5xx) - повторяются автоматически через `RetryInterceptor`
- Ошибки клиента (4xx) - возвращаются в `onError` с сообщением
- Сетевые ошибки - возвращаются в `onError` с понятным сообщением

## Авторизация

Авторизация обрабатывается через `AuthInterceptor`, который добавляет Firebase ID токен в заголовок запроса. 