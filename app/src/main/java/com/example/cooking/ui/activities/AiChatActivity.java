package com.example.cooking.ui.activities;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
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
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;

public class AiChatActivity extends AppCompatActivity {
    public static final String EXTRA_CONTEXT_RECIPE_ID = "context_recipe_id";

    private AiChatViewModel viewModel;
    private RecyclerView recyclerViewMessages;
    private MessageAdapter messageAdapter;
    private TextInputEditText editTextMessage;
    private FloatingActionButton buttonSend;

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
        recyclerViewMessages.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewMessages.setAdapter(messageAdapter);

        editTextMessage = findViewById(R.id.editTextMessage);
        buttonSend = findViewById(R.id.buttonSend);

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
            messageAdapter.submitList(messageList);
            if (messageList != null && !messageList.isEmpty()) {
                recyclerViewMessages.scrollToPosition(messageList.size() - 1);
            }
        });
        
        viewModel.getIsLoading().observe(this, loading ->
            buttonSend.setEnabled(!loading && Boolean.TRUE.equals(viewModel.getCanSend().getValue()))
        );
        viewModel.getCanSend().observe(this, canSend ->
            buttonSend.setEnabled(Boolean.TRUE.equals(canSend) && !Boolean.TRUE.equals(viewModel.getIsLoading().getValue()))
        );
        
        viewModel.getShowMessage().observe(this, message -> {
            if (message != null) {
                Snackbar.make(findViewById(android.R.id.content),
                    message, Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    private void setupEventListeners() {
        buttonSend.setOnClickListener(v -> {
            String text = editTextMessage.getText().toString().trim();
            if (!text.isEmpty()) {
                buttonSend.setEnabled(false);
                if (viewModel.sendMessage(text)) {
                    editTextMessage.setText("");
                } else {
                    buttonSend.setEnabled(Boolean.TRUE.equals(viewModel.getCanSend().getValue())
                            && !Boolean.TRUE.equals(viewModel.getIsLoading().getValue()));
                }
            }
        });
    }
}
