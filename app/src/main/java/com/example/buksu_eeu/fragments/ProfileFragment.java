package com.example.buksu_eeu;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import androidx.fragment.app.Fragment;
import com.bumptech.glide.Glide;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import java.util.Map;

public class ProfileFragment extends Fragment {
    
    private TextView nameText, emailSubText;
    private ShapeableImageView profileImage;
    private View btnChangePhoto;
    private Button logoutBtn;
    private ProgressBar progressBar;
    
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String currentUid;
    private String currentUserPhone = "";
    private TextView statPending, statPickup, statCompleted;
    private View cardPending, cardPickup, cardCompleted;

    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    uploadToCloudinary(imageUri);
                }
            }
    );

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUid = mAuth.getUid();

        initViews(view);

        if (currentUid != null) {
            fetchUserInfo();
            fetchOrderStats();
        }

        btnChangePhoto.setOnClickListener(v -> openGallery());
        
        // Setup Stat Card Listeners
        if (cardPending != null) cardPending.setOnClickListener(v -> navigateToOrders("Pending"));
        if (cardPickup != null) cardPickup.setOnClickListener(v -> navigateToOrders("Ready to pickup"));
        if (cardCompleted != null) cardCompleted.setOnClickListener(v -> navigateToOrders("Picked Up"));

        // Menu items functionality
        View btnEditProfile = view.findViewById(R.id.btn_edit_profile_user);
        if (btnEditProfile != null) btnEditProfile.setOnClickListener(v -> showEditProfileDialog());

        view.findViewById(R.id.btn_logout_user).setOnClickListener(v -> showLogoutConfirmationDialog());
    }

    private void initViews(View view) {
        nameText = view.findViewById(R.id.profile_name_user);
        emailSubText = view.findViewById(R.id.profile_email_user);
        profileImage = view.findViewById(R.id.profile_image_user);
        btnChangePhoto = view.findViewById(R.id.btn_edit_image);
        logoutBtn = view.findViewById(R.id.btn_logout_user);

        // Stats
        statPending = view.findViewById(R.id.stat_pending_user);
        statPickup = view.findViewById(R.id.stat_pickup_user);
        statCompleted = view.findViewById(R.id.stat_completed_user);
        cardPending = view.findViewById(R.id.card_pending_user);
        cardPickup = view.findViewById(R.id.card_pickup_user);
        cardCompleted = view.findViewById(R.id.card_completed_user);
    }

    private void fetchOrderStats() {
        if (currentUid == null) return;

        db.collection("orders")
                .whereEqualTo("userId", currentUid)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int pending = 0;
                    int pickup = 0;
                    int completed = 0;

                    for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                        String status = doc.getString("status");
                        if (status == null) continue;

                        if (status.equalsIgnoreCase("Pending")) {
                            pending++;
                        } else if (status.equalsIgnoreCase("Ready to pickup")) {
                            pickup++;
                        } else if (status.equalsIgnoreCase("Picked Up")) {
                            completed++;
                        }
                    }

                    if (isAdded()) {
                        if (statPending != null) statPending.setText(String.valueOf(pending));
                        if (statPickup != null) statPickup.setText(String.valueOf(pickup));
                        if (statCompleted != null) statCompleted.setText(String.valueOf(completed));
                    }
                });
    }

    private void navigateToOrders(String filter) {
        com.example.buksu_eeu.OrdersFragment.initialFilter = filter;
        BottomNavigationView nav = requireActivity().findViewById(R.id.bottom_nav_view);
        if (nav != null) {
            nav.setSelectedItemId(R.id.nav_orders);
        }
    }

    private void fetchUserInfo() {
        db.collection("users").document(currentUid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String username = doc.getString("username");
                        String email = doc.getString("email");
                        String photoUrl = doc.getString("profilePhoto");
                        String phone = doc.getString("phone");
                        currentUserPhone = phone != null ? phone : "";

                        nameText.setText(username != null ? username : "User Name");
                        emailSubText.setText(email);

                        if (photoUrl != null && !photoUrl.isEmpty()) {
                            Glide.with(this).load(photoUrl).placeholder(R.drawable.logo).circleCrop().into(profileImage);
                        }
                    }
                });
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickImageLauncher.launch(intent);
    }

    private void uploadToCloudinary(Uri imageUri) {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        MediaManager.get().upload(imageUri)
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) {}

                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {}

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String imageUrl = (String) resultData.get("secure_url");
                        updateProfilePhoto(imageUrl);
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                        Toast.makeText(requireContext(), "Upload failed", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {}
                })
                .dispatch();
    }

    private void updateProfilePhoto(String imageUrl) {
        db.collection("users").document(currentUid)
                .update("profilePhoto", imageUrl)
                .addOnSuccessListener(aVoid -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    Glide.with(this).load(imageUrl).placeholder(R.drawable.logo).circleCrop().into(profileImage);
                    Toast.makeText(requireContext(), "Photo updated successfully", Toast.LENGTH_SHORT).show();
                });
    }

    private void showEditProfileDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_profile, null);
        EditText etUsername = dialogView.findViewById(R.id.et_edit_username);
        EditText etPhone = dialogView.findViewById(R.id.et_edit_phone);
        Button btnSave = dialogView.findViewById(R.id.btn_save_profile);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel_edit);

        etUsername.setText(nameText.getText().toString());
        etPhone.setText(currentUserPhone);

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext(), R.style.DarkAlertDialog)
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

            updateProfileDetails(newName, newPhone, dialog);
        });

        dialog.show();
    }

    private void updateProfileDetails(String newName, String newPhone, AlertDialog dialog) {
        Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("username", newName);
        updates.put("phone", newPhone);

        android.util.Log.d("ProfileFragment", "Updating profile for UID: " + currentUid + " with Name: " + newName);
        db.collection("users").document(currentUid)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    android.util.Log.d("ProfileFragment", "Profile updated successfully in Firestore");
                    nameText.setText(newName);
                    currentUserPhone = newPhone;
                    dialog.dismiss();
                    Toast.makeText(requireContext(), "Profile updated", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("ProfileFragment", "Error updating profile", e);
                    Toast.makeText(requireContext(), "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showLogoutConfirmationDialog() {
        new MaterialAlertDialogBuilder(requireContext(), R.style.DarkAlertDialog)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> {
                    mAuth.signOut();
                    startActivity(new Intent(requireContext(), MainActivity.class));
                    requireActivity().finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
