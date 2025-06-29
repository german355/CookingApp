package com.example.cooking.ui.adapters.Recipe;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.cooking.R;
import com.example.cooking.domain.entities.Step;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Objects;

/**
 * Адаптер предназначенный для отображения и/или редактирования списка шагов  рецепта.
 * Адаптер может работать в двух режимах:
 * 1. Режим отображения:  макет {@code R.layout.item_step}.
 * 2. Режим редактирования: использует макет {@code R.layout.item_step_edit}
 *
 */
public class StepAdapter extends ListAdapter<Step, RecyclerView.ViewHolder> {

    private final Context context; // Контекст, обычно Activity или Fragment
    private final StepUpdateListener listener; // Listener для режима редактирования, null для режима отображения

    /**
     * Константа, определяющая тип View для отображения шага (нередактируемый).
     */
    private static final int VIEW_TYPE_DISPLAY = 1;
    /**
     * Константа, определяющая тип View для редактирования шага.
     */
    private static final int VIEW_TYPE_EDIT = 2;


    public interface StepUpdateListener {

        void onStepUpdated(int position, Step step);


        void onStepRemoved(int position);
    }

    /**
     * Конструктор для использования адаптера в режиме РЕДАКТИРОВАНИЯ шагов.
     *
     * @param context Контекст приложения.
     * @param listener Реализация {@link StepUpdateListener} для обработки изменений и удалений шагов. Должен быть не null.
     */
    public StepAdapter(Context context, @NonNull StepUpdateListener listener) {
        super(DIFF_CALLBACK);
        this.context = context;
        this.listener = listener;
    }

    /**
     * Конструктор для использования адаптера в режиме ОТОБРАЖЕНИЯ шагов (только чтение).
     *
     * @param context Контекст приложения.
     */
    public StepAdapter(Context context) {
        super(DIFF_CALLBACK);
        this.context = context;
        this.listener = null; // Явно указываем, что listener отсутствует, активируя режим отображения
    }

    /**
     * Определяет тип View для элемента в указанной позиции.
     * Тип зависит от того, был ли предоставлен {@link StepUpdateListener} в конструкторе.
     *
     * @param position Позиция элемента в списке.
     * @return {@link #VIEW_TYPE_DISPLAY} если listener отсутствует (режим отображения),
     *         иначе {@link #VIEW_TYPE_EDIT} (режим редактирования).
     */
    @Override
    public int getItemViewType(int position) {
        // Возвращаем тип в зависимости от наличия listener'а
        return listener == null ? VIEW_TYPE_DISPLAY : VIEW_TYPE_EDIT;
    }

    @Override
    public long getItemId(int position) {
        // Возвращаем стабильный ID для элемента
        Step item = getItem(position);
        return item != null ? System.identityHashCode(item) : RecyclerView.NO_ID;
    }

