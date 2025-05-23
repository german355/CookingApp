package com.example.cooking.adapters;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cooking.R;
import com.example.cooking.Recipe.Step;

import java.util.ArrayList;
import java.util.List;

/**
 * Адаптер для отображения и редактирования списка шагов приготовления в RecyclerView.
 */
public class EditStepsAdapter extends RecyclerView.Adapter<EditStepsAdapter.StepViewHolder> {

    private final List<Step> steps;
    private final StepInteractionListener listener;

    /**
     * Интерфейс для взаимодействия с элементами списка шагов.
     */
    public interface StepInteractionListener {
        void onStepChanged(int position, Step step);
        void onStepRemove(int position);
    }

    public EditStepsAdapter(List<Step> initialSteps, StepInteractionListener listener) {
        this.steps = new ArrayList<>(initialSteps);
        this.listener = listener;
    }

    /**
     * Обновляет список шагов с использованием DiffUtil и уведомляет об изменении данных.
     */
    public void updateSteps(List<Step> newSteps) {
        StepDiffCallback diffCallback = new StepDiffCallback(this.steps, newSteps);
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);
        
        this.steps.clear();
        this.steps.addAll(newSteps);
        
        diffResult.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public StepViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_step_edit, parent, false);
        return new StepViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StepViewHolder holder, int position) {
        holder.bind(steps.get(position), position);
    }

    @Override
    public int getItemCount() {
        return steps.size();
    }

    class StepViewHolder extends RecyclerView.ViewHolder {
        private final TextView stepNumberTextView;
        private final EditText stepInstructionEditText;
        private final ImageButton removeButton;
        private TextWatcher instructionWatcher;

        StepViewHolder(@NonNull View itemView) {
            super(itemView);
            stepNumberTextView = itemView.findViewById(R.id.text_step_number);
            stepInstructionEditText = itemView.findViewById(R.id.edit_step_instruction);
            removeButton = itemView.findViewById(R.id.button_remove_step);

            // Инициализация текстового слушателя
            instructionWatcher = new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) { }

                @Override
                public void afterTextChanged(Editable s) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION && stepInstructionEditText.hasFocus()) {
                        Step step = steps.get(position);
                        step.setInstruction(s.toString());
                        listener.onStepChanged(position, step);
                    }
                }
            };
            
            setupRemoveButton();
        }

        void bind(Step step, int position) {
            removeTextWatcher();

            stepNumberTextView.setText(String.format("Шаг %d", position + 1));

            stepInstructionEditText.setText(step.getInstruction());
            
            if (position == 0) {
                removeButton.setVisibility(View.GONE);
            } else {
                removeButton.setVisibility(View.VISIBLE);
            }

            attachTextWatcher();
        }
        
        private void removeTextWatcher() {
            stepInstructionEditText.removeTextChangedListener(instructionWatcher);
        }
        
        private void attachTextWatcher() {
            stepInstructionEditText.addTextChangedListener(instructionWatcher);
        }

        private void setupRemoveButton() {
            removeButton.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    listener.onStepRemove(position);
                }
            });
        }
    }
    
    private static class StepDiffCallback extends DiffUtil.Callback {
        private final List<Step> oldList;
        private final List<Step> newList;
        
        public StepDiffCallback(List<Step> oldList, List<Step> newList) {
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