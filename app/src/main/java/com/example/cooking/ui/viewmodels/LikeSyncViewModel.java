package com.example.cooking.ui.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import android.util.Pair;

/**
 * Класс для хранения информации о событии лайка
 */
public class LikeSyncViewModel extends ViewModel {

    // LiveData для событий лайков
    private final MutableLiveData<LikeEvent> likeChangeEvent = new MutableLiveData<>();

    /**
     * Класс для хранения информации о событии лайка
     */
    public static class LikeEvent {
        private final int recipeId;
        private final boolean isLiked;
        private final long timestamp;

        public LikeEvent(int recipeId, boolean isLiked) {
            this.recipeId = recipeId;
            this.isLiked = isLiked;
            this.timestamp = System.currentTimeMillis();
        }

        public int getRecipeId() {
            return recipeId;
        }

        public boolean isLiked() {
            return isLiked;
        }

        public long getTimestamp() {
            return timestamp;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            LikeEvent likeEvent = (LikeEvent) o;
            return recipeId == likeEvent.recipeId && 
                   isLiked == likeEvent.isLiked;
        }
    }

    private LikeEvent lastProcessedLikeEvent;

    /**
     * Возвращает LiveData, за которым могут наблюдать другие компоненты,
     * чтобы узнать об изменении статуса лайка.
     */
    public LiveData<LikeEvent> getLikeChangeEvent() {
        return likeChangeEvent;
    }

    /**
     * Уведомляет об изменении статуса лайка
     * @param recipeId ID рецепта
     * @param isLiked новый статус лайка
     */
    public void notifyLikeChanged(int recipeId, boolean isLiked) {
        LikeEvent newEvent = new LikeEvent(recipeId, isLiked);

        // Проверяем, не является ли это дублирующимся событием
        if (lastProcessedLikeEvent != null &&
            lastProcessedLikeEvent.getRecipeId() == recipeId &&
            lastProcessedLikeEvent.isLiked() == isLiked) {
            return; // Пропускаем дублирующиеся события
        }

        // Сохраняем событие как последнее обработанное
        lastProcessedLikeEvent = newEvent;

        // Отправляем событие
        likeChangeEvent.postValue(newEvent);
    }
}