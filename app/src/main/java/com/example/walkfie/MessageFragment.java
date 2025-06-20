package com.example.walkfie;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
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
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

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

    private FloatingActionButton fabAddFriend;
    private ImageView ivProfileIcon;
    private EditText etSearchBar;
    private RecyclerView rvChatList;
    private LinearLayout noChatsLayout;
    private Button btnFindFriends;
    private ProgressBar messagesProgressBar; // Declare the ProgressBar
    private LinearLayout friendRequestsLayout;
    private RecyclerView rvFriendRequests;
    private ImageView ivNotificationBell;
    private View notificationBadge;
    private RecyclerView rvFriends;

    private ChatListAdapter chatListAdapter;
    private List<ChatListAdapter.ChatItem> dummyChatData; // For demonstrating chat list or empty state
    private FriendRequestAdapter friendRequestAdapter;
    private List<FriendRequestItem> incomingRequests = new ArrayList<>();
    private List<NotificationItem> notifications = new ArrayList<>();
    private NotificationAdapter notificationAdapter;
    private FriendsAdapter friendsAdapter;
    private List<FriendItem> friendsList = new ArrayList<>();

    private MessageFragmentCallback callback;

    // Handler for delayed UI updates (for demonstration purposes)
    private final Handler handler = new Handler(Looper.getMainLooper());

    private String currentUserId;
    private String currentUserProfilePicUrl;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private AlertDialog friendSearchDialog;
    private FriendSearchAdapter friendSearchAdapter;
    private List<UserItem> searchResults = new ArrayList<>();

    // Notification dialog
    private AlertDialog notificationDialog; // Fix: declare notificationDialog

    // User item for search results
    private static class UserItem {
        String userId;
        String displayName;
        String email;
        String profilePicUrl;
        boolean isFriend;
        boolean requestSent;
        UserItem(String userId, String displayName, String email, String profilePicUrl, boolean isFriend, boolean requestSent) {
            this.userId = userId;
            this.displayName = displayName;
            this.email = email;
            this.profilePicUrl = profilePicUrl;
            this.isFriend = isFriend;
            this.requestSent = requestSent;
        }
    }

    // Friend request item
    private static class FriendRequestItem {
        String userId;
        String displayName;
        String profilePicUrl;
        FriendRequestItem(String userId, String displayName, String profilePicUrl) {
            this.userId = userId;
            this.displayName = displayName;
            this.profilePicUrl = profilePicUrl;
        }
    }

    // Notification item
    private static class NotificationItem {
        String id;
        String type;
        String fromUserId;
        String message;
        boolean read;
        long timestamp;
        NotificationItem(String id, String type, String fromUserId, String message, boolean read, long timestamp) {
            this.id = id;
            this.type = type;
            this.fromUserId = fromUserId;
            this.message = message;
            this.read = read;
            this.timestamp = timestamp;
        }
    }

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
        fabAddFriend = view.findViewById(R.id.fabAddFriend);
        ivProfileIcon = view.findViewById(R.id.ivProfileIcon);
        etSearchBar = view.findViewById(R.id.etSearchBar);
        rvChatList = view.findViewById(R.id.rvChatList);
        noChatsLayout = view.findViewById(R.id.noChatsLayout);
        btnFindFriends = view.findViewById(R.id.btnFindFriends);
        messagesProgressBar = view.findViewById(R.id.messagesProgressBar); // Initialize the ProgressBar
        friendRequestsLayout = view.findViewById(R.id.friendRequestsLayout);
        rvFriendRequests = view.findViewById(R.id.rvFriendRequests);
        ivNotificationBell = view.findViewById(R.id.ivNotificationBell);
        notificationBadge = view.findViewById(R.id.notificationBadge);
        rvFriends = view.findViewById(R.id.rvFriends);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // --- Spinner Logic: Show spinner immediately ---
        messagesProgressBar.setVisibility(View.VISIBLE);
        rvChatList.setVisibility(View.GONE);
        noChatsLayout.setVisibility(View.GONE);

        // --- Load real chat data from Firestore ---
        loadChatList();

        // --- Set Click Listeners ---
        fabAddFriend.setOnClickListener(v -> showFriendSearchDialog());

        ivProfileIcon.setOnClickListener(v -> {
            if (callback != null) {
                callback.navigateToProfileFromMessages();
            }
        });

        btnFindFriends.setOnClickListener(v -> {
            if (callback != null) {
                callback.navigateToFriendSearchFromMessages();
            } else {
                Toast.makeText(getContext(), "Find Friends clicked!", Toast.LENGTH_SHORT).show();
            }
        });

        ivNotificationBell.setOnClickListener(v -> showNotificationDialog());

        // Optional: Handle search bar input (e.g., filter chat list)
        etSearchBar.setOnEditorActionListener((v, actionId, event) -> {
            // Implement search logic here
            Toast.makeText(getContext(), "Searching for: " + v.getText().toString(), Toast.LENGTH_SHORT).show();
            return false;
        });

        if (mAuth.getCurrentUser() != null) {
            currentUserId = mAuth.getCurrentUser().getUid();
            db.collection("users").document(currentUserId)
                    .addSnapshotListener((snapshot, e) -> {
                        if (e != null) return;
                        if (snapshot != null && snapshot.exists()) {
                            currentUserProfilePicUrl = snapshot.getString("profilePicUrl");
                            if (currentUserProfilePicUrl != null && !currentUserProfilePicUrl.isEmpty()) {
                                Glide.with(requireContext())
                                        .load(currentUserProfilePicUrl)
                                        .placeholder(R.drawable.ic_default_profile_placeholder)
                                        .error(R.drawable.ic_default_profile_placeholder)
                                        .circleCrop()
                                        .into(ivProfileIcon);
                            } else {
                                ivProfileIcon.setImageResource(R.drawable.ic_default_profile_placeholder);
                            }
                        }
                    });
        }

        // Friend request adapter
        friendRequestAdapter = new FriendRequestAdapter(incomingRequests, new FriendRequestAdapter.OnRequestActionListener() {
            @Override
            public void onAccept(FriendRequestItem item) { acceptFriendRequest(item); }
            @Override
            public void onReject(FriendRequestItem item) { rejectFriendRequest(item); }
        });
        rvFriendRequests.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvFriendRequests.setAdapter(friendRequestAdapter);
        // Load incoming friend requests
        loadIncomingFriendRequests();

        // Load notifications and update badge
        loadNotifications();

        // Friends adapter
        friendsAdapter = new FriendsAdapter(friendsList, friendItem -> {
            if (callback != null) {
                callback.navigateToChatScreen(friendItem.displayName); // You may want to pass userId
            }
        });
        rvFriends.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvFriends.setAdapter(friendsAdapter);
        loadFriendsList();

        return view;
    }

    // --- Load real chat list from Firestore ---
    private void loadChatList() {
        if (db == null || mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();
        db.collection("users").document(uid).collection("chats")
                .orderBy("lastMessageTimestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) return;
                    List<ChatListAdapter.ChatItem> chatItems = new ArrayList<>();
                    if (snapshots != null) {
                        for (var doc : snapshots.getDocuments()) {
                            String partnerName = doc.getString("partnerName");
                            String lastMessage = doc.getString("lastMessage");
                            String lastTime = doc.getString("lastMessageTime");
                            int unread = doc.getLong("unreadCount") != null ? doc.getLong("unreadCount").intValue() : 0;
                            chatItems.add(new ChatListAdapter.ChatItem(partnerName, lastMessage, lastTime, unread));
                        }
                    }
                    // Update UI
                    messagesProgressBar.setVisibility(View.GONE);
                    if (chatItems.isEmpty()) {
                        rvChatList.setVisibility(View.GONE);
                        noChatsLayout.setVisibility(View.VISIBLE);
                    } else {
                        rvChatList.setVisibility(View.VISIBLE);
                        noChatsLayout.setVisibility(View.GONE);
                    }
                    rvChatList.setLayoutManager(new LinearLayoutManager(getContext()));
                    chatListAdapter = new ChatListAdapter(chatItems, chatItem -> {
                        if (callback != null) {
                            callback.navigateToChatScreen(chatItem.partnerName);
                        }
                    });
                    rvChatList.setAdapter(chatListAdapter);
                });
    }

    // --- Friend Search Dialog Implementation ---
    private void showFriendSearchDialog() {
        if (getContext() == null) return;
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_friend_search, null);
        EditText etSearch = dialogView.findViewById(R.id.etFriendSearch);
        RecyclerView rvResults = dialogView.findViewById(R.id.rvFriendResults);
        ProgressBar pbSearch = dialogView.findViewById(R.id.pbFriendSearch);
        TextView tvNoResults = dialogView.findViewById(R.id.tvNoFriendResults);
        friendSearchAdapter = new FriendSearchAdapter(searchResults, userItem -> {
            sendFriendRequest(userItem);
        });
        rvResults.setLayoutManager(new LinearLayoutManager(getContext()));
        rvResults.setAdapter(friendSearchAdapter);
        rvResults.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL));
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext())
                .setTitle("Find Friends")
                .setView(dialogView)
                .setNegativeButton("Close", (d, w) -> d.dismiss());
        friendSearchDialog = builder.create();
        friendSearchDialog.show();
        // Search on enter
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                String query = etSearch.getText().toString().trim();
                if (!android.text.TextUtils.isEmpty(query)) {
                    searchForUsers(query, pbSearch, tvNoResults);
                }
                return true;
            }
            return false;
        });
    }

    // --- Search for users by displayName or email ---
    private void searchForUsers(String query, ProgressBar pbSearch, TextView tvNoResults) {
        pbSearch.setVisibility(View.VISIBLE);
        tvNoResults.setVisibility(View.GONE);
        searchResults.clear();
        friendSearchAdapter.notifyDataSetChanged();
        db.collection("users")
                .whereGreaterThanOrEqualTo("displayName", query)
                .whereLessThanOrEqualTo("displayName", query + "\uf8ff")
                .limit(10)
                .get()
                .addOnSuccessListener(snapshots -> {
                    pbSearch.setVisibility(View.GONE);
                    if (snapshots.isEmpty()) {
                        // Try searching by email if no displayName match
                        db.collection("users")
                                .whereEqualTo("email", query)
                                .get()
                                .addOnSuccessListener(emailSnaps -> {
                                    if (emailSnaps.isEmpty()) {
                                        tvNoResults.setVisibility(View.VISIBLE);
                                    } else {
                                        for (var doc : emailSnaps.getDocuments()) {
                                            String userId = doc.getId();
                                            if (userId.equals(currentUserId)) continue;
                                            String displayName = doc.getString("displayName");
                                            String email = doc.getString("email");
                                            String profilePicUrl = doc.getString("profilePicUrl");
                                            searchResults.add(new UserItem(userId, displayName, email, profilePicUrl, false, false));
                                        }
                                        friendSearchAdapter.notifyDataSetChanged();
                                    }
                                });
                        return;
                    }
                    for (var doc : snapshots.getDocuments()) {
                        String userId = doc.getId();
                        if (userId.equals(currentUserId)) continue;
                        String displayName = doc.getString("displayName");
                        String email = doc.getString("email");
                        String profilePicUrl = doc.getString("profilePicUrl");
                        searchResults.add(new UserItem(userId, displayName, email, profilePicUrl, false, false));
                    }
                    friendSearchAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    pbSearch.setVisibility(View.GONE);
                    tvNoResults.setVisibility(View.VISIBLE);
                    tvNoResults.setText("Error: " + e.getMessage());
                });
    }

    private void sendFriendRequest(UserItem userItem) {
        if (db == null || currentUserId == null) return;
        // Add a friend request document to the target user's 'friendRequests' subcollection
        db.collection("users").document(userItem.userId)
                .collection("friendRequests").document(currentUserId)
                .set(new java.util.HashMap<String, Object>() {{
                    put("fromUserId", currentUserId);
                    put("timestamp", com.google.firebase.Timestamp.now());
                }})
                .addOnSuccessListener(unused -> {
                    userItem.requestSent = true;
                    friendSearchAdapter.notifyDataSetChanged();
                    Toast.makeText(getContext(), "Friend request sent!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to send request: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void loadIncomingFriendRequests() {
        if (db == null || currentUserId == null) return;
        db.collection("users").document(currentUserId)
                .collection("friendRequests")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) return;
                    incomingRequests.clear();
                    if (snapshots != null) {
                        for (var doc : snapshots.getDocuments()) {
                            String fromUserId = doc.getId();
                            db.collection("users").document(fromUserId).get().addOnSuccessListener(userDoc -> {
                                String name = userDoc.getString("displayName");
                                String pic = userDoc.getString("profilePicUrl");
                                incomingRequests.add(new FriendRequestItem(fromUserId, name, pic));
                                friendRequestAdapter.notifyDataSetChanged();
                                friendRequestsLayout.setVisibility(View.VISIBLE);
                            });
                        }
                    }
                    if (incomingRequests.isEmpty()) {
                        friendRequestsLayout.setVisibility(View.GONE);
                    }
                });
    }

    private void acceptFriendRequest(FriendRequestItem item) {
        if (db == null || currentUserId == null) return;
        // Add each other as friends
        db.collection("users").document(currentUserId)
                .collection("friends").document(item.userId)
                .set(new java.util.HashMap<String, Object>() {{
                    put("since", com.google.firebase.Timestamp.now());
                }});
        db.collection("users").document(item.userId)
                .collection("friends").document(currentUserId)
                .set(new java.util.HashMap<String, Object>() {{
                    put("since", com.google.firebase.Timestamp.now());
                }});
        // Remove friend request
        db.collection("users").document(currentUserId)
                .collection("friendRequests").document(item.userId).delete();
        // Add notification to sender
        db.collection("users").document(item.userId)
                .collection("notifications").add(new java.util.HashMap<String, Object>() {{
                    put("type", "friend_accept");
                    put("fromUserId", currentUserId);
                    put("timestamp", com.google.firebase.Timestamp.now());
                }});
        Toast.makeText(getContext(), "Friend request accepted!", Toast.LENGTH_SHORT).show();
    }

    private void rejectFriendRequest(FriendRequestItem item) {
        if (db == null || currentUserId == null) return;
        db.collection("users").document(currentUserId)
                .collection("friendRequests").document(item.userId).delete();
        Toast.makeText(getContext(), "Friend request rejected.", Toast.LENGTH_SHORT).show();
    }

    private void loadNotifications() {
        if (db == null || currentUserId == null) return;
        db.collection("users").document(currentUserId)
                .collection("notifications")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) return;
                    notifications.clear();
                    int unreadCount = 0;
                    if (snapshots != null) {
                        for (var doc : snapshots.getDocuments()) {
                            String id = doc.getId();
                            String type = doc.getString("type");
                            String fromUserId = doc.getString("fromUserId");
                            String message = buildNotificationMessage(type, fromUserId);
                            boolean read = doc.getBoolean("read") != null && doc.getBoolean("read");
                            long timestamp = doc.getTimestamp("timestamp") != null ? doc.getTimestamp("timestamp").toDate().getTime() : 0L;
                            notifications.add(new NotificationItem(id, type, fromUserId, message, read, timestamp));
                            if (!read) unreadCount++;
                        }
                    }
                    // Show/hide badge
                    if (notificationBadge != null) {
                        notificationBadge.setVisibility(unreadCount > 0 ? View.VISIBLE : View.GONE);
                    }
                });
    }

    private String buildNotificationMessage(String type, String fromUserId) {
        if (type == null) return "";
        switch (type) {
            case "friend_accept": return "Your friend request was accepted!";
            case "friend_request": return "You have a new friend request!";
            case "message": return "New message received!";
            default: return "You have a new notification.";
        }
    }

    private void showNotificationDialog() {
        if (getContext() == null) return;
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_notifications, null);
        RecyclerView rvNotifications = dialogView.findViewById(R.id.rvNotifications);
        notificationAdapter = new NotificationAdapter(notifications);
        rvNotifications.setLayoutManager(new LinearLayoutManager(getContext()));
        rvNotifications.setAdapter(notificationAdapter);
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext())
                .setTitle("Notifications")
                .setView(dialogView)
                .setNegativeButton("Close", (d, w) -> d.dismiss());
        notificationDialog = builder.create();
        notificationDialog.show();
    }

    // Add this method near other data-loading methods
    private void loadFriendsList() {
        if (db == null || currentUserId == null) return;
        db.collection("users").document(currentUserId)
                .collection("friends")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) return;
                    friendsList.clear();
                    if (snapshots != null) {
                        for (var doc : snapshots.getDocuments()) {
                            String friendId = doc.getId();
                            db.collection("users").document(friendId).get().addOnSuccessListener(userDoc -> {
                                String name = userDoc.getString("displayName");
                                String pic = userDoc.getString("profilePicUrl");
                                friendsList.add(new FriendItem(friendId, name, pic));
                                friendsAdapter.notifyDataSetChanged();
                            });
                        }
                    }
                });
    }

    // --- FriendSearchAdapter (inner class) ---
    public static class FriendSearchAdapter extends RecyclerView.Adapter<FriendSearchAdapter.FriendViewHolder> {
        private final List<UserItem> userList;
        private final OnUserClickListener listener;

        public interface OnUserClickListener {
            void onUserClick(UserItem userItem);
        }

        public FriendSearchAdapter(List<UserItem> userList, OnUserClickListener listener) {
            this.userList = userList;
            this.listener = listener;
        }

        @NonNull
        @Override
        public FriendViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user_search, parent, false);
            return new FriendViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull FriendViewHolder holder, int position) {
            UserItem userItem = userList.get(position);
            holder.bind(userItem, listener);
        }

        @Override
        public int getItemCount() {
            return userList.size();
        }

        static class FriendViewHolder extends RecyclerView.ViewHolder {
            private final TextView tvName;
            private final TextView tvEmail;
            private final ImageView ivProfilePic;
            private final Button btnSendRequest;

            public FriendViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tvName);
                tvEmail = itemView.findViewById(R.id.tvEmail);
                ivProfilePic = itemView.findViewById(R.id.ivProfilePic);
                btnSendRequest = itemView.findViewById(R.id.btnSendRequest);
            }

            public void bind(UserItem userItem, OnUserClickListener listener) {
                tvName.setText(userItem.displayName);
                tvEmail.setText(userItem.email);
                Glide.with(itemView.getContext())
                        .load(userItem.profilePicUrl)
                        .placeholder(R.drawable.ic_default_profile_placeholder)
                        .error(R.drawable.ic_default_profile_placeholder)
                        .circleCrop()
                        .into(ivProfilePic);
                btnSendRequest.setText(userItem.requestSent ? "Request Sent" : "Send Request");
                btnSendRequest.setEnabled(!userItem.requestSent);

                itemView.setOnClickListener(v -> listener.onUserClick(userItem));
            }
        }
    }

    // --- FriendRequestAdapter (inner class) ---
    public static class FriendRequestAdapter extends RecyclerView.Adapter<FriendRequestAdapter.RequestViewHolder> {
        private final List<FriendRequestItem> requestList;
        private final OnRequestActionListener listener;

        public interface OnRequestActionListener {
            void onAccept(FriendRequestItem item);
            void onReject(FriendRequestItem item);
        }

        public FriendRequestAdapter(List<FriendRequestItem> requestList, OnRequestActionListener listener) {
            this.requestList = requestList;
            this.listener = listener;
        }

        @NonNull
        @Override
        public RequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_friend_request, parent, false);
            return new RequestViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RequestViewHolder holder, int position) {
            FriendRequestItem requestItem = requestList.get(position);
            holder.bind(requestItem, listener);
        }

        @Override
        public int getItemCount() {
            return requestList.size();
        }

        static class RequestViewHolder extends RecyclerView.ViewHolder {
            private final TextView tvName;
            private final ImageView ivProfilePic;
            private final Button btnAccept;
            private final Button btnReject;

            public RequestViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tvName);
                ivProfilePic = itemView.findViewById(R.id.ivProfilePic);
                btnAccept = itemView.findViewById(R.id.btnAccept);
                btnReject = itemView.findViewById(R.id.btnReject);
            }

            public void bind(FriendRequestItem requestItem, OnRequestActionListener listener) {
                tvName.setText(requestItem.displayName);
                Glide.with(itemView.getContext())
                        .load(requestItem.profilePicUrl)
                        .placeholder(R.drawable.ic_default_profile_placeholder)
                        .error(R.drawable.ic_default_profile_placeholder)
                        .circleCrop()
                        .into(ivProfilePic);

                btnAccept.setOnClickListener(v -> listener.onAccept(requestItem));
                btnReject.setOnClickListener(v -> listener.onReject(requestItem));
            }
        }
    }

    // --- NotificationAdapter (inner class) ---
    public static class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {
        private final List<NotificationItem> notificationList;

        public NotificationAdapter(List<NotificationItem> notificationList) {
            this.notificationList = notificationList;
        }

        @NonNull
        @Override
        public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
            return new NotificationViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
            NotificationItem notificationItem = notificationList.get(position);
            holder.bind(notificationItem);
        }

        @Override
        public int getItemCount() {
            return notificationList.size();
        }

        static class NotificationViewHolder extends RecyclerView.ViewHolder {
            private final TextView tvMessage;
            private final TextView tvTimestamp;

            public NotificationViewHolder(@NonNull View itemView) {
                super(itemView);
                tvMessage = itemView.findViewById(R.id.tvNotificationMessage);
                tvTimestamp = itemView.findViewById(R.id.tvNotificationTimestamp);
            }

            public void bind(NotificationItem notificationItem) {
                tvMessage.setText(notificationItem.message);
                tvTimestamp.setText(formatTimestamp(notificationItem.timestamp));
            }

            private String formatTimestamp(long timestamp) {
                // TODO: Implement timestamp formatting
                return String.valueOf(timestamp);
            }
        }
    }

    // --- FriendsAdapter (inner class) ---
    public static class FriendsAdapter extends RecyclerView.Adapter<FriendsAdapter.FriendViewHolder> {
        private final List<FriendItem> friendList;
        private final OnFriendClickListener listener;

        public interface OnFriendClickListener {
            void onFriendClick(FriendItem friendItem);
        }

        public FriendsAdapter(List<FriendItem> friendList, OnFriendClickListener listener) {
            this.friendList = friendList;
            this.listener = listener;
        }

        @NonNull
        @Override
        public FriendViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_friend, parent, false);
            return new FriendViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull FriendViewHolder holder, int position) {
            FriendItem friendItem = friendList.get(position);
            holder.bind(friendItem, listener);
        }

        @Override
        public int getItemCount() {
            return friendList.size();
        }

        static class FriendViewHolder extends RecyclerView.ViewHolder {
            private final TextView tvName;
            private final ImageView ivProfilePic;

            public FriendViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tvName);
                ivProfilePic = itemView.findViewById(R.id.ivProfilePic);
            }

            public void bind(FriendItem friendItem, OnFriendClickListener listener) {
                tvName.setText(friendItem.displayName);
                Glide.with(itemView.getContext())
                        .load(friendItem.profilePicUrl)
                        .placeholder(R.drawable.ic_default_profile_placeholder)
                        .error(R.drawable.ic_default_profile_placeholder)
                        .circleCrop()
                        .into(ivProfilePic);

                itemView.setOnClickListener(v -> listener.onFriendClick(friendItem));
            }
        }
    }

    // Friend item
    public static class FriendItem {
        String userId;
        String displayName;
        String profilePicUrl;
        FriendItem(String userId, String displayName, String profilePicUrl) {
            this.userId = userId;
            this.displayName = displayName;
            this.profilePicUrl = profilePicUrl;
        }
    }
}
