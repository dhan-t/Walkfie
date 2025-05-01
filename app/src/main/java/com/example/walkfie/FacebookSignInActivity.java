package com.example.walkfie;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Arrays;

public class FacebookSignInActivity extends AppCompatActivity {

    private CallbackManager callbackManager;
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_facebook_sign_in);

        firebaseAuth = FirebaseAuth.getInstance();
        callbackManager = CallbackManager.Factory.create();

        findViewById(R.id.startFacebookSignIn).setOnClickListener(v -> signIn());

        animateEntrance(findViewById(R.id.startFacebookSignIn));

        signIn();
    }

    private void animateEntrance(View view) {
        view.setAlpha(0f);
        view.setTranslationY(100f);
        view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(500)
                .setStartDelay(200)
                .start();
    }

    private void signIn() {
        LoginManager.getInstance().logInWithReadPermissions(this, Arrays.asList("email", "public_profile"));

        LoginManager.getInstance().registerCallback(callbackManager,
                new FacebookCallback<LoginResult>() {
                    @Override
                    public void onSuccess(LoginResult loginResult) {
                        handleFacebookAccessToken(loginResult.getAccessToken());
                    }

                    @Override
                    public void onCancel() {
                        Toast.makeText(FacebookSignInActivity.this, "Facebook Login Cancelled", Toast.LENGTH_SHORT).show();
                        finish();
                    }

                    @Override
                    public void onError(FacebookException error) {
                        Toast.makeText(FacebookSignInActivity.this, "Facebook Login Failed", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
    }

    private void handleFacebookAccessToken(AccessToken token) {
        if (token == null) {
            Toast.makeText(this, "Facebook token is null.", Toast.LENGTH_SHORT).show();
            return;
        }

        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());

        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        saveUserToDatabase();
                        startActivity(new Intent(FacebookSignInActivity.this, HomeActivity.class));
                        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                        finish();
                    } else {
                        Toast.makeText(this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
    }

    private void saveUserToDatabase() {
        var user = firebaseAuth.getCurrentUser();
        if (user != null) {
            String uid = user.getUid();
            User profile = new User(
                    user.getDisplayName(),
                    "", // Facebook API sometimes doesn't split first/last names easily
                    user.getEmail()
            );
            FirebaseDatabase.getInstance().getReference("Users").child(uid).setValue(profile);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }
}
