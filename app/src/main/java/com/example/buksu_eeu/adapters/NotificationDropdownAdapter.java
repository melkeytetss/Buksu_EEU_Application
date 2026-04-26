package com.example.buksu_eeu;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationDropdownAdapter extends RecyclerView.Adapter<NotificationDropdownAdapter.NotificationViewHolder> {

    private List<NotificationModel> notifications;
    private OnNotificationClickListener listener;

    public interface OnNotificationClickListener {
        void onDeleteClick(NotificationModel notification, int position);
    }

    public NotificationDropdownAdapter(List<NotificationModel> notifications, OnNotificationClickListener listener) {
        this.notifications = notifications;
        this.listener = listener;
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.notification_item, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        NotificationModel notification = notifications.get(position);
        holder.bind(notification, position);
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    public void updateNotifications(List<NotificationModel> newNotifications) {
        this.notifications = newNotifications;
        notifyDataSetChanged();
    }

    class NotificationViewHolder extends RecyclerView.ViewHolder {
        TextView title, message, timestamp;
        ImageView deleteBtn;

        NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.notification_title);
            message = itemView.findViewById(R.id.notification_message);
            timestamp = itemView.findViewById(R.id.notification_timestamp);
            deleteBtn = itemView.findViewById(R.id.btn_delete_notification);
        }

        void bind(NotificationModel notification, int position) {
            title.setText(notification.getTitle());
            message.setText(notification.getMessage());
            timestamp.setText(formatTimestamp(notification.getTimestamp()));

            deleteBtn.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeleteClick(notification, position);
                }
            });
        }

        private String formatTimestamp(long timestamp) {
            long currentTime = System.currentTimeMillis();
            long diffMs = currentTime - timestamp;
            long diffMins = diffMs / (1000 * 60);
            long diffHours = diffMs / (1000 * 60 * 60);
            long diffDays = diffMs / (1000 * 60 * 60 * 24);

            if (diffMins < 1) {
                return "just now";
            } else if (diffMins < 60) {
                return diffMins + " mins ago";
            } else if (diffHours < 24) {
                return diffHours + " hours ago";
            } else if (diffDays < 7) {
                return diffDays + " days ago";
            } else {
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                return sdf.format(new Date(timestamp));
            }
        }
    }
}
