package com.example.cooking.ui.viewmodels;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.cooking.R;
import com.example.cooking.data.repositories.ChatRepository;
import com.example.cooking.data.repositories.RecipeLocalRepository;
import com.example.cooking.domain.entities.Message;
import com.example.cooking.network.models.chat.ChatMessage;
import com.example.cooking.network.models.chat.ChatMessageResponse;
import com.example.cooking.network.models.chat.ChatHistoryResponse;
import com.example.cooking.network.models.chat.ChatSessionResponse;

import com.example.cooking.domain.entities.Recipe;
import com.example.cooking.utils.AppExecutors;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class AiChatViewModel extends AndroidViewModel {
    private final MutableLiveData<List<Message>> messages = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> canSend = new MutableLiveData<>(true);
    private final MutableLiveData<String> showMessage = new MutableLiveData<>();
    private final ChatRepository chatRepository;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private Integer contextualRecipeId;
    private Runnable cooldownRunnable;
    private boolean recipeFlowActive = false;
    private String inFlightRequestId;
    private int pendingHistoryRefreshAttempts = 0;
    private final List<Runnable> pendingHistoryRefreshRunnables = new ArrayList<>();

    private androidx.lifecycle.Observer<ChatHistoryResponse> historyObserver;
    private androidx.lifecycle.Observer<ChatMessageResponse> messageObserver;
    private androidx.lifecycle.Observer<ChatSessionResponse> clearChatObserver;

    private LiveData<ChatHistoryResponse> historyLiveData;
    private LiveData<ChatMessageResponse> messageLiveData;
    private LiveData<ChatSessionResponse> clearChatLiveData;

    public AiChatViewModel(@NonNull Application application) {
        super(application);
        chatRepository = new ChatRepository(application);
        loadHistory(false);
    }

    public void refreshHistory() {
        loadHistory(true);
    }

    private void loadHistory(boolean mergeWithCurrent) {
        isLoading.setValue(true);
        if (historyLiveData != null && historyObserver != null) {
            historyLiveData.removeObserver(historyObserver);
        }
        historyObserver = response -> {
            isLoading.setValue(false);
            if (response != null && response.getMessageCount() == 0) {
                if (shouldReplaceWithHistory(Collections.emptyList(), mergeWithCurrent)) {
                    List<Message> welcome = new ArrayList<>();
                    welcome.add(new Message(getApplication().getString(R.string.chat_welcome), false));
                    messages.setValue(welcome);
                }
            } else if (response != null && response.getMessages() != null) {
                AppExecutors.getInstance().diskIO().execute(() -> {
                    List<Message> fullList = buildMessagesFromHistory(response.getMessages());
                    mainHandler.post(() -> {
                        if (shouldReplaceWithHistory(fullList, mergeWithCurrent)) {
                            messages.setValue(fullList);
                        }
                    });
                });
            }
        };
        historyLiveData = chatRepository.getChatHistory();
        historyLiveData.observeForever(historyObserver);
    }

    public LiveData<List<Message>> getMessages() {
        return messages;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<Boolean> getCanSend() {
        return canSend;
    }

    public LiveData<String> getShowMessage() {
        return showMessage;
    }

    public void setRecipeContext(Integer recipeId) {
        this.contextualRecipeId = recipeId;
    }

    public boolean sendMessage(String text) {
        if (text == null || text.isEmpty()) return false;
        if (inFlightRequestId != null) {
            return false;
        }
        if (!Boolean.TRUE.equals(canSend.getValue())) {
            showMessage.setValue(getApplication().getString(R.string.chat_rate_limit_wait));
            return false;
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            List<Message> temp = new ArrayList<>(messages.getValue());
            temp.add(new Message(getApplication().getString(R.string.error_need_auth), false));
            messages.setValue(temp);
            return false;
        }

        pendingHistoryRefreshAttempts = 0;
        clearPendingHistoryRefreshes();
        inFlightRequestId = UUID.randomUUID().toString();
        canSend.setValue(false);
        List<Message> currentMessages = messages.getValue() != null ? new ArrayList<>(messages.getValue()) : new ArrayList<>();
        currentMessages.add(new Message(text, true));

        if (shouldUseRecipeFlowLoading(text)) {
            currentMessages.add(
                    Message.recipeFlowLoading(
                            getApplication().getString(R.string.chat_recipe_flow_loading_title),
                            getApplication().getString(R.string.chat_recipe_flow_loading_subtitle)
                    )
            );
        } else {
            currentMessages.add(new Message(Message.MessageType.LOADING));
        }
        messages.setValue(currentMessages);
        isLoading.setValue(true);

        if (messageLiveData != null && messageObserver != null) {
            messageLiveData.removeObserver(messageObserver);
        }
        messageObserver = response -> {
            isLoading.setValue(false);
            List<Message> updatedMessages = new ArrayList<>(messages.getValue());
            boolean removedLoading = false;
            for (int i = updatedMessages.size() - 1; i >= 0; i--) {
                Message.MessageType type = updatedMessages.get(i).getType();
                if (type == Message.MessageType.LOADING || type == Message.MessageType.RECIPE_FLOW_LOADING) {
                    updatedMessages.remove(i);
                    removedLoading = true;
                    break;
                }
            }

            if (response != null && response.isSuccess()) {
                String aiText = response.getAiResponse();
                List<Message> listWithoutRecipes = removedLoading ? updatedMessages : new ArrayList<>(messages.getValue());
                if (aiText != null && !aiText.isEmpty()) {
                    listWithoutRecipes.add(new Message(aiText, false));
                }
                messages.setValue(listWithoutRecipes);

                String recipeFlowState = response.getRecipeFlowState();
                recipeFlowActive = "collecting".equalsIgnoreCase(recipeFlowState);

                if (response.getCreatedRecipeId() != null) {
                    appendRecipePayloadToMessages(
                            response.getCreatedRecipeId(),
                            response.getRecipesIds(),
                            response.getRecipePresentation(),
                            aiText,
                            listWithoutRecipes
                    );
                } else if (response.hasRecipePayload()) {
                    appendRecipePayloadToMessages(
                            null,
                            response.getRecipesIds(),
                            response.getRecipePresentation(),
                            aiText,
                            listWithoutRecipes
                    );
                } else if (!"collecting".equalsIgnoreCase(recipeFlowState)) {
                    recipeFlowActive = false;
                }
                inFlightRequestId = null;
                canSend.setValue(true);
            } else {
                if(removedLoading) {
                    messages.setValue(updatedMessages);
                }
                inFlightRequestId = null;
                Integer retryAfterSeconds = response != null ? response.getRetryAfterSeconds() : null;
                if (retryAfterSeconds != null && retryAfterSeconds > 0) {
                    applyCooldown(retryAfterSeconds, response.getMessage());
                } else if (response != null && response.getMessage() != null && !response.getMessage().isEmpty()) {
                    canSend.setValue(true);
                    showMessage.setValue(response.getMessage());
                } else {
                    canSend.setValue(true);
                    showMessage.setValue(getApplication().getString(R.string.error_sending_message));
                }
            }
        };
        messageLiveData = chatRepository.sendChatMessage(text, contextualRecipeId, inFlightRequestId);
        messageLiveData.observeForever(messageObserver);
        return true;
    }

    private boolean shouldUseRecipeFlowLoading(String messageText) {
        if (recipeFlowActive) {
            return true;
        }
        if (messageText == null) {
            return false;
        }
        String normalized = messageText.toLowerCase();
        return normalized.contains("рецепт")
                && (normalized.contains("созд")
                || normalized.contains("сгенер")
                || normalized.contains("придум")
                || normalized.contains("хочу")
                || normalized.contains("сделай")
                || normalized.contains("приготов"));
    }

    private List<Message> buildMessagesFromHistory(List<ChatMessage> historyMessages) {
        List<Message> fullList = new ArrayList<>();
        for (ChatMessage chatMsg : historyMessages) {
            String text = chatMsg.getMessage();
            if (text != null && !text.isEmpty()) {
                fullList.add(new Message(text, chatMsg.isUser()));
            }
            if (!chatMsg.isUser()) {
                List<Integer> recipeIds = chatMsg.getRecipeIds();
                Integer createdRecipeId = chatMsg.getCreatedRecipeId();
                if (createdRecipeId != null || (recipeIds != null && !recipeIds.isEmpty())) {
                    appendResolvedRecipeMessages(
                            fullList,
                            createdRecipeId,
                            recipeIds,
                            chatMsg.getRecipePresentation(),
                            text
                    );
                }
            }
        }
        return fullList;
    }

    private void appendRecipePayloadToMessages(
            Integer createdRecipeId,
            List<Integer> recipeIds,
            String recipePresentation,
            String assistantText,
            List<Message> baseMessages
    ) {
        AppExecutors.getInstance().diskIO().execute(() -> {
            List<Message> finalList = new ArrayList<>(baseMessages);
            boolean attached = appendResolvedRecipeMessages(
                    finalList,
                    createdRecipeId,
                    recipeIds,
                    recipePresentation,
                    assistantText
            );
            mainHandler.post(() -> {
                if (attached && shouldReplaceWithHistory(finalList, true)) {
                    messages.setValue(finalList);
                } else {
                    scheduleHistoryRefresh();
                }
            });
        });
    }

    private boolean appendResolvedRecipeMessages(
            List<Message> target,
            Integer createdRecipeId,
            List<Integer> recipeIds,
            String recipePresentation,
            String messageText
    ) {
        List<Integer> idsToResolve = new ArrayList<>();
        if (createdRecipeId != null) {
            idsToResolve.add(createdRecipeId);
        }
        if (recipeIds != null) {
            for (Integer id : recipeIds) {
                if (id != null && !idsToResolve.contains(id)) {
                    idsToResolve.add(id);
                }
            }
        }
        if (idsToResolve.isEmpty()) {
            return false;
        }

        List<Recipe> recipes = resolveRecipes(idsToResolve);
        if (recipes.isEmpty()) {
            return false;
        }

        if (shouldRenderCreatedRecipe(createdRecipeId, recipePresentation, messageText, recipes)) {
            target.add(new Message(recipes.get(0), getApplication().getString(R.string.chat_created_recipe_subtitle)));
        } else {
            target.add(new Message(recipes));
        }
        return true;
    }

    private boolean shouldRenderCreatedRecipe(
            Integer createdRecipeId,
            String recipePresentation,
            String messageText,
            List<Recipe> recipes
    ) {
        if (recipes == null || recipes.size() != 1) {
            return false;
        }
        if (createdRecipeId != null) {
            return true;
        }
        if ("hero".equalsIgnoreCase(recipePresentation)) {
            return true;
        }
        if (messageText == null) {
            return false;
        }
        String normalized = messageText.toLowerCase();
        return normalized.contains("создал рецепт") || normalized.contains("готово! я создал");
    }

    private List<Recipe> resolveRecipes(List<Integer> recipeIds) {
        RecipeLocalRepository localRepo = new RecipeLocalRepository(getApplication());
        List<Integer> missingIds = new ArrayList<>();

        for (Integer id : recipeIds) {
            if (id == null) continue;
            Recipe recipe = localRepo.getRecipeByIdSync(id);
            if (recipe == null) {
                missingIds.add(id);
            }
        }

        if (!missingIds.isEmpty()) {
            List<Recipe> fetchedRecipes = chatRepository.fetchRecipesByIdsSync(missingIds);
            for (Recipe recipe : fetchedRecipes) {
                if (localRepo.getRecipeByIdSync(recipe.getId()) != null) {
                    localRepo.updateSync(recipe);
                } else {
                    localRepo.insertSync(recipe);
                }
            }
        }

        List<Recipe> orderedRecipes = new ArrayList<>();
        for (Integer id : recipeIds) {
            if (id == null) continue;
            Recipe recipe = localRepo.getRecipeByIdSync(id);
            if (recipe != null) {
                orderedRecipes.add(recipe);
            }
        }
        return orderedRecipes;
    }

    private void applyCooldown(int retryAfterSeconds, String serverMessage) {
        canSend.setValue(false);
        String message = (serverMessage != null && !serverMessage.isEmpty())
                ? serverMessage
                : getApplication().getString(R.string.chat_rate_limit_message, retryAfterSeconds);
        showMessage.setValue(message);

        if (cooldownRunnable != null) {
            mainHandler.removeCallbacks(cooldownRunnable);
        }
        cooldownRunnable = () -> canSend.postValue(true);
        mainHandler.postDelayed(cooldownRunnable, retryAfterSeconds * 1000L);
    }

    private void scheduleHistoryRefresh() {
        if (pendingHistoryRefreshAttempts >= 2) {
            return;
        }
        pendingHistoryRefreshAttempts++;
        final Runnable[] refreshHolder = new Runnable[1];
        refreshHolder[0] = () -> {
            pendingHistoryRefreshRunnables.remove(refreshHolder[0]);
            refreshHistory();
        };
        pendingHistoryRefreshRunnables.add(refreshHolder[0]);
        mainHandler.postDelayed(refreshHolder[0], pendingHistoryRefreshAttempts == 1 ? 750L : 1500L);
    }

    private void clearPendingHistoryRefreshes() {
        for (Runnable refreshRunnable : pendingHistoryRefreshRunnables) {
            mainHandler.removeCallbacks(refreshRunnable);
        }
        pendingHistoryRefreshRunnables.clear();
    }

    private boolean shouldReplaceWithHistory(List<Message> historyMessages, boolean mergeWithCurrent) {
        if (!mergeWithCurrent) {
            return true;
        }
        List<Message> currentMessages = messages.getValue();
        if (currentMessages == null || currentMessages.isEmpty()) {
            return true;
        }
        if (inFlightRequestId != null || containsLoading(currentMessages)) {
            return false;
        }
        return historyMessages.size() >= currentMessages.size();
    }

    private boolean containsLoading(List<Message> currentMessages) {
        for (Message message : currentMessages) {
            Message.MessageType type = message.getType();
            if (type == Message.MessageType.LOADING || type == Message.MessageType.RECIPE_FLOW_LOADING) {
                return true;
            }
        }
        return false;
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
        clearChatObserver = response -> {
            isLoading.setValue(false);
            if (response != null && response.isSuccess()) {
                List<Message> welcomeList = new ArrayList<>();
                welcomeList.add(new Message(getApplication().getString(R.string.chat_welcome), false));
                messages.setValue(welcomeList);
                recipeFlowActive = false;
                inFlightRequestId = null;
                canSend.setValue(true);
                showMessage.setValue(getApplication().getString(R.string.chat_cleared));
            } else {
                canSend.setValue(true);
                showMessage.setValue(getApplication().getString(R.string.error_clearing_chat));
            }
        };
        clearChatLiveData = chatRepository.startChatSession();
        clearChatLiveData.observeForever(clearChatObserver);
    }


    @Override
    protected void onCleared() {
        super.onCleared();
        if (historyLiveData != null && historyObserver != null) {
            historyLiveData.removeObserver(historyObserver);
        }
        if (messageLiveData != null && messageObserver != null) {
            messageLiveData.removeObserver(messageObserver);
        }
        if (clearChatLiveData != null && clearChatObserver != null) {
            clearChatLiveData.removeObserver(clearChatObserver);
        }
        clearPendingHistoryRefreshes();
        if (cooldownRunnable != null) {
            mainHandler.removeCallbacks(cooldownRunnable);
        }
    }
}
