package com.martist.vitamove.assistant;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.martist.vitamove.R;

import java.util.ArrayList;
import java.util.List;

import io.noties.markwon.Markwon;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int VIEW_TYPE_USER = 1;
    private static final int VIEW_TYPE_ASSISTANT = 2;

    private final List<ChatMessage> messages = new ArrayList<>();
    private final Markwon markwon;

    public ChatAdapter(Markwon markwon) {
        this.markwon = markwon;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        if (viewType == VIEW_TYPE_USER) {
            View view = inflater.inflate(R.layout.item_message_user, parent, false);
            return new UserMessageViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_message_assistant, parent, false);
            return new AssistantMessageViewHolder(view, markwon);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = messages.get(position);

        if (holder instanceof UserMessageViewHolder) {
            ((UserMessageViewHolder) holder).bind(message);
        } else if (holder instanceof AssistantMessageViewHolder) {
            ((AssistantMessageViewHolder) holder).bind(message);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage message = messages.get(position);
        return message.isFromUser() ? VIEW_TYPE_USER : VIEW_TYPE_ASSISTANT;
    }

    public void addMessage(ChatMessage message) {
        messages.add(message);
        notifyItemInserted(messages.size() - 1);
    }


    public void setMessagesEnabled(boolean enabled) {

    }


    public void clearMessages() {
        int size = messages.size();
        messages.clear();
        notifyItemRangeRemoved(0, size);
    }


    public List<ChatMessage> getMessages() {
        return new ArrayList<>(messages);
    }


    public boolean removeLastMessage() {
        if (messages.isEmpty()) {
            return false;
        }
        int lastIndex = messages.size() - 1;
        messages.remove(lastIndex);
        notifyItemRemoved(lastIndex);
        return true;
    }

    static class UserMessageViewHolder extends RecyclerView.ViewHolder {
        private final TextView messageText;

        UserMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.messageText);
        }

        void bind(ChatMessage message) {
            messageText.setText(message.getText());

        }
    }

    static class AssistantMessageViewHolder extends RecyclerView.ViewHolder {
        private final TextView messageText;
        private final Markwon markwon;

        AssistantMessageViewHolder(@NonNull View itemView, Markwon markwon) {
            super(itemView);
            messageText = itemView.findViewById(R.id.messageText);
            this.markwon = markwon;
        }

        void bind(ChatMessage message) {

            markwon.setMarkdown(messageText, message.getText());

        }
    }
} 