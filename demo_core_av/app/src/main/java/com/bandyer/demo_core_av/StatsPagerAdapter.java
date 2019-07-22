/*
 * Copyright (C) 2018 Bandyer S.r.l. All Rights Reserved.
 * See LICENSE.txt for licensing information
 */

package com.bandyer.demo_core_av;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.HashMap;
import java.util.LinkedHashMap;

/**
 * A simple pager adapter that represents Stats pages.
 */
public class StatsPagerAdapter extends PagerAdapter {


    private HashMap<String, StatsPage> mPages = new LinkedHashMap<>();
    private HashMap<Integer, String> mPagesIdentifiers = new LinkedHashMap<>();

    private Context mContext;

    public StatsPagerAdapter(Context context) {
        mContext = context;
    }

    public StatsPage addOrGetPage(String id) {
        if (mPages.containsKey(id)) return mPages.get(id);
        mPagesIdentifiers.put(mPagesIdentifiers.size(), id);
        StatsPage page = new StatsPage();
        mPages.put(id, page);
        notifyDataSetChanged();
        return page;
    }

    @NonNull
    @Override
    @SuppressWarnings("ConstantConditions")
    public Object instantiateItem(@NonNull ViewGroup collection, int position) {
        return mPages.get(mPagesIdentifiers.get(position)).onCreateView(LayoutInflater.from(mContext), collection);
    }

    @Override
    public void destroyItem(@NonNull ViewGroup collection, int position, @NonNull Object view) {
        collection.removeView((View) view);
    }

    @Override
    public int getCount() {
        return mPages.size();
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view == object;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return mPagesIdentifiers.get(position);
    }
}