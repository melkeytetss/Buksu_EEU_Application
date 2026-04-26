package com.example.buksu_eeu;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import org.json.JSONArray;
import org.json.JSONException;

public class OrderConfirmationActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_confirmation);

        double total = getIntent().getDoubleExtra("total_price", 0.0);
        String itemsJson = getIntent().getStringExtra("items_json");

        TextView totalPriceText = findViewById(R.id.conf_total_price);
        RecyclerView recyclerView = findViewById(R.id.recycler_order_summary);

        totalPriceText.setText(String.format("₱%.0f", total));
        
        if (itemsJson != null) {
            try {
                JSONArray itemsArray = new JSONArray(itemsJson);
                OrderSummaryAdapter adapter = new OrderSummaryAdapter(this, itemsArray);
                recyclerView.setLayoutManager(new LinearLayoutManager(this));
                recyclerView.setAdapter(adapter);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        MaterialButton homeBtn = findViewById(R.id.btn_back_to_home);
        homeBtn.setOnClickListener(v -> {
            Intent intent = new Intent(OrderConfirmationActivity.this, NavContainerActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }
}