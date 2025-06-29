# План оптимизации производительности записи рецептов в локальную БД и отрисовки

## Анализ текущего состояния

### Текущая архитектура
- **База данных**: Room с WAL режимом, единая транзакция для замены всех рецептов
- **Сериализация**: Gson для преобразования List<Ingredient> и List<Step> в JSON
- **Потоки**: AppExecutors с единственным потоком для дисковых операций
- **UI**: RecipeListAdapter с DiffUtil, GridLayoutManager 2x2
- **Изображения**: Базовая конфигурация Glide без оптимизаций

### Выявленные проблемы производительности
1. Медленная сериализация Gson при больших объемах данных
2. Блокировки UI thread при обработке больших списков рецептов
3. Неоптимальная работа с изображениями
4. Единственный поток для дисковых операций создает очереди
5. Неэффективные операции вставки в БД

## Фаза 1: Оптимизация сериализации данных (Критическая)

### 1.1 Замена Gson на Moshi
**Проблема**: Gson медленно работает с большими объемами данных  
**Решение**: Замена на Moshi с кодогенерацией

```java
// DataConverters.java - новая реализация
@JsonAdapter(IngredientListAdapter.class)
private static final Moshi MOSHI_INSTANCE = new Moshi.Builder()
    .add(new IngredientListAdapter())
    .add(new StepListAdapter())
    .build();

private static final JsonAdapter<List<Ingredient>> INGREDIENT_ADAPTER = 
    MOSHI_INSTANCE.adapter(Types.newParameterizedType(List.class, Ingredient.class));
```

**Ожидаемый эффект**: Ускорение сериализации в 2-3 раза

### 1.2 Ленивая сериализация
**Проблема**: Сериализация происходит при каждом обращении  
**Решение**: Кэширование сериализованных данных

```java
// RecipeEntity.java - добавить поля для кэша
@Ignore
private String cachedIngredientsJson;
@Ignore  
private String cachedStepsJson;
@Ignore
private boolean ingredientsCacheValid = false;
```

## Фаза 2: Оптимизация работы с базой данных (Критическая)

### 2.1 Многопоточность для дисковых операций
**Проблема**: Единственный поток создает очереди  
**Решение**: Расширение пула потоков для дисковых операций

```java
// AppExecutors.java - модификация
private AppExecutors() {
    this.diskIO = Executors.newFixedThreadPool(2); // Вместо single thread
    this.networkIO = Executors.newFixedThreadPool(THREAD_COUNT);
    this.mainThread = new MainThreadExecutor();
}
```

### 2.2 Умные операции с БД
**Проблема**: replaceAllRecipes() удаляет и вставляет все данные  
**Решение**: Дифференциальные обновления

```java
// RecipeDao.java - новые методы
@Query("SELECT id FROM recipes")
List<Integer> getAllRecipeIds();

@Insert(onConflict = OnConflictStrategy.IGNORE)
void insertNewRecipes(List<RecipeEntity> recipes);

@Update
void updateExistingRecipes(List<RecipeEntity> recipes);

@Query("DELETE FROM recipes WHERE id IN (:idsToDelete)")
void deleteRecipesByIds(List<Integer> idsToDelete);
```

### 2.3 Оптимизированная замена данных
```java
// RecipeLocalRepository.java - новый метод
public void smartReplaceRecipes(List<Recipe> newRecipes) {
    AppExecutors.getInstance().diskIO().execute(() -> {
        List<Integer> existingIds = recipeDao.getAllRecipeIds();
        
        List<RecipeEntity> toInsert = new ArrayList<>();
        List<RecipeEntity> toUpdate = new ArrayList<>();
        Set<Integer> newIds = new HashSet<>();
        
        for (Recipe recipe : newRecipes) {
            newIds.add(recipe.getId());
            RecipeEntity entity = new RecipeEntity(recipe);
            
            if (existingIds.contains(recipe.getId())) {
                toUpdate.add(entity);
            } else {
                toInsert.add(entity);
            }
        }
        
        // Найти удаленные рецепты
        List<Integer> toDelete = existingIds.stream()
            .filter(id -> !newIds.contains(id))
            .collect(Collectors.toList());
        
        // Выполнить операции в транзакции
        AppDatabase.getInstance(context).runInTransaction(() -> {
            if (!toDelete.isEmpty()) {
                recipeDao.deleteRecipesByIds(toDelete);
            }
            if (!toInsert.isEmpty()) {
                recipeDao.insertNewRecipes(toInsert);
            }
            if (!toUpdate.isEmpty()) {
                recipeDao.updateExistingRecipes(toUpdate);
            }
        });
    });
}
```

