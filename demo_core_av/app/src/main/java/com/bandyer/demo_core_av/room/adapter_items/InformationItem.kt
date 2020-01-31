/*
 * Copyright (C) 2018 Bandyer S.r.l. All Rights Reserved.
 * See LICENSE.txt for licensing information
 */
package com.bandyer.demo_core_av.room.adapter_items

import android.view.View
import com.bandyer.demo_core_av.R
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_information.view.*
/**
 * @author kristiyan
 */
class InformationItem(val isHeader: Boolean,val text: String) : AbstractItem<InformationItem, ViewHolder>() {

    override fun getType(): Int = R.id.info

    override fun getLayoutRes(): Int = R.layout.item_information

    override fun getViewHolder(v: View): ViewHolder = ViewHolder(v)
}

class ViewHolder internal constructor(override val containerView: View) : FastAdapter.ViewHolder<InformationItem>(containerView), LayoutContainer {

    override fun bindView(item: InformationItem, payloads: List<Any>) {
        with(containerView){
            item_header!!.text = item.text
            item_header!!.visibility = if (item.isHeader) View.VISIBLE else View.GONE
            container_body!!.visibility = if (item.isHeader) View.GONE else View.VISIBLE
            if (!item.isHeader && item.text != "null") item_body!!.bindJson(item.text)
        }
    }

    override fun unbindView(item: InformationItem) = Unit
}