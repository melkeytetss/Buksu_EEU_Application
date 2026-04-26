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

        setupCategoryButtons(getView());
    }

    private void setupCategoryButtons(View view) {
        if (view == null) return;
        
        com.google.android.material.button.MaterialButton btnAll = view.findViewById(R.id.btn_filter_prod_all);
        com.google.android.material.button.MaterialButton btnPeUpperMale = view.findViewById(R.id.btn_filter_pe_upper_male);
        com.google.android.material.button.MaterialButton btnPeUpperFemale = view.findViewById(R.id.btn_filter_pe_upper_female);
        com.google.android.material.button.MaterialButton btnPeLowerMale = view.findViewById(R.id.btn_filter_pe_lower_male);
        com.google.android.material.button.MaterialButton btnPeLowerFemale = view.findViewById(R.id.btn_filter_pe_lower_female);
        com.google.android.material.button.MaterialButton btnSchoolUpperMale = view.findViewById(R.id.btn_filter_school_upper_male);
        com.google.android.material.button.MaterialButton btnSchoolUpperFemale = view.findViewById(R.id.btn_filter_school_upper_female);
        com.google.android.material.button.MaterialButton btnSchoolLowerMale = view.findViewById(R.id.btn_filter_school_lower_male);
        com.google.android.material.button.MaterialButton btnSchoolLowerFemale = view.findViewById(R.id.btn_filter_school_lower_female);
        com.google.android.material.button.MaterialButton btnAccessories = view.findViewById(R.id.btn_filter_accessories);

        com.google.android.material.button.MaterialButton[] allButtons = {
            btnAll, btnPeUpperMale, btnPeUpperFemale, btnPeLowerMale, btnPeLowerFemale,
            btnSchoolUpperMale, btnSchoolUpperFemale, btnSchoolLowerMale, btnSchoolLowerFemale, btnAccessories
        };

        String[] categoryNames = {
            "All", "PE Upper (Male)", "PE Upper (Female)", "PE Lower (Male)", "PE Lower (Female)",
            "School Uniform Upper (Male)", "School Uniform Upper (Female)", "School Uniform Lower (Male)", "School Uniform Lower (Female)", "Accessories"
        };

        for (int i = 0; i < allButtons.length; i++) {
            com.google.android.material.button.MaterialButton btn = allButtons[i];
            String categoryName = categoryNames[i];

            btn.setOnClickListener(v -> {
                if (!currentCategoryFilter.equals(categoryName)) {
                    currentCategoryFilter = categoryName;

                    int activeColor = requireContext().getColor(R.color.btn_blue);
                    int activeTextColor = requireContext().getColor(R.color.white);
                    int inactiveColor = requireContext().getColor(R.color.soft_white);
                    int inactiveTextColor = requireContext().getColor(R.color.black);

                    for (int j = 0; j < allButtons.length; j++) {
                        boolean isActive = currentCategoryFilter.equals(categoryNames[j]);
                        allButtons[j].setBackgroundTintList(android.content.res.ColorStateList.valueOf(isActive ? activeColor : inactiveColor));
                        allButtons[j].setTextColor(isActive ? activeTextColor : inactiveTextColor);
                    }

                    filterProducts();
                }
            });
        }
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
