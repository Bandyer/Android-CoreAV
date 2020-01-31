/*
 * Copyright (C) 2018 Bandyer S.r.l. All Rights Reserved.
 * See LICENSE.txt for licensing information
 */
package com.bandyer.demo_core_av.design.bottom_sheet.picker

import android.util.DisplayMetrics
import android.widget.LinearLayout
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.LinearLayoutManager
import com.bandyer.demo_core_av.BaseActivity
import com.bandyer.demo_core_av.R
import com.bandyer.demo_core_av.design.bottom_sheet.AbstractBottomSheetDialog
import com.bandyer.demo_core_av.design.bottom_sheet.picker.BottomListPickerItem.PickerItemClick
import com.mikepenz.fastadapter.commons.adapters.FastItemAdapter
import kotlinx.android.synthetic.main.bottom_list_picker.*

/**
 * @author kristiyan
 */
class BottomListPicker<T>(context: BaseActivity, titleName: String?) : AbstractBottomSheetDialog(context) {

    private val fastAdapter: FastItemAdapter<BottomListPickerItem<T>>

    private fun setContentView(context: BaseActivity, @LayoutRes layoutResource: Int) {
        val metrics = DisplayMetrics()
        context.windowManager.defaultDisplay.getMetrics(metrics)
        context.autoDismiss(this)
        val contentView = layoutInflater.inflate(layoutResource, null)
        contentView.layoutParams = LinearLayout.LayoutParams(metrics.widthPixels, (metrics.heightPixels * 0.5f).toInt())
        setContentView(contentView)
        configureBottomSheetBehavior(contentView)
    }

    fun addItem(item: BottomListPickerItem<T>): BottomListPicker<*> {
        fastAdapter.add(item)
        return this
    }

    fun setItems(items: List<T>, defaultSelectedItemPosition: Int, delegate: BottomListPickerItem.Delegate<T>): BottomListPicker<*> {
        fastAdapter.withEventHook(PickerItemClick(delegate))
        for (i in items.indices) {
            val item = items[i]
            fastAdapter.add(BottomListPickerItem<T>()
                    .setItem(item)
                    .apply { withSetSelected(i == defaultSelectedItemPosition) })
        }
        list!!.scrollToPosition(defaultSelectedItemPosition)
        return this
    }

    init {
        setContentView(context, R.layout.bottom_list_picker)
        fastAdapter = FastItemAdapter()
        fastAdapter.withSelectable(true)
        list!!.layoutManager = LinearLayoutManager(context)
        list!!.adapter = fastAdapter
        title!!.text = titleName
    }
}