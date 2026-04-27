package com.example.buksu_eeu;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

public class ProductDetailActivity extends AppCompatActivity {

    private ImageView productImage;
    private TextView productName, productCategory, productPrice, productStock, productDescription, tvQuantity;
    private MaterialButton btnAddToCart;
    private ImageView btnMinus, btnPlus;
    
    private Product product;
    private int quantity = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_detail);

        product = (Product) getIntent().getSerializableExtra("product");
        if (product == null) {
            finish();
            return;
        }

        initViews();
        displayProductDetails();
        setupClickListeners();
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar_detail);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        productImage = findViewById(R.id.detail_product_image);
        productName = findViewById(R.id.detail_product_name);
        productCategory = findViewById(R.id.detail_product_category);
        productPrice = findViewById(R.id.detail_product_price);
        productStock = findViewById(R.id.detail_product_stock);
        productDescription = findViewById(R.id.detail_product_description);
        tvQuantity = findViewById(R.id.tv_quantity);
        btnAddToCart = findViewById(R.id.btn_detail_add_to_cart);
        btnMinus = findViewById(R.id.btn_minus);
        btnPlus = findViewById(R.id.btn_plus);
    }

    private void displayProductDetails() {
        productName.setText(product.getName());
        productCategory.setText(product.getCategory());
        productPrice.setText(String.format("₱%.0f", product.getPrice()));
        productStock.setText("In Stock: " + product.getStock());
        
        if (product.getDescription() != null && !product.getDescription().isEmpty()) {
            productDescription.setText(product.getDescription());
            productDescription.setVisibility(View.VISIBLE);
        } else {
            productDescription.setText("No description available for this product.");
            productDescription.setVisibility(View.VISIBLE);
        }

        Glide.with(this)
                .load(product.getImageUrl())
                .placeholder(R.mipmap.ic_launcher)
                .into(productImage);
        
        if (product.getStock() <= 0) {
            btnAddToCart.setEnabled(false);
            btnAddToCart.setText("Out of Stock");
            productStock.setTextColor(getResources().getColor(R.color.tag_red));
        }
    }

    private void setupClickListeners() {
        btnMinus.setOnClickListener(v -> {
            if (quantity > 1) {
                quantity--;
                tvQuantity.setText(String.valueOf(quantity));
            }
        });

        btnPlus.setOnClickListener(v -> {
            if (quantity < product.getStock()) {
                quantity++;
                tvQuantity.setText(String.valueOf(quantity));
            } else {
                Toast.makeText(this, "Maximum stock reached", Toast.LENGTH_SHORT).show();
            }
        });

        btnAddToCart.setOnClickListener(v -> {
            if (requiresSize(product.getCategory())) {
                showSizeDialog();
            } else {
                CartManager.getInstance().addToCart(product, quantity);
                showCustomToast(product.getName() + " added to cart");
                finish();
            }
        });
    }

    private boolean requiresSize(String category) {
        if (category == null) return false;
        return category.equalsIgnoreCase("Apparel");
    }

    private void showSizeDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(this, R.style.BottomSheetDialogTheme);
        View sheetView = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_size_picker, null);
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
            CartManager.getInstance().addToCart(product, quantity, selectedSize[0]);
            showCustomToast(product.getName() + " (Size: " + selectedSize[0] + ") added to cart");
            dialog.dismiss();
            finish();
        });

        dialog.show();
    }

    private void showCustomToast(String message) {
        LayoutInflater inflater = getLayoutInflater();
        View layout = inflater.inflate(R.layout.custom_toast_add_to_cart, null);

        TextView text = layout.findViewById(R.id.toast_message);
        text.setText(message);

        Toast toast = new Toast(getApplicationContext());
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(layout);
        toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 100);
        toast.show();
    }
}
