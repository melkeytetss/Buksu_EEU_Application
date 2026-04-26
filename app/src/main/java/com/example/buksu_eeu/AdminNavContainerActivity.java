package com.example.buksu_eeu;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.example.buksu_eeu.fragments.AdminDashboardFragment;
import com.example.buksu_eeu.fragments.AdminOrdersFragment;
import com.example.buksu_eeu.fragments.AdminProductsFragment;
import com.example.buksu_eeu.fragments.AdminProfileFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class AdminNavContainerActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_nav_container);

        BottomNavigationView bottomNav = findViewById(R.id.admin_bottom_nav);

        // Default tab
        if (savedInstanceState == null) {
            loadFragment(new AdminDashboardFragment());
            bottomNav.setSelectedItemId(R.id.admin_nav_dashboard);
        }

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            Fragment selected = null;

            if (id == R.id.admin_nav_dashboard) {
                selected = new AdminDashboardFragment();
            } else if (id == R.id.admin_nav_products) {
                selected = new AdminProductsFragment();
            } else if (id == R.id.admin_nav_orders) {
                selected = new AdminOrdersFragment();
            } else if (id == R.id.admin_nav_profile) {
                selected = new AdminProfileFragment();
            }

            if (selected != null) {
                loadFragment(selected);
                return true;
            }
            return false;
        });
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.admin_fragment_container, fragment)
                .commit();
    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            super.onBackPressed();
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
}
