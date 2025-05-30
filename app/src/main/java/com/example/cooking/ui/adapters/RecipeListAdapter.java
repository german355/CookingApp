package com.example.cooking.ui.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import com.example.cooking.ui.activities.MainActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import com.bumptech.glide.Glide;
import com.example.cooking.R;
import com.example.cooking.Recipe.Recipe;
import com.example.cooking.ui.activities.RecipeDetailActivity;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.imageview.ShapeableImageView;

/**
 * Адаптер для {@link RecyclerView}, отображающий список объектов {@link Recipe}.
 * Использует {@link DiffUtil} для эффективного обновления элементов списка,
 * что улучшает производительность и анимации при изменении данных.
 * Предоставляет колбэк {@link OnRecipeLikeListener} для обработки нажатий на кнопку "Нравится".
 */
public class RecipeListAdapter extends ListAdapter<Recipe, RecipeListAdapter.RecipeViewHolder> {
    
    private static final String TAG = "RecipeListAdapter";
    private final OnRecipeLikeListener likeListener;

    /**
     * Коллбэк для {@link DiffUtil}, который определяет, как сравнивать элементы списка
     * для эффективного обновления {@link RecyclerView}.
     */
    private static final DiffUtil.ItemCallback<Recipe> DIFF_CALLBACK = new DiffUtil.ItemCallback<Recipe>() {
        /**
         * Проверяет, являются ли два объекта одним и тем же элементом.
         * Сравнение обычно производится по уникальному идентификатору.
         *
         * @param oldItem Старый элемент.
         * @param newItem Новый элемент.
         * @return True, если элементы представляют один и тот же объект, иначе false.
         */
        @Override
        public boolean areItemsTheSame(@NonNull Recipe oldItem, @NonNull Recipe newItem) {
            // Проверяем, тот же ли это рецепт по ID
            return oldItem.getId() == newItem.getId();
        }

        /**
         * Проверяет, одинаково ли содержимое двух элементов.
         * Вызывается, только если {@link #areItemsTheSame(Recipe, Recipe)} вернул true.
         *
         * @param oldItem Старый элемент.
         * @param newItem Новый элемент.
         * @return True, если содержимое элементов одинаково, иначе false.
         */
        @Override
        public boolean areContentsTheSame(@NonNull Recipe oldItem, @NonNull Recipe newItem) {
            // Проверяем, изменилось ли содержимое рецепта, включая название, статус "лайка" и URL фото.
            return oldItem.getTitle().equals(newItem.getTitle()) &&
                   oldItem.isLiked() == newItem.isLiked() &&
                   (oldItem.getPhoto_url() == null ? newItem.getPhoto_url() == null :
                    oldItem.getPhoto_url().equals(newItem.getPhoto_url()));
        }
    };

    /**
     * Интерфейс для обработки события нажатия на кнопку "Нравится" для рецепта.
     */
    public interface OnRecipeLikeListener {
        /**
         * Вызывается при нажатии на кнопку "Нравится" на карточке рецепта.
         *
         * @param recipe Рецепт, для которого изменился статус "Нравится".
         * @param isLiked Новое состояние "Нравится" (true, если отмечено, false иначе).
         */
        void onRecipeLike(Recipe recipe, boolean isLiked);
    }

    /**
     * Конструктор адаптера.
     *
     * @param likeListener Слушатель для обработки событий нажатия на кнопку "Нравится".
     *                     Не должен быть null, если предполагается обработка лайков.
     */
    public RecipeListAdapter(@NonNull OnRecipeLikeListener likeListener) {
        super(DIFF_CALLBACK);
        this.likeListener = likeListener;
    }

    /**
     * Возвращает объект {@link Recipe} по указанной позиции в списке.
     *
     * @param position Позиция элемента в списке.
     * @return Объект {@link Recipe} или null, если позиция некорректна.
     * @throws IndexOutOfBoundsException если позиция выходит за пределы списка.
     */
    public Recipe getRecipeAt(int position) {
        return getItem(position);
    }
    
    @Override
    public void submitList(List<Recipe> list) {
        Log.d(TAG, "submitList: получен новый список размером " + (list != null ? list.size() : 0) + " рецептов");
        if (list != null && !list.isEmpty()) {
            Log.d(TAG, "Первый рецепт в новом списке: " + list.get(0).getTitle() + " (ID: " + list.get(0).getId() + ")");
        } else {
            Log.d(TAG, "Получен пустой список рецептов!");
        }
        super.submitList(list);
    }

