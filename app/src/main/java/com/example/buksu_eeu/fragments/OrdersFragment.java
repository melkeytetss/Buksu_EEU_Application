package com.example.buksu_eeu;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.List;

public class OrdersFragment extends Fragment {

    private RecyclerView recyclerView;
    private UserOrderAdapter adapter;
    private List<Order> orderList;
    private TextView noOrdersText;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private com.google.firebase.firestore.ListenerRegistration orderListener;
    private String currentStatusFilter = "All";
    private List<com.google.android.material.button.MaterialButton> filterButtons;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_orders, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        noOrdersText = view.findViewById(R.id.no_orders_text);
        recyclerView = view.findViewById(R.id.recycler_user_orders);

        setupFilters(view);
        setupRecyclerView();
        loadUserOrders();
    }

    private void setupFilters(View view) {
        filterButtons = new ArrayList<>();
        filterButtons.add(view.findViewById(R.id.btn_filter_all));
        filterButtons.add(view.findViewById(R.id.btn_filter_pending));
        filterButtons.add(view.findViewById(R.id.btn_filter_confirmed));
        filterButtons.add(view.findViewById(R.id.btn_filter_ready));
        filterButtons.add(view.findViewById(R.id.btn_filter_completed));
        filterButtons.add(view.findViewById(R.id.btn_filter_cancelled));

        for (com.google.android.material.button.MaterialButton btn : filterButtons) {
            btn.setOnClickListener(v -> {
                String filter = "All";
                if (v.getId() == R.id.btn_filter_pending) filter = "Pending";
                else if (v.getId() == R.id.btn_filter_confirmed) filter = "Confirmed the order";
                else if (v.getId() == R.id.btn_filter_ready) filter = "Ready to pickup";
                else if (v.getId() == R.id.btn_filter_completed) filter = "Picked Up";
                else if (v.getId() == R.id.btn_filter_cancelled) filter = "Cancelled";
                
                if (!currentStatusFilter.equals(filter)) {
                    currentStatusFilter = filter;
                    updateFilterButtonsUI(btn);
                    loadUserOrders();
                }
            });
        }
    }

    private void updateFilterButtonsUI(com.google.android.material.button.MaterialButton selectedBtn) {
        int activeColor = getResources().getColor(R.color.btn_blue);
        int activeTextColor = getResources().getColor(R.color.white);
        int inactiveColor = getResources().getColor(R.color.soft_white);
        int inactiveTextColor = getResources().getColor(R.color.black);

        for (com.google.android.material.button.MaterialButton btn : filterButtons) {
            if (btn == selectedBtn) {
                btn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(activeColor));
                btn.setTextColor(activeTextColor);
            } else {
                btn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(inactiveColor));
                btn.setTextColor(inactiveTextColor);
            }
        }
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        orderList = new ArrayList<>();
        adapter = new UserOrderAdapter(requireContext(), orderList);
        recyclerView.setAdapter(adapter);
    }

    private void loadUserOrders() {
        String userId = mAuth.getUid();
        if (userId == null) {
            android.util.Log.e("OrdersFragment", "No user logged in!");
            return;
        }
        android.util.Log.d("OrdersFragment", "Loading orders for userId: " + userId + " with filter: " + currentStatusFilter);
        
        // Remove previous listener if exists
        if (orderListener != null) {
            orderListener.remove();
        }

        Query query = db.collection("orders").whereEqualTo("userId", userId);
        
        if (!currentStatusFilter.equals("All")) {
            query = query.whereEqualTo("status", currentStatusFilter);
        }

        orderListener = query.addSnapshotListener((value, error) -> {
                    if (error != null) {
                        android.util.Log.e("OrdersFragment", "Firestore Error: " + error.getMessage());
                        if (getContext() != null) {
                            android.widget.Toast.makeText(getContext(), "Error: " + error.getMessage(), android.widget.Toast.LENGTH_LONG).show();
                        }
                        return;
                    }
                    if (value != null) {
                        android.util.Log.d("OrdersFragment", "Found " + value.size() + " orders in Firestore");
                        orderList.clear();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            try {
                                Order order = doc.toObject(Order.class);
                                if (order != null) {
                                    order.setOrderId(doc.getId());
                                    orderList.add(order);
                                }
                            } catch (Exception e) {
                                android.util.Log.e("OrdersFragment", "Error parsing order: " + e.getMessage());
                            }
                        }

                        // Sort manually if needed or let Firestore do it after index is created
                        java.util.Collections.sort(orderList, (o1, o2) -> Long.compare(o2.getTimestamp(), o1.getTimestamp()));

                        if (orderList.isEmpty()) {
                            if (getView() != null) {
                                getView().findViewById(R.id.no_orders_container).setVisibility(View.VISIBLE);
                            }
                            recyclerView.setVisibility(View.GONE);
                        } else {
                            if (getView() != null) {
                                getView().findViewById(R.id.no_orders_container).setVisibility(View.GONE);
                            }
                            recyclerView.setVisibility(View.VISIBLE);
                        }
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (orderListener != null) {
            orderListener.remove();
        }
    }
}
