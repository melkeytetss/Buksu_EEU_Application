package com.example.buksu_eeu;

import android.util.Log;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CartManager {
    private static CartManager instance;
    private List<CartItem> cartItems;
    private FirebaseFirestore db;
    private String userId;
    private static final String TAG = "CartManager";
    private OnCartLoadedListener loadListener;

    public interface OnCartLoadedListener {
        void onCartLoaded();
    }

    public interface OnCartChangedListener {
        void onCartChanged(int totalItems);
    }

    private List<OnCartChangedListener> changeListeners = new ArrayList<>();

    private CartManager() {
        cartItems = new ArrayList<>();
        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getUid();
    }

    public static synchronized CartManager getInstance() {
        if (instance == null) {
            instance = new CartManager();
        }
        return instance;
    }

    public void addChangeListener(OnCartChangedListener listener) {
        if (!changeListeners.contains(listener)) {
            changeListeners.add(listener);
        }
        listener.onCartChanged(getTotalItemsCount());
    }

    public void removeChangeListener(OnCartChangedListener listener) {
        changeListeners.remove(listener);
    }

    private void notifyListeners() {
        int count = getTotalItemsCount();
        for (OnCartChangedListener listener : changeListeners) {
            listener.onCartChanged(count);
        }
    }

    public int getTotalItemsCount() {
        int count = 0;
        for (CartItem item : cartItems) {
            count += item.getQuantity();
        }
        return count;
    }

    public void addToCart(Product product) {
        addToCart(product, 1, null);
    }

    public void addToCart(Product product, int quantityToAdd) {
        addToCart(product, quantityToAdd, null);
    }

    public void addToCart(Product product, int quantityToAdd, String size) {
        if (product == null || product.getId() == null) return;
        
        String key = product.getId() + (size != null ? "_" + size : "");
        boolean found = false;
        for (CartItem item : cartItems) {
            String itemKey = (item.getProduct() != null ? item.getProduct().getId() : "") 
                           + (item.getSize() != null ? "_" + item.getSize() : "");
            if (itemKey.equals(key)) {
                item.setQuantity(item.getQuantity() + quantityToAdd);
                found = true;
                break;
            }
        }
        if (!found) {
            cartItems.add(new CartItem(product, quantityToAdd, size));
        }
        saveCartToFirestore();
    }

    public List<CartItem> getCartItems() {
        return cartItems;
    }

    public void removeItem(CartItem item) {
        cartItems.remove(item);
        saveCartToFirestore();
    }

    public void setQuantity(CartItem item, int quantity) {
        item.setQuantity(quantity);
        saveCartToFirestore();
    }

    public double getTotalPrice() {
        double total = 0;
        for (CartItem item : cartItems) {
            if (item.getProduct() != null) {
                total += item.getProduct().getPrice() * item.getQuantity();
            }
        }
        return total;
    }

    public void clearCart() {
        cartItems.clear();
        saveCartToFirestore();
    }

    private void saveCartToFirestore() {
        userId = FirebaseAuth.getInstance().getUid();
        if (userId == null) return;

        Map<String, Object> cartData = new HashMap<>();
        cartData.put("items", cartItems);

        db.collection("carts").document(userId).set(cartData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Cart saved successfully");
                    notifyListeners();
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error saving cart", e));
    }

    public void loadCartFromFirestore(OnCartLoadedListener listener) {
        this.loadListener = listener;
        userId = FirebaseAuth.getInstance().getUid();
        if (userId == null) return;

        db.collection("carts").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        List<Map<String, Object>> items = (List<Map<String, Object>>) documentSnapshot.get("items");
                        if (items != null) {
                            cartItems.clear();
                            for (Map<String, Object> itemMap : items) {
                                try {
                                    Map<String, Object> productMap = (Map<String, Object>) itemMap.get("product");
                                    long quantity = (long) itemMap.get("quantity");
                                    
                                    Product product = new Product();
                                    product.setId((String) productMap.get("id"));
                                    product.setName((String) productMap.get("name"));
                                    product.setCategory((String) productMap.get("category"));
                                    product.setPrice(((Number) productMap.get("price")).doubleValue());
                                    product.setStock(((Number) productMap.get("stock")).intValue());
                                    product.setImageUrl((String) productMap.get("imageUrl"));
                                    product.setDescription((String) productMap.get("description"));
                                    product.setArchived((boolean) productMap.get("archived"));

                                    cartItems.add(new CartItem(product, (int) quantity, (String) itemMap.get("size")));
                                } catch (Exception e) {
                                    Log.e(TAG, "Error parsing cart item", e);
                                }
                            }
                        }
                    }
                    if (loadListener != null) {
                        loadListener.onCartLoaded();
                    }
                    notifyListeners();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading cart", e);
                    if (loadListener != null) {
                        loadListener.onCartLoaded();
                    }
                    notifyListeners();
                });
    }
}
