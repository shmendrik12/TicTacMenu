package com.example.tictacmenu;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

public class LoginActivity extends AppCompatActivity {

    private EditText email;
    private EditText password;
    private CheckBox rememberMe;

    private FirebaseAuth firebaseAuth;
    private GoogleSignInClient googleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;
    private Button loginButton;
    private Button registerButton;
    private Button googleSignInButton;
    private boolean loginInProgress;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        email = findViewById(R.id.eTemail);
        password = findViewById(R.id.eTpass);
        rememberMe = findViewById(R.id.cBstayconnect);
        loginButton = findViewById(R.id.btn);
        registerButton = findViewById(R.id.btnRegister);
        googleSignInButton = findViewById(R.id.btnGoogleSignIn);

        firebaseAuth = FirebaseAuth.getInstance();

        configureGoogleSignIn();

        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Task<GoogleSignInAccount> task =
                            GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                    try {
                        GoogleSignInAccount account = task.getResult(ApiException.class);
                        firebaseAuthWithGoogle(account.getIdToken());
                    } catch (ApiException e) {
                        Toast.makeText(this, "Google sign-in failed: " + e.getStatusCode(),
                                Toast.LENGTH_SHORT).show();
                    }
                });

        // Auto sign-in if Firebase already has a current user + local "remember me" flag
        if (SessionStore.shouldRestore(this) && firebaseAuth.getCurrentUser() != null) {
            openMenu();
        }
    }

    public void onLoginClick(View view) {
        if (loginInProgress) {
            return;
        }
        Credentials credentials = readCredentials();
        if (credentials == null) {
            return;
        }

        setLoginInProgress(true);
        firebaseAuth.signInWithEmailAndPassword(credentials.email, credentials.password)
                .addOnSuccessListener(this::onAuthSuccess)
                .addOnFailureListener(signInError -> {
                    setLoginInProgress(false);
                    showAuthError("Sign-in failed", signInError);
                });
    }

    public void onRegisterClick(View view) {
        if (loginInProgress) {
            return;
        }
        Credentials credentials = readCredentials();
        if (credentials == null) {
            return;
        }
        if (credentials.password.length() < 6) {
            password.setError("Use at least 6 characters");
            password.requestFocus();
            return;
        }
        setLoginInProgress(true);
        createAccount(credentials.email, credentials.password);
    }

    public void onGoogleLoginClick(View view) {
        if (loginInProgress) {
            return;
        }
        if (googleSignInClient == null) {
            Toast.makeText(this,
                    "Google Sign-In is not configured for this Firebase app yet.",
                    Toast.LENGTH_LONG).show();
            return;
        }
        googleSignInLauncher.launch(googleSignInClient.getSignInIntent());
    }

    private void firebaseAuthWithGoogle(String idToken) {
        if (TextUtils.isEmpty(idToken)) {
            Toast.makeText(this, "Google Sign-In did not return an ID token.", Toast.LENGTH_LONG).show();
            return;
        }
        setLoginInProgress(true);
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        firebaseAuth.signInWithCredential(credential)
                .addOnSuccessListener(this::onAuthSuccess)
                .addOnFailureListener(e -> {
                    setLoginInProgress(false);
                    showAuthError("Google sign-in failed", e);
                });
    }

    private void onAuthSuccess(AuthResult result) {
        FirebaseUser user = result.getUser();
        String uid = user != null ? user.getUid() : "";
        String userEmail = user != null && user.getEmail() != null ? user.getEmail() : "";
        SessionStore.signIn(this, userEmail.isEmpty() ? uid : userEmail, rememberMe.isChecked());
        openMenu();
    }

    private void createAccount(String userEmail, String userPassword) {
        firebaseAuth.createUserWithEmailAndPassword(userEmail, userPassword)
                .addOnSuccessListener(this::onAuthSuccess)
                .addOnFailureListener(createError -> {
                    setLoginInProgress(false);
                    showAuthError("Account creation failed", createError);
                });
    }

    /**
     * google-services.json has to contain a Web OAuth client before the plugin can generate
     * default_web_client_id. Look it up dynamically so email/password Firebase keeps working
     * while the project owner completes that Console-side prerequisite.
     */
    private void configureGoogleSignIn() {
        int webClientId = getResources().getIdentifier(
                "default_web_client_id", "string", getPackageName());
        if (webClientId == 0) {
            googleSignInButton.setEnabled(false);
            return;
        }

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(webClientId))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    private void setLoginInProgress(boolean inProgress) {
        loginInProgress = inProgress;
        loginButton.setEnabled(!inProgress);
        registerButton.setEnabled(!inProgress);
        googleSignInButton.setEnabled(!inProgress && googleSignInClient != null);
    }

    private void showAuthError(String prefix, Exception error) {
        String detail = error.getLocalizedMessage();
        Toast.makeText(this, prefix + (TextUtils.isEmpty(detail) ? "." : ": " + detail),
                Toast.LENGTH_LONG).show();
    }

    @Nullable
    private Credentials readCredentials() {
        String userEmail = email.getText() == null ? "" : email.getText().toString().trim();
        String userPassword = password.getText() == null ? "" : password.getText().toString();
        if (userEmail.isEmpty()) {
            email.setError(getString(R.string.email_hint));
            email.requestFocus();
            return null;
        }
        if (userPassword.isEmpty()) {
            password.setError("Password required");
            password.requestFocus();
            return null;
        }
        return new Credentials(userEmail, userPassword);
    }

    private static final class Credentials {
        private final String email;
        private final String password;

        private Credentials(String email, String password) {
            this.email = email;
            this.password = password;
        }
    }

    private void openMenu() {
        Intent intent = new Intent(this, MenuActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
