package com.example.cooking.ui.adapters.Recipe;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cooking.R;
import com.example.cooking.domain.entities.Ingredient;

import java.util.List;
import java.util.Objects;

/**
 * Адаптер для отображения списка ингредиентов в RecyclerView
 * Использует ListAdapter с DiffUtil для эффективных обновлений
 */
public class IngredientViewAdapter extends ListAdapter<IngredientViewAdapter.IngredientWithPortion, IngredientViewAdapter.IngredientViewHolder> {

    private final Context context;
    private int portionCount = 1;

    /**
     * Wrapper класс для ингредиента с информацией о порциях
     * Нужен для корректной работы DiffUtil при изменении только portiontCount
     */
    public static class IngredientWithPortion {
        private final Ingredient ingredient;
        private final int portionCount;

        public IngredientWithPortion(Ingredient ingredient, int portionCount) {
            this.ingredient = ingredient;
            this.portionCount = portionCount;
        }

        public Ingredient getIngredient() { return ingredient; }
        public int getPortionCount() { return portionCount; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            IngredientWithPortion that = (IngredientWithPortion) o;
            return portionCount == that.portionCount && Objects.equals(ingredient, that.ingredient);
        }

        @Override
        public int hashCode() {
            return Objects.hash(ingredient, portionCount);
        }
    }

    public IngredientViewAdapter(Context context, List<Ingredient> ingredients) {
        super(DIFF_CALLBACK);
        this.context = context;
        updateIngredientsInternal(ingredients, portionCount);
    }

    @NonNull
    @Override
    public IngredientViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_ingredient, parent, false);
        return new IngredientViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull IngredientViewHolder holder, int position) {
        IngredientWithPortion item = getItem(position);
        holder.bind(item.getIngredient(), item.getPortionCount());
    }

    /**
     * Эффективно обновляет список ингредиентов используя DiffUtil
     * @param newIngredients новый список ингредиентов
     */
    public void updateIngredients(List<Ingredient> newIngredients) {
        updateIngredientsInternal(newIngredients, this.portionCount);
    }

    /**
     * Эффективно обновляет количество порций используя DiffUtil
     * @param portionCount новое количество порций
     */
    public void updatePortionCount(int portionCount) {
        if (this.portionCount != portionCount) {
            this.portionCount = portionCount;
            // Получаем текущие ингредиенты и обновляем их с новым portionCount
            List<IngredientWithPortion> currentItems = getCurrentList();
            if (!currentItems.isEmpty()) {
                List<IngredientWithPortion> updatedItems = currentItems.stream()
                    .map(item -> new IngredientWithPortion(item.getIngredient(), portionCount))
                    .collect(java.util.stream.Collectors.toList());
                submitList(updatedItems);
            }
        }
    }

    /**
     * Внутренний метод для обновления ингредиентов
     */
    private void updateIngredientsInternal(List<Ingredient> ingredients, int portionCount) {
        if (ingredients == null || ingredients.isEmpty()) {
            submitList(java.util.Collections.emptyList());
            return;
        }

        List<IngredientWithPortion> items = ingredients.stream()
            .map(ingredient -> new IngredientWithPortion(ingredient, portionCount))
            .collect(java.util.stream.Collectors.toList());
        submitList(items);
    }

    /**
     * ViewHolder для ингредиента
     */
    static class IngredientViewHolder extends RecyclerView.ViewHolder {
        private final TextView nameTextView;
        private final TextView amountTextView;

        public IngredientViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.ingredient_name);
            amountTextView = itemView.findViewById(R.id.ingredient_amount);
        }

        /**
         * Привязывает данные ингредиента к view
         * @param ingredient объект ингредиента
         * @param portionCount количество порций
         */
        public void bind(Ingredient ingredient, int portionCount) {
            if (ingredient == null) {
                Log.e("IngredientViewHolder", "Получен null ингредиент");
                nameTextView.setText("Ошибка: ингредиент отсутствует");
                amountTextView.setText("");
                return;
            }
            
            // Устанавливаем название
            String name = ingredient.getName();
            if (name != null && !name.isEmpty()) {
                nameTextView.setText(name);
            } else {
                nameTextView.setText("Без названия");
            }
            
            // Рассчитываем количество с учетом порций
            float calculatedAmount = ingredient.getAmount() * portionCount;
            String unit = ingredient.getUnit();
            
            if (unit == null) {
                unit = "";
            }
            
            // Форматируем вывод количества и единицы измерения
            String formattedAmount;
            if (calculatedAmount == (int) calculatedAmount) {
                // Если число целое, убираем дробную часть
                formattedAmount = String.format("%d %s", (int) calculatedAmount, unit);
            } else {
                // Иначе оставляем одну цифру после запятой
                formattedAmount = String.format("%.1f %s", calculatedAmount, unit);
            }
            
            amountTextView.setText(formattedAmount);
        }
    }

    /**
     * DiffUtil.ItemCallback для эффективного сравнения элементов
     */
    private static final DiffUtil.ItemCallback<IngredientWithPortion> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<IngredientWithPortion>() {
                @Override
                public boolean areItemsTheSame(@NonNull IngredientWithPortion oldItem, @NonNull IngredientWithPortion newItem) {
                    // Используем системный хеш-код для сравнения ингредиентов
                    return System.identityHashCode(oldItem.getIngredient()) == 
                           System.identityHashCode(newItem.getIngredient());
                }

                @Override
                public boolean areContentsTheSame(@NonNull IngredientWithPortion oldItem, @NonNull IngredientWithPortion newItem) {
                    Ingredient oldIngredient = oldItem.getIngredient();
                    Ingredient newIngredient = newItem.getIngredient();
                    
                    return oldItem.getPortionCount() == newItem.getPortionCount() &&
                           Objects.equals(oldIngredient.getName(), newIngredient.getName()) &&
                           oldIngredient.getAmount() == newIngredient.getAmount() &&
                           Objects.equals(oldIngredient.getUnit(), newIngredient.getUnit()) &&
                           Objects.equals(oldIngredient.getType(), newIngredient.getType());
                }
            };
} 