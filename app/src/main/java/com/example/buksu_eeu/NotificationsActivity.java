package com.example.buksu_eeu;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;
import java.util.ArrayList;
import java.util.List;

public class NotificationsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private LinearLayout emptyStateContainer;
    private TextView btnMarkAllRead;
    private NotificationAdapter adapter;
    private List<NotificationModel> notificationList = new ArrayList<>();
    private FirebaseFirestore db;
    private ListenerRegistration listener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        db = FirebaseFirestore.getInstance();

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> onBackPressed());
        }

        btnMarkAllRead = findViewById(R.id.btn_mark_all_read);
        recyclerView = findViewById(R.id.recycler_notifications);
        emptyStateContainer = findViewById(R.id.empty_state_container);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NotificationAdapter(this, notificationList);
        recyclerView.setAdapter(adapter);

        // Swipe to delete
        ItemTouchHelper.SimpleCallback swipeCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                adapter.deleteItem(position);
                updateUI();
            }
        };
        new ItemTouchHelper(swipeCallback).attachToRecyclerView(recyclerView);

        // Mark all as read
        btnMarkAllRead.setOnClickListener(v -> markAllAsRead());

        loadNotifications();
    }

    private void loadNotifications() {
        boolean isAdmin = getIntent().getBooleanExtra("isAdmin", false);
        String recipientId = isAdmin ? "admin" : FirebaseAuth.getInstance().getUid();
        
        if (recipientId == null) return;

        listener = db.collection("notifications")
                .whereEqualTo("recipientId", recipientId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    if (value != null) {
                        notificationList.clear();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            NotificationModel notif = doc.toObject(NotificationModel.class);
                            if (notif != null) {
                                notif.setId(doc.getId());
                                notificationList.add(notif);
                            }
                        }
                        adapter.notifyDataSetChanged();
                        updateUI();
                    }
                });
    }

    private void markAllAsRead() {
        if (notificationList.isEmpty()) return;

        // Update locally first for instant feedback
        for (NotificationModel notif : notificationList) {
            notif.setRead(true);
        }
        adapter.notifyDataSetChanged();

        // Then sync to Firestore
        WriteBatch batch = db.batch();
        for (NotificationModel notif : notificationList) {
            if (notif.getId() != null) {
                batch.update(db.collection("notifications").document(notif.getId()), "read", true);
            }
        }
        batch.commit()
                .addOnSuccessListener(aVoid -> 
                    Toast.makeText(this, "All notifications marked as read", Toast.LENGTH_SHORT).show()
                )
                .addOnFailureListener(e -> 
                    Toast.makeText(this, "Failed to update", Toast.LENGTH_SHORT).show()
                );
    }

    private void updateUI() {
        if (notificationList.isEmpty()) {
            emptyStateContainer.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            btnMarkAllRead.setVisibility(View.GONE);
        } else {
            emptyStateContainer.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            btnMarkAllRead.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (listener != null) {
            listener.remove();
        }
    }
}
