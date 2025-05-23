package com.example.cooking.adapters;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.AutoCompleteTextView;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cooking.R;
import com.example.cooking.Recipe.Ingredient;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

/**
 * Адаптер для отображения и редактирования списка ингредиентов в RecyclerView.
 */
public class EditIngredientsAdapter extends RecyclerView.Adapter<EditIngredientsAdapter.IngredientViewHolder> {

    private final List<Ingredient> ingredients;
    private final Context context;
    private final IngredientInteractionListener listener;

    /**
     * Интерфейс для взаимодействия с элементами списка ингредиентов.
     */
    public interface IngredientInteractionListener {
        void onIngredientUpdated(int position, Ingredient ingredient);
        void onIngredientRemoved(int position);
    }

    public EditIngredientsAdapter(Context context, IngredientInteractionListener listener) {
        this.context = context;
        this.listener = listener;
        this.ingredients = new ArrayList<>();
    }

    /**
     * Обновляет список ингредиентов с использованием DiffUtil и уведомляет об изменении данных.
     */
    public void setIngredients(List<Ingredient> newIngredients) {
        IngredientDiffCallback diffCallback = new IngredientDiffCallback(this.ingredients, newIngredients);
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);
        
        this.ingredients.clear();
        this.ingredients.addAll(newIngredients);
        
        diffResult.dispatchUpdatesTo(this);
    }

    public List<Ingredient> getIngredients() {
        return new ArrayList<>(ingredients);
    }

    @NonNull
    @Override
    public IngredientViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ingredient_edit, parent, false);
        return new IngredientViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull IngredientViewHolder holder, int position) {
        holder.bind(ingredients.get(position), position);
    }

    @Override
    public int getItemCount() {
        return ingredients.size();
    }

    public class IngredientViewHolder extends RecyclerView.ViewHolder {
        private final AutoCompleteTextView nameEditText;
        private final TextInputEditText countEditText;
        private final AutoCompleteTextView typeAutoCompleteTextView;
        private final ImageButton removeButton;
        
        private TextWatcher nameWatcher;
        private TextWatcher countWatcher;
        
        private final ArrayAdapter<String> unitAdapter;

        public IngredientViewHolder(@NonNull View itemView) {
            super(itemView);
            nameEditText = itemView.findViewById(R.id.edit_ingredient_name);
            countEditText = itemView.findViewById(R.id.edit_ingredient_count);
            typeAutoCompleteTextView = itemView.findViewById(R.id.edit_ingredient_type);
            removeButton = itemView.findViewById(R.id.button_remove_ingredient);
            
            // Настройка выпадающего списка для поля названия ингредиента
            ArrayAdapter<String> nameAdapter = new ArrayAdapter<>(
                context,
                android.R.layout.simple_dropdown_item_1line,
                context.getResources().getStringArray(R.array.ingredients_list)
            );
            nameEditText.setAdapter(nameAdapter);
            nameEditText.setThreshold(2); // Показывать подсказки после ввода 2 символов
            
            // Обеспечим корректную ширину выпадающего списка для имени
            nameEditText.setDropDownWidth(android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
            
            // Настройка выпадающего списка для поля единицы измерения
            unitAdapter = new ArrayAdapter<>(
                context,
                android.R.layout.simple_dropdown_item_1line,
                context.getResources().getStringArray(R.array.ingredient_types)
            );
            typeAutoCompleteTextView.setAdapter(unitAdapter);
            
            // Обеспечим корректную ширину выпадающего списка для типа
            typeAutoCompleteTextView.setDropDownWidth(android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
            
            // Настройка поля типа как выпадающего списка
            typeAutoCompleteTextView.setOnClickListener(v -> typeAutoCompleteTextView.showDropDown());
            typeAutoCompleteTextView.setOnItemClickListener((parent, view, pos, id) -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    String type = (String) parent.getItemAtPosition(pos);
                    Ingredient ingredient = ingredients.get(position);
                    ingredient.setType(type);
                    listener.onIngredientUpdated(position, ingredient);
                }
            });
            
            // Инициализируем слушатели текста
            nameWatcher = new SimpleTextWatcher() {
                @Override
                public void afterTextChanged(Editable s) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        Ingredient ingredient = ingredients.get(position);
                        ingredient.setName(s.toString().trim());
                        listener.onIngredientUpdated(position, ingredient);
                    }
                }
            };
            
            countWatcher = new SimpleTextWatcher() {
                @Override
                public void afterTextChanged(Editable s) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        Ingredient ingredient = ingredients.get(position);
                        try {
                            int count = s.toString().isEmpty() ? 0 : Integer.parseInt(s.toString());
                            ingredient.setCount(count);
                            listener.onIngredientUpdated(position, ingredient);
                        } catch (NumberFormatException e) {
                            // Игнорируем неверный формат
                        }
                    }
                }
            };
            
            removeButton.setOnClickListener(v -> {
                if (getAdapterPosition() != RecyclerView.NO_POSITION) {
                    listener.onIngredientRemoved(getAdapterPosition());
                }
            });
        }

        /**
         * Привязывает данные ингредиента к элементам UI.
         */
        public void bind(Ingredient ingredient, int position) {
            removeTextWatchers();
            
            nameEditText.setText(ingredient.getName());
            countEditText.setText(ingredient.getCount() > 0 ? String.valueOf(ingredient.getCount()) : "");
            typeAutoCompleteTextView.setText(ingredient.getType(), false);
            
            if (position == 0) {
                removeButton.setVisibility(View.GONE);
            } else {
                removeButton.setVisibility(View.VISIBLE);
            }
            
            attachTextWatchers();
        }
        
        private void removeTextWatchers() {
            nameEditText.removeTextChangedListener(nameWatcher);
            countEditText.removeTextChangedListener(countWatcher);
        }
        
        private void attachTextWatchers() {
            nameEditText.addTextChangedListener(nameWatcher);
            countEditText.addTextChangedListener(countWatcher);
        }
    }
    
    private static abstract class SimpleTextWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}
    }
    
    private static class IngredientDiffCallback extends DiffUtil.Callback {
        private final List<Ingredient> oldList;
        private final List<Ingredient> newList;
        
        public IngredientDiffCallback(List<Ingredient> oldList, List<Ingredient> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }
        
        @Override
        public int getOldListSize() {
            return oldList.size();
        }
        
        @Override
        public int getNewListSize() {
            return newList.size();
        }
        
        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return oldList.get(oldItemPosition).equals(newList.get(newItemPosition));
        }
        
        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            return oldList.get(oldItemPosition).equals(newList.get(newItemPosition));
        }
    }
} 