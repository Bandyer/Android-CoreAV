/*
 * Copyright (C) 2018 Bandyer S.r.l. All Rights Reserved.
 * See LICENSE.txt for licensing information
 */
package com.bandyer.demo_core_av.room

import android.content.Intent
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.bandyer.demo_core_av.BaseActivity
import com.bandyer.demo_core_av.R
import com.bandyer.demo_core_av.room.adapter_items.InformationItem
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.IItem
import com.mikepenz.fastadapter.adapters.ItemAdapter
import kotlinx.android.synthetic.main.activity_information.*
import java.util.*

/**
 * @author kristiyan
 */
class InformationActivity : BaseActivity() {

    var adapter: FastAdapter<*>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_information)
        list!!.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        val headers = intent.getStringArrayListExtra(INFO_HEADERS)
        val items = intent.getStringArrayListExtra(INFO_ITEMS)
        val itemAdapter = ItemAdapter<IItem<*, *>>()
        adapter = FastAdapter.with<IItem<*, *>, ItemAdapter<IItem<*, *>>>(itemAdapter)
        adapter!!.setHasStableIds(true)
        list!!.adapter = adapter

        for (pos in headers.indices) {
            val header = headers[pos]
            val item = items[pos]
            itemAdapter.add(InformationItem(true, header))
            itemAdapter.add(InformationItem(false, item))
        }

    }

    companion object {
        const val INFO_HEADERS = "headers"
        const val INFO_ITEMS = "items"
        fun show(activity: BaseActivity, headers: ArrayList<String?>?, items: ArrayList<String?>?) {
            val intent = Intent(activity, InformationActivity::class.java)
            intent.putExtra(INFO_HEADERS, headers)
            intent.putExtra(INFO_ITEMS, items)
            activity.startActivity(intent)
        }
    }
}