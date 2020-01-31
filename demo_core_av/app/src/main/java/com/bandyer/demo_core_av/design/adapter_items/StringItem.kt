/*
 * Copyright (C) 2018 Bandyer S.r.l. All Rights Reserved.
 * See LICENSE.txt for licensing information
 */
package com.bandyer.demo_core_av.design.adapter_items

import android.view.View
import com.bandyer.demo_core_av.R
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.string_item.view.*

/**
 * @author kristiyan
 */
class StringItem(val text: String?) : AbstractItem<StringItem?, Holder>() {
    override fun getLayoutRes(): Int = R.layout.string_item
    override fun getViewHolder(v: View): Holder = Holder(v)
    override fun getType(): Int = R.id.stream_item
}

class Holder internal constructor(override val containerView: View) : FastAdapter.ViewHolder<StringItem>(containerView), LayoutContainer {

    override fun bindView(stringItem: StringItem, payloads: List<Any>) {
        containerView.textView!!.text = stringItem.text
    }

    override fun unbindView(item: StringItem) = Unit
}