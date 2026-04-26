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
import java.util.List;

public class CheckoutItemAdapter extends RecyclerView.Adapter<CheckoutItemAdapter.CheckoutItemViewHolder> {

    private Context context;
    private List<CartItem> cartItems;

    public CheckoutItemAdapter(Context context, List<CartItem> cartItems) {
        this.context = context;
        this.cartItems = cartItems;
    }

    @NonNull
    @Override
    public CheckoutItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_checkout, parent, false);
        return new CheckoutItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CheckoutItemViewHolder holder, int position) {
        CartItem cartItem = cartItems.get(position);
        Product product = cartItem.getProduct();

        if (product != null) {
            holder.tvName.setText(product.getName());
            holder.tvPrice.setText(String.format("₱%.0f", product.getPrice()));
            holder.tvQuantity.setText("x" + cartItem.getQuantity());

            // Show size badge if applicable
            if (cartItem.getSize() != null && !cartItem.getSize().isEmpty()) {
                holder.tvSize.setVisibility(View.VISIBLE);
                holder.tvSize.setText("Size: " + cartItem.getSize());
            } else {
                holder.tvSize.setVisibility(View.GONE);
            }

            if (product.getImageUrl() != null && !product.getImageUrl().isEmpty()) {
                Glide.with(context).load(product.getImageUrl()).into(holder.imgProduct);
            } else {
                holder.imgProduct.setImageResource(R.drawable.logo);
            }
        }
    }

    @Override
    public int getItemCount() {
        return cartItems.size();
    }

    public static class CheckoutItemViewHolder extends RecyclerView.ViewHolder {
        ImageView imgProduct;
        TextView tvName, tvPrice, tvQuantity, tvSize;

        public CheckoutItemViewHolder(@NonNull View itemView) {
            super(itemView);
            imgProduct = itemView.findViewById(R.id.img_checkout_item);
            tvName = itemView.findViewById(R.id.tv_checkout_item_name);
            tvSize = itemView.findViewById(R.id.tv_checkout_item_size);
            tvPrice = itemView.findViewById(R.id.tv_checkout_item_price);
            tvQuantity = itemView.findViewById(R.id.tv_checkout_item_quantity);
        }
    }
}
