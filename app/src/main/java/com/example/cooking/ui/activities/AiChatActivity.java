package com.example.cooking.ui.activities;

import android.os.Bundle;
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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_chat);

        Toolbar toolbar = findViewById(R.id.topAppBar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

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
            recyclerViewMessages.scrollToPosition(messages.size() - 1);
        });
        viewModel.getIsLoading().observe(this, loading -> buttonSend.setEnabled(!loading));
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
