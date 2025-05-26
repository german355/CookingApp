package com.example.cooking.ui.viewmodels;

import android.app.Application;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import android.net.Uri;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.cooking.Recipe.Ingredient;
import com.example.cooking.Recipe.Recipe;
import com.example.cooking.Recipe.Step;
import com.example.cooking.data.repositories.UnifiedRecipeRepository;
import com.example.cooking.network.models.GeneralServerResponse;
import com.example.cooking.utils.MySharedPreferences;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * ViewModel для экрана редактирования рецепта.
 */
public class EditRecipeViewModel extends AndroidViewModel {
    private static final String TAG = "EditRecipeViewModel";

    private final UnifiedRecipeRepository unifiedRecipeRepository;
    private final MySharedPreferences preferences;
    private final ExecutorService executor;

    // LiveData для состояний UI
    private final MutableLiveData<Boolean> isSaving = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> saveResult = new MutableLiveData<>(null);

    // LiveData для данных рецепта
    private final MutableLiveData<Integer> recipeId = new MutableLiveData<>();
    private final MutableLiveData<String> title = new MutableLiveData<>("");
    private final MutableLiveData<List<Ingredient>> ingredients = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Step>> steps = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> photoUrl = new MutableLiveData<>(null); // URL исходного фото
    private final MutableLiveData<byte[]> imageBytes = new MutableLiveData<>(null); // Новое выбранное/загруженное фото
    private boolean imageChanged = false; // Флаг, указывающий, было ли изменено изображение
    private Uri selectedImageUri = null; // Uri выбранного изображения

    public EditRecipeViewModel(@NonNull Application application) {
        super(application);
        unifiedRecipeRepository = new UnifiedRecipeRepository(application, Executors.newSingleThreadExecutor());
        preferences = new MySharedPreferences(application);
        executor = Executors.newSingleThreadExecutor();
    }

    /**
     * Устанавливает начальные данные рецепта для редактирования из объекта Recipe.
     * @param recipeToEdit Объект Recipe, полученный из Intent.
     */
    public void setRecipeData(@NonNull Recipe recipeToEdit) {
        recipeId.setValue(recipeToEdit.getId());
        title.setValue(recipeToEdit.getTitle() != null ? recipeToEdit.getTitle() : "");
        photoUrl.setValue(recipeToEdit.getPhoto_url()); // Сохраняем исходный URL
        imageBytes.setValue(null); // Сбрасываем новое/выбранное изображение
        selectedImageUri = null; // Сбрасываем Uri
        imageChanged = false;

        // Получаем списки напрямую
        List<Ingredient> initialIngredients = recipeToEdit.getIngredients();
        List<Step> initialSteps = recipeToEdit.getSteps();

        // Убедимся, что списки не null и содержат хотя бы один элемент
        if (initialIngredients == null || initialIngredients.isEmpty()) {
            initialIngredients = new ArrayList<>();
            initialIngredients.add(new Ingredient()); // Добавляем пустой для редактирования
        }
        if (initialSteps == null || initialSteps.isEmpty()) {
            initialSteps = new ArrayList<>();
            Step firstStep = new Step();
            firstStep.setNumber(1);
            initialSteps.add(firstStep); // Добавляем пустой первый шаг
        }

        // Устанавливаем списки в LiveData
        ingredients.setValue(new ArrayList<>(initialIngredients)); // Используем копию
        steps.setValue(new ArrayList<>(initialSteps)); // Используем копию

        Log.d(TAG, "setRecipeData: Установлены данные для рецепта ID " + recipeId.getValue() + ", Ингредиентов: " + ingredients.getValue().size() + ", Шагов: " + steps.getValue().size());
    }

    // --- Геттеры для LiveData полей ---
    public LiveData<String> getTitle() {
        return title;
    }

    public LiveData<List<Ingredient>> getIngredients() {
        return ingredients;
    }

    public LiveData<List<Step>> getSteps() {
        return steps;
    }

    public LiveData<String> getPhotoUrl() {
        return photoUrl;
    }

