package com.example.walkfie;

import android.net.Uri; // <-- IMPORTANT: Add this import
import android.os.Bundle;
import androidx.annotation.NonNull; // <-- IMPORTANT: Add this import for @NonNull
import androidx.annotation.Nullable; // <-- IMPORTANT: Add this import for @Nullable (though not strictly needed in this simplified version)
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.util.Log; // <-- IMPORTANT: Add this for logging

public class CreatePostFragment extends Fragment {

    // Constants for argument keys
    private static final String ARG_MEDIA_URI = "media_uri";
    private static final String ARG_MEDIA_TYPE = "media_type";

    // Member variables to hold the data passed to the fragment
    private Uri mediaUri;
    private String mediaType;

    public CreatePostFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param mediaUri The URI of the media captured (image/video) for the post.
     * @param mediaType The type of media ("image" or "video").
     * @return A new instance of fragment CreatePostFragment.
     */
    public static CreatePostFragment newInstance(Uri mediaUri, String mediaType) {
        CreatePostFragment fragment = new CreatePostFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_MEDIA_URI, mediaUri); // Uri objects are Parcelable
        args.putString(ARG_MEDIA_TYPE, mediaType);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) { // Changed to @Nullable Bundle
        super.onCreate(savedInstanceState);
        // Retrieve arguments if they exist
        if (getArguments() != null) {
            mediaUri = getArguments().getParcelable(ARG_MEDIA_URI);
            mediaType = getArguments().getString(ARG_MEDIA_TYPE);
            Log.d("CreatePostFragment", "onCreate: Received mediaUri = " + mediaUri + ", mediaType = " + mediaType);
        } else {
            Log.e("CreatePostFragment", "onCreate: No arguments provided for CreatePostFragment.");
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, // Added @NonNull
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_create_post, container, false);

        // --- TODO: Use mediaUri and mediaType here to set up your UI ---
        // For example, if you have an ImageView to show the captured media:
        // ImageView mediaPreview = view.findViewById(R.id.your_media_preview_id);
        // if (mediaUri != null) {
        //     mediaPreview.setImageURI(mediaUri);
        // }
        // TextView mediaTypeText = view.findViewById(R.id.your_media_type_text_id);
        // if (mediaType != null) {
        //     mediaTypeText.setText("Media Type: " + mediaType);
        // }
        // ---------------------------------------------------------------

        return view;
    }

    // You might also want to add an interface for callbacks, similar to HomeActivity's other fragments,
    // to notify the hosting activity when the post is created or cancelled.
    /*
    public interface CreatePostCallback {
        void onPostCreatedSuccessfully();
        void onPostCreationCancelled();
    }

    private CreatePostCallback callback;

    @Override
    public void onAttach(@NonNull android.content.Context context) {
        super.onAttach(context);
        if (context instanceof CreatePostCallback) {
            callback = (CreatePostCallback) context;
        } else {
            Log.w("CreatePostFragment", "Host Activity does not implement CreatePostCallback.");
            // Optionally, throw a RuntimeException if this callback is mandatory
            // throw new RuntimeException(context.toString() + " must implement CreatePostCallback");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        callback = null; // Avoid memory leaks
    }
    */
}