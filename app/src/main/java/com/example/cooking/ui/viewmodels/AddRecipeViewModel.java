package com.example.cooking.ui.viewmodels;

import android.app.Application;
import android.content.Context;
import android.graphics.Bitmap;
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
import com.example.cooking.data.repositories.UnifiedRecipeRepository;
import com.example.cooking.utils.MySharedPreferences;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ViewModel для экрана добавления рецепта
 * Управляет бизнес-логикой создания нового рецепта
 */
public class AddRecipeViewModel extends AndroidViewModel {
    private static final String TAG = "AddRecipeViewModel";
    
    // LiveData для UI состояний
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> saveSuccess = new MutableLiveData<>(false);
    
    // LiveData для валидации полей
    private final MutableLiveData<String> titleError = new MutableLiveData<>();
    private final MutableLiveData<String> ingredientsListError = new MutableLiveData<>();
    private final MutableLiveData<String> stepsListError = new MutableLiveData<>();
    private final MutableLiveData<String> imageError = new MutableLiveData<>();
    
    // Данные рецепта
    private final MutableLiveData<String> title = new MutableLiveData<>("");
    private final MutableLiveData<List<Ingredient>> ingredients = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Step>> steps = new MutableLiveData<>(new ArrayList<>());
    private byte[] imageBytes = null;
    
    // Сервисы
    private final UnifiedRecipeRepository unifiedRecipeRepository;
    private final MySharedPreferences preferences;

    public AddRecipeViewModel(@NonNull Application application) {
        super(application);
        preferences = new MySharedPreferences(application);
        unifiedRecipeRepository = new UnifiedRecipeRepository(application, java.util.concurrent.Executors.newSingleThreadExecutor());

        // Инициализируем списки с одним пустым элементом
        ArrayList<Ingredient> initialIngredients = new ArrayList<>();
        initialIngredients.add(new Ingredient());
        ingredients.setValue(initialIngredients);
        
        ArrayList<Step> initialSteps = new ArrayList<>();
        Step initialStep = new Step();
        initialStep.setNumber(1);
        initialSteps.add(initialStep);
        steps.setValue(initialSteps);
    }
    
    /**
     * Сохраняет новый рецепт
     */
    /**
     * Сохраняет новый рецепт
     */
    public void saveRecipe() {
        // Проверяем подключение к интернету
        if (!isNetworkAvailable()) {
            errorMessage.setValue("Отсутствует подключение к интернету");
            return;
        }
        
        // Проверяем валидность всех полей
        if (!validateAll()) {
            return;
        }
        
        // Показываем индикатор загрузки
        isLoading.setValue(true);
        
        // Получаем ID пользователя
        String userId = preferences.getString("userId", "99");
        
        // Подготавливаем данные
        String titleText = title.getValue();
        List<Ingredient> ingredientsList = ingredients.getValue();
        List<Step> stepsList = steps.getValue();
        
        if (ingredientsList == null || stepsList == null) {
            errorMessage.setValue("Ошибка: не удалось загрузить данные рецепта");
            isLoading.setValue(false);
            return;
        }
        
        // Подготавливаем номера шагов
        for (int i = 0; i < stepsList.size(); i++) {
            stepsList.get(i).setNumber(i + 1); // Номера начинаются с 1
        }
        
        // Создаем объект рецепта
        Recipe newRecipe = new Recipe();
        newRecipe.setTitle(titleText);
        newRecipe.setUserId(userId);
        newRecipe.setIngredients(new ArrayList<>(ingredientsList));
        newRecipe.setSteps(new ArrayList<>(stepsList));
        
        // Сохраняем рецепт через репозиторий
        unifiedRecipeRepository.saveRecipe(
            newRecipe,
            imageBytes,
            new UnifiedRecipeRepository.RecipeSaveCallback() {
                @Override
                public void onSuccess(com.example.cooking.network.models.GeneralServerResponse response, Recipe savedRecipe) {
                    isLoading.postValue(false);
                    if (response.isSuccess() && savedRecipe != null) {
                        saveSuccess.postValue(true);
                        Log.d(TAG, "Рецепт успешно сохранен с ID: " + savedRecipe.getId());
                    } else {
                        String errorMsg = "Не удалось сохранить рецепт";
                        if (response != null && response.getMessage() != null) {
                            errorMsg += ": " + response.getMessage();
                        }
                        errorMessage.postValue(errorMsg);
                        Log.e(TAG, errorMsg);
                    }
                }

                @Override
                public void onFailure(String error, com.example.cooking.network.models.GeneralServerResponse errorResponse) {
                    isLoading.postValue(false);
                    String detailedError = error;
                    if (errorResponse != null && errorResponse.getMessage() != null) {
                        detailedError += " (Сервер: " + errorResponse.getMessage() + ")";
                    }

                    // Если ошибка связана с дублированием рецепта, считаем это успешным сохранением
                    if (detailedError != null && (detailedError.toLowerCase().contains("дубли") || 
                        detailedError.toLowerCase().contains("уже существует") || 
                        detailedError.toLowerCase().contains("повторно") || 
                        detailedError.toLowerCase().contains("duplicate") || 
                        detailedError.toLowerCase().contains("already exists") || 
                        detailedError.toLowerCase().contains("already added"))) {
                        saveSuccess.postValue(true);
                        Log.d(TAG, "Рецепт уже существует или был отправлен повторно: " + detailedError);
                    } else {
                        errorMessage.postValue(detailedError);
                        Log.e(TAG, "Ошибка при сохранении рецепта: " + detailedError);
                    }
                }
            }
        );
    }
    
