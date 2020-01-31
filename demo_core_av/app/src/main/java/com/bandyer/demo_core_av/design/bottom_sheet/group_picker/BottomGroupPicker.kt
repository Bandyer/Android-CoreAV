/*
 * Copyright (C) 2018 Bandyer S.r.l. All Rights Reserved.
 * See LICENSE.txt for licensing information
 */
package com.bandyer.demo_core_av.design.bottom_sheet.group_picker

import android.util.DisplayMetrics
import android.util.SparseArray
import android.widget.LinearLayout
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.GridLayoutManager
import com.bandyer.demo_core_av.BaseActivity
import com.bandyer.demo_core_av.R
import com.bandyer.demo_core_av.design.adapter_items.StringItem
import com.bandyer.demo_core_av.design.bottom_sheet.AbstractBottomSheetDialog
import com.bandyer.demo_core_av.design.bottom_sheet.picker.BottomListPickerItem
import com.bandyer.demo_core_av.design.bottom_sheet.picker.BottomListPickerItem.GroupPickerItemClick
import com.mikepenz.fastadapter.IItem
import com.mikepenz.fastadapter.commons.adapters.FastItemAdapter
import kotlinx.android.synthetic.main.bottom_group_picker.*

/**
 * @author kristiyan
 */
class BottomGroupPicker<T>(context: BaseActivity, titleName: String?, columnsNumber: Int) : AbstractBottomSheetDialog(context) {

    private val fastAdapter: FastItemAdapter<IItem<*, *>?>
    private var columnIndex = 0
    private var columnsNumber = 2
    private val allItems = SparseArray<IItem<*, *>?>()


    private fun setContentView(context: BaseActivity, @LayoutRes layoutResource: Int) {
        val metrics = DisplayMetrics()
        context.windowManager.defaultDisplay.getMetrics(metrics)
        context.autoDismiss(this)
        val contentView = layoutInflater.inflate(layoutResource, null) as LinearLayout
        contentView.layoutParams = LinearLayout.LayoutParams(metrics.widthPixels, (metrics.heightPixels * 0.5f).toInt())
        setContentView(contentView)
        configureBottomSheetBehavior(contentView)
    }

    override fun show() {
        super.show()
        for (i in 0 until allItems.size()) {
            fastAdapter.add(allItems[i])
        }
        fastAdapter.withEventHook(GroupPickerItemClick())
    }

    @Suppress("UNCHECKED_CAST")
    fun setDelegate(delegate: BottomGroupPickerDelegate<T>): BottomGroupPicker<T> {
        setOnDismissListener {
            val items: MutableList<T> = mutableListOf()
            fastAdapter.selectedItems.forEach { item ->
                val bottomListPickerItem = item as BottomListPickerItem<T>?
                items += bottomListPickerItem!!.item!!
            }
            delegate.onClicked(items)
        }
        return this
    }

    fun addGroupItems(title: String?,
                      items: List<T>,
                      defaultSelectedItemPosition: Int): BottomGroupPicker<T> {
        allItems.put(columnIndex, StringItem(title))
        for (i in items.indices) {
            val item = items[i]
            allItems.put(columnsNumber * (i + 1) + columnIndex, BottomListPickerItem<T>()
                    .setItem(item).apply {
                        withSetSelected(i == defaultSelectedItemPosition)
                        withTag(columnIndex)
                    })
        }
        columnIndex += 1
        return this
    }

    init {
        this.columnsNumber = columnsNumber
        setContentView(context, R.layout.bottom_group_picker)
        fastAdapter = FastItemAdapter()
        fastAdapter.withSelectable(true)
        val layoutManager = GridLayoutManager(context, columnsNumber, GridLayoutManager.VERTICAL, false)
        list!!.layoutManager = layoutManager
        list!!.adapter = fastAdapter
        title!!.text = titleName
        context.autoDismiss(this)
    }
}