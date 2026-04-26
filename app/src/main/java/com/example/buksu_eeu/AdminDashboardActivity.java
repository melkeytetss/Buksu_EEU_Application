package com.example.buksu_eeu;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.messaging.FirebaseMessaging;

public class AdminDashboardActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private DrawerLayout drawer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        drawer = findViewById(R.id.drawer_layout);
        ImageView menuIcon = findViewById(R.id.menu_icon);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        menuIcon.setOnClickListener(v -> drawer.openDrawer(GravityCompat.START));
        
        loadDashboardStats();
    }

    private void loadDashboardStats() {
        com.google.firebase.firestore.FirebaseFirestore db = com.google.firebase.firestore.FirebaseFirestore.getInstance();
        android.widget.TextView tvSales = findViewById(R.id.tv_total_sales);
        android.widget.TextView tvOrders = findViewById(R.id.tv_total_orders);
        android.widget.TextView tvProducts = findViewById(R.id.tv_total_products);

        // Fetch Orders for Sales and Count
        db.collection("orders").addSnapshotListener((value, error) -> {
            if (error != null || value == null) return;
            
            double totalSales = 0;
            int totalOrders = value.size();
            
            for (com.google.firebase.firestore.DocumentSnapshot doc : value.getDocuments()) {
                Double price = doc.getDouble("totalPrice");
                if (price != null) totalSales += price;
            }
            
            tvSales.setText(String.format("₱%,.0f", totalSales));
            tvOrders.setText(String.valueOf(totalOrders));
        });

        // Fetch Products Count
        db.collection("products").whereEqualTo("archived", false).addSnapshotListener((value, error) -> {
            if (error != null || value == null) return;
            tvProducts.setText(String.valueOf(value.size()));
        });
    }


    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_dashboard) {
            // Already here
        } else if (id == R.id.nav_products) {
            startActivity(new Intent(this, AdminActivity.class));
            finish();
        } else if (id == R.id.nav_orders) {
            startActivity(new Intent(this, AdminOrdersActivity.class));
            finish();
        } else if (id == R.id.nav_admin_logout) {
            logoutAdmin();
        }
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void logoutAdmin() {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this, R.style.DarkAlertDialog)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> {
                    FirebaseAuth.getInstance().signOut();
                    Intent intent = new Intent(AdminDashboardActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            new com.google.android.material.dialog.MaterialAlertDialogBuilder(this, R.style.DarkAlertDialog)
                    .setTitle("Exit App")
                    .setMessage("Are you sure you want to exit?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        finishAffinity();
                    })
                    .setNegativeButton("No", null)
                    .show();
        }
    }
}