package com.example.buksu_eeu.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.buksu_eeu.AdminOrderAdapter;
import com.example.buksu_eeu.Order;
import com.example.buksu_eeu.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.List;

public class AdminOrdersFragment extends Fragment {

    private FirebaseFirestore db;
    private RecyclerView recyclerView;
    private AdminOrderAdapter adapter;
    private List<Order> masterOrderList = new ArrayList<>();
    private List<Order> filteredOrderList = new ArrayList<>();
    private List<Order> paginatedOrderList = new ArrayList<>();
    private TextView noOrdersText;

    // Pagination & Filter variables
    private int currentPage = 1;
    private static final int ITEMS_PER_PAGE = 10;
    private String currentFilter = "All";
    
    private View paginationLayout;
    private TextView tvPageNumber;
    private View btnPrevPage;
    private View btnNextPage;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_orders, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = FirebaseFirestore.getInstance();

        noOrdersText = view.findViewById(R.id.no_orders_text);
        recyclerView = view.findViewById(R.id.recycler_orders_admin);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new AdminOrderAdapter(requireContext(), paginatedOrderList);
        recyclerView.setAdapter(adapter);

        paginationLayout = view.findViewById(R.id.pagination_layout_admin_orders);
        tvPageNumber = view.findViewById(R.id.tv_page_number_admin);
        btnPrevPage = view.findViewById(R.id.btn_prev_page_admin);
        btnNextPage = view.findViewById(R.id.btn_next_page_admin);

        setupFilters(view);
        setupPaginationListeners();
        loadOrders();
    }

    private void setupFilters(View view) {
        com.google.android.material.button.MaterialButton btnAll = view.findViewById(R.id.btn_filter_all);
        com.google.android.material.button.MaterialButton btnPending = view.findViewById(R.id.btn_filter_pending);
        com.google.android.material.button.MaterialButton btnConfirmed = view.findViewById(R.id.btn_filter_confirmed);
        com.google.android.material.button.MaterialButton btnReady = view.findViewById(R.id.btn_filter_ready);
        com.google.android.material.button.MaterialButton btnCompleted = view.findViewById(R.id.btn_filter_completed);
        com.google.android.material.button.MaterialButton btnCancelled = view.findViewById(R.id.btn_filter_cancelled);

        View.OnClickListener filterListener = v -> {
            String newFilter = "All";
            if (v.getId() == R.id.btn_filter_pending) newFilter = "Pending";
            else if (v.getId() == R.id.btn_filter_confirmed) newFilter = "Confirmed the order";
            else if (v.getId() == R.id.btn_filter_ready) newFilter = "Ready to pickup";
            else if (v.getId() == R.id.btn_filter_completed) newFilter = "Picked Up";
            else if (v.getId() == R.id.btn_filter_cancelled) newFilter = "Cancelled";

            if (!currentFilter.equals(newFilter)) {
                currentFilter = newFilter;
                currentPage = 1;

                // Update UI Colors
                int activeColor = requireContext().getColor(R.color.btn_blue);
                int activeTextColor = requireContext().getColor(R.color.white);
                int inactiveColor = requireContext().getColor(R.color.soft_white);
                int inactiveTextColor = requireContext().getColor(R.color.black);

                btnAll.setBackgroundTintList(android.content.res.ColorStateList.valueOf(newFilter.equals("All") ? activeColor : inactiveColor));
                btnAll.setTextColor(newFilter.equals("All") ? activeTextColor : inactiveTextColor);

                btnPending.setBackgroundTintList(android.content.res.ColorStateList.valueOf(newFilter.equals("Pending") ? activeColor : inactiveColor));
                btnPending.setTextColor(newFilter.equals("Pending") ? activeTextColor : inactiveTextColor);

                btnConfirmed.setBackgroundTintList(android.content.res.ColorStateList.valueOf(newFilter.equals("Confirmed the order") ? activeColor : inactiveColor));
                btnConfirmed.setTextColor(newFilter.equals("Confirmed the order") ? activeTextColor : inactiveTextColor);

                btnReady.setBackgroundTintList(android.content.res.ColorStateList.valueOf(newFilter.equals("Ready to pickup") ? activeColor : inactiveColor));
                btnReady.setTextColor(newFilter.equals("Ready to pickup") ? activeTextColor : inactiveTextColor);

                btnCompleted.setBackgroundTintList(android.content.res.ColorStateList.valueOf(newFilter.equals("Picked Up") ? activeColor : inactiveColor));
                btnCompleted.setTextColor(newFilter.equals("Picked Up") ? activeTextColor : inactiveTextColor);

                btnCancelled.setBackgroundTintList(android.content.res.ColorStateList.valueOf(newFilter.equals("Cancelled") ? activeColor : inactiveColor));
                btnCancelled.setTextColor(newFilter.equals("Cancelled") ? activeTextColor : inactiveTextColor);

                applyFilterAndPagination();
            }
        };

        btnAll.setOnClickListener(filterListener);
        btnPending.setOnClickListener(filterListener);
        btnConfirmed.setOnClickListener(filterListener);
        btnReady.setOnClickListener(filterListener);
        btnCompleted.setOnClickListener(filterListener);
        btnCancelled.setOnClickListener(filterListener);
    }

    private void setupPaginationListeners() {
        btnPrevPage.setOnClickListener(v -> {
            if (currentPage > 1) {
                currentPage--;
                updatePagination();
            }
        });

        btnNextPage.setOnClickListener(v -> {
            int maxPage = (int) Math.ceil((double) filteredOrderList.size() / ITEMS_PER_PAGE);
            if (currentPage < maxPage) {
                currentPage++;
                updatePagination();
            }
        });
    }

    private void loadOrders() {
        db.collection("orders")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;
                    masterOrderList.clear();
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        Order order = doc.toObject(Order.class);
                        if (order != null) {
                            order.setOrderId(doc.getId());
                            masterOrderList.add(order);
                        }
                    }
                    
                    if (masterOrderList.isEmpty()) {
                        noOrdersText.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                        paginationLayout.setVisibility(View.GONE);
                    } else {
                        // Reset to page 1 on new data to prevent out-of-bounds
                        currentPage = 1;
                        applyFilterAndPagination();
                    }
                });
    }

    private void applyFilterAndPagination() {
        filteredOrderList.clear();
        if (currentFilter.equals("All")) {
            filteredOrderList.addAll(masterOrderList);
        } else {
            for (Order order : masterOrderList) {
                if (currentFilter.equals(order.getStatus())) {
                    filteredOrderList.add(order);
                }
            }
        }
        
        if (filteredOrderList.isEmpty()) {
            noOrdersText.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            paginationLayout.setVisibility(View.GONE);
        } else {
            noOrdersText.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
        
        updatePagination();
    }

    private void updatePagination() {
        int totalItems = filteredOrderList.size();
        int totalPages = (int) Math.ceil((double) totalItems / ITEMS_PER_PAGE);
        
        if (totalItems <= ITEMS_PER_PAGE) {
            paginationLayout.setVisibility(View.GONE);
        } else {
            paginationLayout.setVisibility(View.VISIBLE);
        }

        int start = (currentPage - 1) * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, totalItems);

        paginatedOrderList.clear();
        if (start < totalItems) {
            paginatedOrderList.addAll(filteredOrderList.subList(start, end));
        }
        
        adapter.notifyDataSetChanged();

        tvPageNumber.setText("Page " + currentPage + " of " + Math.max(1, totalPages));
        btnPrevPage.setEnabled(currentPage > 1);
        btnNextPage.setEnabled(currentPage < totalPages);
    }
}
