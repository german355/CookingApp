package com.example.cooking.ui.viewmodels;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.cooking.R;
import com.example.cooking.data.repositories.ChatRepository;
import com.example.cooking.model.Message;
import com.example.cooking.network.models.chat.ChatMessage;
import com.example.cooking.network.models.chat.ChatMessageResponse;
import com.example.cooking.network.models.chat.ChatHistoryResponse;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

public class AiChatViewModel extends AndroidViewModel {
    private final MutableLiveData<List<Message>> messages = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> showMessage = new MutableLiveData<>();
    private final ChatRepository chatRepository;

    public AiChatViewModel(@NonNull Application application) {
        super(application);
        chatRepository = new ChatRepository(application);
        loadHistory();
    }

    private void loadHistory() {
        isLoading.setValue(true);
        chatRepository.getChatHistory().observeForever(response -> {
            isLoading.setValue(false);
            if (response != null && response.getMessages() != null) {
                List<Message> domainMessages = new ArrayList<>();
                for (ChatMessage chatMsg : response.getMessages()) {
                    domainMessages.add(new Message(chatMsg.getMessage(), chatMsg.isUser()));
                }
                messages.setValue(domainMessages);
            }
        });
    }

    public LiveData<List<Message>> getMessages() {
        return messages;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public void sendMessage(String text) {
        if (text == null || text.isEmpty()) return;
        
        // Проверяем аутентификацию пользователя
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            // Добавляем сообщение от "собеседника"
            List<Message> temp = new ArrayList<>(messages.getValue());
            temp.add(new Message(getApplication().getString(R.string.error_need_auth), false));
            messages.setValue(temp);
            return;
        }
        
        // Добавляем сообщение пользователя
        List<Message> currentMessages = messages.getValue() != null ? new ArrayList<>(messages.getValue()) : new ArrayList<>();
        currentMessages.add(new Message(text, true));
        
        // Добавляем индикатор загрузки
        currentMessages.add(new Message(Message.MessageType.LOADING));
        messages.setValue(currentMessages);
        
        // Отправка сообщения в AI-чат
        // isLoading.setValue(true); // Теперь управляется через элемент списка
        chatRepository.sendChatMessage(text).observeForever(response -> {
            // Удаляем индикатор загрузки
            List<Message> updatedMessages = messages.getValue() != null ? new ArrayList<>(messages.getValue()) : new ArrayList<>();
            boolean removedLoading = false;
            for (int i = updatedMessages.size() - 1; i >= 0; i--) {
                if (updatedMessages.get(i).getType() == Message.MessageType.LOADING) {
                    updatedMessages.remove(i);
                    removedLoading = true;
                    break;
                }
            }

            // isLoading.setValue(false);
            if (response != null && response.isSuccess()) {
                String aiText = response.getAiResponse();
                List<Message> listToUpdate = removedLoading ? updatedMessages : new ArrayList<>(messages.getValue());
                // Удален индикатор, теперь добавляем AI-сообщение
                listToUpdate.add(new Message(aiText, false));
                messages.setValue(listToUpdate);
            } else {
                // Если индикатор был удален, устанавливаем обновленный список без ответа AI
                // Иначе оставляем как есть, т.к. индикатора не было
                if(removedLoading) {
                    messages.setValue(updatedMessages);
                }
                showMessage.setValue(getApplication().getString(R.string.error_sending_message));
            }
        });
    }

    public void onPhotoButtonClicked() {
        // TODO: обработать выбор фото
    }
    
    public LiveData<String> getShowMessage() {
        return showMessage;
    }

    public void clearChat() {
        // Проверяем аутентификацию пользователя
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            // Добавляем сообщение от "собеседника"
            List<Message> temp = new ArrayList<>(messages.getValue());
            temp.add(new Message(getApplication().getString(R.string.error_need_auth), false));
            messages.setValue(temp);
            return;
        }
        
        isLoading.setValue(true);
        chatRepository.startChatSession().observeForever(response -> {
            isLoading.setValue(false);
            if (response != null && response.isSuccess()) {
                // Очищаем список сообщений
                messages.setValue(new ArrayList<>());
                showMessage.setValue(getApplication().getString(R.string.chat_cleared));
            } else {
                showMessage.setValue(getApplication().getString(R.string.error_clearing_chat));
            }
        });
    }
}
