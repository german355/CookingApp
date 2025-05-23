# Система авторизации в приложении "Cooking App"

## 1. Общий обзор

Аутентификация в приложении реализована на основе **Firebase Authentication** (Email/Password и Google Sign-In) и дополнена серверной синхронизацией через собственный API. Логика управления находится в `AuthViewModel`, который:

1. Использует `FirebaseAuthManager` для входа/регистрации в Firebase.
2. После успешного входа/регистрации вызывает методы `UserService` (`loginFirebaseUser`, `registerFirebaseUser`) для получения внутреннего `userId` и уровня доступа `permission` от сервера.
3. Сохраняет `userId`, `username`, `email` и `permission` в `MySharedPreferences`.
4. Запускает синхронизацию лайков через `LikedRecipesRepository`.
5. Управляет состоянием UI через `LiveData`: `isLoading`, `errorMessage`, `isAuthenticated`, `displayName`, `email`, `permission`.

## 2. Ключевые классы и компоненты

- **FirebaseAuthManager** (`com.example.cooking.auth.FirebaseAuthManager`)
  - Обёртка над Firebase SDK для Email/Password и Google Sign-In.
  - Методы: `signInWithEmailAndPassword`, `registerWithEmailAndPassword`, `updateUserDisplayName`, `signOut`, `initGoogleSignIn`, `handleGoogleSignInResult`.

- **AuthViewModel** (`com.example.cooking.ui.viewmodels.AuthViewModel`)
  - Методы:
    - `signInWithEmailPassword(email, password)`
    - `registerUser(email, password, username)`
    - `signInWithGoogle(activity)`
    - `handleGoogleSignInResult(requestCode, resultCode, data)`
    - `signOut()`
  - LiveData:
    - `LiveData<Boolean> isLoading` — состояние загрузки.
    - `LiveData<String> errorMessage` — текст ошибок.
    - `LiveData<Boolean> isAuthenticated` — флаг аутентификации.
    - `LiveData<String> displayName`, `LiveData<String> email`, `LiveData<Integer> permission` — данные пользователя.
  - Валидация форм: `validateEmail`, `validatePassword`, `validateName`, `validatePasswordConfirmation`.

- **UserService** (`com.example.cooking.network.services.UserService`)
  - Сервис для вызова API:
    - `loginFirebaseUser(email, firebaseId, callback)` — POST `/auth/login`.
    - `registerFirebaseUser(email, username, firebaseId, callback)` — POST `/auth/register`.
  - Колбэк получает `ApiResponse` с полями `success`, `userId`, `permission`, `message`.

- **MySharedPreferences** (`com.example.cooking.utils.MySharedPreferences`)
  - Хранит: `userId`, `username`, `email`, `permission`, `token`.

- **LikedRecipesRepository** (`com.example.cooking.data.repositories.LikedRecipesRepository`)
  - После успешного входа запускает `syncLikedRecipesFromServerIfNeeded(userId)` для загрузки актуального списка лайкнутых рецептов.

## 3. Подробные сценарии

### 3.1. Вход (Email/Password)

1. `AuthFragment` получает ввод пользователя и вызывает `AuthViewModel.signInWithEmailPassword(email, password)`.
2. Во ViewModel:
   1. Валидация: `validateEmail`, `validatePassword`. При невалидных данных устанавливается `errorMessage`.
   2. `isLoading.setValue(true)`.
   3. `authManager.signInWithEmailAndPassword(...)`.
3. В колбэке `onSuccess(FirebaseUser user)`:
   1. `userService.loginFirebaseUser(user.getEmail(), user.getUid(), ...)`.
   2. В `onSuccess(ApiResponse response)`:
      - Получает `internalUserId = response.getUserId()` и `permission = response.getPermission()`.
      - Если `userId` или `permission` отсутствуют — используются значения по умолчанию (`Firebase UID`, `1`).
      - Вызывает `saveUserData(user, internalUserId, permission)`.
      - `isLoading.postValue(false)` и `isAuthenticated.postValue(true)`.
      - `likedRecipesRepository.syncLikedRecipesFromServerIfNeeded(internalUserId)`.
   3. В `onFailure(...)`: сохраняет `Firebase UID` и `permission=1`, `isAuthenticated=true`, `errorMessage="Ошибка синхронизации..."`, `isLoading=false`.
4. В `onError(String message)` (Firebase): `errorMessage.setValue(message)` и `isLoading=false`.

### 3.2. Регистрация (Email/Password)

1. `RegisterActivity` собирает `email`, `password`, `username` и вызывает `AuthViewModel.registerUser(...)`.
2. Во ViewModel:
   1. Валидация: `validateEmail`, `validatePassword`, `validateName`.
   2. `isLoading.setValue(true)`.
   3. `authManager.registerWithEmailAndPassword(email, password, ...)`.
3. В `onSuccess(FirebaseUser user)`:
   1. `authManager.updateUserDisplayName(user, username, ...)`.
   2. В `onSuccess()`: `userService.registerFirebaseUser(user.getEmail(), username, user.getUid(), ...)`.
   3. В `onSuccess(ApiResponse response)`:
      - Получает `userId` и `permission`, сохраняет через `saveUserData`.
      - `isLoading=false`, `isAuthenticated=true`.
   4. В `onFailure(...)`: сохраняет `Firebase UID` и `permission=1`, `isAuthenticated=true`, `isLoading=false`.
4. В `onError(String message)` (Firebase): `errorMessage.setValue(message)` и `isLoading=false`.

### 3.3. Google Sign-In

1. `AuthFragment` инициализирует Google Sign-In через `AuthViewModel.initGoogleSignIn(webClientId)`.
2. При нажатии — `AuthViewModel.signInWithGoogle(activity)`.
3. В `onActivityResult` вызывается `AuthViewModel.handleGoogleSignInResult(requestCode, resultCode, data)`.
4. Аналогично Email/Password: после `onSuccess(FirebaseUser user)` вызывается `userService.loginFirebaseUser`, сохраняются данные и синхронизируются лайки.

### 3.4. Выход из аккаунта

Вызов `AuthViewModel.signOut()` выполняет:
1. `authManager.signOut()` (Firebase).
2. Сброс `userId`, `username`, `email`, `permission` в `MySharedPreferences`.
3. Обнуление LiveData: `isAuthenticated=false`, `displayName=""`, `email=""`, `permission=1`.

## 4. Хранение и валидация данных

- **`saveUserData`**: сохраняет внутренний `userId`, `username`, `email`, `permission` в `SharedPreferences` и обновляет LiveData.
- **Валидация**:
  - `validateEmail(String email)` — обязательный не пустой email, соответствует паттерну.
  - `validatePassword(String password)` — не менее 6 символов.
  - `validateName(String name)` — не менее 2 символов.
  - `validatePasswordConfirmation(String password, String confirmPassword)` — совпадение паролей.

---

*Эта документация отражает текущее состояние реализации аутентификации в приложении.* 