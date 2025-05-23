# Стек технологий приложения "Cooking"

## Технологический стек

Приложение "Cooking" построено с использованием современного технологического стека для разработки Android-приложений, который обеспечивает надежность, производительность и соответствие лучшим практикам разработки.

### Языки программирования

- **Java 11** - основной язык разработки
- **XML** - для описания UI-разметок

### Фреймворки и библиотеки

#### Архитектурные компоненты Android

- **ViewModel** - для хранения и управления данными, связанными с UI
- **LiveData** - компонент наблюдения за данными с учетом жизненного цикла
- **Room** - абстракция над SQLite для работы с локальной базой данных
- **Navigation Component** - для управления навигацией между фрагментами

#### Сетевой слой

- **Retrofit 2** (2.9.0) - REST-клиент для вызова API
- **OkHttp3** (4.12.0) - HTTP-клиент для низкоуровневых запросов
- **Gson** (2.9.0) - для сериализации/десериализации JSON

#### UI и анимации

- **Material Design Components** (1.12.0) - дизайн-система Google
- **RecyclerView** - для эффективного отображения списков
- **ConstraintLayout** (2.2.0) - для построения адаптивных интерфейсов
- **Glide** (4.16.0) - для загрузки и кэширования изображений
- **SwipeRefreshLayout** (1.1.0) - для реализации функции pull-to-refresh

#### Firebase

- **Firebase Authentication** - для управления аутентификацией пользователей
- **Firebase Analytics** - для сбора аналитики
- **Google Sign-In** (21.0.0) - для авторизации через аккаунт Google

#### Многопоточность и асинхронность

- **ExecutorService** — для выполнения фоновых задач (Room, OkHttp, синхронизация данных)

### Инструменты сборки и управления зависимостями

- **Gradle** (8.9) - система сборки
- **Version Catalog** - для централизованного управления версиями зависимостей

### Системные требования

- **Min SDK**: 34 (Android 14)
- **Target SDK**: 34 (Android 14)
- **Compile SDK**: 35

## Версии используемых библиотек

### Основные компоненты
```
androidx.appcompat:appcompat:1.7.0
androidx.activity:activity:1.10.0
com.google.android.material:material:1.12.0
androidx.constraintlayout:constraintlayout:2.2.0
androidx.swiperefreshlayout:swiperefreshlayout:1.1.0
androidx.coordinatorlayout:coordinatorlayout:1.2.0
androidx.core:core:1.12.0
```

### Сетевой стек
```
com.squareup.retrofit2:retrofit:2.9.0
com.squareup.retrofit2:converter-scalars:2.9.0
com.squareup.retrofit2:converter-gson:2.9.0
com.squareup.okhttp3:okhttp:4.12.0
com.squareup.okhttp3:logging-interceptor:4.12.0
com.google.code.gson:gson:2.9.0
io.socket:socket.io-client:2.0.0
```

### Работа с Firebase
```
com.google.firebase:firebase-bom:33.11.0
com.google.firebase:firebase-analytics
com.google.firebase:firebase-auth
com.google.android.gms:play-services-auth:21.0.0
```

### Работа с БД и архитектурные компоненты
```
androidx.room:room-runtime:2.6.1
androidx.room:room-compiler:2.6.1
androidx.room:room-ktx:2.6.1
androidx.lifecycle:lifecycle-viewmodel:2.6.2
androidx.lifecycle:lifecycle-livedata:2.6.2
androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2
```

### Загрузка изображений
```
com.github.bumptech.glide:glide:4.16.0
```

### Инструменты многопоточности
```
org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3
```

## Интеграции

- **Google Fonts** - для использования шрифтов (Montserrat)
- **Firebase Console** - для мониторинга и управления сервисами Firebase

## Требования к окружению разработки

- **Android Studio Iguana** или выше
- **JDK 11** или выше
- **Gradle 8.9** или выше 