package com.example.buksu_eeu;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
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
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private EditText emailInput, passwordInput;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private GoogleSignInClient mGoogleSignInClient;
    private TurnstileView turnstileView;
    private String turnstileToken = null;
    private static final String TURNSTILE_SITE_KEY = "0x4AAAAAACu-FqwwbkqutV5U";
    private static final String TAG = "MainActivity_Auth";

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
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        if (mAuth.getCurrentUser() != null) {
            checkUserRoleAndNavigate(mAuth.getCurrentUser());
        }

        emailInput = findViewById(R.id.email_input);
        passwordInput = findViewById(R.id.password_input);
        Button loginBtn = findViewById(R.id.login_btn);
        MaterialButton googleBtn = findViewById(R.id.google_btn);

        // Navigation to Register
        TextView registerText = findViewById(R.id.register_text);
        registerText.setText(Html.fromHtml(getString(R.string.login_prompt), Html.FROM_HTML_MODE_COMPACT));
        registerText.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        // Forgot Password logic
        TextView forgotPassword = findViewById(R.id.forgot_password);
        forgotPassword.setOnClickListener(v -> showModernForgotPasswordSheet());

        // Initialize Cloudflare Turnstile
        turnstileView = findViewById(R.id.cloudflare_turnstile_login);
        turnstileView.setSiteKey(TURNSTILE_SITE_KEY);

        turnstileView.setOnTokenResolvedListener(token -> {
            turnstileToken = token;
            showCustomToast("Security Check Passed", false);
        });

        turnstileView.setOnFailureListener(error -> {
            turnstileToken = null;
            showCustomToast("Security Check Failed", true);
        });

        loginBtn.setOnClickListener(v -> {
            if (turnstileToken != null) {
                loginUser();
            } else {
                showCustomToast("Please complete the security check", true);
            }
        });

        googleBtn.setOnClickListener(v -> {
            // Force sign out to show account picker
            mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
                Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                googleSignInLauncher.launch(signInIntent);
            });
        });
    }

    private void checkUserRoleAndNavigate(FirebaseUser user) {
        db.collection("users").document(user.getUid()).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document != null && document.exists()) {
                            String role = document.getString("role");
                            if ("admin".equals(role)) {
                                startActivity(new Intent(MainActivity.this, AdminNavContainerActivity.class));
                            } else {
                                    startActivity(new Intent(MainActivity.this, NavContainerActivity.class));
                            }
                        } else {
                            createMissingUserRecord(user);
                        }
                        finish();
                    } else {
                        Toast.makeText(this, "Error checking role: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void createMissingUserRecord(FirebaseUser user) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("username", user.getDisplayName());
        userData.put("email", user.getEmail());
        userData.put("role", "student");
        db.collection("users").document(user.getUid()).set(userData)
                .addOnSuccessListener(aVoid -> {
                        startActivity(new Intent(MainActivity.this, NavContainerActivity.class));
                    finish();
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
                        Toast.makeText(MainActivity.this, "Google Auth failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkAndCreateUserRecord(FirebaseUser user) {
        db.collection("users").document(user.getUid()).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (!task.getResult().exists()) {
                            Map<String, Object> userData = new HashMap<>();
                            userData.put("username", user.getDisplayName());
                            userData.put("email", user.getEmail());
                            userData.put("role", "student");
                            db.collection("users").document(user.getUid()).set(userData)
                                    .addOnSuccessListener(aVoid -> navigateToHome());
                        } else {
                            checkUserRoleAndNavigate(user);
                        }
                    }
                });
    }

    private void navigateToHome() {
           startActivity(new Intent(MainActivity.this, NavContainerActivity.class));
        finish();
    }

    private void showModernForgotPasswordSheet() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this, R.style.BottomSheetDialogTheme);
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_forgot_password, null);
        bottomSheetDialog.setContentView(view);

        EditText fpEmailInput = view.findViewById(R.id.fp_email_input);
        Button btnSendReset = view.findViewById(R.id.btn_send_reset);

        // Pre-fill if they already typed something in the main login
        if (emailInput != null && !TextUtils.isEmpty(emailInput.getText())) {
            fpEmailInput.setText(emailInput.getText().toString());
        }

        btnSendReset.setOnClickListener(v -> {
            String email = fpEmailInput.getText().toString().trim();
            if (TextUtils.isEmpty(email)) {
                showCustomToast("Please enter your email", true);
                return;
            }

            mAuth.sendPasswordResetEmail(email)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            showCustomToast("Reset link sent to your email", false);
                            bottomSheetDialog.dismiss();
                        } else {
                            String error = task.getException() != null ? task.getException().getMessage() : "Failed to send email";
                            showCustomToast(error, true);
                        }
                    });
        });

        bottomSheetDialog.show();
    }

    private void showCustomToast(String message, boolean isError) {
        LayoutInflater inflater = getLayoutInflater();
        View layout = inflater.inflate(R.layout.custom_toast_add_to_cart, null);
        
        com.google.android.material.card.MaterialCardView card = (com.google.android.material.card.MaterialCardView) layout;
        if (isError) {
            card.setCardBackgroundColor(getResources().getColor(R.color.tag_red));
        } else {
            card.setCardBackgroundColor(getResources().getColor(R.color.card_navy));
        }

        TextView text = layout.findViewById(R.id.toast_message);
        text.setText(message);

        Toast toast = new Toast(getApplicationContext());
        toast.setDuration(Toast.LENGTH_LONG);
        toast.setView(layout);
        toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 100);
        toast.show();
    }

    private void loginUser() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        checkUserRoleAndNavigate(mAuth.getCurrentUser());
                    } else {
                        String errorMsg = task.getException() != null ? task.getException().getMessage() : "Authentication failed";
                        Toast.makeText(MainActivity.this, "Login Failed: " + errorMsg, Toast.LENGTH_LONG).show();
                    }
                });
    }
}