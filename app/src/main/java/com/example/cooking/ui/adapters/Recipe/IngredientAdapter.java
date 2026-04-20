package com.example.cooking.ui.adapters.Recipe;

import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cooking.R;
import com.example.cooking.domain.entities.Ingredient;
import com.example.cooking.domain.units.UnitNormalizer;
import com.example.cooking.domain.units.UnitResolution;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Objects;

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

    class ViewHolder extends RecyclerView.ViewHolder {
        private static final int DEBOUNCE_DELAY_MS = 300;

        private final AutoCompleteTextView nameEditText;
        private final TextInputEditText countEditText;
        private final AutoCompleteTextView typeEditText;
        private final ImageButton removeButton;
        private final IngredientUpdateListener listener;
        private final TextInputLayout nameLayout;
        private final TextInputLayout countLayout;
        private final TextInputLayout typeLayout;
        private final String[] unitLabels;
        private final String[] unitValues;
        private final Handler debounceHandler = new Handler(Looper.getMainLooper());

        private Ingredient currentIngredient;
        private int currentPosition;
        private TextWatcher nameWatcher;
        private TextWatcher countWatcher;
        private Runnable pendingNameUpdate;
        private Runnable pendingCountUpdate;

        ViewHolder(@NonNull View itemView, IngredientUpdateListener listener) {
            super(itemView);
            this.listener = listener;
            nameEditText = itemView.findViewById(R.id.edit_ingredient_name);
            countEditText = itemView.findViewById(R.id.edit_ingredient_count);
            typeEditText = itemView.findViewById(R.id.edit_ingredient_type);
            removeButton = itemView.findViewById(R.id.button_remove_ingredient);

            nameLayout = (TextInputLayout) nameEditText.getParent().getParent();
            countLayout = (TextInputLayout) countEditText.getParent().getParent();
            typeLayout = (TextInputLayout) typeEditText.getParent().getParent();

            ArrayAdapter<String> nameAdapter = new ArrayAdapter<>(
                    itemView.getContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    itemView.getContext().getResources().getStringArray(R.array.ingredients_list)
            );
            nameEditText.setAdapter(nameAdapter);
            nameEditText.setThreshold(2);
            nameEditText.setDropDownWidth(ViewGroup.LayoutParams.WRAP_CONTENT);

            unitLabels = itemView.getContext().getResources().getStringArray(R.array.ingredient_types);
            unitValues = itemView.getContext().getResources().getStringArray(R.array.ingredient_type_values);
            ArrayAdapter<String> unitAdapter = new ArrayAdapter<>(
                    itemView.getContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    unitLabels
            );
            typeEditText.setAdapter(unitAdapter);
            typeEditText.setDropDownWidth(ViewGroup.LayoutParams.WRAP_CONTENT);

            typeEditText.setOnClickListener(v -> typeEditText.showDropDown());
            typeEditText.setOnItemClickListener((parent, view, pos, id) -> {
                if (currentIngredient != null) {
                    currentIngredient.setType((String) parent.getItemAtPosition(pos));
                    typeLayout.setError(null);
                    listener.onIngredientUpdated(currentPosition, currentIngredient);
                }
            });

            removeButton.setOnClickListener(v -> {
                if (currentPosition != RecyclerView.NO_POSITION) {
                    listener.onIngredientRemoved(currentPosition);
                }
            });

            setupFocusValidation();
        }

        void bind(Ingredient ingredient, int position) {
            currentIngredient = ingredient;
            currentPosition = position;

            removeWatchers();

            nameLayout.setError(null);
            countLayout.setError(null);
            typeLayout.setError(null);

            nameEditText.setText(ingredient.getName());
            countEditText.setText(ingredient.getCount() > 0 ? String.valueOf(ingredient.getCount()) : "");
            typeEditText.setText(resolveUnitLabel(ingredient.getType()), false);
            removeButton.setVisibility(position == 0 ? View.GONE : View.VISIBLE);

            addWatchers();
        }

        private void removeWatchers() {
            if (nameWatcher != null) {
                nameEditText.removeTextChangedListener(nameWatcher);
            }
            if (countWatcher != null) {
                countEditText.removeTextChangedListener(countWatcher);
            }
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
                    if (currentIngredient == null) {
                        return;
                    }

                    if (pendingNameUpdate != null) {
                        debounceHandler.removeCallbacks(pendingNameUpdate);
                    }

                    pendingNameUpdate = () -> {
                        currentIngredient.setName(s.toString().trim());
                        if (!s.toString().trim().isEmpty()) {
                            nameLayout.setError(null);
                        }
                        listener.onIngredientUpdated(currentPosition, currentIngredient);
                    };
                    debounceHandler.postDelayed(pendingNameUpdate, DEBOUNCE_DELAY_MS);
                }
            };

            countWatcher = new SimpleTextWatcher() {
                @Override
                public void afterTextChanged(Editable s) {
                    if (currentIngredient == null) {
                        return;
                    }

                    if (pendingCountUpdate != null) {
                        debounceHandler.removeCallbacks(pendingCountUpdate);
                    }

                    pendingCountUpdate = () -> {
                        try {
                            float count = s.toString().isEmpty() ? 0f : Float.parseFloat(s.toString());
                            currentIngredient.setCount(count);
                            if (count > 0f) {
                                countLayout.setError(null);
                            }
                            listener.onIngredientUpdated(currentPosition, currentIngredient);
                        } catch (NumberFormatException ignored) {
                        }
                    };
                    debounceHandler.postDelayed(pendingCountUpdate, DEBOUNCE_DELAY_MS);
                }
            };

            nameEditText.addTextChangedListener(nameWatcher);
            countEditText.addTextChangedListener(countWatcher);
        }

        private void setupFocusValidation() {
            nameEditText.setOnFocusChangeListener((v, hasFocus) -> {
                if (!hasFocus && currentIngredient != null) {
                    String name = nameEditText.getText().toString().trim();
                    if (name.isEmpty()) {
                        nameLayout.setError(itemView.getContext().getString(R.string.ingredient_error_name_required));
                    } else {
                        nameLayout.setError(null);
                    }
                }
            });

            countEditText.setOnFocusChangeListener((v, hasFocus) -> {
                if (!hasFocus && currentIngredient != null) {
                    String countText = countEditText.getText().toString().trim();
                    if (countText.isEmpty()) {
                        countLayout.setError(itemView.getContext().getString(R.string.ingredient_error_amount_required));
                    } else {
                        try {
                            float count = Float.parseFloat(countText);
                            if (count <= 0f) {
                                countLayout.setError(itemView.getContext().getString(R.string.ingredient_error_amount_positive));
                            } else {
                                countLayout.setError(null);
                            }
                        } catch (NumberFormatException e) {
                            countLayout.setError(itemView.getContext().getString(R.string.ingredient_error_amount_invalid));
                        }
                    }
                }
            });

            typeEditText.setOnFocusChangeListener((v, hasFocus) -> {
                if (!hasFocus && currentIngredient != null) {
                    String type = typeEditText.getText().toString().trim();
                    if (type.isEmpty()) {
                        typeLayout.setError(itemView.getContext().getString(R.string.ingredient_error_unit_required));
                    } else {
                        typeLayout.setError(null);
                    }
                }
            });
        }

        private String resolveUnitLabel(String storedValue) {
            UnitResolution resolution = UnitNormalizer.resolve(storedValue);
            String normalizedValue = resolution.getNormalizedValue();
            for (int i = 0; i < unitValues.length; i++) {
                if (unitValues[i].equals(normalizedValue)) {
                    return unitLabels[i];
                }
            }
            return resolution.getDisplayValue();
        }
    }

    private static final DiffUtil.ItemCallback<Ingredient> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<Ingredient>() {
                @Override
                public boolean areItemsTheSame(@NonNull Ingredient oldItem, @NonNull Ingredient newItem) {
                    return oldItem == newItem;
                }

                @Override
                public boolean areContentsTheSame(@NonNull Ingredient oldItem, @NonNull Ingredient newItem) {
                    return Objects.equals(oldItem.getName(), newItem.getName())
                            && oldItem.getCount() == newItem.getCount()
                            && Objects.equals(oldItem.getType(), newItem.getType());
                }
            };

    static abstract class SimpleTextWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }
    }
}
