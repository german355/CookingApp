package com.example.cooking.ui.activities;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.cooking.R;
import com.example.cooking.model.Message;
import com.example.cooking.ui.adapters.MessageAdapter;
import com.example.cooking.ui.viewmodels.AiChatViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;

public class AiChatActivity extends AppCompatActivity {

    private AiChatViewModel viewModel;
    private RecyclerView recyclerViewMessages;
    private MessageAdapter messageAdapter;
    private TextInputEditText editTextMessage;
    private FloatingActionButton buttonPhoto;
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
            viewModel.clearChat();
            return true;
        } else if (id == android.R.id.home) {
            onBackPressed();
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

        recyclerViewMessages = findViewById(R.id.recyclerViewMessages);
        messageAdapter = new MessageAdapter(new ArrayList<Message>());
        recyclerViewMessages.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewMessages.setAdapter(messageAdapter);

        editTextMessage = findViewById(R.id.editTextMessage);
        buttonPhoto = findViewById(R.id.buttonPhoto);
        buttonSend = findViewById(R.id.buttonSend);

        setupObservers();
        setupEventListeners();
    }

    private void setupObservers() {
        viewModel.getMessages().observe(this, messages -> {
            messageAdapter.setMessages(messages);
            if (!messages.isEmpty()) {
                recyclerViewMessages.scrollToPosition(messages.size() - 1);
            }
        });
        
        viewModel.getIsLoading().observe(this, loading -> buttonSend.setEnabled(!loading));
        
        viewModel.getShowMessage().observe(this, message -> {
            if (message != null && !message.isEmpty()) {
                Snackbar.make(findViewById(android.R.id.content), 
                    message, Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    private void setupEventListeners() {
        buttonSend.setOnClickListener(v -> {
            String text = editTextMessage.getText().toString().trim();
            if (!text.isEmpty()) {
                viewModel.sendMessage(text);
                editTextMessage.setText("");
            }
        });
        buttonPhoto.setOnClickListener(v -> viewModel.onPhotoButtonClicked());
    }
}
