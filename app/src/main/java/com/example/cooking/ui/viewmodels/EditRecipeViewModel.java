package com.example.cooking.ui.viewmodels;

import android.app.Application;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.cooking.Recipe.Ingredient;
import com.example.cooking.Recipe.Recipe;
import com.example.cooking.Recipe.Step;
import com.example.cooking.network.services.RecipeService;
import com.example.cooking.utils.MySharedPreferences;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * ViewModel для экрана редактирования рецепта.
 */
public class EditRecipeViewModel extends AndroidViewModel {
    private static final String TAG = "EditRecipeViewModel";

    private final RecipeService recipeService;
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
        recipeService = new RecipeService(application);
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
     * Сохраняет (обновляет) рецепт
     */
    public void saveRecipe() {
        Log.d(TAG, "saveRecipe: Метод вызван");
        if (!validateAll()) {
            Log.w(TAG, "saveRecipe: Валидация ВСЕХ полей не пройдена");
            return;
        }
        Log.d(TAG, "saveRecipe: Валидация ВСЕХ полей пройдена");

        if (!isNetworkAvailable()) {
            Log.w(TAG, "saveRecipe: Сеть недоступна");
            errorMessage.setValue("Нет подключения к интернету для сохранения рецепта.");
            return;
        }
        Log.d(TAG, "saveRecipe: Сеть доступна");

        isSaving.setValue(true);
        errorMessage.setValue(null);

        String currentTitle = title.getValue();
        List<Ingredient> currentIngredients = ingredients.getValue();
        List<Step> currentSteps = steps.getValue();
        byte[] currentImageBytes = imageBytes.getValue();
        Integer currentRecipeId = recipeId.getValue();
        String userId = preferences.getString("userId", "0");
        int permission = preferences.getInt("permission", 1);

        if (currentRecipeId == null) {
            errorMessage.setValue("Ошибка: ID рецепта отсутствует.");
            isSaving.setValue(false);
            return;
        }
        Log.d(TAG, "saveRecipe: ID рецепта = " + currentRecipeId);
        Log.d(TAG, String.valueOf(permission));
        Log.d(TAG, userId);
        Log.d(TAG, "saveRecipe: Вызов recipeService.updateRecipe...");
        recipeService.updateRecipe(
            currentTitle,
            currentIngredients,
            currentSteps,
            userId,
            currentRecipeId,
            imageChanged ? currentImageBytes : null,
            permission,
            new RecipeService.RecipeSaveCallback() {
                @Override
                public void onSuccess(com.example.cooking.network.models.GeneralServerResponse response, Recipe updatedRecipe) {
                    isSaving.postValue(false);
                    if (response.isSuccess()) {
                        saveResult.postValue(true);
                        Log.d(TAG, "Рецепт успешно обновлен: " + response.getMessage());
                        // updatedRecipe содержит данные, которые были отправлены на сервер и успешно обновлены там.
                        // Эти же данные были использованы для обновления локальной БД в RecipeService.
                        // Здесь можно дополнительно что-то сделать с updatedRecipe, если нужно.
                        if (updatedRecipe != null) {
                            Log.d(TAG, "Обновленный рецепт (из колбэка): ID " + updatedRecipe.getId() + ", Title: " + updatedRecipe.getTitle());
                        }
                    } else {
                        saveResult.postValue(false);
                        errorMessage.postValue("Ошибка обновления: " + response.getMessage());
                        Log.w(TAG, "Ошибка при обновлении рецепта (сервер ответил success=false): " + response.getMessage());
                    }
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

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getApplication().getSystemService(Application.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

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

    private void executeIfActive(Runnable task) {
        if (!executor.isShutdown()) {
            executor.execute(task);
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executor.shutdown();
    }

    // Новый метод для установки Uri
    public void setImageUri(Uri imageUri) {
        if (imageUri == null) return;
        selectedImageUri = imageUri; 
        Log.d(TAG, "setImageUri: Установлен Uri: " + imageUri);
        processSelectedImage(imageUri); // Запускаем обработку
    }
}

