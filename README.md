# 🍳 Cooking App

**Мобильное приложение для любителей кулинарии на Android**

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://www.android.com/)
[![Java](https://img.shields.io/badge/Language-Java-orange.svg)](https://www.java.com/)
[![Min SDK](https://img.shields.io/badge/Min%20SDK-23-blue.svg)](https://developer.android.com/studio/releases/platforms)
[![Target SDK](https://img.shields.io/badge/Target%20SDK-34-blue.svg)](https://developer.android.com/studio/releases/platforms)

## 📱 О проекте

Cooking App - это современное мобильное приложение для Android, предназначенное для любителей кулинарии. Приложение позволяет пользователям находить, создавать, сохранять и делиться рецептами, а также получать помощь от искусственного интеллекта в кулинарных вопросах.

## ✨ Основные возможности

- 🔍 **Каталог рецептов** - Просмотр и поиск рецептов с фильтрацией
- 📝 **Создание рецептов** - Добавление собственных рецептов с фотографиями
- ✏️ **Редактирование** - Изменение существующих рецептов
- ❤️ **Избранное** - Сохранение понравившихся рецептов
- 🔐 **Аутентификация** - Вход через email/пароль или Google аккаунт
- 🤖 **AI-помощник** - Интегрированный чат с ИИ для кулинарных советов
- 📱 **Офлайн режим** - Работа без интернета с локальным кэшированием
- 🔗 **Deep Links** - Возможность делиться рецептами через ссылки

## 🛠 Технологический стек

### Архитектура
- **MVVM** (Model-View-ViewModel) с элементами Clean Architecture
- **Repository Pattern** для работы с данными
- **Use Cases** для бизнес-логики

### Основные технологии
- **Java** - Язык программирования
- **Android Architecture Components** - ViewModel, LiveData, Navigation
- **Room** - Локальная база данных
- **Retrofit** + **OkHttp** - Сетевые запросы
- **RxJava 3** - Реактивное программирование
- **Firebase Auth** - Аутентификация
- **Glide** - Загрузка и кэширование изображений
- **Material Design** - UI компоненты

### Зависимости
```gradle
// Сеть
implementation 'com.squareup.retrofit2:retrofit:2.9.0'
implementation 'com.squareup.okhttp3:okhttp:4.12.0'

// База данных
implementation 'androidx.room:room-runtime:2.6.1'

// UI и навигация
implementation 'androidx.navigation:navigation-fragment:2.7.7'
implementation 'com.google.android.material:material:1.9.0'

// Реактивное программирование
implementation 'io.reactivex.rxjava3:rxjava:3.1.8'

// Аутентификация
implementation 'com.google.firebase:firebase-auth'
```

## 🏗 Структура проекта

```
app/src/main/java/com/example/cooking/
├── auth/                    # Аутентификация Firebase
├── config/                  # Конфигурация приложения
├── data/                    # Слой данных
│   ├── database/           # Room база данных
│   ├── models/             # Модели данных
│   └── repositories/       # Репозитории
├── domain/                  # Бизнес-логика
│   ├── entities/           # Доменные сущности
│   ├── services/           # Сервисы
│   ├── usecases/           # Use Cases
│   └── validators/         # Валидаторы
├── network/                 # Сетевой слой
│   ├── api/                # API интерфейсы
│   ├── interceptors/       # HTTP интерцепторы
│   ├── models/             # DTO модели
│   └── services/           # Сетевые сервисы
└── ui/                      # Пользовательский интерфейс
    ├── activities/         # Activities
    ├── adapters/           # RecyclerView адаптеры
    ├── fragments/          # Fragments
    ├── viewmodels/         # ViewModels
    └── widgets/            # Кастомные виджеты
```

## 🚀 Установка и запуск

### Требования
- Android Studio Arctic Fox или новее
- Android SDK API level 23+
- JDK 11+

### Шаги установки

1. **Клонирование репозитория**
```bash
git clone <repository-url>
cd CookingApp
```

2. **Настройка Firebase**
   - Создайте проект в Firebase Console
   - Добавьте Android приложение с package name `com.example.cooking`
   - Скачайте `google-services.json` и поместите в папку `app/`

3. **Настройка keystore (для release сборки)**
   - Создайте файл `gradle.properties` в корне проекта
   - Добавьте переменные:
   ```properties
   RELEASE_STORE_PASSWORD=your_store_password
   RELEASE_KEY_ALIAS=your_key_alias
   RELEASE_KEY_PASSWORD=your_key_password
   ```

4. **Сборка и запуск**
```bash
./gradlew assembleDebug
```

## 📖 Документация

Подробная документация находится в папке [`documentation/`](./documentation/):

- [**Введение**](./documentation/01_introduction.md) - Обзор проекта и возможностей
- [**Архитектура**](./documentation/03_architecture.md) - Детали архитектуры и паттернов
- [**API Backend**](./documentation/04_backend_api.md) - Описание REST API
- [**База данных**](./documentation/05_database.md) - Схема локальной БД
- [**Функциональность**](./documentation/06_features.md) - Подробное описание функций

## 🏛 Архитектура

Приложение построено на основе **MVVM** архитектуры с элементами **Clean Architecture**:

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   UI Layer      │    │  Domain Layer   │    │   Data Layer    │
│                 │    │                 │    │                 │
│ • Activities    │◄──►│ • Use Cases     │◄──►│ • Repositories  │
│ • Fragments     │    │ • Entities      │    │ • Room DB       │
│ • ViewModels    │    │ • Validators    │    │ • Network API   │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

### Принципы
- **Разделение ответственности** - каждый слой имеет четкую зону ответственности
- **Инверсия зависимостей** - верхние слои не зависят от нижних
- **Тестируемость** - изолированные компоненты легко тестировать
- **Масштабируемость** - модульная структура облегчает расширение

## 🔄 Поток данных

1. **UI** инициирует действие
2. **ViewModel** вызывает **Use Case**
3. **Use Case** обращается к **Repository**
4. **Repository** получает данные из сети или локальной БД
5. Данные возвращаются через **LiveData**
6. **UI** обновляется автоматически

## 🧪 Тестирование

```bash
# Unit тесты
./gradlew test

# Инструментальные тесты
./gradlew connectedAndroidTest
```

## 📄 Лицензия

Этот проект является учебным и создан в образовательных целях.

## 👥 Разработка

Проект разработан с использованием современных практик Android разработки:
- Clean Architecture принципы
- SOLID принципы
- Repository pattern
- Dependency Injection
- Reactive Programming с RxJava

---

**Cooking App** - Готовьте с удовольствием! 🍳✨ 


Если вы нашли баг или ошибку то напишите об этом ассистенту в приложении или на почту cookingappteam@gmail.com
