package com.example.walkfie;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Objects;

public class GoogleSignInActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 9001;
    private GoogleSignInClient googleSignInClient;
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_google_sign_in);

        firebaseAuth = FirebaseAuth.getInstance();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(Secrets.getGoogleWebClientId(this)) // <-- IMPORTANT
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(this, gso);

        findViewById(R.id.startGoogleSignIn).setOnClickListener(v -> signIn());

        // Animation
        animateEntrance(findViewById(R.id.startGoogleSignIn));

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
        // 1. Sign out first to clear any cached account
        googleSignInClient.signOut().addOnCompleteListener(this, task -> {
            // 2. After sign out, start the sign-in intent
            Intent signInIntent = googleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_SIGN_IN);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            var task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class); // <- fix: properly get or catch
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                Toast.makeText(this, "Google Sign-In failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);

        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        saveUserToDatabase(acct);
                        startActivity(new Intent(GoogleSignInActivity.this, WelcomeActivity.class));
                        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                        finish();
                    } else {
                        Toast.makeText(this, "Authentication Failed.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveUserToDatabase(GoogleSignInAccount acct) {
        String uid = Objects.requireNonNull(firebaseAuth.getCurrentUser()).getUid();
        User user = new User(
                acct.getGivenName(),
                acct.getFamilyName() != null ? acct.getFamilyName() : "",
                acct.getEmail()
        );
        FirebaseDatabase.getInstance().getReference("Users").child(uid).setValue(user);
    }
}