## Фаза 3: Оптимизация RecyclerView (Высокая)

### 3.1 Улучшенные настройки RecyclerView
```java
// HomeFragment.java - расширенные оптимизации
private void optimizeRecyclerView() {
    // Увеличить кэш View
    recyclerView.setItemViewCacheSize(25);
    
    // Настроить RecycledViewPool
    RecyclerView.RecycledViewPool pool = new RecyclerView.RecycledViewPool();
    pool.setMaxRecycledViews(0, 30); // ViewType 0, max 30 views
    recyclerView.setRecycledViewPool(pool);
    
    // Отключить изменения анимации для лучшей производительности
    recyclerView.getItemAnimator().setChangeDuration(0);
    
    // Предварительная загрузка
    if (layoutManager instanceof GridLayoutManager) {
        ((GridLayoutManager) layoutManager).setInitialPrefetchItemCount(6);
    }
    
    // Отключить nested scrolling если не нужен
    recyclerView.setNestedScrollingEnabled(false);
}
```

### 3.2 Оптимизация ViewHolder
```java
// RecipeListAdapter.java - оптимизация ViewHolder
static class RecipeViewHolder extends RecyclerView.ViewHolder {
    // Кэширование размеров для избежания повторных измерений
    private static int cardWidth = -1;
    private static int cardHeight = -1;
    
    RecipeViewHolder(View itemView, boolean isChatMode) {
        super(itemView);
        // ... существующий код ...
        
        // Предварительное вычисление размеров
        if (cardWidth == -1) {
            cardWidth = itemView.getContext().getResources()
                .getDimensionPixelSize(R.dimen.recipe_card_width);
            cardHeight = itemView.getContext().getResources()
                .getDimensionPixelSize(R.dimen.recipe_card_height);
        }
    }
}
```

## Фаза 4: Оптимизация загрузки изображений (Высокая)

### 4.1 Глобальная конфигурация Glide
```java
// MyApplication.java - добавить конфигурацию
@Override
public void onCreate() {
    super.onCreate();
    
    // Конфигурация Glide
    RequestOptions globalOptions = new RequestOptions()
        .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
        .format(DecodeFormat.PREFER_RGB_565)
        .downsample(DownsampleStrategy.AT_MOST)
        .encodeQuality(85);
        
    Glide.get(this).setDefaultRequestOptions(globalOptions);
}
```

### 4.2 Оптимизированная загрузка в адаптере
```java
// RecipeListAdapter.java - улучшенная загрузка изображений
private void loadRecipeImage(ShapeableImageView imageView, String photoUrl) {
    if (photoUrl != null && !photoUrl.isEmpty()) {
        Glide.with(imageView.getContext())
            .load(photoUrl)
            .placeholder(R.drawable.white_card_background)
            .error(R.drawable.white_card_background)
            .override(400, 300) // Фиксированный размер
            .centerCrop()
            .dontAnimate() // Отключаем анимации для производительности
            .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
            .into(imageView);
    } else {
        imageView.setImageResource(R.drawable.white_card_background);
    }
}
```

## Фаза 5: Оптимизация пакетной обработки (Средняя)

### 5.1 Улучшенный батчинг в UnifiedRecipeRepository
```java
// UnifiedRecipeRepository.java - оптимизированный батчинг
private void processRecipesInOptimizedBatches(List<Recipe> recipes, Set<Integer> likedIds) {
    final int OPTIMAL_BATCH_SIZE = 20; // Уменьшено с 50 для лучшей отзывчивости
    final int PAUSE_BETWEEN_BATCHES_MS = 5; // Уменьшено с 10
    
    for (int i = 0; i < recipes.size(); i += OPTIMAL_BATCH_SIZE) {
        int endIndex = Math.min(i + OPTIMAL_BATCH_SIZE, recipes.size());
        List<Recipe> batch = recipes.subList(i, endIndex);
        
        // Параллельная обработка лайков в батче
        batch.parallelStream().forEach(recipe -> {
            recipe.setLiked(likedIds.contains(recipe.getId()));
        });
        
        // Проверка прерывания потока
        if (Thread.currentThread().isInterrupted()) {
            break;
        }
        
        // Микропауза между батчами
        if (endIndex < recipes.size()) {
            try {
                Thread.sleep(PAUSE_BETWEEN_BATCHES_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}
```

## Фаза 6: Оптимизация DiffUtil (Средняя)

