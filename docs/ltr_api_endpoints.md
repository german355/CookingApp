# API для клиент-серверного взаимодействия в системе Learning to Rank

Этот документ детально описывает все API эндпоинты, необходимые для взаимодействия Android-клиента с серверной частью системы Learning to Rank (LTR) кулинарного приложения. Android-приложение выступает ИСКЛЮЧИТЕЛЬНО в роли КЛИЕНТА, отправляющего данные и отображающего полученные результаты.

## Базовая информация

- **Базовый URL**: `http://api.cooking-server.com/v2`
- **Формат данных**: JSON
- **Аутентификация**: Bearer Token
- **Заголовки для всех запросов**:
  - `Authorization: Bearer {token}`
  - `Content-Type: application/json`
  - `Accept: application/json`
  - `User-Agent: CookingApp/2.1.0 Android/{android_version}`
  - `X-API-Key: {api_key}`

## 1. Эндпоинты для получения данных с сервера

### 1.1. Запрос персонализированных результатов поиска

**Эндпоинт**: `GET /search`

**Описание**: Запрашивает у сервера поисковые результаты, ранжированные с использованием серверной LTR-модели и персонализированные под конкретного пользователя.

**Параметры запроса**:

| Параметр | Тип | Обязательный | Описание |
|----------|-----|--------------|----------|
| q | string | Да | Поисковый запрос пользователя |
| page | integer | Нет | Номер страницы (по умолчанию 1) |
| per_page | integer | Нет | Количество результатов на странице (по умолчанию 20, максимум 50) |
| use_personalization | boolean | Нет | Применять ли персонализацию на сервере (по умолчанию true) |
| user_preferences | object | Нет | Объект с предпочтениями пользователя для передачи серверу |
| favorite_categories | array | Нет | Массив ID предпочитаемых категорий для учета на сервере |
| dietary_restrictions | array | Нет | Массив строк с диетическими ограничениями |
| cooking_time_max | integer | Нет | Максимальное время приготовления в минутах |

**Пример запроса от клиента**:
```
GET /search?q=куриный%20суп&page=1&per_page=20&use_personalization=true
```

С телом запроса для дополнительных параметров:
```json
{
  "user_preferences": {
    "favorite_categories": [3, 8, 12],
    "dietary_restrictions": ["vegetarian", "no_gluten"],
    "cooking_time_max": 60
  }
}
```

**Пример ответа сервера**:
```json
{
  "status": "success",
  "total_results": 128,
  "page": 1,
  "per_page": 20,
  "results": [
    {
      "recipe_id": 12345,
      "title": "Куриный суп с лапшой",
      "description": "Классический куриный суп с домашней лапшой",
      "rating": 4.8,
      "votes_count": 243,
      "cooking_time": 45,
      "image_url": "http://cooking-server.com/images/recipes/12345.jpg",
      "ingredients_count": 8,
      "is_favorite": false,
      "category": {
        "id": 3,
        "name": "Супы"
      },
      "server_score": 0.98,
      "personalization_reason": "Похож на рецепты, которые вам нравятся"
    },
    // ... другие результаты ...
  ],
  "search_metadata": {
    "query_analysis": "Запрос на поиск куриного супа",
    "applied_filters": ["personalization"],
    "suggestion": null
  }
}
```

### 1.2. Получение персональных настроек пользователя с сервера

**Эндпоинт**: `GET /user/preferences`

**Описание**: Получает обработанные сервером персональные настройки и предпочтения пользователя, вычисленные на основе предыдущих взаимодействий.

**Параметры запроса**: Нет

**Пример ответа сервера**:
```json
{
  "status": "success",
  "preferences": {
    "favorite_categories": [3, 8, 12],
    "frequent_ingredients": ["chicken", "tomato", "pasta"],
    "average_cooking_time": 45,
    "dietary_preferences": ["low_carb"],
    "taste_profile": {
      "sweet": 0.3,
      "salty": 0.7,
      "spicy": 0.5,
      "sour": 0.2,
      "bitter": 0.1
    },
    "search_history": {
      "recent_queries": ["паста карбонара", "фрикадельки", "салат цезарь"],
      "frequent_terms": ["быстрый", "простой", "курица", "мясо"]
    },
    "personalization_score": 0.75
  }
}
```

### 1.3. Получение рекомендованных рецептов

**Эндпоинт**: `GET /recommendations`

**Описание**: Запрашивает у сервера персонализированные рекомендации рецептов, вычисленные на основе предыдущих взаимодействий пользователя.

**Параметры запроса**:

| Параметр | Тип | Обязательный | Описание |
|----------|-----|--------------|----------|
| type | string | Нет | Тип рекомендаций: "similar", "daily", "trending" (по умолчанию "daily") |
| recipe_id | integer | Нет | ID рецепта для получения похожих (обязателен при type="similar") |
| count | integer | Нет | Количество рекомендаций (по умолчанию 10, максимум 50) |

**Пример запроса от клиента**:
```
GET /recommendations?type=similar&recipe_id=12345&count=5
```