    /**
     * Устанавливает название рецепта
     */
    public void setTitle(String title) {
        this.title.setValue(title);
        validateTitle();
    }
    
    /**
     * Добавляет пустой ингредиент в список
     */
    public void addEmptyIngredient() {
        List<Ingredient> currentList = ingredients.getValue();
        if (currentList == null) {
            currentList = new ArrayList<>();
        }
        currentList.add(new Ingredient());
        ingredients.setValue(currentList);
        // Убираем валидацию при добавлении нового пустого ингредиента
        // validateIngredientsList();
    }
    
    /**
     * Обновляет ингредиент по позиции
     */
    public void updateIngredient(int position, Ingredient ingredient) {
        List<Ingredient> currentList = ingredients.getValue();
        if (currentList != null && position >= 0 && position < currentList.size()) {
            currentList.set(position, ingredient);
            ingredients.setValue(currentList);
        }
    }
    
    /**
     * Удаляет ингредиент по позиции
     */
    public void removeIngredient(int position) {
        List<Ingredient> currentList = ingredients.getValue();
        // Не удаляем, если это последний ингредиент
        if (currentList != null && currentList.size() > 1 && position >= 0 && position < currentList.size()) {
            currentList.remove(position);
            ingredients.setValue(currentList);
            validateIngredientsList();
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
        // Создаем новый список, чтобы гарантировать обновление LiveData
        List<Step> newList = new ArrayList<>(currentList);
        Step newStep = new Step();
        newStep.setNumber(newList.size() + 1);
        newList.add(newStep);
        steps.postValue(newList); // Используем postValue для асинхронного обновления
    }
    
    /**
     * Обновляет шаг по позиции
     */
    public void updateStep(int position, Step step) {
        List<Step> currentList = steps.getValue();
        if (currentList != null && position >= 0 && position < currentList.size()) {
            // Создаем новый список, чтобы гарантировать обновление LiveData
            List<Step> newList = new ArrayList<>(currentList);
            newList.set(position, step);
            steps.postValue(newList); // Используем postValue для асинхронного обновления
        }
    }
    
    /**
     * Удаляет шаг по позиции и обновляет нумерацию
     */
    public void removeStep(int position) {
        List<Step> currentList = steps.getValue();
        if (currentList != null && position >= 0 && position < currentList.size()) {
            // Создаем новый список, чтобы гарантировать обновление LiveData
            List<Step> newList = new ArrayList<>(currentList);
            newList.remove(position);
            
            // Обновляем номера оставшихся шагов
            for (int i = 0; i < newList.size(); i++) {
                newList.get(i).setNumber(i + 1);
            }
            
            steps.postValue(newList); // Используем postValue для асинхронного обновления
        }
    }
    
    /**
     * Обрабатывает выбранное изображение и преобразует его в массив байтов
     */
    public void processSelectedImage(Uri imageUri) {
        try {
            InputStream inputStream = getApplication().getContentResolver().openInputStream(imageUri);
            Bitmap bitmap = android.graphics.BitmapFactory.decodeStream(inputStream);
            
            // Сжимаем изображение до разумного размера
            Bitmap resizedBitmap = resizeBitmap(bitmap, 800);
            
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream);
            imageBytes = byteArrayOutputStream.toByteArray();
            
            if (inputStream != null) {
                inputStream.close();
            }
            
            Log.d(TAG, "Изображение обработано, размер: " + imageBytes.length + " байт");
            
            // Очищаем ошибку изображения, если она была
            imageError.setValue(null);
            validateImage();
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при обработке изображения", e);
            imageError.setValue("Ошибка при обработке изображения: " + e.getMessage());
            imageBytes = null;
        }
    }
    
    /**
     * Изменяет размер Bitmap до указанного максимального значения
     */
    private Bitmap resizeBitmap(Bitmap bitmap, int maxSide) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        
        if (width <= maxSide && height <= maxSide) {
            return bitmap; // Нет необходимости менять размер
        }
        
        float ratio = (float) width / height;
        int newWidth, newHeight;
        