    public LiveData<byte[]> getImageBytes() {
        return imageBytes;
    }

    public LiveData<Boolean> getIsSaving() {
        return isSaving;
    }

    public LiveData<Boolean> getSaveResult() {
        return saveResult;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    // --- Методы для управления списками (аналогично AddRecipeViewModel) ---

    /**
     * Добавляет пустой ингредиент в список
     */
    public void addEmptyIngredient() {
        List<Ingredient> currentList = ingredients.getValue();
        if (currentList == null) {
            currentList = new ArrayList<>();
        }
        // Создаем копию списка, чтобы LiveData заметила изменение
        List<Ingredient> newList = new ArrayList<>(currentList);
        newList.add(new Ingredient());
        ingredients.setValue(newList);
        // Убираем валидацию при добавлении нового пустого ингредиента
        // validateIngredientsList(); // Перепроверяем валидность списка
    }

    /**
     * Обновляет ингредиент по позиции
     */
    public void updateIngredient(int position, Ingredient ingredient) {
        List<Ingredient> currentList = ingredients.getValue();
        if (currentList != null && position >= 0 && position < currentList.size()) {
            // Создаем копию списка для обновления LiveData
            List<Ingredient> newList = new ArrayList<>(currentList);
            newList.set(position, ingredient);
            ingredients.setValue(newList);
            // Валидацию можно вызвать здесь или положиться на TextWatcher в адаптере
            validateIngredientsList();
        }
    }

    /**
     * Удаляет ингредиент по позиции
     */
    public void removeIngredient(int position) {
        List<Ingredient> currentList = ingredients.getValue();
        // Не удаляем, если это последний ингредиент
        if (currentList != null && currentList.size() > 1 && position >= 0 && position < currentList.size()) {
            List<Ingredient> newList = new ArrayList<>(currentList);
            newList.remove(position);
            ingredients.setValue(newList);
            validateIngredientsList(); // Перепроверяем валидность списка
        }
    }

    /**
     * Добавляет пустой шаг в список
     */
    public void addEmptyStep() {
        List<Step> currentList = steps.getValue();
        if (currentList == null) {
            currentList = new ArrayList<>();
        }
        List<Step> newList = new ArrayList<>(currentList);
        Step newStep = new Step();
        newStep.setNumber(newList.size() + 1); // Устанавливаем номер
        newList.add(newStep);
        steps.setValue(newList);
        // Убираем валидацию при добавлении нового пустого шага
        // validateStepsList(); // Перепроверяем валидность списка
    }

    /**
     * Обновляет шаг по позиции
     */
    public void updateStep(int position, Step step) {
        List<Step> currentList = steps.getValue();
        if (currentList != null && position >= 0 && position < currentList.size()) {
             List<Step> newList = new ArrayList<>(currentList);
             // Номер шага может быть обновлен здесь или при удалении/добавлении
             // Убедимся, что номер шага соответствует позиции + 1
             step.setNumber(position + 1);
             newList.set(position, step);
             steps.setValue(newList);
             validateStepsList();
        }
    }

    /**
     * Удаляет шаг по позиции и обновляет нумерацию
     */
    public void removeStep(int position) {
        List<Step> currentList = steps.getValue();
        // Не удаляем, если это последний шаг
        if (currentList != null && currentList.size() > 1 && position >= 0 && position < currentList.size()) {
            List<Step> newList = new ArrayList<>(currentList);
            newList.remove(position);
            // Обновляем нумерацию оставшихся шагов
            for (int i = 0; i < newList.size(); i++) {
                newList.get(i).setNumber(i + 1);
            }
            steps.setValue(newList);
            validateStepsList(); // Перепроверяем валидность списка
        }
    }

    // --- Обработка и загрузка изображений ---

    /**
     * Загружает изображение по URL (обычно для отображения существующего фото)
     */
    public void loadImageFromUrl(String url) {
        if (url == null || url.isEmpty() || imageBytes.getValue() != null) {
            Log.d(TAG, "loadImageFromUrl: URL пуст или уже есть новое изображение, загрузка пропущена.");
            return;
        }
        //isLoading.setValue(true); // Возможно, индикатор загрузки не нужен здесь
        executeIfActive(() -> {
            try {
                java.net.URL imageUrl = new java.net.URL(url);
                Bitmap bitmap = BitmapFactory.decodeStream(imageUrl.openConnection().getInputStream());
                if (bitmap != null) {
                    Bitmap resizedBitmap = resizeBitmap(bitmap, 800);
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
                    byte[] bytes = baos.toByteArray();
                    if (imageBytes.getValue() == null && selectedImageUri == null) { // Устанавливаем только если нет нового
                        imageBytes.postValue(bytes);
                        imageChanged = false;
                    }
                    Log.d(TAG, "loadImageFromUrl: Исходное изображение загружено.");
                } else {
                     Log.e(TAG, "loadImageFromUrl: Не удалось декодировать Bitmap.");
                    //errorMessage.postValue("Не удалось загрузить исходное изображение");
                }
            } catch (Exception e) {
                Log.e(TAG, "loadImageFromUrl: Ошибка при загрузке изображения", e);
                //errorMessage.postValue("Ошибка загрузки изображения: " + e.getMessage());
            } finally {
                 //isLoading.postValue(false);
            }
        });
    }

    /**
     * Обрабатывает новое выбранное изображение из галереи
     */
    public void processSelectedImage(Uri imageUri) {
        if (imageUri == null) {
            errorMessage.setValue("Ошибка при выборе изображения: Uri = null");
            return;
        }
        isSaving.setValue(true);
        executeIfActive(() -> {
            try {
                InputStream inputStream = getApplication().getContentResolver().openInputStream(imageUri);
                if (inputStream != null) {
                    Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                    inputStream.close();
                    if (bitmap != null) {
                        Bitmap resizedBitmap = resizeBitmap(bitmap, 800);
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos); // Качество 85
                        byte[] bytes = baos.toByteArray();
                        imageBytes.postValue(bytes);
                        imageChanged = true;
                        photoUrl.postValue(null); // Сбрасываем старый URL, т.к. есть новое фото
                        Log.d(TAG, "processSelectedImage: Изображение обработано, размер: " + bytes.length + " байт");
                    } else {
                        errorMessage.postValue("Не удалось декодировать изображение");
                    }
                } else {
                    errorMessage.postValue("Не удалось открыть поток данных для изображения");
                }
            } catch (Exception e) {
                Log.e(TAG, "processSelectedImage: Ошибка при обработке изображения", e);
                errorMessage.postValue("Ошибка обработки изображения: " + e.getMessage());
                imageBytes.postValue(null);
                imageChanged = false;
            } finally {
                isSaving.postValue(false);
            }
        });
    }

