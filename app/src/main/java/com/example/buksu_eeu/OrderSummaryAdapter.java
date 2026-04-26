package com.example.buksu_eeu;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class OrderSummaryAdapter extends RecyclerView.Adapter<OrderSummaryAdapter.SummaryViewHolder> {

    private Context context;
    private JSONArray items;

    public OrderSummaryAdapter(Context context, JSONArray items) {
        this.context = context;
        this.items = items;
    }

    @NonNull
    @Override
    public SummaryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_order_summary, parent, false);
        return new SummaryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SummaryViewHolder holder, int position) {
        try {
            JSONObject item = items.getJSONObject(position);
            holder.tvName.setText(item.getString("name"));
            holder.tvPrice.setText(String.format("₱%.0f", item.getDouble("price") * item.getInt("quantity")));
            holder.tvQuantity.setText("x" + item.getInt("quantity"));
            
            String imageUrl = item.getString("imageUrl");
            if (imageUrl != null && !imageUrl.isEmpty()) {
                Glide.with(context).load(imageUrl).into(holder.imgProduct);
            } else {
                holder.imgProduct.setImageResource(R.drawable.logo);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getItemCount() {
        return items.length();
    }

    public static class SummaryViewHolder extends RecyclerView.ViewHolder {
        ImageView imgProduct;
        TextView tvName, tvPrice, tvQuantity;

        public SummaryViewHolder(@NonNull View itemView) {
            super(itemView);
            imgProduct = itemView.findViewById(R.id.img_summary_item);
            tvName = itemView.findViewById(R.id.tv_summary_item_name);
            tvPrice = itemView.findViewById(R.id.tv_summary_item_price);
            tvQuantity = itemView.findViewById(R.id.tv_summary_item_qty);
        }
    }
}
