package com.jieli.broadcastbox;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.jieli.broadcastbox.multidevice.MultiOTAProcessor;
import com.jieli.jl_bt_ota.util.JL_Log;
import com.jieli.otasdk.R;
import com.jieli.otasdk.ui.base.BaseActivity;

public class BroadcastBoxActivity extends BaseActivity {
    private final String tag = getClass().getSimpleName();
    public boolean isSkipDestroyViewModel = false;
    Fragment[] fragments = new Fragment[]{new ConnectFragment(), new FilesFragment(), new UpgradeFragment()};

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_broadcast_box);

        BottomNavigationView bottomNav = findViewById(R.id.bnv_bottomBar);
        bottomNav.setItemIconTintList(null);// 不隐藏显示不出原来的logo

        Toolbar toolbar = findViewById(R.id.toolbar);
        TextView tvTitle = findViewById(R.id.toolbar_title);
        ViewPager2 viewPager2 = findViewById(R.id.vp2_container);
        tvTitle.setText(getString(R.string.connect));
        toolbar.inflateMenu(R.menu.menu_toolbar);
        toolbar.getMenu().findItem(R.id.switch_function).setVisible(true);
        toolbar.getMenu().findItem(R.id.choose_way).setVisible(false);

        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Fragment fragment;
                View menuItemView;
                switch (item.getItemId()) {
                    case R.id.switch_function:
                        menuItemView = findViewById(R.id.switch_function);
                        switchPopupWindow(BroadcastBoxActivity.this, menuItemView);
                        break;
                    case R.id.choose_way:
                        fragment =fragments[1]; //navHostFragment.getChildFragmentManager().findFragmentById(R.id.fcv_host_fragment);
                        menuItemView = findViewById(R.id.choose_way);
                        if (fragment != null) {
                            ((FilesFragment) fragment).chooseFilePopupWindow(menuItemView);
                        }
                        break;
                }
                return false;
            }
        });

        bottomNav.setOnItemSelectedListener(item -> {
            JL_Log.i(tag, "getTitle=" + item.getTitle());
            if (item.getTitle().equals(getString(R.string.connect))) {
                tvTitle.setText(getString(R.string.connect));
                viewPager2.setCurrentItem(0, false);
                toolbar.getMenu().findItem(R.id.switch_function).setVisible(true);
                toolbar.getMenu().findItem(R.id.choose_way).setVisible(false);
            } else if (item.getTitle().equals(getString(R.string.files))) {
                tvTitle.setText(getString(R.string.files));
                toolbar.getMenu().findItem(R.id.switch_function).setVisible(false);
                toolbar.getMenu().findItem(R.id.choose_way).setVisible(true);
                viewPager2.setCurrentItem(1, false);
            } else if (item.getTitle().equals(getString(R.string.upgrade))) {
                tvTitle.setText(getString(R.string.upgrade));
                viewPager2.setCurrentItem(2, false);
                toolbar.getMenu().findItem(R.id.switch_function).setVisible(false);
                toolbar.getMenu().findItem(R.id.choose_way).setVisible(false);
            }
            return true;
        });
        viewPager2.setUserInputEnabled(false);
        viewPager2.setOffscreenPageLimit(3);
        viewPager2.setAdapter(new FragmentStateAdapter(this) {
            @NonNull
            @Override
            public Fragment createFragment(int position) {
                return fragments[position];
            }

            @Override
            public int getItemCount() {
                return fragments.length;
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        MultiOTAProcessor.Companion.getInstance().destroy();
    }

}