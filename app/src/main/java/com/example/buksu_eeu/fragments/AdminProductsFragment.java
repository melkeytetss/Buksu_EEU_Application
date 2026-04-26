package com.example.buksu_eeu.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.buksu_eeu.AddProductActivity;
import com.example.buksu_eeu.AdminProductAdapter;
import com.example.buksu_eeu.Product;
import com.example.buksu_eeu.R;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.List;

public class AdminProductsFragment extends Fragment {

    private FirebaseFirestore db;
    private RecyclerView recyclerView;
    private AdminProductAdapter adapter;
    private List<Product> originalProductList = new ArrayList<>();
    private List<Product> displayedProductList = new ArrayList<>();

    private TextInputEditText searchInput;
    private ChipGroup categoryChipGroup;

    private String currentSearchQuery = "";
    private String currentCategoryFilter = "All";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_products, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = FirebaseFirestore.getInstance();

        searchInput = view.findViewById(R.id.search_product_input);
        categoryChipGroup = view.findViewById(R.id.category_chip_group);
        recyclerView = view.findViewById(R.id.recycler_products_admin);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new AdminProductAdapter(requireContext(), displayedProductList);
        recyclerView.setAdapter(adapter);

        ExtendedFloatingActionButton fab = view.findViewById(R.id.fab_add_product);
        fab.setOnClickListener(v -> startActivity(new Intent(requireContext(), AddProductActivity.class)));

        setupFilters();
        loadProducts();
    }

    private void setupFilters() {
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString().trim().toLowerCase();
                filterProducts();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        categoryChipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                currentCategoryFilter = "All";
            } else {
                int checkedId = checkedIds.get(0);
                if (checkedId == R.id.chip_pe_upper_male) currentCategoryFilter = "PE Upper (Male)";
                else if (checkedId == R.id.chip_pe_upper_female) currentCategoryFilter = "PE Upper (Female)";
                else if (checkedId == R.id.chip_pe_lower_male) currentCategoryFilter = "PE Lower (Male)";
                else if (checkedId == R.id.chip_pe_lower_female) currentCategoryFilter = "PE Lower (Female)";
                else if (checkedId == R.id.chip_school_upper_male) currentCategoryFilter = "School Uniform Upper (Male)";
                else if (checkedId == R.id.chip_school_upper_female) currentCategoryFilter = "School Uniform Upper (Female)";
                else if (checkedId == R.id.chip_school_lower_male) currentCategoryFilter = "School Uniform Lower (Male)";
                else if (checkedId == R.id.chip_school_lower_female) currentCategoryFilter = "School Uniform Lower (Female)";
                else if (checkedId == R.id.chip_accessories) currentCategoryFilter = "Accessories";
                else currentCategoryFilter = "All";
            }
            filterProducts();
        });
    }

    private void loadProducts() {
        db.collection("products")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;
                    originalProductList.clear();
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        Product product = doc.toObject(Product.class);
                        if (product != null) {
                            product.setId(doc.getId());
                            originalProductList.add(product);
                        }
                    }
                    filterProducts();
                });
    }

    private void filterProducts() {
        displayedProductList.clear();

        for (Product product : originalProductList) {
            boolean matchesSearch = true;
            boolean matchesCategory = true;

            if (!currentSearchQuery.isEmpty()) {
                String productName = product.getName() != null ? product.getName().toLowerCase() : "";
                if (!productName.contains(currentSearchQuery)) {
                    matchesSearch = false;
                }
            }

            if (!currentCategoryFilter.equals("All")) {
                String productCategory = product.getCategory() != null ? product.getCategory() : "";
                if (!productCategory.equalsIgnoreCase(currentCategoryFilter)) {
                    matchesCategory = false;
                }
            }

            if (matchesSearch && matchesCategory) {
                displayedProductList.add(product);
            }
        }
        
        adapter.notifyDataSetChanged();
    }
}