    /**
            new UnifiedRecipeRepository.RecipeSaveCallback() {
                @Override
                public void onSuccess(com.example.cooking.network.models.GeneralServerResponse response, Recipe updatedRecipe) {
                    isSaving.postValue(false);
                    saveResult.postValue(true);
                    Log.d(TAG, "Рецепт успешно обновлен");
                }

                @Override
                public void onFailure(String error, com.example.cooking.network.models.GeneralServerResponse errorResponse) {
                    isSaving.postValue(false);
                    saveResult.postValue(false);
                    String detailedError = error;
                    if (errorResponse != null && errorResponse.getMessage() != null) {
                        detailedError += " (Сервер: " + errorResponse.getMessage() + ")";
                    }
                    errorMessage.postValue(detailedError);
                    Log.e(TAG, "Ошибка при обновлении рецепта: " + detailedError);
                }
            }
        );
    }

    // --- Валидация ---

    private boolean validateAll() {
        boolean isTitleValid = validateTitle();
        boolean areIngredientsValid = validateIngredientsList();
        boolean areStepsValid = validateStepsList();
        // Валидация изображения не требуется для сохранения,
        // но можно добавить, если нужно обязательно фото
        return isTitleValid && areIngredientsValid && areStepsValid;
    }

    private boolean validateTitle() {
        String currentTitle = title.getValue();
        if (currentTitle == null || currentTitle.trim().isEmpty()) {
            // titleError.setValue("Название не может быть пустым"); // Ошибки показываются в Activity
            return false;
        } else {
            // titleError.setValue(null);
            return true;
        }
    }

    private boolean validateIngredientsList() {
        List<Ingredient> currentList = ingredients.getValue();
        if (currentList == null || currentList.isEmpty()) {
            // ingredientsListError.setValue("Добавьте хотя бы один ингредиент");
            return false;
        }
        for (Ingredient ingredient : currentList) {
            if (ingredient.getName() == null || ingredient.getName().trim().isEmpty() ||
                ingredient.getType() == null || ingredient.getType().trim().isEmpty() ||
                ingredient.getCount() <= 0) {
                // ingredientsListError.setValue("Заполните все поля для каждого ингредиента");
                 Log.w(TAG, "validateIngredientsList: Невалидный ингредиент: " + ingredient);
                return false; // Нашли хотя бы один невалидный
            }
        }
        // ingredientsListError.setValue(null); // Все ингредиенты валидны
        return true;
    }

     private boolean validateStepsList() {
        List<Step> currentList = steps.getValue();
        if (currentList == null || currentList.isEmpty()) {
             // stepsListError.setValue("Добавьте хотя бы один шаг");
            return false;
        }
        for (Step step : currentList) {
            if (step.getInstruction() == null || step.getInstruction().trim().isEmpty()) {
                 // stepsListError.setValue("Заполните описание для каждого шага");
                 Log.w(TAG, "validateStepsList: Невалидный шаг: " + step);
                return false; // Нашли хотя бы один невалидный
            }
        }
        // stepsListError.setValue(null); // Все шаги валидны
        return true;
    }

    // --- Сеттеры для UI --- (для title, т.к. он редактируется напрямую)
    public void setTitle(String newTitle) {
        if (!newTitle.equals(title.getValue())) {
            title.setValue(newTitle);
            // validateTitle(); // Валидация при изменении
        }
    }

    // --- Геттеры LiveData ---
    public LiveData<Boolean> getIsSaving() { return isSaving; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<Boolean> getSaveResult() { return saveResult; }
    public LiveData<String> getTitle() { return title; }
    public LiveData<List<Ingredient>> getIngredients() { return ingredients; }
    public LiveData<List<Step>> getSteps() { return steps; }
    public LiveData<String> getPhotoUrl() { return photoUrl; } // URL исходного фото
    public LiveData<byte[]> getImageBytes() { return imageBytes; } // Новое/загруженное фото

    // --- Методы для сброса сообщений/результатов --- 
    public void clearErrorMessage() {
        errorMessage.setValue(null);
    }

    public void clearSaveResult() {
        saveResult.setValue(null);
    }

    // --- Вспомогательные методы ---

    /**
     * Проверяет подключение к интернету
     * @return true если есть подключение к интернету, иначе false
     */
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = 
            (ConnectivityManager) getApplication().getSystemService(android.content.Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }
    
