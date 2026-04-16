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
import java.util.Locale;
import java.util.UUID;

public class AiChatViewModel extends AndroidViewModel {
    private static final String[] RECIPE_REFERENCE_KEYWORDS = {
            "recipe",
            "\u0440\u0435\u0446\u0435\u043f\u0442"
    };
    private static final String[] RECIPE_CREATION_KEYWORDS = {
            "\u0441\u043e\u0437\u0434",
            "\u0441\u0433\u0435\u043d\u0435\u0440",
            "\u043f\u0440\u0438\u0434\u0443\u043c",
            "\u0445\u043e\u0447\u0443",
            "\u0441\u0434\u0435\u043b\u0430\u0439",
            "\u043f\u0440\u0438\u0433\u043e\u0442\u043e\u0432",
            "creat",
            "generat",
            "want",
            "make",
            "cook"
    };
    private static final String[] CREATED_RECIPE_PHRASES = {
            "\u0441\u043e\u0437\u0434\u0430\u043b \u0440\u0435\u0446\u0435\u043f\u0442",
            "\u0433\u043e\u0442\u043e\u0432\u043e",
            "\u044f \u0441\u043e\u0437\u0434\u0430\u043b",
            "created a recipe",
            "here's your recipe"
    };

    private final MutableLiveData<List<Message>> messages = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> canSend = new MutableLiveData<>(true);
    private final SingleLiveEvent<String> showMessage = new SingleLiveEvent<>();
    private final ChatRepository chatRepository;
    private final RecipeLocalRepository recipeLocalRepository;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private Integer contextualRecipeId;
    private Runnable cooldownRunnable;
    private boolean recipeFlowActive = false;
    private String inFlightRequestId;
    private String activeHistoryLoadId;
    private long lastHistoryLoadTime = 0;
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
        recipeLocalRepository = new RecipeLocalRepository(application);
        messages.setValue(createWelcomeMessages());
        loadHistory(false);
    }

    private List<Message> getCurrentMessages() {
        List<Message> current = messages.getValue();
        return current != null ? new ArrayList<>(current) : new ArrayList<>();
    }

    public void refreshHistory() {
        long now = System.currentTimeMillis();
        if (now - lastHistoryLoadTime < 5000) {
            return;
        }
        loadHistory(true);
    }

    private void loadHistory(boolean mergeWithCurrent) {
        isLoading.setValue(true);
        if (historyLiveData != null && historyObserver != null) {
            historyLiveData.removeObserver(historyObserver);
        }
        final String loadId = UUID.randomUUID().toString();
        activeHistoryLoadId = loadId;
        historyObserver = response -> {
            if (!loadId.equals(activeHistoryLoadId)) return;
            lastHistoryLoadTime = System.currentTimeMillis();
            isLoading.setValue(false);
            if (response != null && response.getMessageCount() == 0) {
                if (shouldReplaceWithHistory(Collections.emptyList(), mergeWithCurrent)) {
                    messages.setValue(createWelcomeMessages());
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

    public SingleLiveEvent<String> getShowMessage() {
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
            List<Message> temp = getCurrentMessages();
            temp.add(new Message(getApplication().getString(R.string.error_need_auth), false));
            messages.setValue(temp);
            return false;
        }

        pendingHistoryRefreshAttempts = 0;
        clearPendingHistoryRefreshes();
        activeHistoryLoadId = null;
        final String requestId = UUID.randomUUID().toString();
        inFlightRequestId = requestId;
        canSend.setValue(false);
        List<Message> currentMessages = getCurrentMessages();
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

        clearActiveMessageRequest();
        inFlightRequestId = requestId;
        messageObserver = response -> {
            if (!requestId.equals(inFlightRequestId)) {
                return;
            }
            if (messageLiveData != null && messageObserver != null) {
                messageLiveData.removeObserver(messageObserver);
                messageLiveData = null;
                messageObserver = null;
            }
            isLoading.setValue(false);
            List<Message> updatedMessages = getCurrentMessages();
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
                List<Message> listWithoutRecipes = removedLoading ? updatedMessages : getCurrentMessages();
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
        messageLiveData = chatRepository.sendChatMessage(text, contextualRecipeId, requestId);
        messageLiveData.observeForever(messageObserver);
        return true;
    }

    private boolean shouldUseRecipeFlowLoading(String messageText) {
        if (recipeFlowActive) {
            return true;
        }
        String normalized = normalizeForMatching(messageText);
        return containsAny(normalized, RECIPE_REFERENCE_KEYWORDS)
                && containsAny(normalized, RECIPE_CREATION_KEYWORDS);
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
        return containsAny(normalizeForMatching(messageText), CREATED_RECIPE_PHRASES);
    }

    private List<Recipe> resolveRecipes(List<Integer> recipeIds) {
        List<Integer> missingIds = new ArrayList<>();

        for (Integer id : recipeIds) {
            if (id == null) continue;
            Recipe recipe = recipeLocalRepository.getRecipeByIdSync(id);
            if (recipe == null) {
                missingIds.add(id);
            }
        }

        if (!missingIds.isEmpty()) {
            List<Recipe> fetchedRecipes = chatRepository.fetchRecipesByIdsSync(missingIds);
            for (Recipe recipe : fetchedRecipes) {
                if (recipeLocalRepository.getRecipeByIdSync(recipe.getId()) != null) {
                    recipeLocalRepository.updateSync(recipe);
                } else {
                    recipeLocalRepository.insertSync(recipe);
                }
            }
        }

        List<Recipe> orderedRecipes = new ArrayList<>();
        for (Integer id : recipeIds) {
            if (id == null) continue;
            Recipe recipe = recipeLocalRepository.getRecipeByIdSync(id);
            if (recipe != null) {
                orderedRecipes.add(recipe);
            }
        }
        return orderedRecipes;
    }

    private void applyCooldown(int retryAfterSeconds, String serverMessage) {
        int clampedSeconds = Math.min(retryAfterSeconds, 60);
        canSend.setValue(false);
        String message = (serverMessage != null && !serverMessage.isEmpty())
                ? serverMessage
                : getApplication().getString(R.string.chat_rate_limit_message, clampedSeconds);
        showMessage.setValue(message);

        if (cooldownRunnable != null) {
            mainHandler.removeCallbacks(cooldownRunnable);
        }
        cooldownRunnable = () -> canSend.postValue(true);
        mainHandler.postDelayed(cooldownRunnable, clampedSeconds * 1000L);
    }

    private void scheduleHistoryRefresh() {
        if (pendingHistoryRefreshAttempts >= 2) {
            return;
        }
        pendingHistoryRefreshAttempts++;
        final Runnable[] refreshHolder = new Runnable[1];
        refreshHolder[0] = () -> {
            pendingHistoryRefreshRunnables.remove(refreshHolder[0]);
            loadHistory(true);
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
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            List<Message> temp = getCurrentMessages();
            temp.add(new Message(getApplication().getString(R.string.error_need_auth), false));
            messages.setValue(temp);
            return;
        }

        isLoading.setValue(true);
        canSend.setValue(false);
        clearPendingHistoryRefreshes();
        pendingHistoryRefreshAttempts = 0;
        activeHistoryLoadId = null;
        clearActiveMessageRequest();
        clearActiveClearChatObserver();
        clearChatObserver = response -> {
            if (clearChatLiveData != null && clearChatObserver != null) {
                clearChatLiveData.removeObserver(clearChatObserver);
                clearChatLiveData = null;
                clearChatObserver = null;
            }
            isLoading.setValue(false);
            if (response != null && response.isSuccess()) {
                messages.setValue(createWelcomeMessages());
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

    private void clearActiveMessageRequest() {
        if (messageLiveData != null && messageObserver != null) {
            messageLiveData.removeObserver(messageObserver);
        }
        messageLiveData = null;
        messageObserver = null;
        inFlightRequestId = null;
    }

    private void clearActiveClearChatObserver() {
        if (clearChatLiveData != null && clearChatObserver != null) {
            clearChatLiveData.removeObserver(clearChatObserver);
        }
        clearChatLiveData = null;
        clearChatObserver = null;
    }

    private String normalizeForMatching(String text) {
        if (text == null) {
            return "";
        }
        return text.toLowerCase(Locale.ROOT);
    }

    private boolean containsAny(String text, String[] keywords) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }


    @Override
    protected void onCleared() {
        super.onCleared();
        if (historyLiveData != null && historyObserver != null) {
            historyLiveData.removeObserver(historyObserver);
        }
        clearActiveMessageRequest();
        clearActiveClearChatObserver();
        clearPendingHistoryRefreshes();
        if (cooldownRunnable != null) {
            mainHandler.removeCallbacks(cooldownRunnable);
        }
    }

    private List<Message> createWelcomeMessages() {
        List<Message> welcome = new ArrayList<>();
        welcome.add(new Message(getApplication().getString(R.string.chat_welcome_with_bugs), false));
        return welcome;
    }
}
