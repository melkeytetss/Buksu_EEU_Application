package com.example.buksu_eeu;

import android.content.Context;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import android.content.Context;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class UserOrderAdapter extends RecyclerView.Adapter<UserOrderAdapter.UserOrderViewHolder> {

    private Context context;
    private List<Order> orderList;

    public UserOrderAdapter(Context context, List<Order> orderList) {
        this.context = context;
        this.orderList = orderList;
    }

    @NonNull
    @Override
    public UserOrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_order_user, parent, false);
        return new UserOrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserOrderViewHolder holder, int position) {
        Order order = orderList.get(position);

        // Format Date
        Calendar cal = Calendar.getInstance(Locale.ENGLISH);
        cal.setTimeInMillis(order.getTimestamp());
        String date = DateFormat.format("dd/MM/yyyy", cal).toString();
        holder.tvDate.setText("Date: " + date);

        holder.tvStatus.setText(order.getStatus());
        holder.tvTotal.setText(String.format("₱%.0f", order.getTotalPrice()));

        // Update status color
        updateStatusColor(holder.tvStatus, order.getStatus());

        // Build items summary
        StringBuilder summary = new StringBuilder();
        if (order.getItems() != null) {
            for (Map<String, Object> item : order.getItems()) {
                summary.append(item.get("name")).append(" (x").append(item.get("quantity")).append("), ");
            }
        }
        String finalSummary = summary.toString();
        if (finalSummary.endsWith(", ")) {
            finalSummary = finalSummary.substring(0, finalSummary.length() - 2);
        }
        holder.tvItems.setText(finalSummary);

        if ("Pending".equals(order.getStatus())) {
            holder.btnCancel.setVisibility(View.VISIBLE);
            holder.btnCancel.setText("Cancel Order");
            holder.btnCancel.setTextColor(context.getResources().getColor(R.color.tag_red));
            holder.btnCancel.setOnClickListener(v -> showCancelConfirmation(order));
        } else if ("Cancelled".equals(order.getStatus()) || "Completed".equals(order.getStatus()) || "Picked Up".equals(order.getStatus())) {
            holder.btnCancel.setVisibility(View.VISIBLE);
            holder.btnCancel.setText("Delete Record");
            holder.btnCancel.setTextColor(context.getResources().getColor(R.color.text_grey));
            holder.btnCancel.setOnClickListener(v -> showDeleteConfirmation(order));
        } else {
            holder.btnCancel.setVisibility(View.GONE);
        }

        // Tap card to see full order detail
        holder.itemView.setOnClickListener(v -> showOrderDetailSheet(order));
    }

    private void showOrderDetailSheet(Order order) {
        BottomSheetDialog dialog = new BottomSheetDialog(context, R.style.BottomSheetDialogTheme);
        View sheetView = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_order_detail, null);
        dialog.setContentView(sheetView);

        // Header
        Calendar cal = Calendar.getInstance(Locale.ENGLISH);
        cal.setTimeInMillis(order.getTimestamp());
        String date = DateFormat.format("dd/MM/yyyy", cal).toString();
        ((TextView) sheetView.findViewById(R.id.tv_detail_date)).setText("Date: " + date);

        TextView statusView = sheetView.findViewById(R.id.tv_detail_status);
        statusView.setText(order.getStatus());
        updateStatusColor(statusView, order.getStatus());

        ((TextView) sheetView.findViewById(R.id.tv_detail_total))
                .setText(String.format("₱%.0f", order.getTotalPrice()));

        // Items list
        RecyclerView recycler = sheetView.findViewById(R.id.recycler_order_detail_items);
        recycler.setLayoutManager(new LinearLayoutManager(context));
        recycler.setAdapter(new OrderDetailAdapter(order.getItems()));

        dialog.show();

        // Expand sheet fully so user can scroll all items
        View bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheet != null) {
            BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            behavior.setSkipCollapsed(true);
        }
    }

    private void showCancelConfirmation(Order order) {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(context)
                .setTitle("Cancel Order")
                .setMessage("Are you sure you want to cancel this order? Stock will be returned to the inventory.")
                .setPositiveButton("Yes, Cancel", (dialog, which) -> cancelOrderInFirestore(order))
                .setNegativeButton("No", null)
                .show();
    }

    private void cancelOrderInFirestore(Order order) {
        com.google.firebase.firestore.FirebaseFirestore db = com.google.firebase.firestore.FirebaseFirestore.getInstance();
        
        db.runTransaction(transaction -> {
            // 1. Return stock
            if (order.getItems() != null) {
                for (Map<String, Object> item : order.getItems()) {
                    String productId = (String) item.get("productId");
                    long quantity = ((Number) item.get("quantity")).longValue();
                    
                    com.google.firebase.firestore.DocumentReference productRef = db.collection("products").document(productId);
                    com.google.firebase.firestore.DocumentSnapshot productDoc = transaction.get(productRef);
                    
                    if (productDoc.exists()) {
                        long currentStock = productDoc.getLong("stock");
                        transaction.update(productRef, "stock", currentStock + quantity);
                    }
                }
            }

            // 2. Update order status
            com.google.firebase.firestore.DocumentReference orderRef = db.collection("orders").document(order.getOrderId());
            transaction.update(orderRef, "status", "Cancelled");

            return null;
        }).addOnSuccessListener(aVoid -> {
            android.widget.Toast.makeText(context, "Order cancelled and stock returned", android.widget.Toast.LENGTH_SHORT).show();
        }).addOnFailureListener(e -> {
            android.widget.Toast.makeText(context, "Failed to cancel order: " + e.getMessage(), android.widget.Toast.LENGTH_SHORT).show();
        });
    }

    private void showDeleteConfirmation(Order order) {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(context)
                .setTitle("Delete Order Record")
                .setMessage("Are you sure you want to remove this order from your history? This cannot be undone.")
                .setPositiveButton("Yes, Delete", (dialog, which) -> deleteOrderInFirestore(order))
                .setNegativeButton("No", null)
                .show();
    }

    private void deleteOrderInFirestore(Order order) {
        com.google.firebase.firestore.FirebaseFirestore db = com.google.firebase.firestore.FirebaseFirestore.getInstance();
        db.collection("orders").document(order.getOrderId()).delete()
                .addOnSuccessListener(aVoid -> {
                    android.widget.Toast.makeText(context, "Order record deleted", android.widget.Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    android.widget.Toast.makeText(context, "Failed to delete record: " + e.getMessage(), android.widget.Toast.LENGTH_SHORT).show();
                });
    }

    private void updateStatusColor(TextView statusView, String status) {
        int color;
        switch (status) {
            case "Confirmed the order":
            case "Completed":
                color = context.getResources().getColor(R.color.success_glow);
                break;
            case "Ready to pickup":
                color = context.getResources().getColor(R.color.gold_accent);
                break;
            case "Picked Up":
                color = context.getResources().getColor(R.color.electric_blue);
                break;
            case "Cancelled":
                color = context.getResources().getColor(R.color.tag_red);
                break;
            default:
                color = context.getResources().getColor(R.color.gold_accent); // Pending/Default
                break;
        }
        statusView.setTextColor(color);
    }

    @Override
    public int getItemCount() {
        return orderList.size();
    }

    public static class UserOrderViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate, tvStatus, tvItems, tvTotal;
        android.widget.Button btnCancel;

        public UserOrderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tv_order_date);
            tvStatus = itemView.findViewById(R.id.tv_order_status);
            tvItems = itemView.findViewById(R.id.tv_order_items);
            tvTotal = itemView.findViewById(R.id.tv_order_total);
            btnCancel = itemView.findViewById(R.id.btn_cancel_order);
        }
    }

    // ── Inner adapter for order detail sheet ──────────────────────────────────
    private class OrderDetailAdapter extends RecyclerView.Adapter<OrderDetailAdapter.DetailVH> {

        private final List<Map<String, Object>> items;

        OrderDetailAdapter(List<Map<String, Object>> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public DetailVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(context).inflate(R.layout.item_student_order_detail, parent, false);
            return new DetailVH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull DetailVH holder, int position) {
            Map<String, Object> item = items.get(position);
            String name = (String) item.get("name");
            String size = (String) item.get("size");
            String productId = (String) item.get("productId");
            Long qtyObj = (Long) item.get("quantity");
            Double priceObj = (Double) item.get("price");

            int qty = qtyObj != null ? qtyObj.intValue() : 1;
            double price = priceObj != null ? priceObj : 0.0;

            holder.tvName.setText(name != null ? name : "Unknown");
            holder.tvPrice.setText(String.format("₱%.0f each", price));
            holder.tvQty.setText("x" + qty);
            holder.tvSubtotal.setText(String.format("₱%.0f", price * qty));

            if (size != null && !size.isEmpty()) {
                holder.tvSize.setVisibility(View.VISIBLE);
                holder.tvSize.setText("Size: " + size);
            } else {
                holder.tvSize.setVisibility(View.GONE);
            }

            // Fetch product image from Firestore using productId
            holder.imgItem.setImageResource(R.mipmap.ic_launcher); // placeholder first
            if (productId != null) {
                FirebaseFirestore.getInstance().collection("products").document(productId)
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            String imageUrl = doc.getString("imageUrl");
                            if (imageUrl != null && !imageUrl.isEmpty()) {
                                Glide.with(context)
                                    .load(imageUrl)
                                    .placeholder(R.mipmap.ic_launcher)
                                    .into(holder.imgItem);
                            }
                        }
                    });
            }
        }

        @Override
        public int getItemCount() {
            return items != null ? items.size() : 0;
        }

        class DetailVH extends RecyclerView.ViewHolder {
            ImageView imgItem;
            TextView tvName, tvSize, tvPrice, tvQty, tvSubtotal;

            DetailVH(@NonNull View itemView) {
                super(itemView);
                imgItem = itemView.findViewById(R.id.img_student_order_item);
                tvName = itemView.findViewById(R.id.tv_student_order_item_name);
                tvSize = itemView.findViewById(R.id.tv_student_order_item_size);
                tvPrice = itemView.findViewById(R.id.tv_student_order_item_price);
                tvQty = itemView.findViewById(R.id.tv_student_order_item_qty);
                tvSubtotal = itemView.findViewById(R.id.tv_student_order_item_subtotal);
            }
        }
    }
}