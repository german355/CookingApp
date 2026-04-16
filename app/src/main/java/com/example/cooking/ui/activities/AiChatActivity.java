package com.example.cooking.ui.activities;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.EditorInfo;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.cooking.R;
import com.example.cooking.domain.entities.Message;
import com.example.cooking.ui.adapters.MessageAdapter;
import com.example.cooking.ui.viewmodels.AiChatViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import java.util.List;

public class AiChatActivity extends AppCompatActivity {
    public static final String EXTRA_CONTEXT_RECIPE_ID = "context_recipe_id";

    private AiChatViewModel viewModel;
    private RecyclerView recyclerViewMessages;
    private MessageAdapter messageAdapter;
    private TextInputEditText editTextMessage;
    private FloatingActionButton buttonSend;
    private LinearLayout loadingStatusContainer;
    private CircularProgressIndicator loadingStatusIndicator;
    private TextView loadingStatusText;
    private List<Message> currentMessages;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.chat_toolbar_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        
        if (id == R.id.action_clear_chat) {
            new AlertDialog.Builder(this)
                .setTitle(R.string.chat_clear_title)
                .setMessage(R.string.chat_clear_message)
                .setPositiveButton(android.R.string.yes, (dialog, which) -> viewModel.clearChat())
                .setNegativeButton(R.string.cancel, null)
                .show();
            return true;
        } else if (id == android.R.id.home) {
            finish();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_chat);

        Toolbar toolbar = findViewById(R.id.topAppBar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        viewModel = new ViewModelProvider(this).get(AiChatViewModel.class);
        if (getIntent() != null && getIntent().hasExtra(EXTRA_CONTEXT_RECIPE_ID)) {
            int recipeId = getIntent().getIntExtra(EXTRA_CONTEXT_RECIPE_ID, -1);
            if (recipeId > 0) {
                viewModel.setRecipeContext(recipeId);
            }
        }

        recyclerViewMessages = findViewById(R.id.recyclerViewMessages);
        messageAdapter = new MessageAdapter();
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerViewMessages.setLayoutManager(layoutManager);
        recyclerViewMessages.setAdapter(messageAdapter);

        editTextMessage = findViewById(R.id.editTextMessage);
        buttonSend = findViewById(R.id.buttonSend);
        loadingStatusContainer = findViewById(R.id.loadingStatusContainer);
        loadingStatusIndicator = findViewById(R.id.loadingStatusIndicator);
        loadingStatusText = findViewById(R.id.loadingStatusText);

        setupObservers();
        setupEventListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewModel.refreshHistory();
    }

    private void setupObservers() {
        viewModel.getMessages().observe(this, messageList -> {
            currentMessages = messageList;
            messageAdapter.submitList(messageList);
            if (messageList != null && !messageList.isEmpty()) {
                recyclerViewMessages.scrollToPosition(messageList.size() - 1);
            }
            updateLoadingState();
        });
        
        viewModel.getIsLoading().observe(this, loading -> {
            updateSendAvailability();
            updateLoadingState();
        });
        viewModel.getCanSend().observe(this, canSend -> updateSendAvailability());
        
        viewModel.getShowMessage().observe(this, message -> {
            if (message != null) {
                Snackbar.make(findViewById(android.R.id.content),
                    message, Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    private void setupEventListeners() {
        editTextMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                updateSendAvailability();
            }
        });

        editTextMessage.setOnEditorActionListener((v, actionId, event) -> {
            boolean imeSend = actionId == EditorInfo.IME_ACTION_SEND
                    || actionId == EditorInfo.IME_ACTION_DONE;
            boolean hardwareEnter = actionId == EditorInfo.IME_NULL
                    && event != null
                    && event.getAction() == KeyEvent.ACTION_DOWN
                    && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                    && !event.isShiftPressed();
            if (!imeSend && !hardwareEnter) {
                return false;
            }
            return trySendMessage();
        });

        buttonSend.setOnClickListener(v -> trySendMessage());
        updateSendAvailability();
    }

    private boolean trySendMessage() {
        String text = getTrimmedMessage();
        if (text.isEmpty()) {
            updateSendAvailability();
            return false;
        }
        if (!viewModel.sendMessage(text)) {
            updateSendAvailability();
            return false;
        }
        editTextMessage.setText("");
        updateSendAvailability();
        return true;
    }

    private String getTrimmedMessage() {
        Editable editable = editTextMessage.getText();
        return editable == null ? "" : editable.toString().trim();
    }

    private void updateSendAvailability() {
        boolean enabled = !getTrimmedMessage().isEmpty()
                && Boolean.TRUE.equals(viewModel.getCanSend().getValue());
        buttonSend.setEnabled(enabled);
        buttonSend.setAlpha(enabled ? 1f : 0.38f);
    }

    private void updateLoadingState() {
        boolean isLoading = Boolean.TRUE.equals(viewModel.getIsLoading().getValue());
        if (!isLoading) {
            loadingStatusContainer.setVisibility(LinearLayout.GONE);
            return;
        }

        boolean hasPendingAssistantReply = false;
        if (currentMessages != null) {
            for (Message message : currentMessages) {
                Message.MessageType type = message.getType();
                if (type == Message.MessageType.LOADING || type == Message.MessageType.RECIPE_FLOW_LOADING) {
                    hasPendingAssistantReply = true;
                    break;
                }
            }
        }

        loadingStatusContainer.setVisibility(LinearLayout.VISIBLE);
        loadingStatusIndicator.show();
        loadingStatusText.setText(
                hasPendingAssistantReply
                        ? R.string.ai_chat_status_generating
                        : R.string.ai_chat_status_syncing
        );
    }
}
