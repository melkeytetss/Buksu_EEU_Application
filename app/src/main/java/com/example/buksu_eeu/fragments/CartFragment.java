package com.example.buksu_eeu;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.List;

public class CartFragment extends Fragment implements CartAdapter.OnCartChangedListener {

    private RecyclerView recyclerView;
    private CartAdapter adapter;
    private TextView totalPriceText;
    private View emptyCartView;
    private MaterialButton checkoutBtn;
    private FirebaseFirestore db;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_cart, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();

        recyclerView = view.findViewById(R.id.recycler_cart);
        totalPriceText = view.findViewById(R.id.cart_total_price);
        emptyCartView = view.findViewById(R.id.empty_cart_container);
        checkoutBtn = view.findViewById(R.id.btn_checkout);
        MaterialButton clearCartBtn = view.findViewById(R.id.btn_clear_cart);

        setupRecyclerView();

        if (clearCartBtn != null) {
            clearCartBtn.setOnClickListener(v -> {
                if (CartManager.getInstance().getCartItems().isEmpty()) return;
                
                new MaterialAlertDialogBuilder(requireContext(), R.style.DarkAlertDialog)
                        .setTitle("Clear Cart")
                        .setMessage("Are you sure you want to remove all items from your cart?")
                        .setPositiveButton("Clear All", (dialog, which) -> {
                            CartManager.getInstance().clearCart();
                            updateCartUI();
                            if (adapter != null) adapter.notifyDataSetChanged();
                            Toast.makeText(requireContext(), "Cart cleared", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }

        MaterialButton startShoppingBtn = view.findViewById(R.id.btn_start_shopping);
        if (startShoppingBtn != null) {
            startShoppingBtn.setOnClickListener(v -> {
                com.google.android.material.bottomnavigation.BottomNavigationView nav = requireActivity().findViewById(R.id.bottom_nav_view);
                if (nav != null) {
                    nav.setSelectedItemId(R.id.nav_home);
                }
            });
        }

        
        CartManager.getInstance().loadCartFromFirestore(() -> {
            if (getView() != null) {
                updateCartUI();
                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                }
            }
        });

        checkoutBtn.setOnClickListener(v -> {
            List<CartItem> cartItems = CartManager.getInstance().getCartItems();
            if (cartItems.isEmpty()) {
                showCustomErrorToast("Your cart is empty!");
            } else {
                Intent intent = new Intent(requireContext(), CheckoutActivity.class);
                requireContext().startActivity(intent);
            }
        });
    }

    private void showCustomErrorToast(String message) {
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        View layout = inflater.inflate(R.layout.custom_toast_add_to_cart, null);
        
        com.google.android.material.card.MaterialCardView card = (com.google.android.material.card.MaterialCardView) layout;
        card.setCardBackgroundColor(requireContext().getResources().getColor(R.color.tag_red));

        TextView text = layout.findViewById(R.id.toast_message);
        text.setText(message);

        Toast toast = new Toast(requireContext());
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(layout);
        toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 100);
        toast.show();
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new CartAdapter(requireContext(), CartManager.getInstance().getCartItems(), this);
        recyclerView.setAdapter(adapter);

        // Add Swipe-to-Delete functionality
        ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int swipeDir) {
                int position = viewHolder.getAdapterPosition();
                CartItem item = CartManager.getInstance().getCartItems().get(position);
                CartManager.getInstance().removeItem(item);
                adapter.notifyItemRemoved(position);
                updateCartUI();
                Toast.makeText(requireContext(), "Item removed", Toast.LENGTH_SHORT).show();
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }

    private void updateCartUI() {
        List<CartItem> cartItems = CartManager.getInstance().getCartItems();
        MaterialButton clearCartBtn = getView() != null ? getView().findViewById(R.id.btn_clear_cart) : null;

        if (cartItems.isEmpty()) {
            emptyCartView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            totalPriceText.setText("₱0.00");
            if (clearCartBtn != null) clearCartBtn.setVisibility(View.GONE);
        } else {
            emptyCartView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            double total = CartManager.getInstance().getTotalPrice();
            totalPriceText.setText(String.format("₱%.0f", total));
            if (clearCartBtn != null) clearCartBtn.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onCartChanged() {
        updateCartUI();
    }
}
