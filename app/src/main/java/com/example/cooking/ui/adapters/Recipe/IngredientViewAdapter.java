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
import com.example.cooking.domain.units.MeasurementSystem;
import com.example.cooking.domain.units.UnitFormatter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Adapter for rendering recipe ingredients in the detail screen.
 */
public class IngredientViewAdapter extends ListAdapter<IngredientViewAdapter.IngredientWithPortion, IngredientViewAdapter.IngredientViewHolder> {

    private final Context context;
    private int portionCount = 1;
    private MeasurementSystem measurementSystem;

    public static class IngredientWithPortion {
        private final Ingredient ingredient;
        private final int portionCount;

        public IngredientWithPortion(Ingredient ingredient, int portionCount) {
            this.ingredient = ingredient;
            this.portionCount = portionCount;
        }

        public Ingredient getIngredient() {
            return ingredient;
        }

        public int getPortionCount() {
            return portionCount;
        }

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

    public IngredientViewAdapter(Context context, List<Ingredient> ingredients, MeasurementSystem measurementSystem) {
        super(DIFF_CALLBACK);
        this.context = context;
        this.measurementSystem = measurementSystem;
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
        holder.bind(item.getIngredient(), item.getPortionCount(), measurementSystem);
    }

    public void updateIngredients(List<Ingredient> newIngredients) {
        updateIngredientsInternal(newIngredients, this.portionCount);
    }

    public void updatePortionCount(int portionCount) {
        if (this.portionCount != portionCount) {
            this.portionCount = portionCount;
            List<IngredientWithPortion> currentItems = getCurrentList();
            if (!currentItems.isEmpty()) {
                List<IngredientWithPortion> updatedItems = new ArrayList<>(currentItems.size());
                for (IngredientWithPortion item : currentItems) {
                    updatedItems.add(new IngredientWithPortion(item.getIngredient(), portionCount));
                }
                submitList(updatedItems);
            }
        }
    }

    public void updateMeasurementSystem(MeasurementSystem measurementSystem) {
        if (measurementSystem != null && this.measurementSystem != measurementSystem) {
            this.measurementSystem = measurementSystem;
            notifyDataSetChanged();
        }
    }

    private void updateIngredientsInternal(List<Ingredient> ingredients, int portionCount) {
        if (ingredients == null || ingredients.isEmpty()) {
            submitList(java.util.Collections.emptyList());
            return;
        }

        List<IngredientWithPortion> items = new ArrayList<>(ingredients.size());
        for (Ingredient ingredient : ingredients) {
            items.add(new IngredientWithPortion(ingredient, portionCount));
        }
        submitList(items);
    }

    static class IngredientViewHolder extends RecyclerView.ViewHolder {
        private final TextView nameTextView;
        private final TextView amountTextView;

        public IngredientViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.ingredient_name);
            amountTextView = itemView.findViewById(R.id.ingredient_amount);
        }

        public void bind(Ingredient ingredient, int portionCount, MeasurementSystem measurementSystem) {
            if (ingredient == null) {
                Log.e("IngredientViewHolder", "Ingredient is null");
                nameTextView.setText(itemView.getContext().getString(R.string.ingredient_detail_missing_name));
                amountTextView.setText("");
                return;
            }

            String name = ingredient.getName();
            if (name != null && !name.isEmpty()) {
                nameTextView.setText(name);
            } else {
                nameTextView.setText(itemView.getContext().getString(R.string.ingredient_detail_unnamed));
            }

            float calculatedAmount = ingredient.getAmount() * portionCount;
            amountTextView.setText(UnitFormatter.format(calculatedAmount, ingredient.getType(), measurementSystem));
        }
    }

    private static final DiffUtil.ItemCallback<IngredientWithPortion> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<IngredientWithPortion>() {
                @Override
                public boolean areItemsTheSame(@NonNull IngredientWithPortion oldItem, @NonNull IngredientWithPortion newItem) {
                    return System.identityHashCode(oldItem.getIngredient()) ==
                            System.identityHashCode(newItem.getIngredient());
                }

                @Override
                public boolean areContentsTheSame(@NonNull IngredientWithPortion oldItem, @NonNull IngredientWithPortion newItem) {
                    Ingredient oldIngredient = oldItem.getIngredient();
                    Ingredient newIngredient = newItem.getIngredient();

                    return oldItem.getPortionCount() == newItem.getPortionCount()
                            && Objects.equals(oldIngredient.getName(), newIngredient.getName())
                            && oldIngredient.getAmount() == newIngredient.getAmount()
                            && Objects.equals(oldIngredient.getUnit(), newIngredient.getUnit())
                            && Objects.equals(oldIngredient.getType(), newIngredient.getType());
                }
            };
}
