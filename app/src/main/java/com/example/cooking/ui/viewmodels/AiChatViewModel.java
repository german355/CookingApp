package com.example.cooking.ui.viewmodels;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.cooking.R;
import com.example.cooking.data.repositories.ChatRepository;
import com.example.cooking.data.repositories.RecipeLocalRepository;
import com.example.cooking.model.Message;
import com.example.cooking.network.models.chat.ChatMessage;
import com.example.cooking.network.models.chat.ChatMessageResponse;
import com.example.cooking.network.models.chat.ChatHistoryResponse;
import com.example.cooking.Recipe.Recipe;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
            if (response != null && response.getMessageCount() == 0) {
                // Если нет сообщений — показать приветственный экран без истории
                List<Message> welcome = new ArrayList<>();
                welcome.add(new Message(getApplication().getString(R.string.chat_welcome), false));
                messages.setValue(welcome);
            } else if (response != null && response.getMessages() != null) {
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
                List<Message> listWithoutRecipes = removedLoading ? updatedMessages : new ArrayList<>(messages.getValue());
                listWithoutRecipes.add(new Message(aiText, false));
                messages.setValue(listWithoutRecipes);
                
                if (response.getHasRecipes() && response.getRecipesIds() != null && !response.getRecipesIds().isEmpty()) {
                    ExecutorService executor = Executors.newSingleThreadExecutor();
                    executor.execute(() -> {
                        RecipeLocalRepository localRepo = new RecipeLocalRepository(getApplication());
                        List<Recipe> recipes = new ArrayList<>();
                        for (int id : response.getRecipesIds()) {
                            Recipe recipe = localRepo.getRecipeByIdSync(id);
                            if (recipe != null) recipes.add(recipe);
                        }
                        if (!recipes.isEmpty()) {
                            List<Message> finalList = new ArrayList<>(listWithoutRecipes);
                            finalList.add(new Message(recipes));
                            messages.postValue(finalList);
                        }
                    });
                }
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
    
    public void onPhotoButtonClicked() {
        // TODO: обработать выбор фото
    }
}
