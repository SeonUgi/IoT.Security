package com.example.iotsecurity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;

import android.content.Intent;
import android.os.Bundle;

import com.google.android.material.tabs.TabLayout;
import java.util.ArrayList;

/**
 * Product를 클릭했을 때 실행되는 activity
 * 장치를 제어할 수 있는 Control Fragment
 * 장치에 대한 정보와 DPD 출력이 가능한 Detail Fragment
 * 위 2개의 fragment를 제어함.
 *
 * ver. 2020.09.27 : 클릭된 product 정보를 bundle로 직접 fragment에 전달
 * 추후 db에 대한 id를 전달하여 fragment에서 id로 조회 가능하게 업데이트
 */
public class ProductDetail extends AppCompatActivity {

    ViewPager pager;
    Product product;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.product_detail_activity);

        pager = findViewById(R.id.pager);
        pager.setOffscreenPageLimit(3);

        // fragment에 전달할 객체
        Intent intent = getIntent();
        product = (Product)intent.getSerializableExtra("product");
        Bundle bundle = new Bundle();
        bundle.putSerializable("product", product);

        // fragment 전용 adapter
        devicePagerAdapter adapter = new devicePagerAdapter(getSupportFragmentManager());

        // Control Panel
        ControlFragment controlFragment = new ControlFragment();
        controlFragment.setArguments(bundle);
        adapter.addItem(controlFragment);

        // Device Detail
        DetailFragment detailFragment = new DetailFragment();
        // fragment에 데이터 전달
        detailFragment.setArguments(bundle);
        // 어댑터에 fragment 추가
        adapter.addItem(detailFragment);



        pager.setAdapter(adapter);

        TabLayout tabs = findViewById(R.id.tabs);
        tabs.addOnTabSelectedListener(new TabLayout.ViewPagerOnTabSelectedListener(pager));
        pager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabs));

    }

    class devicePagerAdapter extends FragmentStatePagerAdapter {
        ArrayList<Fragment> fragments = new ArrayList<Fragment>();

        public devicePagerAdapter(@NonNull FragmentManager fm) {
            super(fm);
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            return fragments.get(position);
        }

        @Override
        public int getCount() {
            return fragments.size();
        }

        public void addItem(Fragment item) {
            fragments.add(item);
        }
    }
}