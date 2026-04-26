package com.example.buksu_eeu.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.buksu_eeu.MainActivity;
import com.example.buksu_eeu.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import android.widget.ImageButton;
import android.widget.EditText;
import android.widget.Button;
import android.widget.Toast;
import android.text.TextUtils;
import java.util.Map;
import java.util.HashMap;
import com.bumptech.glide.Glide;
import android.widget.ImageView;

public class AdminProfileFragment extends Fragment {

    private FirebaseFirestore db;
    private ListenerRegistration userListener;
    private TextView adminName;
    private TextView adminEmail;
    private ImageView profileImage;
    private String currentUid;
    private String currentPhone = "";
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        currentUid = FirebaseAuth.getInstance().getUid();

        adminName = view.findViewById(R.id.admin_name_text);
        adminEmail = view.findViewById(R.id.admin_email_text);
        profileImage = view.findViewById(R.id.admin_profile_image);
        
        View logoutBtn = view.findViewById(R.id.admin_logout_btn);
        ImageButton editBtn = view.findViewById(R.id.btn_admin_edit_profile);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            adminEmail.setText(user.getEmail() != null ? user.getEmail() : "");
            
            if (user.getPhotoUrl() != null) {
                Glide.with(this).load(user.getPhotoUrl()).placeholder(R.drawable.logo).circleCrop().into(profileImage);
            }
        }

        logoutBtn.setOnClickListener(v -> showLogoutDialog());
        editBtn.setOnClickListener(v -> showEditProfileDialog());
        
        loadAdminData();
        loadQuickStats(view);
    }

    private void loadAdminData() {
        if (currentUid != null) {
            userListener = db.collection("users").document(currentUid).addSnapshotListener((doc, error) -> {
                if (!isAdded() || getView() == null) return;
                if (doc != null && doc.exists()) {
                    String name = doc.getString("username");
                    adminName.setText(name != null && !name.isEmpty() ? name : "Administrator");
                    if (doc.contains("phone")) {
                        currentPhone = doc.getString("phone");
                    }
                }
            });
        }
    }

    private void loadQuickStats(View view) {
        TextView tvProducts = view.findViewById(R.id.stat_products);
        TextView tvOrders = view.findViewById(R.id.stat_orders);
        TextView tvUsers = view.findViewById(R.id.stat_users);

        // Fetch Products count
        db.collection("products").whereEqualTo("archived", false).get().addOnSuccessListener(queryDocumentSnapshots -> {
            if (isAdded() && tvProducts != null) {
                tvProducts.setText(String.valueOf(queryDocumentSnapshots.size()));
            }
        });

        // Fetch Orders count
        db.collection("orders").get().addOnSuccessListener(queryDocumentSnapshots -> {
            if (isAdded() && tvOrders != null) {
                tvOrders.setText(String.valueOf(queryDocumentSnapshots.size()));
            }
        });

        // Fetch Users count
        db.collection("users").whereEqualTo("role", "student").get().addOnSuccessListener(queryDocumentSnapshots -> {
            if (isAdded() && tvUsers != null) {
                tvUsers.setText(String.valueOf(queryDocumentSnapshots.size()));
            }
        });
    }

    private void showEditProfileDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_profile, null);
        EditText etUsername = dialogView.findViewById(R.id.et_edit_username);
        EditText etPhone = dialogView.findViewById(R.id.et_edit_phone);
        Button btnSave = dialogView.findViewById(R.id.btn_save_profile);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel_edit);

        etUsername.setText(adminName.getText().toString());
        etPhone.setText(currentPhone);

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext(), R.style.DarkAlertDialog)
                .setView(dialogView)
                .create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String newName = etUsername.getText().toString().trim();
            String newPhone = etPhone.getText().toString().trim();

            if (TextUtils.isEmpty(newName)) {
                etUsername.setError("Name cannot be empty");
                return;
            }

            Map<String, Object> updates = new HashMap<>();
            updates.put("username", newName);
            updates.put("phone", newPhone);

            db.collection("users").document(currentUid)
                    .update(updates)
                    .addOnSuccessListener(aVoid -> {
                        dialog.dismiss();
                        Toast.makeText(requireContext(), "Profile updated", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(requireContext(), "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });

        dialog.show();
    }

    private void showLogoutDialog() {
        new MaterialAlertDialogBuilder(requireContext(), R.style.DarkAlertDialog)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> {
                    FirebaseAuth.getInstance().signOut();
                    Intent intent = new Intent(requireContext(), MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (userListener != null) {
            userListener.remove();
            userListener = null;
        }
    }
}
