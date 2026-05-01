package com.example.quranplayer.adapters;

import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.util.LinkedList;

public class ViewPagerAdapter extends FragmentStateAdapter {
    private final LinkedList<Fragment> fragments = new LinkedList<>();
    private final LinkedList<String> titles = new LinkedList<>();
    private LinkedList<Drawable> icons = new LinkedList<>();
    private int color1;
    private int color2;

    public ViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }
    public void addFragment(Fragment fragment , String title, Drawable icon){
        fragments.add(fragment);
        titles.add(title);
        this.icons.add(icon);
    }
    public String getFragmentTitle(int position){
        return titles.get(position);
    }
    public Drawable getIcon(int position){
        return this.icons.get(position);
    }
    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return fragments.get(position);
    }

    @Override
    public int getItemCount() {
        return fragments.size();
    }

    public void setColor1(int color1) {
        this.color1 = color1;
    }

    public void setColor2(int color2) {
        this.color2 = color2;
    }

    public int getColor1() {
        return color1;
    }

    public int getColor2() {
        return color2;
    }
}