        if (width > height) {
            newWidth = maxSide;
            newHeight = (int) (maxSide / ratio);
        } else {
            newHeight = maxSide;
            newWidth = (int) (maxSide * ratio);
        }
        
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
    }
    
    /**
     * Проверяет все поля на валидность
     */
    private boolean validateAll() {
        titleError.setValue(null);
        ingredientsListError.setValue(null);
        stepsListError.setValue(null);
        imageError.setValue(null);
        
        boolean isValid = true;
        
        // Проверяем название
        if (!validateTitle()) {
            isValid = false;
        }
        
        // Проверяем ингредиенты
        if (!validateIngredientsList()) {
            isValid = false;
        }
        
        // Проверяем шаги
        if (!validateStepsList()) {
            isValid = false;
        }
        
        // Проверяем изображение
        if (!validateImage()) {
            isValid = false;
        }
        
        return isValid;
    }
    
    /**
     * Проверяет валидность названия рецепта
     */
    private boolean validateTitle() {
        String currentTitle = title.getValue();
        if (currentTitle == null || currentTitle.trim().isEmpty()) {
            titleError.setValue("Название рецепта не может быть пустым");
            return false;
        } else if (currentTitle.trim().length() < 3) {
            titleError.setValue("Название должно содержать минимум 3 символа");
            return false;
        } else if (currentTitle.length() > 100) {
            titleError.setValue("Название не должно превышать 100 символов");
            return false;
        } else {
            titleError.setValue(null);
            return true;
        }
    }
    
    /**
     * Проверяет валидность списка ингредиентов
     */
    private boolean validateIngredientsList() {
        List<Ingredient> currentList = ingredients.getValue();
        if (currentList == null || currentList.isEmpty()) {
            ingredientsListError.setValue("Добавьте хотя бы один ингредиент");
            Log.d(TAG, "validateIngredientsList: Список ингредиентов пуст");
            return false;
        }
        
        boolean allValid = true;
        for (int i = 0; i < currentList.size(); i++) {
            Ingredient ingredient = currentList.get(i);
            String name = ingredient.getName();
            String type = ingredient.getType();
            int count = ingredient.getCount();
            
            Log.d(TAG, "validateIngredientsList: Проверка ингредиента #" + i + 
                    ": name='" + name + "', type='" + type + "', count=" + count);
            
            // Проверяем наличие значений
            if ((name == null || name.trim().isEmpty()) ||
                (type == null || type.trim().isEmpty()) ||
                count <= 0) {
                
                Log.d(TAG, "validateIngredientsList: Ингредиент #" + i + " не прошел валидацию");
                
                // Вместо немедленного возврата, отмечаем, что есть невалидный ингредиент
                allValid = false;
                break;
            }
        }
        
        if (!allValid) {
            ingredientsListError.setValue("Заполните все поля для каждого ингредиента (название, количество > 0, тип)");
            return false;
        }
        
        // Все ингредиенты валидны
        Log.d(TAG, "validateIngredientsList: Все ингредиенты прошли валидацию");
        ingredientsListError.setValue(null);
        return true;
    }
    
    /**
     * Проверяет валидность списка шагов
     */
    private boolean validateStepsList() {
        List<Step> currentList = steps.getValue();
        if (currentList == null || currentList.isEmpty()) {
            stepsListError.setValue("Добавьте хотя бы один шаг приготовления");
            return false;
        }
        for (Step step : currentList) {
            if (step.getInstruction() == null || step.getInstruction().trim().isEmpty()) {
                stepsListError.setValue("Заполните описание для каждого шага");
                return false;
            }
        }
        stepsListError.setValue(null);
        return true;
    }
    
    /**
     * Проверяет, выбрано ли изображение
     */
    private boolean validateImage() {
        if (imageBytes == null) {
            imageError.setValue("Выберите изображение для рецепта");
            return false;
        } else {
            imageError.setValue(null);
            return true;
        }
    }
    
    /**
     * Проверяет подключение к интернету
     */
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = 
            (ConnectivityManager) getApplication().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }
    
    // Геттеры для LiveData
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }
    
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }
    
    public LiveData<Boolean> getSaveSuccess() {
        return saveSuccess;
    }
    
    public LiveData<String> getTitleError() {
        return titleError;
    }
    
    public LiveData<String> getIngredientsListError() {
        return ingredientsListError;
    }
    
    public LiveData<String> getStepsListError() {
        return stepsListError;
    }
    
    public LiveData<String> getImageError() {
        return imageError;
    }
    
    public LiveData<List<Ingredient>> getIngredients() {
        return ingredients;
    }
    
    public LiveData<List<Step>> getSteps() {
        return steps;
    }
    
    public LiveData<String> getTitle() {
        return title;
    }

    /**
     * Возвращает true, если изображение было выбрано и обработано.
     */
    public boolean hasImage() {
        return imageBytes != null && imageBytes.length > 0;
    }
}