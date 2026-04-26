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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.buksu_eeu.AdminOrderAdapter;
import com.example.buksu_eeu.NotificationsActivity;
import com.example.buksu_eeu.Order;
import com.example.buksu_eeu.R;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import androidx.core.content.ContextCompat;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class AdminDashboardFragment extends Fragment {

    private FirebaseFirestore db;
    private TextView tvSales, tvOrders, tvProducts, tvNoRecentOrders;
    private RecyclerView recyclerRecentOrders;
    private LineChart chartWeeklyRevenue, chartWeeklyOrders;
    private AdminOrderAdapter recentOrderAdapter;
    private List<Order> recentOrderList = new ArrayList<>();
    
    private ListenerRegistration ordersListener;
    private ListenerRegistration productsListener;
    private ListenerRegistration recentOrdersListener;
    private ListenerRegistration notificationsListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = FirebaseFirestore.getInstance();

        tvSales = view.findViewById(R.id.tv_total_sales);
        tvOrders = view.findViewById(R.id.tv_total_orders);
        tvProducts = view.findViewById(R.id.tv_total_products);
        tvNoRecentOrders = view.findViewById(R.id.tv_no_recent_orders);
        recyclerRecentOrders = view.findViewById(R.id.recycler_recent_orders);
        chartWeeklyRevenue = view.findViewById(R.id.chart_weekly_revenue);
        chartWeeklyOrders = view.findViewById(R.id.chart_weekly_orders);

        View btnNotifications = view.findViewById(R.id.btn_notifications_admin);
        View badgeNotifications = view.findViewById(R.id.notification_badge_admin);

        if (btnNotifications != null) {
            btnNotifications.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), NotificationsActivity.class);
                intent.putExtra("isAdmin", true);
                startActivity(intent);
            });
        }

        recyclerRecentOrders.setLayoutManager(new LinearLayoutManager(requireContext()));
        recentOrderAdapter = new AdminOrderAdapter(requireContext(), recentOrderList);
        recyclerRecentOrders.setAdapter(recentOrderAdapter);

        setupChart(chartWeeklyRevenue);
        setupChart(chartWeeklyOrders);
        loadStats();
        loadRecentOrders();
        loadUnreadNotifications(badgeNotifications);
    }

    private void setupChart(LineChart chart) {
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.setDrawGridBackground(false);
        chart.getAxisRight().setEnabled(false);

        chart.getAxisLeft().setTextColor(getResources().getColor(R.color.white));
        chart.getAxisLeft().setDrawGridLines(true);
        chart.getAxisLeft().setGridColor(getResources().getColor(R.color.white_10));
        chart.getAxisLeft().setAxisMinimum(0f);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(getResources().getColor(R.color.white));
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
    }

    private void loadStats() {
        ordersListener = db.collection("orders").addSnapshotListener((value, error) -> {
            if (error != null || value == null) return;
            if (!isAdded()) return;
            
            double totalSales = 0;
            int totalOrders = value.size();

            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            long todayStart = cal.getTimeInMillis();

            float[] dailySales = new float[7];
            float[] dailyOrdersCount = new float[7];
            String[] days = new String[7];
            SimpleDateFormat sdf = new SimpleDateFormat("EEE", Locale.getDefault());

            for(int i = 0; i < 7; i++) {
                Calendar c = Calendar.getInstance();
                c.add(Calendar.DAY_OF_YEAR, -(6 - i));
                days[i] = sdf.format(c.getTime());
                dailySales[i] = 0;
                dailyOrdersCount[i] = 0;
            }

            for (DocumentSnapshot doc : value.getDocuments()) {
                String status = doc.getString("status");
                boolean isCompleted = "completed".equalsIgnoreCase(status) || "Picked Up".equalsIgnoreCase(status);

                Double price = doc.getDouble("totalPrice");
                if (price != null && isCompleted) totalSales += price;

                Long ts = doc.getLong("timestamp");
                if (ts != null && price != null) {
                    long diffMillis = todayStart - ts;
                    if (diffMillis <= 0) {
                        if (isCompleted) dailySales[6] += price;
                        dailyOrdersCount[6] += 1;
                    } else {
                        int daysAgo = (int) Math.ceil((double) diffMillis / (1000 * 60 * 60 * 24));
                        if (daysAgo >= 1 && daysAgo <= 6) {
                            if (isCompleted) dailySales[6 - daysAgo] += price;
                            dailyOrdersCount[6 - daysAgo] += 1;
                        }
                    }
                }
            }
            
            tvSales.setText(String.format("₱%,.0f", totalSales));
            tvOrders.setText(String.valueOf(totalOrders));
            
            updateRevenueChart(dailySales, days);
            updateOrdersChart(dailyOrdersCount, days);
        });

        productsListener = db.collection("products").whereEqualTo("archived", false).addSnapshotListener((value, error) -> {
            if (error != null || value == null) return;
            if (!isAdded()) return;
            tvProducts.setText(String.valueOf(value.size()));
        });
    }

    private void updateRevenueChart(float[] sales, String[] days) {
        ArrayList<Entry> entries = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            entries.add(new Entry(i, sales[i]));
        }

        LineDataSet dataSet = new LineDataSet(entries, "Revenue");
        dataSet.setColor(getResources().getColor(R.color.gold_accent));
        dataSet.setCircleColor(getResources().getColor(R.color.gold_accent));
        dataSet.setCircleRadius(4f);
        dataSet.setLineWidth(3f);
        dataSet.setValueTextColor(getResources().getColor(R.color.white));
        dataSet.setValueTextSize(10f);
        dataSet.setDrawFilled(true);
        dataSet.setFillDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.fade_gold));
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        LineData lineData = new LineData(dataSet);
        chartWeeklyRevenue.setData(lineData);

        XAxis xAxis = chartWeeklyRevenue.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(days));

        chartWeeklyRevenue.animateX(1000);
        chartWeeklyRevenue.invalidate();
    }

    private void updateOrdersChart(float[] orders, String[] days) {
        ArrayList<Entry> entries = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            entries.add(new Entry(i, orders[i]));
        }

        LineDataSet dataSet = new LineDataSet(entries, "Orders");
        dataSet.setColor(getResources().getColor(R.color.electric_cyan));
        dataSet.setCircleColor(getResources().getColor(R.color.electric_cyan));
        dataSet.setCircleRadius(4f);
        dataSet.setLineWidth(3f);
        dataSet.setValueTextColor(getResources().getColor(R.color.white));
        dataSet.setValueTextSize(10f);
        dataSet.setDrawFilled(true);
        dataSet.setFillDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.fade_cyan));
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        LineData lineData = new LineData(dataSet);
        chartWeeklyOrders.setData(lineData);

        XAxis xAxis = chartWeeklyOrders.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(days));

        chartWeeklyOrders.animateX(1000);
        chartWeeklyOrders.invalidate();
    }

    private void loadRecentOrders() {
        recentOrdersListener = db.collection("orders")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(5)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;
                    if (!isAdded()) return;
                    
                    recentOrderList.clear();
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        Order order = doc.toObject(Order.class);
                        if (order != null) {
                            order.setOrderId(doc.getId());
                            recentOrderList.add(order);
                        }
                    }
                    recentOrderAdapter.notifyDataSetChanged();
                    tvNoRecentOrders.setVisibility(recentOrderList.isEmpty() ? View.VISIBLE : View.GONE);
                    recyclerRecentOrders.setVisibility(recentOrderList.isEmpty() ? View.GONE : View.VISIBLE);
                });
    }

    private void loadUnreadNotifications(View badge) {
        if (badge == null) return;
        notificationsListener = db.collection("notifications")
                .whereEqualTo("recipientId", "admin")
                .whereEqualTo("read", false)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;
                    if (!isAdded()) return;
                    badge.setVisibility(value.isEmpty() ? View.GONE : View.VISIBLE);
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (ordersListener != null) ordersListener.remove();
        if (productsListener != null) productsListener.remove();
        if (recentOrdersListener != null) recentOrdersListener.remove();
        if (notificationsListener != null) notificationsListener.remove();
    }
}
