package com.example.buksu_eeu;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.List;

public class AdminActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private DrawerLayout drawer;
    private RecyclerView recyclerView;
    private AdminProductAdapter adapter;
    private List<Product> productList;
    private FirebaseFirestore db;
    private View notificationIndicator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        db = FirebaseFirestore.getInstance();
        
        drawer = findViewById(R.id.drawer_layout);
        ImageView menuIcon = findViewById(R.id.menu_icon);
        
        // Fix: Access notification bell from the included layout
        View notificationLayout = findViewById(R.id.admin_notification_layout);
        ImageView notificationBell = notificationLayout.findViewById(R.id.notification_bell);
        notificationIndicator = notificationLayout.findViewById(R.id.notification_indicator);
        
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        menuIcon.setOnClickListener(v -> drawer.openDrawer(GravityCompat.START));
        
        notificationBell.setOnClickListener(v -> {
            startActivity(new Intent(AdminActivity.this, NotificationsActivity.class));
        });

        MaterialButton addProductBtn = findViewById(R.id.add_product_btn);
        addProductBtn.setOnClickListener(v -> {
            startActivity(new Intent(AdminActivity.this, AddProductActivity.class));
        });

        setupRecyclerView();
        loadProductsFromFirestore();
        listenForNotifications();
    }

    private void setupRecyclerView() {
        recyclerView = findViewById(R.id.recycler_products_admin);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        productList = new ArrayList<>();
        adapter = new AdminProductAdapter(this, productList);
        recyclerView.setAdapter(adapter);
    }

    private void loadProductsFromFirestore() {
        db.collection("products")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    if (value != null) {
                        productList.clear();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            Product product = doc.toObject(Product.class);
                            if (product != null) {
                                product.setId(doc.getId());
                                productList.add(product);
                            }
                        }
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    private void listenForNotifications() {
        db.collection("notifications")
                .whereEqualTo("recipientId", "admin")
                .whereEqualTo("read", false)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    if (value != null && !value.isEmpty()) {
                        notificationIndicator.setVisibility(View.VISIBLE);
                    } else {
                        notificationIndicator.setVisibility(View.GONE);
                    }
                });
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_dashboard) {
            startActivity(new Intent(this, AdminDashboardActivity.class));
            finish();
        } else if (id == R.id.nav_products) {
            // Already here
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
                    Intent intent = new Intent(AdminActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}