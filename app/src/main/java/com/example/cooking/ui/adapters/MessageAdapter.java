package com.example.cooking.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.view.Gravity;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.cooking.R;
import com.example.cooking.model.Message;
import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    private List<Message> messages;

    public MessageAdapter(List<Message> messages) {
        this.messages = messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        holder.bind(messages.get(position));
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        private final TextView textView;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.textViewMessage);
        }

        public void bind(Message message) {
            textView.setText(message.getText());
            // выравнивание сообщения через FrameLayout.LayoutParams
            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) textView.getLayoutParams();
            if (message.isUser()) {
                params.gravity = Gravity.END;
                textView.setBackgroundResource(R.drawable.bg_message_bubble_user);
            } else {
                params.gravity = Gravity.START;
                textView.setBackgroundResource(R.drawable.bg_message_bubble);
            }
            textView.setLayoutParams(params);
        }
    }
}
