package com.example.buksu_eeu;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class NavContainerActivity extends AppCompatActivity implements CartManager.OnCartChangedListener {

    private BottomNavigationView bottomNavView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nav_container);
        overridePendingTransition(0, 0);

        bottomNavView = findViewById(R.id.bottom_nav_view);

        setupBottomNavigation();

        if (savedInstanceState == null) {
            loadFragment(new HomeFragment(), false);
            bottomNavView.setSelectedItemId(R.id.nav_home);
        }

        CartManager.getInstance().addChangeListener(this);
    }

    private void setupBottomNavigation() {
        bottomNavView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            
            if (item.getItemId() == R.id.nav_home) {
                selectedFragment = new HomeFragment();
            } else if (item.getItemId() == R.id.nav_cart) {
                selectedFragment = new CartFragment();
            } else if (item.getItemId() == R.id.nav_orders) {
                selectedFragment = new OrdersFragment();
            } else if (item.getItemId() == R.id.nav_profile) {
                selectedFragment = new ProfileFragment();
            }

            if (selectedFragment != null) {
                loadFragment(selectedFragment, false);
            }
            return true;
        });
    }

    private void loadFragment(Fragment fragment, boolean addToBackStack) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        if (addToBackStack) {
            transaction.addToBackStack(null);
        }
        transaction.commit();
    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            super.onBackPressed();
            overridePendingTransition(0, 0);
        } else {
            new com.google.android.material.dialog.MaterialAlertDialogBuilder(this, R.style.DarkAlertDialog)
                    .setTitle("Exit App")
                    .setMessage("Are you sure you want to exit?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        finishAffinity();
                    })
                    .setNegativeButton("No", null)
                    .show();
        }
    }

    @Override
    public void onCartChanged(int totalItems) {
        com.google.android.material.badge.BadgeDrawable badge = bottomNavView.getOrCreateBadge(R.id.nav_cart);
        if (totalItems > 0) {
            badge.setVisible(true);
            badge.setNumber(totalItems);
            badge.setBackgroundColor(getResources().getColor(R.color.tag_red));
            badge.setBadgeTextColor(getResources().getColor(R.color.white));
        } else {
            badge.setVisible(false);
            badge.clearNumber();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        CartManager.getInstance().removeChangeListener(this);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, 0);
    }
}
