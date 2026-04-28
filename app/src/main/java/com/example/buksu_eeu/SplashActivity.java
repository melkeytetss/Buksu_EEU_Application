package com.example.buksu_eeu;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        View logoContainer = findViewById(R.id.logo_container);
        View appName = findViewById(R.id.app_name_text);
        View tagline = findViewById(R.id.app_tagline);
        View progress = findViewById(R.id.splash_progress);

        // Cinematic Animations
        logoContainer.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(1000)
            .setStartDelay(200)
            .start();

        appName.animate()
            .alpha(1f)
            .setDuration(800)
            .setStartDelay(600)
            .start();

        tagline.animate()
            .alpha(0.6f)
            .setDuration(800)
            .setStartDelay(800)
            .start();

        progress.animate()
            .alpha(1f)
            .setDuration(500)
            .setStartDelay(1000)
            .start();

        new Handler().postDelayed(() -> {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) {
                startActivity(new Intent(SplashActivity.this, MainActivity.class));
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                finish();
            } else {
                com.google.firebase.messaging.FirebaseMessaging.getInstance().getToken()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null) {
                            String token = task.getResult();
                            FirebaseFirestore.getInstance().collection("users")
                                    .document(user.getUid())
                                    .update("fcmToken", token);
                        }
                        checkUserRoleAndNavigate(user);
                    });
            }
        }, 3000); // Increased to 3 seconds for animation to breathe
    }

    private void checkUserRoleAndNavigate(FirebaseUser user) {
        FirebaseFirestore.getInstance().collection("users").document(user.getUid()).get()
            .addOnSuccessListener(doc -> {
                if (doc.exists() && "admin".equals(doc.getString("role"))) {
                    startActivity(new Intent(SplashActivity.this, AdminNavContainerActivity.class));
                } else {
                        startActivity(new Intent(SplashActivity.this, NavContainerActivity.class));
                }
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                finish();
            })
            .addOnFailureListener(e -> {
                    startActivity(new Intent(SplashActivity.this, NavContainerActivity.class));
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                finish();
            });
    }
}