    /**
     * Обновляет рецепт на сервере
     */
    public void updateRecipe() {
        // Проверяем подключение к интернету
        if (!isNetworkAvailable()) {
            errorMessage.setValue("Отсутствует подключение к интернету");
            return;
        }

        // Получаем текущие данные
        Integer currentRecipeId = recipeId.getValue();
        String currentTitle = title.getValue();
        List<Ingredient> currentIngredients = ingredients.getValue();
        List<Step> currentSteps = steps.getValue();
        String currentPhotoUrl = photoUrl.getValue();
        byte[] currentImageBytes = imageBytes.getValue();

        // Проверяем, что у нас есть все необходимые данные
        if (currentRecipeId == null || currentTitle == null || currentIngredients == null || currentSteps == null) {
            errorMessage.setValue("Ошибка: отсутствуют необходимые данные");
            return;
        }

        // Проверяем, что есть хотя бы один ингредиент и шаг
        if (currentIngredients.isEmpty() || currentSteps.isEmpty()) {
            errorMessage.setValue("Добавьте хотя бы один ингредиент и один шаг");
            return;
        }

        // Обновляем номера шагов
        for (int i = 0; i < currentSteps.size(); i++) {
            currentSteps.get(i).setNumber(i + 1);
        }

        // Устанавливаем флаг сохранения
        isSaving.setValue(true);

        // Создаем объект рецепта для обновления
        Recipe updatedRecipe = new Recipe();
        updatedRecipe.setId(currentRecipeId);
        updatedRecipe.setTitle(currentTitle);
        updatedRecipe.setIngredients(new ArrayList<>(currentIngredients));
        updatedRecipe.setSteps(new ArrayList<>(currentSteps));
        updatedRecipe.setPhoto_url(currentPhotoUrl);

        // Обновляем рецепт через репозиторий
        unifiedRecipeRepository.updateRecipe(
                updatedRecipe,
                currentImageBytes,
                new UnifiedRecipeRepository.RecipeSaveCallback() {
                    @Override
                    public void onSuccess(com.example.cooking.network.models.GeneralServerResponse response, Recipe updatedRecipe) {
                        isSaving.postValue(false);
                        saveResult.postValue(true);
                        Log.d(TAG, "Рецепт успешно обновлен");
                    }

                    @Override
                    public void onFailure(String error, com.example.cooking.network.models.GeneralServerResponse errorResponse) {
                        isSaving.postValue(false);
                        String errorMsg = "Ошибка при обновлении рецепта: " + error;
                        if (errorResponse != null && errorResponse.getMessage() != null) {
                            errorMsg += " (" + errorResponse.getMessage() + ")";
                        }
                        errorMessage.postValue(errorMsg);
                        Log.e(TAG, errorMsg);
                    }
                });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executor.shutdown();
    }
    
