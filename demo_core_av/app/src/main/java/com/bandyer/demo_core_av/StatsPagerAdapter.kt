/*
 * Copyright (C) 2018 Bandyer S.r.l. All Rights Reserved.
 * See LICENSE.txt for licensing information
 */
package com.bandyer.demo_core_av

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewpager.widget.PagerAdapter
import java.util.*

/**
 * A simple pager adapter that represents Stats pages.
 */
class StatsPagerAdapter(private val mContext: Context) : PagerAdapter() {
    private val mPages: HashMap<String, StatsPage?> = LinkedHashMap()
    private val mPagesIdentifiers: HashMap<Int, String> = LinkedHashMap()
    fun addOrGetPage(id: String): StatsPage? {
        if (mPages.containsKey(id)) return mPages[id]
        mPagesIdentifiers[mPagesIdentifiers.size] = id
        val page = StatsPage()
        mPages[id] = page
        notifyDataSetChanged()
        return page
    }

    override fun instantiateItem(collection: ViewGroup, position: Int): Any {
        return mPages[mPagesIdentifiers[position]]!!.onCreateView(LayoutInflater.from(mContext), collection)
    }

    override fun destroyItem(collection: ViewGroup, position: Int, view: Any) {
        collection.removeView(view as View)
    }

    override fun getCount(): Int {
        return mPages.size
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view === `object`
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return mPagesIdentifiers[position]
    }

}