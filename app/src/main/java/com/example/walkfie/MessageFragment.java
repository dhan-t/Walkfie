package com.example.walkfie;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import android.os.Handler; // Import Handler
import android.os.Looper; // Import Looper

// Define a callback interface for MessageFragment actions
interface MessageFragmentCallback {
    void navigateToProfileFromMessages(); // To navigate to ProfileFragment when icon clicked
    void navigateToFriendSearchFromMessages(); // To navigate to friend search when "Find Friends" clicked
    void navigateToChatScreen(String chatPartnerName); // To open a specific chat
}

public class MessageFragment extends Fragment {

    private ImageView ivProfileIcon;
    private EditText etSearchBar;
    private RecyclerView rvChatList;
    private LinearLayout noChatsLayout;
    private Button btnFindFriends;
    private ProgressBar messagesProgressBar; // Declare the ProgressBar

    private ChatListAdapter chatListAdapter;
    private List<ChatListAdapter.ChatItem> dummyChatData; // For demonstrating chat list or empty state

    private MessageFragmentCallback callback;

    // Handler for delayed UI updates (for demonstration purposes)
    private final Handler handler = new Handler(Looper.getMainLooper());

    public void setMessageFragmentCallback(MessageFragmentCallback callback) {
        this.callback = callback;
    }

    public MessageFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(@NonNull android.content.Context context) {
        super.onAttach(context);
        if (context instanceof MessageFragmentCallback) {
            callback = (MessageFragmentCallback) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement MessageFragmentCallback");
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_message, container, false);

        // Initialize views
        ivProfileIcon = view.findViewById(R.id.ivProfileIcon);
        etSearchBar = view.findViewById(R.id.etSearchBar);
        rvChatList = view.findViewById(R.id.rvChatList);
        noChatsLayout = view.findViewById(R.id.noChatsLayout);
        btnFindFriends = view.findViewById(R.id.btnFindFriends);
        messagesProgressBar = view.findViewById(R.id.messagesProgressBar); // Initialize the ProgressBar

        // --- Spinner Logic: Show spinner immediately ---
        messagesProgressBar.setVisibility(View.VISIBLE);
        rvChatList.setVisibility(View.GONE);
        noChatsLayout.setVisibility(View.GONE);

        // --- Simulate Data Loading with a Delay ---
        handler.postDelayed(() -> {
            // This block runs after the specified delay

            // --- Dummy Data Setup ---
            // In a real app, you'd load this from a database or API
            dummyChatData = new ArrayList<>();
            // Uncomment the line below to test with chat data
            populateDummyChatData(); // This method fills dummyChatData

            // Configure RecyclerView
            rvChatList.setLayoutManager(new LinearLayoutManager(getContext()));
            chatListAdapter = new ChatListAdapter(dummyChatData, chatItem -> {
                // Handle chat item click - navigate to actual chat screen
                if (callback != null) {
                    callback.navigateToChatScreen(chatItem.partnerName);
                } else {
                    Toast.makeText(getContext(), "Opening chat with " + chatItem.partnerName, Toast.LENGTH_SHORT).show();
                }
            });
            rvChatList.setAdapter(chatListAdapter);

            // --- Conditional UI Visibility AFTER loading ---
            messagesProgressBar.setVisibility(View.GONE); // Hide spinner

            if (dummyChatData.isEmpty()) {
                rvChatList.setVisibility(View.GONE);
                noChatsLayout.setVisibility(View.VISIBLE);
            } else {
                rvChatList.setVisibility(View.VISIBLE);
                noChatsLayout.setVisibility(View.GONE);
            }

        }, 2500); // 2000 milliseconds = 2 seconds delay. Adjust as needed.


        // --- Set Click Listeners ---
        ivProfileIcon.setOnClickListener(v -> {
            if (callback != null) {
                callback.navigateToProfileFromMessages();
            } else {
                Toast.makeText(getContext(), "Profile icon clicked (from messages)", Toast.LENGTH_SHORT).show();
            }
        });

        btnFindFriends.setOnClickListener(v -> {
            if (callback != null) {
                callback.navigateToFriendSearchFromMessages();
            } else {
                Toast.makeText(getContext(), "Find Friends clicked!", Toast.LENGTH_SHORT).show();
            }
        });

        // Optional: Handle search bar input (e.g., filter chat list)
        etSearchBar.setOnEditorActionListener((v, actionId, event) -> {
            // Implement search logic here
            Toast.makeText(getContext(), "Searching for: " + v.getText().toString(), Toast.LENGTH_SHORT).show();
            return false;
        });

        return view;
    }

    // Populate with some dummy chat items
    private void populateDummyChatData() {
        dummyChatData.add(new ChatListAdapter.ChatItem("Alice Smith", "Hey, how's it going?", "2m ago", 1));
        dummyChatData.add(new ChatListAdapter.ChatItem("Bob Johnson", "Sounds good! See you then.", "1h ago", 0));
        dummyChatData.add(new ChatListAdapter.ChatItem("Charlie Brown", "Did you get my last message?", "Yesterday", 3));
        dummyChatData.add(new ChatListAdapter.ChatItem("Diana Prince", "Let's plan that walk soon!", "Last Week", 0));
        dummyChatData.add(new ChatListAdapter.ChatItem("Eve Adams", "I'm here! Where are you?", "3 days ago", 5));
    }

    @Override
    public void onStop() {
        super.onStop();
        // Crucial: Remove any pending callbacks when the fragment stops
        handler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        // Crucial: Remove any pending callbacks when the fragment is detached
        handler.removeCallbacksAndMessages(null);
        callback = null;
    }
}