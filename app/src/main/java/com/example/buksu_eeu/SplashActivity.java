package com.example.buksu_eeu;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Using the logo background from the theme
        
        new Handler().postDelayed(() -> {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) {
                startActivity(new Intent(SplashActivity.this, MainActivity.class));
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
        }, 2000); // 2 seconds delay
    }

    private void checkUserRoleAndNavigate(FirebaseUser user) {
        FirebaseFirestore.getInstance().collection("users").document(user.getUid()).get()
            .addOnSuccessListener(doc -> {
                if (doc.exists() && "admin".equals(doc.getString("role"))) {
                    startActivity(new Intent(SplashActivity.this, AdminNavContainerActivity.class));
                } else {
                        startActivity(new Intent(SplashActivity.this, NavContainerActivity.class));
                }
                finish();
            })
            .addOnFailureListener(e -> {
                    startActivity(new Intent(SplashActivity.this, NavContainerActivity.class));
                finish();
            });
    }
}
