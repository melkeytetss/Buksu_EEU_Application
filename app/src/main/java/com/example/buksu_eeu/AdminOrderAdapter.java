package com.example.buksu_eeu;

import android.content.Context;
import android.content.Intent;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.List;
import java.util.Map;

public class AdminOrderAdapter extends RecyclerView.Adapter<AdminOrderAdapter.OrderViewHolder> {

    private Context context;
    private List<Order> orderList;
    private FirebaseFirestore db;

    public AdminOrderAdapter(Context context, List<Order> orderList) {
        this.context = context;
        this.orderList = orderList;
        this.db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_order_admin, parent, false);
        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        Order order = orderList.get(position);
        holder.customerName.setText(order.getCustomerName());
        holder.status.setText(order.getStatus());
        holder.totalPrice.setText(String.format("₱%.0f", order.getTotalPrice()));

        updateStatusBadge(holder.status, order.getStatus());

        StringBuilder itemsSummary = new StringBuilder("Items: ");
        if (order.getItems() != null) {
            for (Map<String, Object> item : order.getItems()) {
                itemsSummary.append(item.get("name")).append(" (x").append(item.get("quantity")).append("), ");
            }
        }
        String summary = itemsSummary.toString();
        if (summary.endsWith(", ")) {
            summary = summary.substring(0, summary.length() - 2);
        }
        holder.itemsSummary.setText(summary);

        holder.updateStatusBtn.setOnClickListener(v -> showStatusDialog(order));
        
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, AdminOrderDetailActivity.class);
            intent.putExtra("orderId", order.getOrderId());
            context.startActivity(intent);
        });
    }

    private void updateStatusBadge(TextView statusView, String status) {
        int color;
        switch (status) {
            case "Confirmed the order":
                color = context.getResources().getColor(R.color.link_blue);
                break;
            case "Ready to pickup":
                color = context.getResources().getColor(R.color.header_yellow);
                break;
            case "Picked Up":
                color = context.getResources().getColor(R.color.stock_green);
                break;
            default:
                color = context.getResources().getColor(R.color.tag_red);
                break;
        }
        statusView.getBackground().setTint(color);
    }

    private void showStatusDialog(Order order) {
        String currentStatus = order.getStatus();
        String nextStatus;
        String confirmMessage;

        switch (currentStatus) {
            case "Pending":
                nextStatus = "Confirmed the order";
                confirmMessage = "Confirm this order?";
                break;
            case "Confirmed the order":
                nextStatus = "Ready to pickup";
                confirmMessage = "Mark this order as ready for pickup?";
                break;
            case "Ready to pickup":
                nextStatus = "Picked Up";
                confirmMessage = "Mark this order as picked up (completed)?";
                break;
            default:
                Toast.makeText(context, "This order is already completed", Toast.LENGTH_SHORT).show();
                return;
        }

        new MaterialAlertDialogBuilder(context, R.style.DarkAlertDialog)
                .setTitle("Update Order Status")
                .setMessage(confirmMessage + "\n\nCurrent: " + currentStatus + "\nNext: " + nextStatus)
                .setPositiveButton(nextStatus, (dialog, which) -> updateStatusInFirestore(order, nextStatus))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateStatusInFirestore(Order order, String newStatus) {
        db.collection("orders").document(order.getOrderId())
                .update("status", newStatus)
                .addOnSuccessListener(aVoid -> {
                    // Create Notification for User
                    String userId = order.getUserId();
                    if (userId != null) {
                        sendNotification(userId, "Order Status Updated", "Your order is now: " + newStatus);
                    }
                    Toast.makeText(context, "Status updated to: " + newStatus, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(context, "Error updating status", Toast.LENGTH_SHORT).show());
    }

    private void sendNotification(String recipientId, String title, String message) {
        NotificationModel notification = new NotificationModel(recipientId, title, message);
        db.collection("notifications").add(notification);
    }

    @Override
    public int getItemCount() {
        return orderList.size();
    }

    public static class OrderViewHolder extends RecyclerView.ViewHolder {
        TextView customerName, status, itemsSummary, totalPrice;
        MaterialButton updateStatusBtn;

        public OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            customerName = itemView.findViewById(R.id.order_customer_name);
            status = itemView.findViewById(R.id.order_status);
            itemsSummary = itemView.findViewById(R.id.order_items_summary);
            totalPrice = itemView.findViewById(R.id.order_total_price);
            updateStatusBtn = itemView.findViewById(R.id.btn_update_status);
        }
    }
}