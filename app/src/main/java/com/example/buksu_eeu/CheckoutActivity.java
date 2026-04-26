package com.example.buksu_eeu;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CheckoutActivity extends AppCompatActivity {

    private TextInputEditText nameInput, emailInput, phoneInput;
    private MaterialButton btnSaveInfo, btnPlaceOrder;
    private MaterialCardView infoSummaryCard;
    private TextView tvSummaryName, tvSummaryEmail, tvSummaryPhone, totalPriceText;
    private RecyclerView recyclerView;
    
    private FirebaseFirestore db;
    private String savedName, savedEmail, savedPhone;
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "PickupPrefs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);

        db = FirebaseFirestore.getInstance();
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        
        initViews();
        setupOrderSummary();
        loadSavedInformation();

        btnSaveInfo.setOnClickListener(v -> saveInformation());
        btnPlaceOrder.setOnClickListener(v -> showFinalConfirmation());
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar_checkout);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        nameInput = findViewById(R.id.checkout_name);
        emailInput = findViewById(R.id.checkout_email);
        phoneInput = findViewById(R.id.checkout_phone);
        btnSaveInfo = findViewById(R.id.btn_save_info);
        btnPlaceOrder = findViewById(R.id.btn_place_order);
        
        infoSummaryCard = findViewById(R.id.info_summary_card);
        tvSummaryName = findViewById(R.id.tv_summary_name);
        tvSummaryEmail = findViewById(R.id.tv_summary_email);
        tvSummaryPhone = findViewById(R.id.tv_summary_phone);
        
        totalPriceText = findViewById(R.id.checkout_total_price);
        recyclerView = findViewById(R.id.recycler_checkout_items);
    }


    private void setupOrderSummary() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new CheckoutItemAdapter(this, CartManager.getInstance().getCartItems()));
        
        double total = CartManager.getInstance().getTotalPrice();
        totalPriceText.setText(String.format("₱%.0f", total));
    }

    private void loadSavedInformation() {
        String name = sharedPreferences.getString("name", "");
        String email = sharedPreferences.getString("email", "");
        String phone = sharedPreferences.getString("phone", "");

        if (!TextUtils.isEmpty(name)) {
            nameInput.setText(name);
            emailInput.setText(email);
            phoneInput.setText(phone);

            // Auto-lock if we have saved data
            saveInformation();
        }
    }

    private void saveInformation() {
        if (nameInput.getText() == null || emailInput.getText() == null || phoneInput.getText() == null) return;

        savedName = nameInput.getText().toString().trim();
        savedEmail = emailInput.getText().toString().trim();
        savedPhone = phoneInput.getText().toString().trim();

        if (TextUtils.isEmpty(savedName) || TextUtils.isEmpty(savedEmail) || TextUtils.isEmpty(savedPhone)) {
            showCustomToast("Please fill all fields", true);
            return;
        }

        // Save to SharedPreferences
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("name", savedName);
        editor.putString("email", savedEmail);
        editor.putString("phone", savedPhone);
        editor.apply();

        tvSummaryName.setText(savedName);
        tvSummaryEmail.setText(savedEmail);
        tvSummaryPhone.setText(savedPhone);
        
        infoSummaryCard.setVisibility(View.VISIBLE);
        btnPlaceOrder.setEnabled(true);
        showCustomToast("Pickup details locked", false);
    }

    private void showFinalConfirmation() {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this, R.style.DarkAlertDialog)
                .setTitle("Confirm Your Order")
                .setMessage("Grand Total: " + totalPriceText.getText().toString() + "\nAre you sure you want to place this order?")
                .setPositiveButton("Place Order", (dialog, which) -> placeOrderInFirestore())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void placeOrderInFirestore() {
        String userId = FirebaseAuth.getInstance().getUid();
        List<CartItem> items = CartManager.getInstance().getCartItems();
        
        if (items.isEmpty()) return;

        db.runTransaction(transaction -> {
            // Phase 1: READ EVERYTHING FIRST
            List<com.google.firebase.firestore.DocumentSnapshot> productDocs = new ArrayList<>();
            for (CartItem item : items) {
                com.google.firebase.firestore.DocumentReference productRef = db.collection("products").document(item.getProduct().getId());
                productDocs.add(transaction.get(productRef));
            }

            // Phase 2: VALIDATE AND WRITE EVERYTHING
            for (int i = 0; i < items.size(); i++) {
                CartItem item = items.get(i);
                com.google.firebase.firestore.DocumentSnapshot productDoc = productDocs.get(i);
                
                Long currentStockObj = productDoc.getLong("stock");
                long currentStock = currentStockObj != null ? currentStockObj : 0;
                
                if (currentStock < item.getQuantity()) {
                    throw new com.google.firebase.firestore.FirebaseFirestoreException(
                        "Insufficient stock for " + item.getProduct().getName(),
                        com.google.firebase.firestore.FirebaseFirestoreException.Code.ABORTED
                    );
                }
                
                // Decrease stock
                transaction.update(productDoc.getReference(), "stock", currentStock - item.getQuantity());
            }

            // 3. Create the order
            Map<String, Object> order = new HashMap<>();
            order.put("userId", userId);
            order.put("customerName", savedName);
            order.put("customerEmail", savedEmail);
            order.put("customerPhone", savedPhone);
            order.put("totalPrice", CartManager.getInstance().getTotalPrice());
            order.put("timestamp", System.currentTimeMillis());
            order.put("status", "Pending");

            List<Map<String, Object>> itemMaps = new ArrayList<>();
            for (CartItem item : items) {
                Product product = item.getProduct();
                Map<String, Object> itemMap = new HashMap<>();
                itemMap.put("productId", product.getId());
                itemMap.put("name", product.getName());
                itemMap.put("quantity", item.getQuantity());
                itemMap.put("price", product.getPrice());
                if (item.getSize() != null && !item.getSize().isEmpty()) {
                    itemMap.put("size", item.getSize());
                }
                itemMaps.add(itemMap);
            }
            order.put("items", itemMaps);

            com.google.firebase.firestore.DocumentReference orderRef = db.collection("orders").document();
            transaction.set(orderRef, order);

            return null;
        }).addOnSuccessListener(result -> {
            sendNotification("admin", "New Order Received", "A new order has been placed by " + savedName);
            CartManager.getInstance().clearCart();
            showCustomToast("Order placed successfully!", false);
            
            Intent intent = new Intent(this, NavContainerActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        }).addOnFailureListener(e -> {
            String message = e.getMessage() != null ? e.getMessage() : "Failed to place order";
            showCustomToast(message, true);
        });
    }

    private void showCustomToast(String message, boolean isError) {
        LayoutInflater inflater = getLayoutInflater();
        View layout = inflater.inflate(R.layout.custom_toast_add_to_cart, null);
        
        com.google.android.material.card.MaterialCardView card = (com.google.android.material.card.MaterialCardView) layout;
        if (isError) card.setCardBackgroundColor(getResources().getColor(R.color.tag_red));

        TextView text = layout.findViewById(R.id.toast_message);
        text.setText(message);

        Toast toast = new Toast(getApplicationContext());
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(layout);
        toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 100);
        toast.show();
    }

    private void sendNotification(String recipientId, String title, String message) {
        NotificationModel notification = new NotificationModel(recipientId, title, message);
        db.collection("notifications").add(notification);
    }
}