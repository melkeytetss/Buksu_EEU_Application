package com.example.buksu_eeu;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;

public class OrderConfirmationActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_confirmation);

        double total = getIntent().getDoubleExtra("total_price", 0.0);
        TextView totalPriceText = findViewById(R.id.conf_total_price);
        totalPriceText.setText(String.format("₱%.0f", total));

        MaterialButton homeBtn = findViewById(R.id.btn_back_to_home);
        homeBtn.setOnClickListener(v -> {
            Intent intent = new Intent(OrderConfirmationActivity.this, NavContainerActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }
}