package edu.osu.recordvideo;

import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;

/*
 * Display video record and playback Fragments via a ViewPager. This
 * code is based on Chap. 11 of:
 *
 * Bill Phillips, Chris Stewart, and Kristin Marsicano, "Android
 * Programming: The Big Nerd Ranch Guide," 3rd ed., Pearson/Big Nerd
 * Ranch, 2017
 *
 * and Mohit Gupt's blog post:
 *
 * https://www.truiton.com/2015/06/android-tabs-example-fragments-viewpager/
 */
public class VideoPagerActivity extends AppCompatActivity implements TabLayout.OnTabSelectedListener {

    private Toolbar mToolbar;
    private TabLayout mTabLayout;
    private ViewPager mViewPager;
    private PagerAdapter mPagerAdapter;

    private final String TAG = getClass().getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_pager);
        mToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        mTabLayout = findViewById(R.id.tab_layout);
        mTabLayout.addTab(mTabLayout.newTab().setText("Record Video"));
        mTabLayout.addTab(mTabLayout.newTab().setText("Play Video"));
        mTabLayout.setTabGravity(TabLayout.GRAVITY_FILL);

        mViewPager = findViewById(R.id.view_pager);
        mPagerAdapter = new PagerAdapter(getSupportFragmentManager(), mTabLayout.getTabCount());
        mViewPager.setAdapter(mPagerAdapter);
        mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(mTabLayout));
        mTabLayout.setOnTabSelectedListener(this);
    }

    @Override
    public void onTabSelected(TabLayout.Tab tab) {
        Log.d(TAG, "onTabSelected(tab " + tab.getPosition() + ")");
        mViewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(TabLayout.Tab tab) {
        Log.d(TAG, "onTabUnselected(tab " + tab.getPosition() + ")");
    }

    @Override
    public void onTabReselected(TabLayout.Tab tab) {
        Log.d(TAG, "onTabReselected(tab " + tab.getPosition() + ")");
    }
}
