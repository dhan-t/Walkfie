package com.example.walkfie;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.ChatViewHolder> {

    // A simple data model for demonstration
    public static class ChatItem {
        String partnerName;
        String lastMessage;
        String lastMessageTime;
        int unreadCount;
        // int profilePicResId; // In a real app, this would be a URL or URI

        public ChatItem(String partnerName, String lastMessage, String lastMessageTime, int unreadCount) {
            this.partnerName = partnerName;
            this.lastMessage = lastMessage;
            this.lastMessageTime = lastMessageTime;
            this.unreadCount = unreadCount;
        }
    }

    private List<ChatItem> chatList;
    private OnChatClickListener onChatClickListener;

    public interface OnChatClickListener {
        void onChatClick(ChatItem chatItem);
    }

    public ChatListAdapter(List<ChatItem> chatList, OnChatClickListener onChatClickListener) {
        this.chatList = chatList;
        this.onChatClickListener = onChatClickListener;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_preview, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatItem chatItem = chatList.get(position);
        holder.tvChatPartnerName.setText(chatItem.partnerName);
        holder.tvLastMessage.setText(chatItem.lastMessage);
        holder.tvLastMessageTime.setText(chatItem.lastMessageTime);
        holder.ivChatProfilePic.setImageResource(R.drawable.ic_default_profile_placeholder); // Dummy pic

        if (chatItem.unreadCount > 0) {
            holder.tvUnreadCount.setVisibility(View.VISIBLE);
            holder.tvUnreadCount.setText(String.valueOf(chatItem.unreadCount));
        } else {
            holder.tvUnreadCount.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (onChatClickListener != null) {
                onChatClickListener.onChatClick(chatItem);
            }
        });
    }

    @Override
    public int getItemCount() {
        return chatList.size();
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        ImageView ivChatProfilePic;
        TextView tvChatPartnerName;
        TextView tvLastMessage;
        TextView tvLastMessageTime;
        TextView tvUnreadCount;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            ivChatProfilePic = itemView.findViewById(R.id.ivChatProfilePic);
            tvChatPartnerName = itemView.findViewById(R.id.tvChatPartnerName);
            tvLastMessage = itemView.findViewById(R.id.tvLastMessage);
            tvLastMessageTime = itemView.findViewById(R.id.tvLastMessageTime);
            tvUnreadCount = itemView.findViewById(R.id.tvUnreadCount);
        }
    }
}