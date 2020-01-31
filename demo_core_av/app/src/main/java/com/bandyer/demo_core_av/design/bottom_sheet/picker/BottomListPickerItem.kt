/*
 * Copyright (C) 2018 Bandyer S.r.l. All Rights Reserved.
 * See LICENSE.txt for licensing information
 */
package com.bandyer.demo_core_av.design.bottom_sheet.picker

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.bandyer.demo_core_av.R
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.IItem
import com.mikepenz.fastadapter.items.AbstractItem
import com.mikepenz.fastadapter.listeners.ClickEventHook
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.bottom_list_picker_item.view.*

/**
 * @author kristiyan
 */
class BottomListPickerItem<T> : AbstractItem<BottomListPickerItem<*>?, BottomListPickerItem.Holder<T>>() {

    var item: T? = null
        private set

    override fun getViewHolder(v: View): Holder<T> = Holder(v)

    override fun getType(): Int = R.id.picker_item

    override fun getLayoutRes(): Int = R.layout.bottom_list_picker_item

    interface Delegate<T> {
        fun onClicked(adapterPosition: Int, item: T?)
    }

    fun setItem(item: T): BottomListPickerItem<T> {
        this.item = item
        return this
    }

    override fun bindView(holder: Holder<T>, payloads: List<Any>) {
        super.bindView(holder, payloads)
        holder.containerView.checked!!.isChecked = isSelected
    }

    class Holder<T> internal constructor(override val containerView: View) : FastAdapter.ViewHolder<BottomListPickerItem<T>>(containerView), LayoutContainer {

        override fun bindView(listItem: BottomListPickerItem<T>, payloads: List<Any>) {
            containerView.item!!.text = listItem.item.toString()
        }

        override fun unbindView(item: BottomListPickerItem<T>) {}
    }

    class PickerItemClick<T> internal constructor(val delegate: Delegate<T>) : ClickEventHook<BottomListPickerItem<T>>() {

        override fun onBind(viewHolder: RecyclerView.ViewHolder): View? {
            return if (viewHolder is Holder<*>) viewHolder.containerView.findViewById(R.id.picker_item) else null
        }

        override fun onClick(v: View, position: Int, fastAdapter: FastAdapter<BottomListPickerItem<T>>, item: BottomListPickerItem<T>) {
            if (item.isSelected) return
            val selections = fastAdapter.selections
            if (selections.isEmpty()) {
                fastAdapter.select(position)
                delegate.onClicked(position, item.item)
                return
            }
            val selectedPosition = selections.iterator().next()
            fastAdapter.deselect(selectedPosition)
            fastAdapter.notifyItemChanged(selectedPosition)
            fastAdapter.select(position)
            delegate.onClicked(position, item.item)
        }

    }


    class GroupPickerItemClick : ClickEventHook<IItem<*, *>?>() {
        override fun onBind(viewHolder: RecyclerView.ViewHolder): View? {
            return if (viewHolder is Holder<*>) viewHolder.containerView.findViewById(R.id.picker_item) else null
        }

        override fun onClick(v: View, position: Int, fastAdapter: FastAdapter<IItem<*,*>?>, item: IItem<*,*>?) {
            if (item?.isSelected != false) return
            val selections = fastAdapter.selections
            if (selections.isEmpty()) {
                fastAdapter.select(position)
                return
            }
            selections.forEach { selectedPosition ->
                if (item.tag == fastAdapter.getItem(selectedPosition!!)?.tag) {
                    fastAdapter.deselect(selectedPosition)
                    fastAdapter.notifyItemChanged(selectedPosition)
                }
            }
            fastAdapter.select(position)
        }
    }
}