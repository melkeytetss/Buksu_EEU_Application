package com.example.buksu_eeu;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.bumptech.glide.Glide;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class AddProductActivity extends AppCompatActivity {

    private ImageView selectedImage;
    private TextInputEditText nameInput, priceInput, stockInput, descInput;
    private AutoCompleteTextView categoryDropdown;
    private MaterialButton saveBtn;
    private ProgressBar progressBar;
    private Uri imageUri;
    
    private FirebaseFirestore db;
    private String productId;
    private String existingImageUrl;
    private boolean isEditMode = false;

    private static final String CLOUD_NAME = "dv2hhwzre";
    private static final String UPLOAD_PRESET = "buksu_products"; 

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    imageUri = result.getData().getData();
                    selectedImage.setImageURI(imageUri);
                    selectedImage.setAlpha(1.0f);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_product);

        db = FirebaseFirestore.getInstance();
        
        try {
            Map config = new HashMap();
            config.put("cloud_name", CLOUD_NAME);
            MediaManager.init(this, config);
        } catch (Exception e) {
            // Already initialized
        }

        initViews();
        setupCategoryDropdown();
        checkEditMode();

        findViewById(R.id.image_picker_card).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            imagePickerLauncher.launch(intent);
        });

        saveBtn.setOnClickListener(v -> handleSaveProduct());
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar_add_product);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            getSupportActionBar().setTitle("");
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        selectedImage = findViewById(R.id.selected_image);
        nameInput = findViewById(R.id.product_name_input);
        priceInput = findViewById(R.id.price_input);
        stockInput = findViewById(R.id.stock_input);
        descInput = findViewById(R.id.description_input);
        categoryDropdown = findViewById(R.id.category_dropdown);
        saveBtn = findViewById(R.id.save_product_btn);
        progressBar = findViewById(R.id.upload_progress);
    }

    private void setupCategoryDropdown() {
        String[] categories = {
            "Apparel", "Accessories", "Bags", "Headwear",
            "Drinkware", "Home Decor", "Gift Sets & Boxes",
            "Crafts & Collectibles"
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, categories);
        categoryDropdown.setAdapter(adapter);
    }

    private void checkEditMode() {
        Intent intent = getIntent();
        if (intent.hasExtra("isEdit")) {
            isEditMode = true;
            productId = intent.getStringExtra("productId");
            nameInput.setText(intent.getStringExtra("name"));
            categoryDropdown.setText(intent.getStringExtra("category"), false);
            priceInput.setText(String.valueOf(intent.getDoubleExtra("price", 0.0)));
            stockInput.setText(String.valueOf(intent.getIntExtra("stock", 0)));
            descInput.setText(intent.getStringExtra("description"));
            existingImageUrl = intent.getStringExtra("imageUrl");
            
            Glide.with(this).load(existingImageUrl).into(selectedImage);
            selectedImage.setAlpha(1.0f);
            
            saveBtn.setText("Update Product");
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Edit Product");
            }
        }
    }

    private void handleSaveProduct() {
        String name = nameInput.getText().toString().trim();
        String price = priceInput.getText().toString().trim();
        String stock = stockInput.getText().toString().trim();
        String category = categoryDropdown.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(price) || TextUtils.isEmpty(category)) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (imageUri == null && !isEditMode) {
            Toast.makeText(this, "Please select an image", Toast.LENGTH_SHORT).show();
            return;
        }

        saveBtn.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);

        if (imageUri != null) {
            uploadImageToCloudinary(name, category, Double.parseDouble(price), Integer.parseInt(stock), descInput.getText().toString().trim());
        } else {
            saveToFirestore(name, category, Double.parseDouble(price), Integer.parseInt(stock), existingImageUrl, descInput.getText().toString().trim());
        }
    }

    private void uploadImageToCloudinary(String name, String category, double price, int stock, String description) {
        MediaManager.get().upload(imageUri)
                .unsigned(UPLOAD_PRESET)
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) {}

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String imageUrl = (String) resultData.get("secure_url");
                        saveToFirestore(name, category, price, stock, imageUrl, description);
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        saveBtn.setEnabled(true);
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(AddProductActivity.this, "Upload failed: " + error.getDescription(), Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {}
                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {}
                }).dispatch();
    }

    private void saveToFirestore(String name, String category, double price, int stock, String imageUrl, String description) {
        Map<String, Object> product = new HashMap<>();
        product.put("name", name);
        product.put("category", category);
        product.put("price", price);
        product.put("stock", stock);
        product.put("imageUrl", imageUrl);
        product.put("description", description);
        product.put("timestamp", System.currentTimeMillis());

        if (isEditMode) {
            db.collection("products").document(productId).update(product)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Product updated!", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        saveBtn.setEnabled(true);
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            product.put("archived", false);
            db.collection("products").add(product)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(this, "Product added!", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        saveBtn.setEnabled(true);
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "Save failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }
}