**Пример ответа сервера**:
```json
{
  "status": "success",
  "recommendation_type": "similar",
  "source_recipe_id": 12345,
  "recommendations": [
    {
      "recipe_id": 12346,
      "title": "Куриный суп с вермишелью",
      "similarity_score": 0.92,
      "similarity_factors": ["ингредиенты", "время приготовления"]
    },
    // ... другие рекомендации ...
  ]
}
```

## 2. Эндпоинты для отправки данных на сервер

### 2.1. Отправка данных о клике на результат поиска

**Эндпоинт**: `POST /search/click`

**Описание**: Отправляет на сервер данные о клике пользователя на результат поиска для последующего использования в LTR-моделях.

**Тело запроса от клиента**:

```json
{
  "recipe_id": 12345,
  "query": "куриный суп",
  "position": 2,
  "timestamp": 1682628583000,
  "session_id": "session456",
  "search_results_count": 20,
  "click_context": {
    "previous_views": [45678, 78901],
    "is_from_suggestion": false,
    "view_type": "search_results"
  }
}
```

**Пример ответа сервера**:
```json
{
  "status": "success",
  "message": "Click data received successfully"
}
```

### 2.2. Отправка обратной связи о релевантности

**Эндпоинт**: `POST /search/feedback`

**Описание**: Отправляет на сервер явную обратную связь пользователя о релевантности результата поиска для улучшения LTR-моделей.

**Тело запроса от клиента**:

```json
{
  "recipe_id": 12345,
  "query": "куриный суп",
  "relevance_score": 4,
  "feedback_type": "explicit",
  "timestamp": 1682628583000,
  "comments": "Отличный рецепт, очень похож на то, что я искал"
}
```

**Пример ответа сервера**:
```json
{
  "status": "success",
  "message": "Feedback received, thank you!"
}
```

### 2.3. Отправка данных о пользовательских предпочтениях

**Эндпоинт**: `PUT /user/preferences`

**Описание**: Отправляет на сервер данные о предпочтениях пользователя, которые будут использоваться для персонализации.

**Тело запроса от клиента**:

```json
{
  "favorite_categories": [3, 8, 12, 15],
  "dietary_preferences": ["low_carb", "high_protein"],
  "cooking_time_max": 60
}
```

**Пример ответа сервера**:
```json
{
  "status": "success",
  "message": "User preferences received and processed"
}
```

### 2.4. Синхронизация избранных рецептов с сервером

**Эндпоинт**: `POST /user/favorites/sync`

**Описание**: Отправляет на сервер данные об избранных рецептах пользователя для улучшения персонализации и ранжирования.

**Тело запроса от клиента**:

```json
{
  "favorites": [
    {
      "recipe_id": 12345,
      "added_at": 1682628583000
    },
    {
      "recipe_id": 67890,
      "added_at": 1682528583000
    },
    {
      "recipe_id": 13579,
      "added_at": 1682028583000
    }
  ],
  "device_id": "device123",
  "last_sync_timestamp": 1682000000000
}
```

**Пример ответа сервера**:
```json
{
  "status": "success",
  "message": "Favorites received and processed",
  "favorites_count": 3,
  "last_sync_timestamp": 1682628590000
}
```

### 2.5. Отправка данных о просмотре рецепта

**Эндпоинт**: `POST /recipe/view`

**Описание**: Отправляет на сервер данные о просмотре рецепта, включая продолжительность просмотра.

**Тело запроса от клиента**:

```json
{
  "recipe_id": 12345,
  "view_duration_ms": 45000,
  "scroll_depth_percent": 85,
  "timestamp": 1682628583000,
  "came_from": "search_results",
  "source_query": "куриный суп",
  "source_position": 2
}
```

**Пример ответа сервера**:
```json
{
  "status": "success",
  "message": "View data received successfully"
}
```

## 3. Эндпоинты для работы в режиме офлайн

### 3.1. Получение данных для кеширования

**Эндпоинт**: `GET /cache/data`

**Описание**: Получает данные для кеширования на клиенте и использования при отсутствии подключения к сети. Все алгоритмы ранжирования остаются на сервере.

**Параметры запроса**:

| Параметр | Тип | Обязательный | Описание |
|----------|-----|--------------|----------|
| last_sync | timestamp | Да | Временная метка последней синхронизации |
| types | array | Нет | Типы данных для кеширования: ["trending", "favorites", "recent"] |
| max_size | integer | Нет | Максимальный размер кеша (в элементах) |

**Пример запроса от клиента**:
```
GET /cache/data?last_sync=1682000000000&types=trending,favorites&max_size=50
```

**Пример ответа сервера**:
```json
{
  "status": "success",
  "cache_data": {
    "trending": [
      {
        "recipe_id": 12345,
        "title": "Куриный суп с лапшой",
        "image_url": "http://cooking-server.com/images/recipes/12345.jpg",
        "cooking_time": 45,
        "category_id": 3
      },
      // ... другие рецепты ...
    ],
    "favorites": [
      {
        "recipe_id": 67890,
        "title": "Паста карбонара",
        "image_url": "http://cooking-server.com/images/recipes/67890.jpg",
        "cooking_time": 30,
        "category_id": 8
      },
      // ... другие рецепты ...
    ]
  },
  "cache_timestamp": 1682628590000,
  "cache_ttl_hours": 24
}
```

