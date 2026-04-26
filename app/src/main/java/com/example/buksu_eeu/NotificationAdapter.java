package com.example.buksu_eeu;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotifViewHolder> {

    private Context context;
    private List<NotificationModel> notifications;
    private FirebaseFirestore db;

    public NotificationAdapter(Context context, List<NotificationModel> notifications) {
        this.context = context;
        this.notifications = notifications;
        this.db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public NotifViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_notification, parent, false);
        return new NotifViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotifViewHolder holder, int position) {
        NotificationModel notif = notifications.get(position);

        holder.title.setText(notif.getTitle());
        holder.message.setText(notif.getMessage());

        // Relative time like "2 min ago"
        CharSequence timeAgo = DateUtils.getRelativeTimeSpanString(
                notif.getTimestamp(),
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_RELATIVE
        );
        holder.time.setText(timeAgo);

        // Show gold dot for unread, dim for read
        if (!notif.isRead()) {
            holder.unreadDot.setVisibility(View.VISIBLE);
            holder.itemView.setAlpha(1f);
        } else {
            holder.unreadDot.setVisibility(View.GONE);
            holder.itemView.setAlpha(0.6f);
        }

        // Tap to toggle read/unread
        holder.itemView.setOnClickListener(v -> {
            boolean newReadState = !notif.isRead();
            // Update locally for instant feedback
            notif.setRead(newReadState);
            notifyItemChanged(holder.getAdapterPosition());
            // Sync to Firestore
            if (notif.getId() != null) {
                db.collection("notifications").document(notif.getId())
                        .update("read", newReadState);
            }
        });
    }

    public void deleteItem(int position) {
        if (position < 0 || position >= notifications.size()) return;
        NotificationModel notif = notifications.get(position);
        // Remove locally first for instant visual feedback
        notifications.remove(position);
        notifyItemRemoved(position);
        notifyItemRangeChanged(position, notifications.size());
        // Then delete from Firestore
        if (notif.getId() != null) {
            db.collection("notifications").document(notif.getId()).delete();
        }
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    public static class NotifViewHolder extends RecyclerView.ViewHolder {
        TextView title, message, time;
        View unreadDot;

        public NotifViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.notif_title);
            message = itemView.findViewById(R.id.notif_message);
            time = itemView.findViewById(R.id.notif_time);
            unreadDot = itemView.findViewById(R.id.notif_unread_dot);
        }
    }
}