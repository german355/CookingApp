package com.example.cooking.ui.adapters.Recipe;

import android.text.Editable;
import android.text.TextWatcher;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.AutoCompleteTextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.example.cooking.R;
import com.example.cooking.domain.entities.Ingredient;
import com.google.android.material.textfield.TextInputEditText;
import java.util.Objects;
import android.widget.ArrayAdapter;
import com.google.android.material.textfield.TextInputLayout;

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
        
        // TextInputLayout для отображения ошибок
        private final TextInputLayout nameLayout;
        private final TextInputLayout countLayout;
        private final TextInputLayout typeLayout;
        
        private Ingredient currentIngredient;
        private int currentPosition;

        // TextWatcher для отслеживания изменений и предотвращения рекурсии
        private TextWatcher nameWatcher;
        private TextWatcher countWatcher;
        
        // Handler для debounce
        private final Handler debounceHandler = new Handler(Looper.getMainLooper());
        private Runnable pendingNameUpdate;
        private Runnable pendingCountUpdate;
        private static final int DEBOUNCE_DELAY_MS = 300; // 300ms задержка

        ViewHolder(@NonNull View itemView, IngredientUpdateListener listener) {
            super(itemView);
            this.listener = listener;
            nameEditText = itemView.findViewById(R.id.edit_ingredient_name);
            countEditText = itemView.findViewById(R.id.edit_ingredient_count);
            typeEditText = itemView.findViewById(R.id.edit_ingredient_type);
            removeButton = itemView.findViewById(R.id.button_remove_ingredient);
            
            // Получаем TextInputLayout через getParent()
            nameLayout = (TextInputLayout) nameEditText.getParent().getParent();
            countLayout = (TextInputLayout) countEditText.getParent().getParent();
            typeLayout = (TextInputLayout) typeEditText.getParent().getParent();
            
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
                    typeLayout.setError(null); // Очищаем ошибку при выборе
                    listener.onIngredientUpdated(currentPosition, currentIngredient);
                }
            });

            removeButton.setOnClickListener(v -> {
                if (currentPosition != RecyclerView.NO_POSITION) {
                    listener.onIngredientRemoved(currentPosition);
                }
            });
            
            // Добавляем локальную валидацию при потере фокуса
            setupFocusValidation();
        }

        void bind(Ingredient ingredient, int position) {
            currentIngredient = ingredient;
            currentPosition = position;

            // Удаляем старые Watcher'ы перед установкой нового текста
            removeWatchers();

            // Очищаем ошибки валидации при связывании новых данных
            nameLayout.setError(null);
            countLayout.setError(null);
            typeLayout.setError(null);

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
            
            // Очищаем pending callbacks для предотвращения memory leaks
            if (pendingNameUpdate != null) {
                debounceHandler.removeCallbacks(pendingNameUpdate);
                pendingNameUpdate = null;
            }
            if (pendingCountUpdate != null) {
                debounceHandler.removeCallbacks(pendingCountUpdate);
                pendingCountUpdate = null;
            }
        }

        private void addWatchers() {
            nameWatcher = new SimpleTextWatcher() {
                @Override
                public void afterTextChanged(Editable s) {
                    if (currentIngredient != null) {
                        // Отменяем предыдущее обновление если оно ещё не выполнилось
                        if (pendingNameUpdate != null) {
                            debounceHandler.removeCallbacks(pendingNameUpdate);
                        }
                        
                        // Создаём новое отложенное обновление
                        pendingNameUpdate = () -> {
                            currentIngredient.setName(s.toString().trim());
                            // Очищаем ошибку при успешном вводе
                            if (!s.toString().trim().isEmpty()) {
                                nameLayout.setError(null);
                            }
                            listener.onIngredientUpdated(currentPosition, currentIngredient);
                        };
                        
                        // Запускаем с задержкой
                        debounceHandler.postDelayed(pendingNameUpdate, DEBOUNCE_DELAY_MS);
                    }
                }
            };

            countWatcher = new SimpleTextWatcher() {
                @Override
                public void afterTextChanged(Editable s) {
                    if (currentIngredient != null) {
                        // Отменяем предыдущее обновление если оно ещё не выполнилось
                        if (pendingCountUpdate != null) {
                            debounceHandler.removeCallbacks(pendingCountUpdate);
                        }
                        
                        // Создаём новое отложенное обновление
                        pendingCountUpdate = () -> {
                            try {
                                int count = s.toString().isEmpty() ? 0 : Integer.parseInt(s.toString());
                                currentIngredient.setCount(count);
                                // Очищаем ошибку при успешном вводе корректного числа
                                if (count > 0) {
                                    countLayout.setError(null);
                                }
                                listener.onIngredientUpdated(currentPosition, currentIngredient);
                            } catch (NumberFormatException e) {
                                // Игнорируем ошибки парсинга
                            }
                        };
                        
                        // Запускаем с задержкой
                        debounceHandler.postDelayed(pendingCountUpdate, DEBOUNCE_DELAY_MS);
                    }
                }
            };
            
            nameEditText.addTextChangedListener(nameWatcher);
            countEditText.addTextChangedListener(countWatcher);
        }

        private void setupFocusValidation() {
            // Валидация названия ингредиента при потере фокуса
            nameEditText.setOnFocusChangeListener((v, hasFocus) -> {
                if (!hasFocus && currentIngredient != null) {
                    String name = nameEditText.getText().toString().trim();
                    if (name.isEmpty()) {
                        nameLayout.setError("Укажите название ингредиента");
                    } else {
                        nameLayout.setError(null);
                    }
                }
            });
            
            // Валидация количества при потере фокуса
            countEditText.setOnFocusChangeListener((v, hasFocus) -> {
                if (!hasFocus && currentIngredient != null) {
                    String countText = countEditText.getText().toString().trim();
                    if (countText.isEmpty()) {
                        countLayout.setError("Укажите количество");
                    } else {
                        try {
                            float count = Float.parseFloat(countText);
                            if (count <= 0) {
                                countLayout.setError("Количество должно быть больше 0");
                            } else {
                                countLayout.setError(null);
                            }
                        } catch (NumberFormatException e) {
                            countLayout.setError("Введите корректное число");
                        }
                    }
                }
            });
            
            // Валидация типа ингредиента при потере фокуса
            typeEditText.setOnFocusChangeListener((v, hasFocus) -> {
                if (!hasFocus && currentIngredient != null) {
                    String type = typeEditText.getText().toString().trim();
                    if (type.isEmpty()) {
                        typeLayout.setError("Выберите единицу измерения");
                    } else {
                        typeLayout.setError(null);
                    }
                }
            });
        }
    }

    // DiffUtil для эффективного обновления
    private static final DiffUtil.ItemCallback<Ingredient> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<Ingredient>() {
        @Override
        public boolean areItemsTheSame(@NonNull Ingredient oldItem, @NonNull Ingredient newItem) {
    
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