/*
 * Copyright (C) 2018 Bandyer S.r.l. All Rights Reserved.
 * See LICENSE.txt for licensing information
 */
package com.bandyer.demo_core_av.room.adapter_items

import android.view.View
import com.bandyer.core_av.Stream
import com.bandyer.demo_core_av.R
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_stream.view.*

/**
 * @author kristiyan
 */
class StreamItem(val stream: Stream) : AbstractItem<StreamItem?, StreamItem.ViewHolder>() {

    //The layout to be used for this type of item
    override fun getLayoutRes(): Int = R.layout.item_stream
    override fun getViewHolder(v: View): ViewHolder = ViewHolder(v)

    override fun getType(): Int = R.id.stream_item
    override fun getIdentifier(): Long = stream.streamId.hashCode().toLong()

    /**
     * our ViewHolder
     */
    class ViewHolder internal constructor(override val containerView: View) : FastAdapter.ViewHolder<StreamItem>(containerView), LayoutContainer {

        override fun bindView(item: StreamItem, payloads: List<Any>) {
            containerView.stream!!.text = item.stream.streamId
        }

        override fun unbindView(item: StreamItem) {
            containerView.stream!!.text = null
        }
    }

}