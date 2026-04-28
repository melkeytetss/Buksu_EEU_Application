package com.example.buksu_eeu;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private EditText emailInput, passwordInput, usernameInput, phoneInput;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private TurnstileView turnstileView;
    private String turnstileToken = null;

    // Cloudflare Turnstile Site Key
    private static final String TURNSTILE_SITE_KEY = "0x4AAAAAACu-FqwwbkqutV5U";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        emailInput = findViewById(R.id.email_input);
        passwordInput = findViewById(R.id.password_input);
        usernameInput = findViewById(R.id.username_input);
        phoneInput = findViewById(R.id.phone_input);
        Button registerBtn = findViewById(R.id.register_btn);
        
        // Initialize Cloudflare Turnstile
        turnstileView = findViewById(R.id.cloudflare_turnstile_register);
        turnstileView.setSiteKey(TURNSTILE_SITE_KEY);
        
        turnstileView.setOnTokenResolvedListener(token -> {
            turnstileToken = token;
            showCustomToast("Security Check Passed", false);
        });

        turnstileView.setOnFailureListener(error -> {
            turnstileToken = null;
            showCustomToast("Security Check Failed: " + error.getMessage(), true);
        });

        TextView loginLink = findViewById(R.id.login_link_text);
        loginLink.setText(Html.fromHtml(getString(R.string.register_prompt), Html.FROM_HTML_MODE_COMPACT));
        loginLink.setOnClickListener(v -> finish());

        registerBtn.setOnClickListener(v -> {
            if (turnstileToken != null) {
                registerUser();
            } else {
                showCustomToast("Please complete the security check", true);
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
        String phone = phoneInput.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password) || TextUtils.isEmpty(username) || TextUtils.isEmpty(phone)) {
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
                        saveUserToFirestore(userId, username, email, phone);
                    } else {
                        String errorMsg = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                        Toast.makeText(RegisterActivity.this, "Registration failed: " + errorMsg, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveUserToFirestore(String userId, String username, String email, String phone) {
        Map<String, Object> user = new HashMap<>();
        user.put("username", username);
        user.put("email", email);
        user.put("phone", phone);
        user.put("role", "student");

        db.collection("users").document(userId)
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    showCustomToast("Account created successfully!", false);
                    navigateToHome();
                })
                .addOnFailureListener(e -> showCustomToast("Database Error", true));
    }

    private void showCustomToast(String message, boolean isError) {
        LayoutInflater inflater = getLayoutInflater();
        View layout;
        if (isError) {
            layout = inflater.inflate(R.layout.custom_toast_add_to_cart, null);
            com.google.android.material.card.MaterialCardView card = (com.google.android.material.card.MaterialCardView) layout;
            card.setCardBackgroundColor(getResources().getColor(R.color.tag_red));
        } else {
            layout = inflater.inflate(R.layout.custom_toast_success, null);
        }

        TextView text = layout.findViewById(R.id.toast_message);
        text.setText(message);

        Toast toast = new Toast(getApplicationContext());
        toast.setDuration(Toast.LENGTH_LONG);
        toast.setView(layout);
        toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 100);
        toast.show();
    }
}