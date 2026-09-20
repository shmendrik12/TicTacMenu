package com.example.tictacmenu;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationView;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;

/** Main navigation drawer introduced in the 014a/014b course steps. */
public class MenuActivity extends AppCompatActivity {
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigation_view);
        MaterialToolbar toolbar = findViewById(R.id.menu_toolbar);

        ActionBarDrawerToggle drawerToggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.drawer_open, R.string.drawer_close);
        drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();

        View header = navigationView.getHeaderView(0);
        TextView name = header.findViewById(R.id.header_user_name);
        String displayName = SessionStore.displayName(this);
        name.setText(displayName.isEmpty() ? getString(R.string.guest) : displayName);

        navigationView.setNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                showFragment(new HomeFragment());
            } else if (itemId == R.id.nav_local_game) {
                startActivity(new Intent(this, MainActivity.class));
            } else if (itemId == R.id.nav_rtdb_prep) {
                startActivity(new Intent(this, Main2Activity.class));
            } else if (itemId == R.id.nav_profile) {
                showFragment(new ProfileFragment());
            } else if (itemId == R.id.nav_disconnect) {
                logoutAndOpenLogin();
                return true;
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        if (savedInstanceState == null) {
            showFragment(new HomeFragment());
            navigationView.setCheckedItem(R.id.nav_home);
        }
    }

    public void logoutAndOpenLogin() {
        SessionStore.signOut(this);
        FirebaseAuth.getInstance().signOut();
        GoogleSignIn.getClient(this, GoogleSignInOptions.DEFAULT_SIGN_IN).signOut();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void showFragment(@NonNull Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.content_container, fragment)
                .commit();
    }
}
