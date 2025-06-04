package com.example.cooking.ui.viewmodels;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.cooking.model.Message;
import java.util.ArrayList;
import java.util.List;

public class AiChatViewModel extends AndroidViewModel {
    private final MutableLiveData<List<Message>> messages = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    public AiChatViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<List<Message>> getMessages() {
        return messages;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public void sendMessage(String text) {
        if (text == null || text.isEmpty()) return;
        // Добавляем сообщение пользователя
        List<Message> current = new ArrayList<>(messages.getValue());
        current.add(new Message(text, true));
        messages.setValue(current);
        // Запускаем фоновый поток для имитации AI-ответа
        isLoading.setValue(true);
        new Thread(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            String aiResponse = "Ответ AI: " + text;
            List<Message> updated = new ArrayList<>(messages.getValue());
            updated.add(new Message(aiResponse, false));
            // Обновляем LiveData на главном потоке
            new Handler(Looper.getMainLooper()).post(() -> {
                messages.setValue(updated);
                isLoading.setValue(false);
            });
        }).start();
    }

    public void onPhotoButtonClicked() {
        // TODO: обработать выбор фото
    }
}
