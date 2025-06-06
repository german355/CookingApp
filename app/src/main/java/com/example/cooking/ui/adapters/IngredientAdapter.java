package com.example.cooking.ui.adapters;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.AutoCompleteTextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.example.cooking.R;
import com.example.cooking.Recipe.Ingredient;
import com.google.android.material.textfield.TextInputEditText;
import java.util.Objects;
import android.widget.ArrayAdapter;

public class IngredientAdapter extends ListAdapter<Ingredient, IngredientAdapter.ViewHolder> {

    private final IngredientUpdateListener listener;

    public interface IngredientUpdateListener {
        void onIngredientUpdated(int position, Ingredient ingredient);
        void onIngredientRemoved(int position);
    }

    public IngredientAdapter(@NonNull IngredientUpdateListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ingredient_edit, parent, false);
        return new ViewHolder(view, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position), position);
    }

    // ViewHolder с обработчиками изменений
    class ViewHolder extends RecyclerView.ViewHolder {
        private final AutoCompleteTextView nameEditText;
        private final TextInputEditText countEditText;
        private final AutoCompleteTextView typeEditText;
        private final ImageButton removeButton;
        private final IngredientUpdateListener listener;
        private Ingredient currentIngredient;
        private int currentPosition;

        // TextWatcher для отслеживания изменений и предотвращения рекурсии
        private TextWatcher nameWatcher;
        private TextWatcher countWatcher;

        ViewHolder(@NonNull View itemView, IngredientUpdateListener listener) {
            super(itemView);
            this.listener = listener;
            nameEditText = itemView.findViewById(R.id.edit_ingredient_name);
            countEditText = itemView.findViewById(R.id.edit_ingredient_count);
            typeEditText = itemView.findViewById(R.id.edit_ingredient_type);
            removeButton = itemView.findViewById(R.id.button_remove_ingredient);
            
            // Настройка выпадающего списка для поля названия ингредиента
            ArrayAdapter<String> nameAdapter = new ArrayAdapter<>(
                itemView.getContext(),
                android.R.layout.simple_dropdown_item_1line,
                itemView.getContext().getResources().getStringArray(R.array.ingredients_list)
            );
            nameEditText.setAdapter(nameAdapter);
            nameEditText.setThreshold(2); // Показывать подсказки после ввода 2 символов
            
            // Обеспечим корректную ширину выпадающего списка для имени
            nameEditText.setDropDownWidth(android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
            
            // Настройка выпадающего списка для поля типа ингредиента
            ArrayAdapter<String> unitAdapter = new ArrayAdapter<>(
                itemView.getContext(),
                android.R.layout.simple_dropdown_item_1line,
                itemView.getContext().getResources().getStringArray(R.array.ingredient_types)
            );
            typeEditText.setAdapter(unitAdapter);
            
            // Обеспечим корректную ширину выпадающего списка для типа
            typeEditText.setDropDownWidth(android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
            
            // Настройка поля типа как выпадающего списка
            typeEditText.setOnClickListener(v -> typeEditText.showDropDown());
            typeEditText.setOnItemClickListener((parent, view, pos, id) -> {
                String type = (String) parent.getItemAtPosition(pos);
                if (currentIngredient != null) {
                    currentIngredient.setType(type);
                    listener.onIngredientUpdated(currentPosition, currentIngredient);
                }
            });

            removeButton.setOnClickListener(v -> {
                if (currentPosition != RecyclerView.NO_POSITION) {
                    listener.onIngredientRemoved(currentPosition);
                }
            });
        }

        void bind(Ingredient ingredient, int position) {
            currentIngredient = ingredient;
            currentPosition = position;

            // Удаляем старые Watcher'ы перед установкой нового текста
            removeWatchers();

            nameEditText.setText(ingredient.getName());
            countEditText.setText(ingredient.getCount() > 0 ? String.valueOf(ingredient.getCount()) : "");
            typeEditText.setText(ingredient.getType(), false);
            
            // Скрываем кнопку удаления только для первого ингредиента (индекс 0)
            // Остальные ингредиенты можно удалять, даже если они стали первыми после удаления предыдущих
            if (position == 0) {
                removeButton.setVisibility(View.GONE);
            } else {
                removeButton.setVisibility(View.VISIBLE);
            }

            // Добавляем новые Watcher'ы
            addWatchers();
        }

        private void removeWatchers() {
            if (nameWatcher != null) nameEditText.removeTextChangedListener(nameWatcher);
            if (countWatcher != null) countEditText.removeTextChangedListener(countWatcher);
        }

        private void addWatchers() {
            nameWatcher = new SimpleTextWatcher() {
                @Override
                public void afterTextChanged(Editable s) {
                    if (currentIngredient != null) {
                        currentIngredient.setName(s.toString().trim());
                        listener.onIngredientUpdated(currentPosition, currentIngredient);
                    }
                }
            };

            countWatcher = new SimpleTextWatcher() {
                @Override
                public void afterTextChanged(Editable s) {
                    if (currentIngredient != null) {
                        try {
                            int count = s.toString().isEmpty() ? 0 : Integer.parseInt(s.toString());
                            currentIngredient.setCount(count);
                            listener.onIngredientUpdated(currentPosition, currentIngredient);
                        } catch (NumberFormatException e) {
                            // Можно обработать ошибку ввода, если нужно
                        }
                    }
                }
            };
            
            nameEditText.addTextChangedListener(nameWatcher);
            countEditText.addTextChangedListener(countWatcher);
        }
    }

    // DiffUtil для эффективного обновления
    private static final DiffUtil.ItemCallback<Ingredient> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<Ingredient>() {
        @Override
        public boolean areItemsTheSame(@NonNull Ingredient oldItem, @NonNull Ingredient newItem) {
            // TODO: Добавить уникальный ID для ингредиента, если возможно, иначе сравнивать по позиции?
            // Временное сравнение по ссылкам, что не идеально для ListAdapter
            return oldItem == newItem; // Или использовать позицию/уникальный ID
        }

        @Override
        public boolean areContentsTheSame(@NonNull Ingredient oldItem, @NonNull Ingredient newItem) {
            return Objects.equals(oldItem.getName(), newItem.getName()) &&
                   oldItem.getCount() == newItem.getCount() &&
                   Objects.equals(oldItem.getType(), newItem.getType());
        }
    };

    // Вспомогательный класс для TextWatcher
    static abstract class SimpleTextWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}
    }
} 