    /**
     * Вызывается, когда {@link RecyclerView} нуждается в новом {@link RecipeViewHolder}
     * для отображения элемента.
     *
     * @param parent   ViewGroup, в которую будет добавлено новое View после его привязки к
     *                 позиции адаптера.
     * @param viewType Тип представления нового View.
     * @return Новый экземпляр {@link RecipeViewHolder}, который содержит View для элемента списка.
     */
    @NonNull
    @Override
    public RecipeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.recipe_card, parent, false);
        return new RecipeViewHolder(view);
    }

    /**
     * Вызывается {@link RecyclerView} для отображения данных в указанной позиции.
     * Этот метод должен обновить содержимое {@link RecipeViewHolder#itemView}, чтобы отразить элемент
     * в данной позиции в наборе данных.
     *
     * @param holder   {@link RecipeViewHolder}, который должен быть обновлен для представления
     *                 содержимого элемента в данной позиции в наборе данных.
     * @param position Позиция элемента в наборе данных адаптера.
     */
    @Override
    public void onBindViewHolder(@NonNull RecipeViewHolder holder, int position) {
        Recipe recipe = getItem(position);
        
        Log.d(TAG, "onBindViewHolder: привязка данных для позиции " + position + ", рецепт: " + recipe.getTitle() + " (ID: " + recipe.getId() + ")");
        
        holder.titleTextView.setText(recipe.getTitle());
        
        // Загрузка изображения рецепта
        if (recipe.getPhoto_url() != null && !recipe.getPhoto_url().isEmpty()){
            Glide.with(holder.imageView.getContext())
                    .load(recipe.getPhoto_url())
                    .placeholder(R.drawable.white_card_background) // Заглушка во время загрузки
                    .error(R.drawable.white_card_background)       // Изображение при ошибке загрузки
                    .centerCrop()
                    .into(holder.imageView);
        } else {
            // Если URL фото отсутствует, устанавливаем изображение по умолчанию
            holder.imageView.setImageResource(R.drawable.white_card_background); // TODO: Рассмотреть использование более информативной заглушки
        }
        
        Log.d(TAG, "Binding ViewHolder for Recipe ID: " + recipe.getId() + ", Title: " + recipe.getTitle() + ", isLiked: " + recipe.isLiked());
        
        holder.favoriteButton.setChecked(recipe.isLiked());
        
        // Динамическое изменение цвета иконки "Нравится"
        if (recipe.isLiked()) {
            holder.favoriteButton.setButtonTintList(ColorStateList.valueOf(Color.parseColor("#FF0031"))); // Красный цвет для "лайка"
            Log.d(TAG, "Recipe ID: " + recipe.getId() + " is Liked. Setting RED tint.");
        } else {
            holder.favoriteButton.setButtonTintList(null); // Сброс на цвет по умолчанию (из темы)
            Log.d(TAG, "Recipe ID: " + recipe.getId() + " is NOT Liked. Setting NULL tint.");
        }
        
        // Убедимся, что кнопка избранного всегда поверх других элементов в CardView
        holder.favoriteButton.bringToFront();
        
        // Обработчик нажатия на кнопку "Нравится"
        holder.favoriteButton.setOnClickListener(v -> {
            boolean isChecked = holder.favoriteButton.isChecked();

            if (likeListener != null) {
                // Временно отключаем кнопку для предотвращения многократных быстрых нажатий,
                // пока выполняется операция добавления/удаления из избранного.
                holder.favoriteButton.setEnabled(false);
                
                likeListener.onRecipeLike(recipe, isChecked);
                
                // Включаем кнопку обратно с небольшой задержкой.
                // Это простое решение для предотвращения двойных кликов.
                // В более сложных сценариях может потребоваться управление состоянием через ViewModel.
                holder.favoriteButton.postDelayed(() -> holder.favoriteButton.setEnabled(true), 500); // 500 мс задержка
            }
        });
        
        // Обработчик нажатия на саму карточку рецепта для перехода к детальному экрану
        holder.cardView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), RecipeDetailActivity.class);
            intent.putExtra(RecipeDetailActivity.EXTRA_SELECTED_RECIPE, recipe);
            Log.d(TAG, "Starting RecipeDetailActivity for recipe: " + recipe.getTitle() + " (ID: " + recipe.getId() + ")");
            if (recipe.getPhoto_url() != null) {
                Log.d(TAG, "Photo URL: " + recipe.getPhoto_url());
            }
            // Извлекаем Activity из ContextWrapper
            Context context = v.getContext();
            Activity activity = null;
            Context baseContext = context;
            while (baseContext instanceof ContextWrapper) {
                if (baseContext instanceof Activity) {
                    activity = (Activity) baseContext;
                    break;
                }
                baseContext = ((ContextWrapper) baseContext).getBaseContext();
            }
            if (activity != null) {
                Log.d(TAG, "Launching RecipeDetailActivity via Activity for result");
                activity.startActivityForResult(intent, 200);
            } else {
                Log.d(TAG, "Activity not found, launching normally");
                context.startActivity(intent);
            }
        });
    }

    /**
     * ViewHolder, представляющий элемент списка рецептов (карточку рецепта).
     * Хранит ссылки на View-компоненты макета элемента списка.
     */
    static class RecipeViewHolder extends RecyclerView.ViewHolder {
        /** Текстовое поле для отображения названия рецепта. */
        TextView titleTextView;
        /** View для отображения изображения рецепта. */
        ShapeableImageView imageView;
        /** Корневой элемент карточки рецепта, обрабатывающий клики для перехода к деталям. */
        CardView cardView;
        /** Кнопка (чекбокс) для добавления/удаления рецепта из избранного. */
        MaterialCheckBox favoriteButton;

        /**
         * Конструктор ViewHolder.
         *
         * @param itemView View-компонент элемента списка (инфлейченный из XML-макета).
         */
        RecipeViewHolder(View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.recipe_title);
            imageView = itemView.findViewById(R.id.recipe_image);
            cardView = itemView.findViewById(R.id.recipe_card);
            favoriteButton = itemView.findViewById(R.id.favorite_button);
        }
    }
} 