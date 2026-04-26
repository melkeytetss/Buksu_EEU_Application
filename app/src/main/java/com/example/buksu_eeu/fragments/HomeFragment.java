package com.example.buksu_eeu;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.RadioGroup;
import android.widget.RadioButton;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.messaging.FirebaseMessaging;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HomeFragment extends Fragment {

    private RecyclerView recyclerView;
    private ProductAdapter adapter;
    private List<Product> productList;
    private List<Product> filteredList;
    private List<Product> paginatedList;
    private FirebaseFirestore db;
    private EditText searchInput;
    private TextView userNameHome;
    private ImageView btnFilter;
    private LinearLayout categoryContainer;
    
    private androidx.viewpager2.widget.ViewPager2 viewPagerSlider;
    private ProductSliderAdapter sliderAdapter;
    private List<Product> sliderList;
    private android.os.Handler sliderHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable sliderRunnable = new Runnable() {
        @Override
        public void run() {
            if (viewPagerSlider != null && sliderAdapter != null && sliderAdapter.getItemCount() > 0) {
                int nextItem = (viewPagerSlider.getCurrentItem() + 1) % sliderAdapter.getItemCount();
                viewPagerSlider.setCurrentItem(nextItem, true);
                sliderHandler.postDelayed(this, 3000);
            }
        }
    };
    
    private int currentPage = 1;
    private static final int ITEMS_PER_PAGE = 10;
    private String currentSort = "Newest"; // Default sort
    private String selectedCategory = "All"; // Default category
    private ListenerRegistration userListener;
    private ListenerRegistration productsListener;
    private ListenerRegistration notificationsListener;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        
        userNameHome = view.findViewById(R.id.user_name_home);
        searchInput = view.findViewById(R.id.search_input);
        btnFilter = view.findViewById(R.id.btn_filter);
        categoryContainer = view.findViewById(R.id.category_container);
        
        setupRecyclerView(view);
        setupSearch();
        setupPagination(view);
        
        btnFilter.setOnClickListener(v -> showFilterDialog());
        
        // Notifications
        View btnNotifications = view.findViewById(R.id.btn_notifications_home);
        View badgeNotifications = view.findViewById(R.id.notification_badge_home);
        
        if (btnNotifications != null) {
            btnNotifications.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), NotificationsActivity.class);
                startActivity(intent);
            });
        }
        
        loadUnreadNotifications(badgeNotifications);

        // Slider Setup
        viewPagerSlider = view.findViewById(R.id.viewPager_product_slider);
        sliderList = new java.util.ArrayList<>();
        sliderAdapter = new ProductSliderAdapter(requireContext(), sliderList);
        if (viewPagerSlider != null) {
            viewPagerSlider.setAdapter(sliderAdapter);
            viewPagerSlider.registerOnPageChangeCallback(new androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
                @Override
                public void onPageSelected(int position) {
                    super.onPageSelected(position);
                    sliderHandler.removeCallbacks(sliderRunnable);
                    sliderHandler.postDelayed(sliderRunnable, 3000);
                }
            });
        }
        
        loadProductsFromFirestore();
        loadUserData();
        getAndLogFCMToken();
    }

    private void getAndLogFCMToken() {
        FirebaseMessaging.getInstance().getToken()
            .addOnCompleteListener(task -> {
                if (!task.isSuccessful()) return;
                String token = task.getResult();
                android.util.Log.d("FCM_TOKEN", token);
            });
    }

    private void setupSearch() {
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentPage = 1;
                applySortAndFilter();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void showFilterDialog() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext(), R.style.BottomSheetDialogTheme);
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_filter_products, null);
        bottomSheetDialog.setContentView(dialogView);

        RadioGroup rgSort = dialogView.findViewById(R.id.rg_sort_options);
        MaterialButton btnApply = dialogView.findViewById(R.id.btn_apply_filter);

        // Pre-select current sort
        switch (currentSort) {
            case "Price: Low to High": rgSort.check(R.id.rb_sort_price_low); break;
            case "Price: High to Low": rgSort.check(R.id.rb_sort_price_high); break;
            case "Name: A-Z": rgSort.check(R.id.rb_sort_name); break;
            default: rgSort.check(R.id.rb_sort_newest); break;
        }

        btnApply.setOnClickListener(v -> {
            int selectedId = rgSort.getCheckedRadioButtonId();
            if (selectedId == R.id.rb_sort_price_low) currentSort = "Price: Low to High";
            else if (selectedId == R.id.rb_sort_price_high) currentSort = "Price: High to Low";
            else if (selectedId == R.id.rb_sort_name) currentSort = "Name: A-Z";
            else currentSort = "Newest";

            applySortAndFilter();
            bottomSheetDialog.dismiss();
        });

        bottomSheetDialog.show();
    }

    private void applySortAndFilter() {
        String query = searchInput.getText().toString().toLowerCase();
        filteredList.clear();

        for (Product product : productList) {
            String productName = product.getName() != null ? product.getName() : "";
            String productCategory = product.getCategory() != null ? product.getCategory() : "";

            boolean matchesQuery = productName.toLowerCase().contains(query) ||
                                 productCategory.toLowerCase().contains(query);
            boolean matchesCategory = selectedCategory.equals("All") || 
                                     productCategory.equalsIgnoreCase(selectedCategory);

            if (matchesQuery && matchesCategory) {
                filteredList.add(product);
            }
        }

        // Apply Sorting
        switch (currentSort) {
            case "Price: Low to High":
                Collections.sort(filteredList, (p1, p2) -> Double.compare(p1.getPrice(), p2.getPrice()));
                break;
            case "Price: High to Low":
                Collections.sort(filteredList, (p1, p2) -> Double.compare(p2.getPrice(), p1.getPrice()));
                break;
            case "Name: A-Z":
                Collections.sort(filteredList, (p1, p2) -> p1.getName().compareToIgnoreCase(p2.getName()));
                break;
            default: // Newest - default by list order from Firestore
                break;
        }

        currentPage = 1;
        updatePaginationUI();
    }

    private void setupPagination(View view) {
        view.findViewById(R.id.btn_prev_page).setOnClickListener(v -> {
            if (currentPage > 1) {
                currentPage--;
                updatePaginationUI();
            }
        });

        view.findViewById(R.id.btn_next_page).setOnClickListener(v -> {
            int maxPage = (int) Math.ceil((double) filteredList.size() / ITEMS_PER_PAGE);
            if (currentPage < maxPage) {
                currentPage++;
                updatePaginationUI();
            }
        });
    }

    private void updatePaginationUI() {
        int start = (currentPage - 1) * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, filteredList.size());
        
        paginatedList.clear();
        if (start < filteredList.size()) {
            paginatedList.addAll(filteredList.subList(start, end));
        }
        
        adapter.updateList(paginatedList);
        
        if (getView() != null) {
            TextView tvPage = getView().findViewById(R.id.tv_page_number);
            int totalPages = (int) Math.ceil((double) filteredList.size() / ITEMS_PER_PAGE);
            tvPage.setText("Page " + currentPage + " of " + Math.max(1, totalPages));
            
            getView().findViewById(R.id.btn_prev_page).setEnabled(currentPage > 1);
            getView().findViewById(R.id.btn_next_page).setEnabled(currentPage < totalPages);
            
            if (totalPages <= 1) {
                getView().findViewById(R.id.pagination_container).setVisibility(View.GONE);
            } else {
                getView().findViewById(R.id.pagination_container).setVisibility(View.VISIBLE);
            }
            
            ((androidx.core.widget.NestedScrollView) getView().findViewById(R.id.main_scroll_view)).smoothScrollTo(0, 0);
        }
    }

    private void loadUserData() {
        String uid = FirebaseAuth.getInstance().getUid();
        android.util.Log.d("HomeFragment", "Loading user data for UID: " + uid);
        if (uid != null) {
            userListener = db.collection("users").document(uid).addSnapshotListener((doc, error) -> {
                if (error != null) {
                    android.util.Log.e("HomeFragment", "Error loading user data", error);
                    return;
                }
                if (!isAdded() || getView() == null) return;
                if (doc != null && doc.exists()) {
                    String username = doc.getString("username");
                    android.util.Log.d("HomeFragment", "Fetched username: " + username);
                    if (username != null && !username.isEmpty()) {
                        userNameHome.setText(username);
                    } else {
                        userNameHome.setText("User");
                    }
                } else {
                    android.util.Log.d("HomeFragment", "User document does not exist");
                }
            });
        }
    }

    private void loadUnreadNotifications(View badge) {
        if (badge == null) return;
        String userId = FirebaseAuth.getInstance().getUid();
        if (userId == null) return;
        
        notificationsListener = db.collection("notifications")
                .whereEqualTo("recipientId", userId)
                .whereEqualTo("read", false)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;
                    if (!isAdded()) return;
                    badge.setVisibility(value.isEmpty() ? View.GONE : View.VISIBLE);
                });
    }

    private void setupRecyclerView(View view) {
        recyclerView = view.findViewById(R.id.recycler_products);
        recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        productList = new ArrayList<>();
        filteredList = new ArrayList<>();
        paginatedList = new ArrayList<>();
        adapter = new ProductAdapter(requireContext(), paginatedList);
        recyclerView.setAdapter(adapter);
    }

    private void loadProductsFromFirestore() {
        productsListener = db.collection("products").whereEqualTo("archived", false)
            .addSnapshotListener((value, error) -> {
                if (!isAdded() || getView() == null) return;
                if (error != null) return;
                if (value != null) {
                    productList.clear();
                    Set<String> categories = new HashSet<>();
                    categories.add("All");
                    
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        Product product = doc.toObject(Product.class);
                        if (product != null) {
                            product.setId(doc.getId());
                            productList.add(product);
                            if (product.getCategory() != null) {
                                categories.add(product.getCategory());
                            }
                        }
                    }
                    
                    if (sliderAdapter != null && viewPagerSlider != null) {
                        sliderList.clear();
                        int sliderCount = Math.min(5, productList.size());
                        for(int i=0; i<sliderCount; i++) {
                            sliderList.add(productList.get(i));
                        }
                        sliderAdapter.updateData(sliderList);
                        if (sliderList.isEmpty()) {
                            viewPagerSlider.setVisibility(View.GONE);
                        } else {
                            viewPagerSlider.setVisibility(View.VISIBLE);
                        }
                    }
                    
                    updateCategoryUI(new ArrayList<>(categories));
                    applySortAndFilter();
                }
            });
    }

    private void updateCategoryUI(List<String> categories) {
        if (!isAdded() || getContext() == null) return;

        categoryContainer.removeAllViews();
        Collections.sort(categories); // Alphabetical, but 'All' should be first
        if (categories.contains("All")) {
            categories.remove("All");
            categories.add(0, "All");
        }

        for (String category : categories) {
            View categoryView = LayoutInflater.from(requireContext()).inflate(R.layout.item_category_pill, categoryContainer, false);
            MaterialButton btn = categoryView.findViewById(R.id.btn_category_pill);
            btn.setText(category);
            
            // Highlight selected
            if (category.equalsIgnoreCase(selectedCategory)) {
                btn.setBackgroundTintList(getResources().getColorStateList(R.color.btn_blue));
                btn.setTextColor(getResources().getColor(R.color.white));
            } else {
                btn.setBackgroundTintList(getResources().getColorStateList(R.color.white));
                btn.setTextColor(getResources().getColor(R.color.bg_navy));
            }

            btn.setOnClickListener(v -> {
                selectedCategory = category;
                applySortAndFilter();
                updateCategoryUI(categories); // Refresh colors
            });

            categoryContainer.addView(categoryView);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (sliderHandler != null && sliderRunnable != null) {
            sliderHandler.removeCallbacks(sliderRunnable);
        }
        if (userListener != null) {
            userListener.remove();
            userListener = null;
        }
        if (productsListener != null) {
            productsListener.remove();
            productsListener = null;
        }
        if (notificationsListener != null) {
            notificationsListener.remove();
            notificationsListener = null;
        }
    }
}
