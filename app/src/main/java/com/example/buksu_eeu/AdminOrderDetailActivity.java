package com.example.buksu_eeu;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AdminOrderDetailActivity extends AppCompatActivity {

    private TextView customerName, customerEmail, customerPhone, orderStatus, totalPrice;
    private RecyclerView recyclerItems;
    private DetailItemAdapter itemAdapter;
    private List<Map<String, Object>> itemsList = new ArrayList<>();
    private FirebaseFirestore db;
    private String orderId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_order_detail);

        db = FirebaseFirestore.getInstance();
        orderId = getIntent().getStringExtra("orderId");

        Toolbar toolbar = findViewById(R.id.toolbar_order_detail);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            getSupportActionBar().setTitle("");
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        customerName = findViewById(R.id.detail_customer_name);
        customerEmail = findViewById(R.id.detail_customer_email);
        customerPhone = findViewById(R.id.detail_customer_phone);
        orderStatus = findViewById(R.id.detail_order_status);
        totalPrice = findViewById(R.id.detail_total_price);
        recyclerItems = findViewById(R.id.recycler_detail_items);

        recyclerItems.setLayoutManager(new LinearLayoutManager(this));
        itemAdapter = new DetailItemAdapter(itemsList);
        recyclerItems.setAdapter(itemAdapter);

        if (orderId != null) {
            loadOrderDetails();
        } else {
            Toast.makeText(this, "Error: Missing Order ID", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void loadOrderDetails() {
        db.collection("orders").document(orderId).addSnapshotListener((doc, error) -> {
            if (error != null) {
                Toast.makeText(this, "Failed to load order: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                return;
            }
            if (doc != null && doc.exists()) {
                Order order = doc.toObject(Order.class);
                if (order != null) {
                    customerName.setText(order.getCustomerName() != null ? order.getCustomerName() : "N/A");
                    customerEmail.setText(order.getCustomerEmail() != null ? order.getCustomerEmail() : "N/A");
                    customerPhone.setText(order.getCustomerPhone() != null ? order.getCustomerPhone() : "N/A");
                    
                    orderStatus.setText(order.getStatus() != null ? order.getStatus() : "Unknown");
                    totalPrice.setText(String.format("₱%,.0f", order.getTotalPrice()));

                    itemsList.clear();
                    if (order.getItems() != null) {
                        itemsList.addAll(order.getItems());
                    }
                    itemAdapter.notifyDataSetChanged();
                }
            }
        });
    }

    private static class DetailItemAdapter extends RecyclerView.Adapter<DetailItemAdapter.ViewHolder> {

        private final List<Map<String, Object>> items;

        public DetailItemAdapter(List<Map<String, Object>> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_order_detail_product, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Map<String, Object> item = items.get(position);
            
            String name = (String) item.get("name");
            Long qtyObj = (Long) item.get("quantity");
            Double priceObj = (Double) item.get("price");
            
            int qty = qtyObj != null ? qtyObj.intValue() : 1;
            double price = priceObj != null ? priceObj : 0.0;
            double subtotal = price * qty;

            holder.nameText.setText(name != null ? name : "Unknown Product");
            holder.priceText.setText(String.format("₱%,.0f each", price));
            holder.quantityText.setText("x" + qty);
            holder.subtotalText.setText(String.format("₱%,.0f", subtotal));

            // Show size if present
            String size = (String) item.get("size");
            if (size != null && !size.isEmpty()) {
                holder.sizeText.setVisibility(View.VISIBLE);
                holder.sizeText.setText("Size: " + size);
            } else {
                holder.sizeText.setVisibility(View.GONE);
            }
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView nameText, priceText, quantityText, subtotalText, sizeText;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                nameText = itemView.findViewById(R.id.detail_item_name);
                priceText = itemView.findViewById(R.id.detail_item_price);
                sizeText = itemView.findViewById(R.id.detail_item_size);
                quantityText = itemView.findViewById(R.id.detail_item_quantity);
                subtotalText = itemView.findViewById(R.id.detail_item_subtotal);
            }
        }
    }
}
