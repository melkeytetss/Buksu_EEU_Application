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

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder> {

    private Context context;
    private List<CartItem> cartItems;
    private OnCartChangedListener listener;

    public interface OnCartChangedListener {
        void onCartChanged();
    }

    public CartAdapter(Context context, List<CartItem> cartItems, OnCartChangedListener listener) {
        this.context = context;
        this.cartItems = cartItems;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_cart, parent, false);
        return new CartViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CartViewHolder holder, int position) {
        CartItem item = cartItems.get(position);
        Product product = item.getProduct();

        holder.name.setText(product.getName());
        holder.price.setText(String.format("₱%.0f", product.getPrice()));
        holder.quantity.setText(String.valueOf(item.getQuantity()));

        // Show size badge if size is set
        if (item.getSize() != null && !item.getSize().isEmpty()) {
            holder.sizeText.setVisibility(android.view.View.VISIBLE);
            holder.sizeText.setText("Size: " + item.getSize());
        } else {
            holder.sizeText.setVisibility(android.view.View.GONE);
        }

        Glide.with(context)
                .load(product.getImageUrl())
                .placeholder(R.mipmap.ic_launcher)
                .into(holder.image);

        holder.btnPlus.setOnClickListener(v -> {
            if (item.getQuantity() < product.getStock()) {
                item.setQuantity(item.getQuantity() + 1);
                notifyItemChanged(position);
                listener.onCartChanged();
            }
        });

        holder.btnMinus.setOnClickListener(v -> {
            if (item.getQuantity() > 1) {
                item.setQuantity(item.getQuantity() - 1);
                notifyItemChanged(position);
                listener.onCartChanged();
            }
        });

        holder.btnRemove.setOnClickListener(v -> {
            CartManager.getInstance().removeItem(item);
            notifyDataSetChanged();
            listener.onCartChanged();
        });
    }

    @Override
    public int getItemCount() {
        return cartItems.size();
    }

    public static class CartViewHolder extends RecyclerView.ViewHolder {
        ImageView image, btnPlus, btnMinus;
        View btnRemove;
        TextView name, price, quantity, sizeText;

        public CartViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.cart_item_image);
            name = itemView.findViewById(R.id.cart_item_name);
            sizeText = itemView.findViewById(R.id.cart_item_size);
            price = itemView.findViewById(R.id.cart_item_price);
            quantity = itemView.findViewById(R.id.tv_quantity);
            btnPlus = itemView.findViewById(R.id.btn_plus);
            btnMinus = itemView.findViewById(R.id.btn_minus);
            btnRemove = itemView.findViewById(R.id.btn_remove);
        }
    }
}