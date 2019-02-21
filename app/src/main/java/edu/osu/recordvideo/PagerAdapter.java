package edu.osu.recordvideo;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
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
public class PagerAdapter extends FragmentStatePagerAdapter {
    private int mNumTabs;

    private final String TAG = getClass().getSimpleName();

    public PagerAdapter(FragmentManager fm, int numTabs) {
        super(fm);
        mNumTabs = numTabs;
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                VideoRecordFragment videoRecordFragment = new VideoRecordFragment();
                return videoRecordFragment;
            case 1:
                VideoPlayFragment videoPlayFragment = new VideoPlayFragment();
                return videoPlayFragment;
            default:
                Log.d(TAG, "Could not find tab fragment");
                return null;
        }
    }

    @Override
    public int getCount() {
        return mNumTabs;
    }
}
