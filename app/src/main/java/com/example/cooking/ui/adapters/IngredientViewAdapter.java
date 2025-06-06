package com.example.cooking.ui.adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cooking.R;
import com.example.cooking.Recipe.Ingredient;

import java.util.List;

/**
 * Адаптер для отображения списка ингредиентов в RecyclerView
 */
public class IngredientViewAdapter extends RecyclerView.Adapter<IngredientViewAdapter.IngredientViewHolder> {

    private final Context context;
    private List<Ingredient> ingredients;
    private int portionCount = 1;

    public IngredientViewAdapter(Context context, List<Ingredient> ingredients) {
        this.context = context;
        this.ingredients = ingredients;
    }

    @NonNull
    @Override
    public IngredientViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_ingredient, parent, false);
        return new IngredientViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull IngredientViewHolder holder, int position) {
        Ingredient ingredient = ingredients.get(position);
        holder.bind(ingredient, portionCount);
    }

    @Override
    public int getItemCount() {
        return ingredients == null ? 0 : ingredients.size();
    }

    /**
     * Обновляет список ингредиентов
     * @param newIngredients новый список ингредиентов
     */
    public void updateIngredients(List<Ingredient> newIngredients) {
        this.ingredients = newIngredients;
        notifyDataSetChanged();
    }

    /**
     * Обновляет количество порций
     * @param portionCount новое количество порций
     */
    public void updatePortionCount(int portionCount) {
        this.portionCount = portionCount;
        notifyDataSetChanged();
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
            
            // Логируем для отладки
            Log.d("IngredientViewHolder", "Отображение ингредиента: " + name + 
                  ", количество: " + calculatedAmount + " " + unit);
        }
    }
} 