### 3.2. Отправка накопленных данных о взаимодействиях

**Эндпоинт**: `POST /interactions/batch`

**Описание**: Отправляет на сервер накопленные данные о взаимодействиях пользователя, собранные в режиме офлайн.

**Тело запроса от клиента**:

```json
{
  "interactions": [
    {
      "type": "view",
      "recipe_id": 12345,
      "view_duration_ms": 45000,
      "timestamp": 1682628583000
    },
    {
      "type": "click",
      "recipe_id": 67890,
      "query": "паста",
      "position": 3,
      "timestamp": 1682628183000
    },
    {
      "type": "favorite",
      "recipe_id": 13579,
      "is_favorite": true,
      "timestamp": 1682627583000
    }
  ],
  "device_id": "device123",
  "offline_since": 1682627000000
}
```

**Пример ответа сервера**:
```json
{
  "status": "success",
  "message": "Batch interactions received",
  "processed_count": 3,
  "server_timestamp": 1682628590000
}
```

## 4. Форматы данных для взаимодействия клиента с сервером

### 4.1. Формат данных о взаимодействии пользователя

```json
{
  "event_type": "click|view|save|favorite|unfavorite",
  "recipe_id": 12345,
  "query": "текст поискового запроса",
  "position": 3,
  "timestamp": 1682628583000,
  "duration_ms": 45000,
  "user_id": "user123",
  "session_id": "session456",
  "device_info": {
    "model": "Samsung Galaxy S21",
    "os_version": "Android 12",
    "app_version": "2.1.0",
    "connection_type": "wifi|mobile|offline"
  }
}
```

### 4.2. Формат данных о рецепте, возвращаемых сервером

```json
{
  "recipe_id": 12345,
  "title": "Куриный суп с лапшой",
  "description": "Классический куриный суп с домашней лапшой",
  "cooking_time": 45,
  "difficulty": "easy",
  "portions": 4,
  "calories_per_portion": 320,
  "image_url": "http://cooking-server.com/images/recipes/12345.jpg",
  "ingredients": [
    {
      "name": "Курица",
      "amount": "500",
      "unit": "г"
    },
    // ... другие ингредиенты ...
  ],
  "instructions": [
    "Поставить курицу вариться в большой кастрюле",
    "Нарезать овощи и добавить их к курице",
    // ... другие шаги ...
  ],
  "tags": ["суп", "курица", "обед", "классика"],
  "category": {
    "id": 3,
    "name": "Супы"
  },
  "author": {
    "id": 789,
    "name": "Шеф Иванов"
  },
  "rating": {
    "average": 4.8,
    "votes_count": 243
  },
  "server_ranking_info": {
    "base_score": 0.85,
    "personalization_score": 0.13,
    "final_score": 0.98,
    "ranking_factors": [
      {
        "name": "ingredient_match",
        "contribution": 0.4
      },
      {
        "name": "favorite_similarity",
        "contribution": 0.3
      },
      {
        "name": "popularity",
        "contribution": 0.2
      },
      {
        "name": "freshness",
        "contribution": 0.1
      }
    ]
  }
}
```

## 5. Коды ошибок и обработка ошибок

### 5.1. Общие коды ошибок

| Код | Описание |
|-----|----------|
| 400 | Неверный запрос (отсутствуют обязательные параметры или неверный формат) |
| 401 | Ошибка аутентификации (неверный токен) |
| 403 | Доступ запрещен (недостаточно прав) |
| 404 | Ресурс не найден |
| 429 | Слишком много запросов (превышен лимит) |
| 500 | Внутренняя ошибка сервера |

### 5.2. Формат ответа при ошибке

```json
{
  "status": "error",
  "code": 400,
  "message": "Invalid request parameters",
  "details": {
    "missing_params": ["query"],
    "invalid_params": {
      "per_page": "Should be a number between 1 and 50"
    }
  },
  "request_id": "req-12345"
}
```

## 6. Рекомендации по интеграции для Android-клиента

### 6.1. Управление сетевыми запросами

- Используйте Retrofit для работы с API
- Реализуйте кеширование ответов с использованием OkHttp Cache
- Применяйте Rate Limiting для предотвращения излишней нагрузки на сервер
- Используйте Interceptors для добавления заголовков авторизации

### 6.2. Отправка событий взаимодействия

- Группируйте события в пакеты при отправке для уменьшения количества запросов
- Используйте WorkManager для отправки данных в фоне
- Реализуйте механизм сохранения событий при отсутствии подключения
- Добавьте повторные попытки для неудачных запросов

### 6.3. Обработка ответов сервера

- Всегда проверяйте поле "status" в ответе сервера
- Реализуйте обработку всех возможных кодов ошибок
- При получении ошибки 429 используйте экспоненциальное увеличение задержки
- Логируйте детали ошибок для отладки

### 6.4. Управление кешем

- Кешируйте данные с учетом TTL, указанного сервером
- Реализуйте очистку устаревших данных
- Используйте Room для хранения кешированных данных
- Приоритизируйте свежие данные при наличии подключения к сети