### 6.1 Асинхронный DiffUtil с улучшенной логикой
```java
// RecipeListAdapter.java - оптимизированный DiffCallback
private static final DiffUtil.ItemCallback<Recipe> OPTIMIZED_DIFF_CALLBACK = 
    new DiffUtil.ItemCallback<Recipe>() {
    
    @Override
    public boolean areItemsTheSame(@NonNull Recipe oldItem, @NonNull Recipe newItem) {
        return oldItem.getId() == newItem.getId();
    }

    @Override
    public boolean areContentsTheSame(@NonNull Recipe oldItem, @NonNull Recipe newItem) {
        // Быстрая проверка основных полей без equals()
        return Objects.equals(oldItem.getTitle(), newItem.getTitle()) &&
               Objects.equals(oldItem.getPhoto_url(), newItem.getPhoto_url()) &&
               oldItem.isLiked() == newItem.isLiked() &&
               Objects.equals(oldItem.getMealType(), newItem.getMealType());
    }

    @Override
    public Object getChangePayload(@NonNull Recipe oldItem, @NonNull Recipe newItem) {
        // Возвращаем конкретные изменения для частичных обновлений
        Bundle payload = new Bundle();
        
        if (oldItem.isLiked() != newItem.isLiked()) {
            payload.putBoolean("liked_changed", true);
            payload.putBoolean("new_liked_state", newItem.isLiked());
        }
        
        if (!Objects.equals(oldItem.getPhoto_url(), newItem.getPhoto_url())) {
            payload.putBoolean("image_changed", true);
            payload.putString("new_image_url", newItem.getPhoto_url());
        }
        
        return payload.isEmpty() ? null : payload;
    }
};
```

### 6.2 Обработка частичных обновлений
```java
// RecipeListAdapter.java - частичные обновления
@Override
public void onBindViewHolder(@NonNull RecipeViewHolder holder, int position, 
                           @NonNull List<Object> payloads) {
    if (payloads.isEmpty()) {
        super.onBindViewHolder(holder, position, payloads);
        return;
    }
    
    // Обработка частичных обновлений
    for (Object payload : payloads) {
        if (payload instanceof Bundle) {
            Bundle bundle = (Bundle) payload;
            
            if (bundle.getBoolean("liked_changed", false)) {
                boolean newLikedState = bundle.getBoolean("new_liked_state", false);
                updateLikeButton(holder, newLikedState);
            }
            
            if (bundle.getBoolean("image_changed", false)) {
                String newImageUrl = bundle.getString("new_image_url");
                loadRecipeImage(holder.imageView, newImageUrl);
            }
        }
    }
}
```

## Фаза 7: Мониторинг производительности (Низкая)

### 7.1 Добавление метрик производительности
```java
// PerformanceMonitor.java - новый класс
public class PerformanceMonitor {
    private static final String TAG = "PerformanceMonitor";
    
    public static void measureDatabaseOperation(String operation, Runnable task) {
        long startTime = System.currentTimeMillis();
        task.run();
        long endTime = System.currentTimeMillis();
        
        long duration = endTime - startTime;
        if (duration > 100) { // Логируем только долгие операции
            Log.w(TAG, operation + " took " + duration + "ms");
        }
    }
    
    public static void measureRecyclerViewBind(Runnable bindTask) {
        long startTime = System.nanoTime();
        bindTask.run();
        long endTime = System.nanoTime();
        
        long durationMs = (endTime - startTime) / 1_000_000;
        if (durationMs > 16) { // 16ms = 60fps
            Log.w(TAG, "RecyclerView bind took " + durationMs + "ms");
        }
    }
}
```

## План внедрения

### Порядок реализации (по приоритету):
1. **Фаза 1-2**: Оптимизация сериализации и БД (критично)
2. **Фаза 3-4**: RecyclerView и изображения (высокий приоритет)  
3. **Фаза 5-6**: Батчинг и DiffUtil (средний приоритет)
4. **Фаза 7**: Мониторинг (низкий приоритет)

### Ожидаемые результаты:
- **Сокращение времени записи в БД на 60-70%**
- **Улучшение отзывчивости UI в 2-3 раза**
- **Снижение потребления памяти на 30-40%**
- **Плавная прокрутка без задержек при любом количестве рецептов**

### Риски и ограничения:
- Необходимость тестирования на разных версиях Android
- Временное увеличение сложности кода
- Требуется миграция данных для изменений в БД

### Метрики для измерения:
- Время выполнения операций БД (цель: <100ms для 1000 рецептов)
- FPS при прокрутке RecyclerView (цель: стабильные 60fps)
- Время загрузки экрана (цель: <500ms)
- Потребление памяти (цель: снижение на 30%) 