package com.example.buksu_eeu;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;

public class ProductSliderAdapter extends RecyclerView.Adapter<ProductSliderAdapter.SliderViewHolder> {

    private Context context;
    private List<Product> sliderItems;

    public ProductSliderAdapter(Context context, List<Product> sliderItems) {
        this.context = context;
        this.sliderItems = sliderItems;
    }

    @NonNull
    @Override
    public SliderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_slider_product, parent, false);
        return new SliderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SliderViewHolder holder, int position) {
        Product product = sliderItems.get(position);
        
        holder.title.setText(product.getName());
        holder.price.setText(String.format("₱%.2f", product.getPrice()));

        if (product.getImageUrl() != null && !product.getImageUrl().isEmpty()) {
            Glide.with(context)
                .load(product.getImageUrl())
                .placeholder(R.drawable.logo)
                .into(holder.image);
        } else {
            holder.image.setImageResource(R.drawable.logo);
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ProductDetailActivity.class);
            intent.putExtra("productId", product.getId());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return sliderItems.size();
    }

    public void updateData(List<Product> newItems) {
        this.sliderItems = newItems;
        notifyDataSetChanged();
    }

    static class SliderViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView title, price;

        SliderViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.slider_image);
            title = itemView.findViewById(R.id.slider_title);
            price = itemView.findViewById(R.id.slider_price);
        }
    }
}
