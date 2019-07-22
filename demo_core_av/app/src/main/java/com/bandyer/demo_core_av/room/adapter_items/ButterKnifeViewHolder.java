/*
 * Copyright (C) 2018 Bandyer S.r.l. All Rights Reserved.
 * See LICENSE.txt for licensing information
 */

package com.bandyer.demo_core_av.room.adapter_items;

import android.view.View;

import com.mikepenz.fastadapter.FastAdapter;
import com.mikepenz.fastadapter.IItem;

import butterknife.ButterKnife;

/**
 * @author kristiyan
 **/
public abstract class ButterKnifeViewHolder<T extends IItem> extends FastAdapter.ViewHolder<T> {

    public ButterKnifeViewHolder(View itemView) {
        super(itemView);
        ButterKnife.bind(this, itemView);
    }
}