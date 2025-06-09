# Детализированный план миграции репозиториев на RxJava

## Цель
- Убрать колбэки в существующих репозиториях, используя RxJava3
- Сохранить имена методов, менять только сигнатуры
- Исключить любые изменения в части AI-чата (ChatRepository и методы chat в ApiService)

## 1. Подготовка
1. Создать ветку `feature/rxjava-migration`
2. Зафиксировать текущее состояние (git tag или архив)
3. Провести анализ в папке `app/src/main/java/com/example/cooking/data/repositories`:
   - RecipeRemoteRepository.java (`getRecipes`)
   - RecipeLocalRepository.java (`getAllRecipes`, `getAllRecipesSync`, `insertAll`, `insert`, `update`, `updateLikeStatus`, `getLikedRecipes`, `getRecipeById`, `getRecipeByIdSync`, `clearAll`, `clearAllSync`, `deleteRecipe`, `replaceAllRecipes`)
   - UnifiedRecipeRepository.java (`loadRemoteRecipes`, `saveRecipe`, `updateRecipe`, `insert`, `deleteRecipe`, внутренние интерфейсы колбэков)
   - LikedRecipesRepository.java (`getLikedRecipes`)
   - NetworkRepository.java (методы `executeInBackground`, `isNetworkAvailable`)
   - **Не трогать** ChatRepository.java и раздел AI-chat в ApiService.java
4. Найти все интерфейсы и enqueue/ApiCallHandler вызовы с callback

## 2. Добавление зависимостей
В `app/build.gradle` добавить:
```groovy
implementation "io.reactivex.rxjava3:rxjava:3.1.5"
implementation "io.reactivex.rxjava3:rxandroid:3.0.0"
implementation "com.squareup.retrofit2:adapter-rxjava3:2.9.0"
```

## 3. Рефакторинг ApiService.java
- Для всех методов в разделе "Рецепты" (`@GET("recipes")`, `@GET("recipes/liked")`, `searchRecipesSimple`, `addRecipe`, `addRecipeWithoutPhoto`, `updateRecipe`, `deleteRecipe`, `toggleLikeRecipe`, `searchRecipes`) заменить `Call<...>` на `Single<...>`
- Удалить импорты `retrofit2.Call`, `retrofit2.Callback`
- Добавить в билдер Retrofit:
  ```java
  .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
  ```

## 4. Рефакторинг RecipeRemoteRepository.java
Сохраняя название `getRecipes`:
```java
public Single<List<Recipe>> getRecipes() {
    return apiService.getRecipes()
        .map(response -> {
            List<Recipe> list = response.getRecipes();
            if (list != null) return list;
            else throw new IllegalStateException("Список пуст");
        })
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread());
}
``` 
- Удалить интерфейс `RecipesCallback` и старый вызов `ApiCallHandler`

## 5. Рефакторинг RecipeLocalRepository.java
Для каждого метода:
- `public Flowable<List<Recipe>> getAllRecipes()`
  - Если DAO поддерживает `Flowable`, то вернуть `recipeDao.getAllRecipesRx().map(entities->...)`
  - Иначе: `Flowable.fromCallable(() -> recipeDao.getAllRecipesSync()).map(...)`
- `public Single<List<Recipe>> getAllRecipesSync()` → `Single.fromCallable(() -> { List<Recipe> ... })`
- `insertAll`, `insert`, `update`, `updateLikeStatus`, `clearAll`, `deleteRecipe` → `public Completable insertAll(...){ return Completable.fromAction(() -> recipeDao.insertAll(...)); }`
- `getLikedRecipes()` → `public Flowable<List<Recipe>> getLikedRecipes()`
- `getRecipeById`, `getRecipeByIdSync` аналогично через `Single`
- Метод `replaceAllRecipes` через `Completable`
- Удалить `executeInBackground` и LiveData-трансформации, если заменены на Rx

## 6. Рефакторинг UnifiedRecipeRepository.java
- Заменить `loadRemoteRecipes(RecipesCallback)` на `public Single<List<Recipe>> loadRemoteRecipes()` → `return recipeRemoteRepository.getRecipes()`
- `saveRecipe`, `updateRecipe`, `insert`, `deleteRecipe`:
  - Сигнатуры → `Single<GeneralServerResponse>` или `Completable`
  - Реализация через `apiService.addRecipe(...).subscribeOn(...).observeOn(...)`
- Удалить вложенные интерфейсы колбэков (RecipeSaveCallback, DeleteRecipeCallback)

## 7. Обновление ViewModel и UI
- В `SharedRecipeViewModel`:
  - Добавить `CompositeDisposable disposables`
  - Заменить вызовы с callback на `disposables.add(repository.getRecipes().subscribe(...))`
  - Очистка `disposables.clear()` в `onCleared()`
- Activity/Fragment:
  - Подписка на LiveData или Rx (autoDispose)
  - Удалить callback-listeners

## 8. Тестирование
- Unit-тесты с `TestScheduler`, мок ApiService (Single/Flowable)
- Интеграционные тесты для UnifiedRecipeRepository

## 9. Документация и ревью
- Обновить README (раздел RxJava)
- Провести PR-review, убедиться в отсутствии callback-интерфейсов
