[**Главная**](./README.md) | [**Введение**](./01_introduction.md) | [**Архитектура**](./03_architecture.md) | [**База данных**](./05_database.md) | [**Функции**](./06_features.md)
***
# API бэкенда

Приложение взаимодействует с бэкенд-сервером через REST API. Все эндпоинты определены в интерфейсе `ApiService.java`.

**Базовый URL**: `http://89.35.130.107/`

---

## Аутентификация

### `POST /auth/register`

Регистрация нового пользователя.

-   **Тело запроса**: `UserRegisterRequest`
    ```json
    {
      "email": "user@example.com",
      "name": "string",
      "firebaseId": "string"
    }
    ```
-   **Ответ**: `ApiResponse`

### `POST /auth/login`

Вход пользователя. Идентификация происходит через Firebase токен, который добавляется в заголовки через `AuthInterceptor`.

-   **Ответ**: `ApiResponse`

### `POST /auth/password-reset-request`

Запрос на сброс пароля.

-   **Тело запроса**: `PasswordResetRequest`
    ```json
    {
      "email": "user@example.com"
    }
    ```
-   **Ответ**: `ApiResponse`

---

## Рецепты

### `GET /recipes`

Получение списка всех рецептов.

-   **Ответ**: `RecipesResponse`

### `POST /recipes/add`

Добавление нового рецепта. Отправляется как `multipart/form-data`.

-   **Параметры**:
    -   `title` (RequestBody)
    -   `ingredients` (RequestBody)
    -   `instructions` (RequestBody)
    -   `photo` (MultipartBody.Part, опционально)
-   **Ответ**: `GeneralServerResponse`

### `PUT /recipes/update/{id}`

Обновление существующего рецепта по его ID.

-   **Параметры URL**: `id` (int) - ID рецепта
-   **Заголовки**: `X-User-Permission` - токен, подтверждающий право на редактирование
-   **Тело запроса**: `multipart/form-data` с полями `title`, `ingredients`, `instructions`, `photo`.
-   **Ответ**: `GeneralServerResponse`

### `DELETE /recipes/{id}`

Удаление рецепта по его ID.

-   **Параметры URL**: `id` (int) - ID рецепта
-   **Заголовки**: `X-User-Permission` - токен, подтверждающий право на удаление
-   **Ответ**: `GeneralServerResponse`

---

## Лайки

### `GET /recipes/liked`

Получение списка ID лайкнутых рецептов пользователя.

-   **Ответ**: `LikedRecipesResponse`

### `POST /recipes/{recipeId}/like`

Поставить или убрать лайк с рецепта.

-   **Параметры URL**: `recipeId` (int) - ID рецепта
-   **Ответ**: `GeneralServerResponse`

---

## Поиск

### `GET /recipes/search-simple`

Простой поиск рецептов.

-   **Query параметры**: `q` (String) - поисковый запрос
-   **Ответ**: `SearchResponse`

### `GET /search`

Расширенный поиск с пагинацией.

-   **Query параметры**:
    -   `q` (String) - поисковый запрос
    -   `page` (int) - номер страницы
    -   `per_page` (int) - элементов на странице
-   **Ответ**: `SearchResponse`

---

## AI Чат

### `POST /chatbot/start-session`

Начало новой сессии чата.

-   **Ответ**: `ChatSessionResponse`

### `POST /chatbot/send-message`

Отправка сообщения в чат.

-   **Тело запроса**: `ChatMessageRequest`
-   **Ответ**: `ChatMessageResponse`

### `GET /chatbot/get-history`

Получение истории сообщений текущей сессии.

-   **Ответ**: `ChatHistoryResponse`
***
[**⬆ К оглавлению**](./README.md) 