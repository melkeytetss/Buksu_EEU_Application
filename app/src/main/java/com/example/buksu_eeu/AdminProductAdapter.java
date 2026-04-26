package com.example.buksu_eeu;

import android.content.Context;
import android.content.Intent;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.List;

public class AdminProductAdapter extends RecyclerView.Adapter<AdminProductAdapter.AdminProductViewHolder> {

    private Context context;
    private List<Product> productList;
    private FirebaseFirestore db;

    public AdminProductAdapter(Context context, List<Product> productList) {
        this.context = context;
        this.productList = productList;
        this.db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public AdminProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_product_admin, parent, false);
        return new AdminProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AdminProductViewHolder holder, int position) {
        Product product = productList.get(position);
        holder.name.setText(product.getName());
        holder.price.setText(String.format("₱%.0f", product.getPrice()));
        holder.stockText.setText(String.format("%d units", product.getStock()));

        // Handle Archive UI
        if (product.isArchived()) {
            ColorMatrix matrix = new ColorMatrix();
            matrix.setSaturation(0); // Grayscale
            ColorMatrixColorFilter filter = new ColorMatrixColorFilter(matrix);
            holder.image.setColorFilter(filter);
            holder.archiveBtn.setText("Unarchive");
            holder.archiveBtn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(context.getResources().getColor(R.color.stock_green)));
        } else {
            holder.image.clearColorFilter();
            holder.archiveBtn.setText("Archive");
            holder.archiveBtn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(context.getResources().getColor(R.color.archive_red)));
        }

        if (product.getStock() > 0) {
            holder.stockStatus.setText(product.isArchived() ? "Archived" : "In Stock");
            holder.stockStatus.setBackgroundResource(R.drawable.rounded_card_bg);
            holder.stockStatus.getBackground().setTint(context.getResources().getColor(product.isArchived() ? R.color.text_grey : R.color.stock_green));
        } else {
            holder.stockStatus.setText("Out of Stock");
            holder.stockStatus.setBackgroundResource(R.drawable.rounded_card_bg);
            holder.stockStatus.getBackground().setTint(context.getResources().getColor(R.color.tag_red));
        }

        Glide.with(context)
                .load(product.getImageUrl())
                .placeholder(R.mipmap.ic_launcher)
                .into(holder.image);

        holder.archiveBtn.setOnClickListener(v -> {
            boolean newState = !product.isArchived();
            db.collection("products").document(product.getId())
                    .update("archived", newState)
                    .addOnSuccessListener(aVoid -> Toast.makeText(context, newState ? "Product Archived" : "Product Restored", Toast.LENGTH_SHORT).show());
        });

        holder.editBtn.setOnClickListener(v -> {
            Intent intent = new Intent(context, AddProductActivity.class);
            intent.putExtra("productId", product.getId());
            intent.putExtra("name", product.getName());
            intent.putExtra("category", product.getCategory());
            intent.putExtra("price", product.getPrice());
            intent.putExtra("stock", product.getStock());
            intent.putExtra("imageUrl", product.getImageUrl());
            intent.putExtra("description", product.getDescription());
            intent.putExtra("isEdit", true);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    public static class AdminProductViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView name, price, stockText, stockStatus;
        MaterialButton editBtn, archiveBtn;

        public AdminProductViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.product_image_admin);
            name = itemView.findViewById(R.id.product_name_admin);
            price = itemView.findViewById(R.id.product_price_admin);
            stockText = itemView.findViewById(R.id.product_units_admin);
            stockStatus = itemView.findViewById(R.id.product_status_admin);
            editBtn = itemView.findViewById(R.id.admin_edit_btn);
            archiveBtn = itemView.findViewById(R.id.admin_archive_btn);
        }
    }
}