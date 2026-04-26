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

        setupRecyclerView();
        loadUserOrders();
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
        android.util.Log.d("OrdersFragment", "Loading orders for userId: " + userId);
        db.collection("orders")
                .whereEqualTo("userId", userId)
                .addSnapshotListener((value, error) -> {
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
}