    /**
     * Проверяет валидность всех полей рецепта
     */
    private boolean validateAll() {
        boolean isValid = true;
        
        if (!validateTitle()) {
            isValid = false;
        }
        
        if (!validateIngredientsList()) {
            isValid = false;
        }
        
        if (!validateStepsList()) {
            isValid = false;
        }
        
        if (!validateImage()) {
            isValid = false;
        }
        
        return isValid;
    }
    
    /**
     * Проверяет валидность заголовка рецепта
     */
    private boolean validateTitle() {
        String currentTitle = title.getValue();
        if (currentTitle == null || currentTitle.trim().isEmpty()) {
            errorMessage.setValue("Введите название рецепта");
            return false;
        }
        return true;
    }
    
    /**
     * Проверяет валидность списка ингредиентов
     */
    private boolean validateIngredientsList() {
        List<Ingredient> currentList = ingredients.getValue();
        if (currentList == null || currentList.isEmpty()) {
            errorMessage.setValue("Добавьте хотя бы один ингредиент");
            return false;
        }
        // Проверяем, что у всех ингредиентов заполнены обязательные поля
        for (Ingredient ingredient : currentList) {
            if (ingredient.getName() == null || ingredient.getName().trim().isEmpty()) {
                errorMessage.setValue("Заполните название для всех ингредиентов");
                return false;
            }
        }
        return true;
    }
    
    /**
     * Проверяет валидность списка шагов
     */
    private boolean validateStepsList() {
        List<Step> currentList = steps.getValue();
        if (currentList == null || currentList.isEmpty()) {
            errorMessage.setValue("Добавьте хотя бы один шаг приготовления");
            return false;
        }
        // Проверяем, что у всех шагов заполнено описание
        for (Step step : currentList) {
            if (step.getInstruction() == null || step.getInstruction().trim().isEmpty()) {
                errorMessage.setValue("Заполните описание для всех шагов");
                return false;
            }
        }
        return true;
    }
    
