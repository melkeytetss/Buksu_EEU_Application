package com.example.buksu_eeu;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private EditText emailInput, passwordInput, usernameInput;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private GoogleSignInClient mGoogleSignInClient;
    private TurnstileView turnstileView;
    private String turnstileToken = null;

    // Cloudflare Turnstile Site Key
    private static final String TURNSTILE_SITE_KEY = "0x4AAAAAACu-FqwwbkqutV5U";

    private final ActivityResultLauncher<Intent> googleSignInLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Intent data = result.getData();
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                    try {
                        GoogleSignInAccount account = task.getResult(ApiException.class);
                        firebaseAuthWithGoogle(account.getIdToken());
                    } catch (ApiException e) {
                        Toast.makeText(this, "Google sign in failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        emailInput = findViewById(R.id.email_input);
        passwordInput = findViewById(R.id.password_input);
        usernameInput = findViewById(R.id.username_input);
        Button registerBtn = findViewById(R.id.register_btn);
        MaterialButton googleBtn = findViewById(R.id.google_btn);
        
        // Initialize Cloudflare Turnstile
        turnstileView = findViewById(R.id.cloudflare_turnstile_register);
        turnstileView.setSiteKey(TURNSTILE_SITE_KEY);
        
        turnstileView.setOnTokenResolvedListener(token -> {
            turnstileToken = token;
            Toast.makeText(RegisterActivity.this, "Security Check Passed", Toast.LENGTH_SHORT).show();
        });

        turnstileView.setOnFailureListener(error -> {
            turnstileToken = null;
            Toast.makeText(RegisterActivity.this, "Security Check Failed: " + error.getMessage(), Toast.LENGTH_SHORT).show();
        });

        TextView loginLink = findViewById(R.id.login_link_text);
        loginLink.setText(Html.fromHtml(getString(R.string.register_prompt), Html.FROM_HTML_MODE_COMPACT));
        loginLink.setOnClickListener(v -> finish());

        registerBtn.setOnClickListener(v -> {
            if (turnstileToken != null) {
                registerUser();
            } else {
                Toast.makeText(RegisterActivity.this, "Please complete the security check", Toast.LENGTH_SHORT).show();
            }
        });

        googleBtn.setOnClickListener(v -> {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            googleSignInLauncher.launch(signInIntent);
        });
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            checkAndCreateUserRecord(user);
                        }
                    } else {
                        String errorMsg = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                        Toast.makeText(RegisterActivity.this, "Google Auth failed: " + errorMsg, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkAndCreateUserRecord(FirebaseUser user) {
        db.collection("users").document(user.getUid()).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (!task.getResult().exists()) {
                            // New user via Google, create Firestore record
                            Map<String, Object> userData = new HashMap<>();
                            userData.put("username", user.getDisplayName());
                            userData.put("email", user.getEmail());
                            userData.put("role", "student"); // Default role

                            db.collection("users").document(user.getUid()).set(userData)
                                    .addOnSuccessListener(aVoid -> navigateToHome());
                        } else {
                            // User already exists
                            navigateToHome();
                        }
                    } else {
                        navigateToHome(); 
                    }
                });
    }

    private void navigateToHome() {
            startActivity(new Intent(RegisterActivity.this, NavContainerActivity.class));
        finish();
    }

    private void registerUser() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        String username = usernameInput.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password) || TextUtils.isEmpty(username)) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 8) {
            Toast.makeText(this, "Password must be at least 8 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        String userId = mAuth.getCurrentUser().getUid();
                        saveUserToFirestore(userId, username, email);
                    } else {
                        String errorMsg = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                        Toast.makeText(RegisterActivity.this, "Registration failed: " + errorMsg, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveUserToFirestore(String userId, String username, String email) {
        Map<String, Object> user = new HashMap<>();
        user.put("username", username);
        user.put("email", email);
        user.put("role", "student");

        db.collection("users").document(userId)
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(RegisterActivity.this, "Account created successfully!", Toast.LENGTH_SHORT).show();
                    navigateToHome();
                })
                .addOnFailureListener(e -> Toast.makeText(RegisterActivity.this, "Database Error", Toast.LENGTH_SHORT).show());
    }
}