    /**
     * Создает новый ViewHolder в зависимости от типа View.
     */
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_EDIT) {
            View view = inflater.inflate(R.layout.item_step_edit, parent, false);
            // Listener не может быть null, если viewType == VIEW_TYPE_EDIT, согласно getItemViewType
            assert listener != null; 
            return new StepEditViewHolder(view, listener);
        } else { // VIEW_TYPE_DISPLAY
            View view = inflater.inflate(R.layout.item_step, parent, false);
            return new StepDisplayViewHolder(view);
        }
    }

    /**
     * Привязывает данные из объекта  к ViewHolder'у.
     */
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Step step = getItem(position);
        if (holder.getItemViewType() == VIEW_TYPE_EDIT) {
            ((StepEditViewHolder) holder).bind(step, position);
        } else if (holder.getItemViewType() == VIEW_TYPE_DISPLAY) {
            // Для отображения номер шага начинается с 1
            ((StepDisplayViewHolder) holder).bind(step, position + 1);
        }
        
        // Принудительно запрашиваем перерисовку элемента
        holder.itemView.invalidate();
    }

    /**
     * ViewHolder для ОТОБРАЖЕНИЯ шага рецепта в режиме только для чтения.
     */
    static class StepDisplayViewHolder extends RecyclerView.ViewHolder {
        private final TextView stepNumberTextView;
        private final TextView stepInstructionTextView;
        private final ShapeableImageView stepImageView;
        private final LinearLayout buttonContainer;

        /**
         * Конструктор ViewHolder'а для отображения шага.
         */
        public StepDisplayViewHolder(@NonNull View itemView) {
            super(itemView);
            stepNumberTextView = itemView.findViewById(R.id.text_step_number);
            stepInstructionTextView = itemView.findViewById(R.id.edit_step_instruction); // ID может быть обманчивым, используется и для отображения
            stepImageView = itemView.findViewById(R.id.step_image);
            buttonContainer = itemView.findViewById(R.id.button_container); // Может быть null, если макет изменится
        }

        /**
         * Привязывает данные шага к элементам View и форматирует их для отображения.
         * Номер шага отображается начиная с 1.
         */
        public void bind(Step step, int displayNumber) {
            if (step == null) {
                Log.e("StepDisplayViewHolder", "Step object is null for displayNumber: " + displayNumber);
                stepNumberTextView.setText(String.format(itemView.getContext().getString(R.string.step_label_format), displayNumber));
                stepInstructionTextView.setText(R.string.step_description_error);
                stepImageView.setVisibility(View.GONE);
                return;
            }

            stepNumberTextView.setText(String.format(itemView.getContext().getString(R.string.step_label_format), displayNumber));
            stepInstructionTextView.setText(step.getInstruction() != null ? step.getInstruction() : itemView.getContext().getString(R.string.step_no_description));

            String imageUrl = step.getUrl();
            if (imageUrl != null && !imageUrl.isEmpty()) {
                stepImageView.setVisibility(View.VISIBLE);
                Glide.with(itemView.getContext())
                     .load(imageUrl)
                     .placeholder(R.drawable.placeholder_image) // Заглушка во время загрузки
                     .error(R.drawable.error_image)           // Изображение при ошибке загрузки
                     .into(stepImageView);
            } else {
                stepImageView.setVisibility(View.GONE);
            }
            
            // Контейнер с кнопками (например, для удаления/редактирования) всегда скрыт в режиме отображения
            if (buttonContainer != null) { 
                buttonContainer.setVisibility(View.GONE);
            }
        }
    }

    /**
     * ViewHolder для РЕДАКТИРОВАНИЯ шага рецепта.
     * Позволяет изменять инструкцию шага и удалять шаг.
     */
    class StepEditViewHolder extends RecyclerView.ViewHolder {
        /** TextView для отображения номера шага. */
        private final TextView stepNumberTextView;
        /** EditText для редактирования инструкции шага. */
        private final EditText stepInstructionEditText;
        /** Кнопка для удаления шага. */
        private final ImageButton removeButton;
        /** ImageView для отображения изображения шага (если есть, нередактируемое в этом ViewHolder). */
        private final ShapeableImageView stepImageView;
        /** TextInputLayout для отображения ошибок валидации. */
        private final TextInputLayout stepDescriptionLayout;
        /** Listener для уведомления об изменениях. */
        private final StepUpdateListener updateListener; // Переименовано для ясности, что это поле класса
        /** Флаг для предотвращения срабатывания TextWatcher при инициализации View. */
        private boolean isInitializingText = true;
        
        // Handler для debounce
        private final Handler debounceHandler = new Handler(Looper.getMainLooper());
        private Runnable pendingTextUpdate;
        private static final int DEBOUNCE_DELAY_MS = 300; // 300ms задержка

        /**
         * Конструктор ViewHolder'а для редактирования шага.
         * @param itemView View элемента списка.
         * @param listener Listener для обработки обновлений и удалений шагов. Гарантированно не null.
         */
        public StepEditViewHolder(@NonNull View itemView, @NonNull StepUpdateListener listener) {
            super(itemView);
            this.updateListener = listener; // Сохраняем listener
            stepNumberTextView = itemView.findViewById(R.id.text_step_number);
            stepInstructionEditText = itemView.findViewById(R.id.edit_step_instruction);
            removeButton = itemView.findViewById(R.id.button_remove_step);
            stepImageView = itemView.findViewById(R.id.step_image);
            stepDescriptionLayout = itemView.findViewById(R.id.stepDescriptionInputLayout);

            setupTextWatchers();
            setupRemoveButton();
            setupFocusValidation();
        }

        /**
         * Привязывает данные шага к элементам View и настраивает их для редактирования.
         * Номер шага отображается начиная с 1.
    
         */
        void bind(Step step, int position) {
            isInitializingText = true; // Устанавливаем флаг перед обновлением EditText

            // Очищаем pending callbacks для предотвращения memory leaks
            if (pendingTextUpdate != null) {
                debounceHandler.removeCallbacks(pendingTextUpdate);
                pendingTextUpdate = null;
            }

            // Очищаем ошибки валидации при связывании новых данных
            stepDescriptionLayout.setError(null);

            stepNumberTextView.setText(String.format(itemView.getContext().getString(R.string.step_label_format), position + 1));
            stepInstructionEditText.setText(step != null ? step.getInstruction() : "");

            // Кнопка удаления видима для всех шагов, кроме первого.
            removeButton.setVisibility(position == 0 ? View.GONE : View.VISIBLE);

            String imageUrl = (step != null) ? step.getUrl() : null;
            if (imageUrl != null && !imageUrl.isEmpty()) {
                stepImageView.setVisibility(View.VISIBLE);
                Glide.with(itemView.getContext())
                     .load(imageUrl)
                     .placeholder(R.drawable.placeholder_image)
                     .error(R.drawable.error_image)
                     .into(stepImageView);
            } else {
                stepImageView.setVisibility(View.GONE);
            }

            isInitializingText = false; // Сбрасываем флаг после обновления EditText
        }

        /**
         * Настраивает TextWatcher для для отслеживания изменений
         * в инструкции шага и уведомления.
         */
        private void setupTextWatchers() {
            stepInstructionEditText.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { /* Не используется */ }
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) { /* Не используется */ }
                
                @Override public void afterTextChanged(Editable s) {
                    // Реагируем на изменения только если инициализация завершена и ViewHolder все еще действителен
                    if (!isInitializingText && getAdapterPosition() != RecyclerView.NO_POSITION) {
                        // Отменяем предыдущее обновление если оно ещё не выполнилось
                        if (pendingTextUpdate != null) {
                            debounceHandler.removeCallbacks(pendingTextUpdate);
                        }
                        
                        // Создаём новое отложенное обновление
                        pendingTextUpdate = () -> {
                            Step currentStep = getItem(getAdapterPosition()); 
                            if (currentStep != null) {
                                currentStep.setInstruction(s.toString());
                                // Очищаем ошибку при успешном вводе (минимум 7 символов)
                                if (s.toString().trim().length() >= 7) {
                                    stepDescriptionLayout.setError(null);
                                }
                                updateListener.onStepUpdated(getAdapterPosition(), currentStep);
                            }
                        };
                        
                        // Запускаем с задержкой
                        debounceHandler.postDelayed(pendingTextUpdate, DEBOUNCE_DELAY_MS);
                    }
                }
            });
        }

        /**
         * Настраивает OnClickListener для {@link #removeButton} для удаления шага
         * и уведомления {@link #updateListener}.
         */
        private void setupRemoveButton() {
            removeButton.setOnClickListener(v -> {
                // Уведомляем listener только если ViewHolder все еще действителен
                if (getAdapterPosition() != RecyclerView.NO_POSITION) {
                    updateListener.onStepRemoved(getAdapterPosition());
                } else {
                    Log.w("StepEditViewHolder", "Remove button clicked but adapter position is NO_POSITION");
                }
            });
        }

        private void setupFocusValidation() {
            // Валидация описания шага при потере фокуса
            stepInstructionEditText.setOnFocusChangeListener((v, hasFocus) -> {
                if (!hasFocus) {
                    String instruction = stepInstructionEditText.getText().toString().trim();
                    if (instruction.isEmpty()) {
                        stepDescriptionLayout.setError("Опишите действие для этого шага");
                    } else if (instruction.length() < 7) {
                        stepDescriptionLayout.setError("Описание должно содержать минимум 7 символов");
                    } else {
                        stepDescriptionLayout.setError(null);
                    }
                }
            });
        }
    }

    /**
     * Коллбэк для {@link DiffUtil}, который определяет, как сравнивать элементы списка {@link Step}
     * для эффективного обновления {@link RecyclerView}.
     */
    private static final DiffUtil.ItemCallback<Step> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<Step>() {
                @Override
                public boolean areItemsTheSame(@NonNull Step oldItem, @NonNull Step newItem) {
                    // Используем системный хеш-код для сравнения объектов
                    return System.identityHashCode(oldItem) == System.identityHashCode(newItem);
                }

                @Override
                public boolean areContentsTheSame(@NonNull Step oldItem, @NonNull Step newItem) {
                    // Сравниваем только необходимые поля
                    return oldItem.getNumber() == newItem.getNumber() &&
                           Objects.equals(oldItem.getInstruction(), newItem.getInstruction()) &&
                           Objects.equals(oldItem.getUrl(), newItem.getUrl());
                }
                
                @Override
                public Object getChangePayload(@NonNull Step oldItem, @NonNull Step newItem) {
                    // Возвращаем полезную нагрузку, если нужно обработать частичное обновление
                    return super.getChangePayload(oldItem, newItem);
                }
            };
}