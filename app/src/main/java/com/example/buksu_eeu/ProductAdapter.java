package com.example.buksu_eeu;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import java.util.ArrayList;
import java.util.List;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {

    private Context context;
    private List<Product> productList;

    public ProductAdapter(Context context, List<Product> productList) {
        this.context = context;
        this.productList = productList;
    }

    public void updateList(List<Product> newList) {
        this.productList = new ArrayList<>(newList);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_product, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        Product product = productList.get(position);
        holder.name.setText(product.getName());
        holder.category.setText(product.getCategory());
        holder.price.setText(String.format("₱%.0f", product.getPrice()));

        if (product.getStock() <= 0) {
            holder.stockBadge.setVisibility(View.VISIBLE);
            holder.stockBadge.setText("Out of Stock");
            holder.addToCartBtn.setAlpha(0.3f);
            holder.addToCartBtn.setEnabled(false);
        } else if (product.getStock() <= 5) {
            holder.stockBadge.setVisibility(View.VISIBLE);
            holder.stockBadge.setText(String.format("Only %d left", product.getStock()));
            holder.addToCartBtn.setAlpha(1f);
            holder.addToCartBtn.setEnabled(true);
        } else {
            holder.stockBadge.setVisibility(View.GONE);
            holder.addToCartBtn.setAlpha(1f);
            holder.addToCartBtn.setEnabled(true);
        }

        Glide.with(context)
                .load(product.getImageUrl())
                .placeholder(R.mipmap.ic_launcher)
                .into(holder.image);

        holder.addToCartBtn.setOnClickListener(v -> {
            if (product.getStock() <= 0) {
                showCustomToast("This product is out of stock");
                return;
            }
            if (requiresSize(product.getCategory())) {
                showSizeDialog(product);
            } else {
                CartManager.getInstance().addToCart(product);
                showCustomToast(product.getName() + " added to cart");
            }
        });

        holder.itemView.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(context, ProductDetailActivity.class);
            intent.putExtra("product", product);
            context.startActivity(intent);
        });
    }

    private boolean requiresSize(String category) {
        if (category == null) return false;
        return category.contains("PE") || category.contains("School Uniform");
    }

    private void showSizeDialog(Product product) {
        BottomSheetDialog dialog = new BottomSheetDialog(context, R.style.BottomSheetDialogTheme);
        View sheetView = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_size_picker, null);
        dialog.setContentView(sheetView);

        TextView productTitle = sheetView.findViewById(R.id.size_sheet_product_name);
        ChipGroup chipGroup = sheetView.findViewById(R.id.size_chip_group);
        View confirmBtn = sheetView.findViewById(R.id.btn_confirm_size);

        productTitle.setText(product.getName());

        final String[] selectedSize = {null};

        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                Chip chip = sheetView.findViewById(checkedIds.get(0));
                selectedSize[0] = chip.getText().toString();
            } else {
                selectedSize[0] = null;
            }
        });

        confirmBtn.setOnClickListener(v -> {
            if (selectedSize[0] == null) {
                showCustomToast("Please select a size");
                return;
            }
            CartManager.getInstance().addToCart(product, 1, selectedSize[0]);
            showCustomToast(product.getName() + " (Size: " + selectedSize[0] + ") added to cart");
            dialog.dismiss();
        });

        dialog.show();
    }

    private void showCustomToast(String message) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View layout = inflater.inflate(R.layout.custom_toast_add_to_cart, null);

        TextView text = layout.findViewById(R.id.toast_message);
        text.setText(message);

        Toast toast = new Toast(context);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(layout);
        toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 100);
        toast.show();
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    public static class ProductViewHolder extends RecyclerView.ViewHolder {
        ImageView image, addToCartBtn;
        TextView name, category, price, stockBadge;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.product_image);
            name = itemView.findViewById(R.id.product_name);
            category = itemView.findViewById(R.id.product_category);
            price = itemView.findViewById(R.id.product_price);
            stockBadge = itemView.findViewById(R.id.product_stock_badge);
            addToCartBtn = itemView.findViewById(R.id.btn_add_to_cart);
        }
    }
}