    /**
     * Проверяет валидность изображения
     */
    private boolean validateImage() {
        if (imageBytes.getValue() == null && (photoUrl.getValue() == null || photoUrl.getValue().isEmpty())) {
            errorMessage.setValue("Добавьте изображение рецепта");
            return false;
        }
        return true;
    }
    
    /**
     * Изменяет размер Bitmap, сохраняя пропорции
     */
    private Bitmap resizeBitmap(Bitmap bitmap, int maxSide) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        float ratio = (float) width / height;

        if (width > height) {
            if (width > maxSide) {
                width = maxSide;
                height = (int) (width / ratio);
            }
        } else {
            if (height > maxSide) {
                height = maxSide;
                width = (int) (height * ratio);
            }
        }
        return Bitmap.createScaledBitmap(bitmap, width, height, true);
    }
    
    /**
     * Выполняет задачу в фоновом потоке, если executor активен
     */
    private void executeIfActive(Runnable task) {
        if (!executor.isShutdown()) {
            executor.execute(task);
        }
    }

    // Новый метод для установки Uri
    public void setImageUri(Uri imageUri) {
        if (imageUri == null) return;
        selectedImageUri = imageUri; 
        Log.d(TAG, "setImageUri: Установлен Uri: " + imageUri);
        processSelectedImage(imageUri); // Запускаем обработку
    }
    
    /**
     * Очищает результат сохранения
     */
    public void clearSaveResult() {
        saveResult.setValue(null);
    }
    
    /**
     * Очищает сообщение об ошибке
     */
    public void clearErrorMessage() {
        errorMessage.setValue(null);
    }
    
    /**
     * Сохраняет рецепт
     */
    public void saveRecipe() {
        String recipeTitle = title.getValue();
        List<Ingredient> recipeIngredients = ingredients.getValue();
        List<Step> recipeSteps = steps.getValue();
        
        if (recipeTitle == null || recipeTitle.trim().isEmpty()) {
            errorMessage.setValue("Введите название рецепта");
            return;
        }
        
        if (recipeIngredients == null || recipeIngredients.isEmpty()) {
            errorMessage.setValue("Добавьте хотя бы один ингредиент");
            return;
        }
        
        if (recipeSteps == null || recipeSteps.isEmpty()) {
            errorMessage.setValue("Добавьте хотя бы один шаг приготовления");
            return;
        }
        
        isSaving.setValue(true);
        
        // Создаем новый рецепт или обновляем существующий
        Recipe recipe = new Recipe();
        if (recipeId.getValue() != null) {
            recipe.setId(recipeId.getValue());
        }
        recipe.setTitle(recipeTitle);
        // Преобразуем List в ArrayList, так как Recipe ожидает ArrayList
        recipe.setIngredients(recipeIngredients != null ? new ArrayList<>(recipeIngredients) : new ArrayList<>());
        recipe.setSteps(recipeSteps != null ? new ArrayList<>(recipeSteps) : new ArrayList<>());
        recipe.setLiked(false);
        recipe.setUserId(preferences.getUserId());
        
        // Сохраняем рецепт в репозитории
        if (recipe.getId() == 0) {
            // Создаем новый рецепт
            unifiedRecipeRepository.insert(recipe, new UnifiedRecipeRepository.RecipeSaveCallback() {
                @Override
                public void onSuccess(GeneralServerResponse response, Recipe updatedRecipe) {
                    isSaving.postValue(false);
                    if (response != null && response.isSuccess()) {
                        saveResult.postValue(true);
                    } else {
                        errorMessage.postValue("Ошибка при сохранении рецепта");
                        saveResult.postValue(false);
                    }
                }

                @Override
                public void onFailure(String error, GeneralServerResponse errorResponse) {
                    isSaving.postValue(false);
                    errorMessage.postValue("Ошибка при сохранении рецепта: " + error);
                    saveResult.postValue(false);
                }
            });
        } else {
            // Обновляем существующий рецепт на сервере
            updateRecipe();
        }
    }
    
    /**
     * Устанавливает заголовок рецепта
     * @param title заголовок рецепта
     */
    public void setTitle(String title) {
        this.title.setValue(title);
    }
}

