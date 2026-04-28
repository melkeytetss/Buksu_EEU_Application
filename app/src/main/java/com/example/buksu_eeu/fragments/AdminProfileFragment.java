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
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import android.net.Uri;
import android.app.Activity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

public class AdminProfileFragment extends Fragment {

    private FirebaseFirestore db;
    private ListenerRegistration userListener;
    private TextView adminName;
    private TextView adminEmail;
    private ImageView profileImage;
    private String currentUid;
    private String currentPhone = "";
    private View progressBar;

    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    uploadToCloudinary(imageUri);
                }
            }
    );
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
        
        View btnEditImage = view.findViewById(R.id.btn_edit_admin_image);
        View logoutBtn = view.findViewById(R.id.admin_logout_btn_new);
        ImageButton editBtn = view.findViewById(R.id.btn_admin_edit_profile);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            adminEmail.setText(user.getEmail() != null ? user.getEmail() : "");
            // Initial load from Firebase Auth (will be overridden by Firestore in loadAdminData)
            if (user.getPhotoUrl() != null) {
                Glide.with(this).load(user.getPhotoUrl()).placeholder(R.drawable.logo).circleCrop().into(profileImage);
            }
        }

        if (btnEditImage != null) btnEditImage.setOnClickListener(v -> openGallery());
        if (logoutBtn != null) logoutBtn.setOnClickListener(v -> showLogoutDialog());
        editBtn.setOnClickListener(v -> showEditProfileDialog());
        
        View userCard = view.findViewById(R.id.card_users_stat);
        if (userCard != null) {
            userCard.setOnClickListener(v -> showUsersDialog());
        }

        View productCard = view.findViewById(R.id.card_products_stat);
        if (productCard != null) {
            productCard.setOnClickListener(v -> {
                com.google.android.material.bottomnavigation.BottomNavigationView nav = requireActivity().findViewById(R.id.admin_bottom_nav);
                if (nav != null) nav.setSelectedItemId(R.id.admin_nav_products);
            });
        }

        View orderCard = view.findViewById(R.id.card_orders_stat);
        if (orderCard != null) {
            orderCard.setOnClickListener(v -> {
                com.google.android.material.bottomnavigation.BottomNavigationView nav = requireActivity().findViewById(R.id.admin_bottom_nav);
                if (nav != null) nav.setSelectedItemId(R.id.admin_nav_orders);
            });
        }
        
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
                    
                    if (doc.contains("profilePhoto")) {
                        String photoUrl = doc.getString("profilePhoto");
                        if (photoUrl != null && !photoUrl.isEmpty()) {
                            Glide.with(this).load(photoUrl).placeholder(R.drawable.logo).circleCrop().into(profileImage);
                        }
                    } else if (FirebaseAuth.getInstance().getCurrentUser() != null && FirebaseAuth.getInstance().getCurrentUser().getPhotoUrl() != null) {
                        // Fallback to Google photo if no Cloudinary photo exists
                        Glide.with(this).load(FirebaseAuth.getInstance().getCurrentUser().getPhotoUrl()).placeholder(R.drawable.logo).circleCrop().into(profileImage);
                    }

                    if (doc.contains("phone")) {
                        currentPhone = doc.getString("phone");
                    }
                }
            });
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickImageLauncher.launch(intent);
    }

    private void uploadToCloudinary(Uri imageUri) {
        Toast.makeText(requireContext(), "Uploading photo...", Toast.LENGTH_SHORT).show();
        
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
                        Toast.makeText(requireContext(), "Upload failed: " + error.getDescription(), Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(requireContext(), "Profile photo updated", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Failed to update database", Toast.LENGTH_SHORT).show();
                });
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

    private void showUsersDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_user_list, null);
        androidx.recyclerview.widget.RecyclerView recyclerView = dialogView.findViewById(R.id.recycler_users_list);
        View btnClose = dialogView.findViewById(R.id.btn_close_users);
        android.widget.EditText searchInput = dialogView.findViewById(R.id.search_user_input);
        TextView noUsersText = dialogView.findViewById(R.id.no_users_text);
        
        // Pagination Views
        View paginationLayout = dialogView.findViewById(R.id.pagination_layout_users);
        TextView tvPageNumber = dialogView.findViewById(R.id.tv_page_number_users);
        com.google.android.material.button.MaterialButton btnPrev = dialogView.findViewById(R.id.btn_prev_page_users);
        com.google.android.material.button.MaterialButton btnNext = dialogView.findViewById(R.id.btn_next_page_users);

        java.util.List<com.example.buksu_eeu.UserModel> masterUserList = new java.util.ArrayList<>();
        java.util.List<com.example.buksu_eeu.UserModel> filteredUserList = new java.util.ArrayList<>();
        java.util.List<com.example.buksu_eeu.UserModel> paginatedUserList = new java.util.ArrayList<>();
        
        com.example.buksu_eeu.UserAdapter adapter = new com.example.buksu_eeu.UserAdapter(requireContext(), paginatedUserList);

        recyclerView.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext(), android.R.style.Theme_Material_Light_NoActionBar_Fullscreen)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.color.abyss);
        }

        btnClose.setOnClickListener(v -> dialog.dismiss());

        // State variables (effectively final for inner classes)
        final int[] currentPage = {1};
        final int ITEMS_PER_PAGE = 10;
        final String[] currentSearchQuery = {""};

        Runnable updatePagination = new Runnable() {
            @Override
            public void run() {
                int totalItems = filteredUserList.size();
                int totalPages = (int) Math.ceil((double) totalItems / ITEMS_PER_PAGE);
                
                if (totalItems <= ITEMS_PER_PAGE) {
                    paginationLayout.setVisibility(View.GONE);
                } else {
                    paginationLayout.setVisibility(View.VISIBLE);
                }

                int start = (currentPage[0] - 1) * ITEMS_PER_PAGE;
                int end = Math.min(start + ITEMS_PER_PAGE, totalItems);

                paginatedUserList.clear();
                if (start < totalItems) {
                    paginatedUserList.addAll(filteredUserList.subList(start, end));
                }
                
                adapter.notifyDataSetChanged();

                tvPageNumber.setText("Page " + currentPage[0] + " of " + Math.max(1, totalPages));
                btnPrev.setEnabled(currentPage[0] > 1);
                btnNext.setEnabled(currentPage[0] < totalPages);
            }
        };

        Runnable applyFilterAndPagination = new Runnable() {
            @Override
            public void run() {
                filteredUserList.clear();
                String query = currentSearchQuery[0].toLowerCase().trim();
                
                if (query.isEmpty()) {
                    filteredUserList.addAll(masterUserList);
                } else {
                    for (com.example.buksu_eeu.UserModel user : masterUserList) {
                        boolean matchesName = user.getUsername() != null && user.getUsername().toLowerCase().contains(query);
                        boolean matchesEmail = user.getEmail() != null && user.getEmail().toLowerCase().contains(query);
                        boolean matchesPhone = user.getPhone() != null && user.getPhone().toLowerCase().contains(query);
                        
                        if (matchesName || matchesEmail || matchesPhone) {
                            filteredUserList.add(user);
                        }
                    }
                }
                
                if (filteredUserList.isEmpty()) {
                    noUsersText.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                    paginationLayout.setVisibility(View.GONE);
                } else {
                    noUsersText.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                }
                
                currentPage[0] = 1;
                updatePagination.run();
            }
        };

        // Search logic
        searchInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery[0] = s.toString();
                applyFilterAndPagination.run();
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });

        // Pagination buttons
        btnPrev.setOnClickListener(v -> {
            if (currentPage[0] > 1) {
                currentPage[0]--;
                updatePagination.run();
            }
        });

        btnNext.setOnClickListener(v -> {
            int totalPages = (int) Math.ceil((double) filteredUserList.size() / ITEMS_PER_PAGE);
            if (currentPage[0] < totalPages) {
                currentPage[0]++;
                updatePagination.run();
            }
        });

        // Load data
        db.collection("users").whereEqualTo("role", "student").get().addOnSuccessListener(queryDocumentSnapshots -> {
            masterUserList.clear();
            for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                com.example.buksu_eeu.UserModel user = doc.toObject(com.example.buksu_eeu.UserModel.class);
                if (user != null) {
                    user.setUid(doc.getId());
                    masterUserList.add(user);
                }
            }
            applyFilterAndPagination.run();
        });

        dialog.show();
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
