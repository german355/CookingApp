# Полное руководство по интеграции API

Вся система аутентификации была переработана для повышения безопасности. Все эндпоинты, работающие с приватными данными пользователя, теперь требуют аутентификации через Firebase ID токен.

## Часть 1: Общие принципы

### 1.1 Получение токена на клиенте (JavaScript)

Перед отправкой любого запроса к защищенным эндпоинтам необходимо получить актуальный токен от Firebase.

```javascript
import { getAuth } from "firebase/auth";

const auth = getAuth();
const user = auth.currentUser;

let idToken = null;
if (user) {
  try {
    // Рекомендуется принудительно обновлять токен, если он мог устареть
    idToken = await user.getIdToken(true); 
  } catch (error) {
    console.error("Ошибка получения ID токена:", error);
    // Здесь нужно обработать ошибку (например, разлогинить пользователя)
  }
}

if (!idToken) {
  // Обработка случая, когда пользователь не аутентифицирован
  return; 
}
```

### 1.2 Отправка токена и обработка ошибок

Во все запросы к защищенным эндпоинтам нужно добавлять заголовок `Authorization`.

```javascript
const headers = {
  // 'Content-Type' зависит от типа запроса. 
  // Для JSON: 'application/json'. 
  // Для FormData: не указывать, браузер выставит сам.
  'Authorization': `Bearer ${idToken}` // idToken из примера выше
};

try {
  const response = await fetch('URL_ЭНДПОИНТА', {
    method: 'POST', // GET, PUT, DELETE
    headers: headers,
    body: ... // Тело запроса, если оно есть
  });

  if (!response.ok) {
    // Обработка ошибок аутентификации и других
    if (response.status === 401) {
      // Проблема с токеном (неверный, истек) -> нужно перелогиниться
      console.error("Ошибка аутентификации (401).");
    } else if (response.status === 403) {
      // Нет прав доступа (например, не подтвержден email или попытка изменить чужой рецепт)
      console.error("Доступ запрещен (403).");
    } else {
      // Другие ошибки сервера
      console.error(`Ошибка: ${response.status}`);
    }
  }
  
  const data = await response.json();
  // ... обработка успешного ответа
  
} catch (error) {
  console.error("Сетевая ошибка или ошибка выполнения запроса:", error);
}


### 2.4 Чат-бот (`/chatbot`)

#### **Начать новую сессию**
- **Эндпоинт**: `POST /chatbot/start-session`
- **Изменение**: Тело запроса стало пустым. Добавлен `Authorization`.

**До:**
```javascript
const res = await fetch('/chatbot/start-session', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ user_id: 123 })
});
```
**После:**
```javascript
const idToken = await user.getIdToken();
const res = await fetch('/chatbot/start-session', {
  method: 'POST',
  headers: { 'Authorization': `Bearer ${idToken}` }
});
```

#### **Отправить сообщение**
- **Эндпоинт**: `POST /chatbot/send-message`
- **Изменение**: Убран `user_id` из тела. Добавлен `Authorization`.

**До:**
```javascript
const res = await fetch('/chatbot/send-message', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ user_id: 123, message: "Привет!" })
});
```
**После:**
```javascript
const idToken = await user.getIdToken();
const res = await fetch('/chatbot/send-message', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${idToken}`
  },
  body: JSON.stringify({ message: "Привет!" })
});
```

#### **Получить историю сообщений**
- **Эндпоинт**: `GET /chatbot/get-history`
- **Изменение**: Убран `user_id` из параметров URL. Добавлен `Authorization`.

**До:**
```javascript
const res = await fetch('/chatbot/get-history?user_id=123');
```
**После:**
```javascript
const idToken = await user.getIdToken();
const res = await fetch('/chatbot/get-history', {
  method: 'GET',
  headers: { 'Authorization': `Bearer ${idToken}` }
});
``` 