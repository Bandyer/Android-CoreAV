/*
 * Copyright (C) 2018 Bandyer S.r.l. All Rights Reserved.
 * See LICENSE.txt for licensing information
 */

package com.bandyer.demo_core_av.design.adapter_items;

import androidx.annotation.NonNull;
import android.view.View;
import android.widget.TextView;

import com.bandyer.demo_core_av.R;
import com.bandyer.demo_core_av.room.adapter_items.ButterKnifeViewHolder;
import com.mikepenz.fastadapter.items.AbstractItem;

import java.util.List;

import butterknife.BindView;


/**
 * @author kristiyan
 **/

public class StringItem extends AbstractItem<StringItem, StringItem.Holder> {

    private String text;

    public StringItem(String text) {
        this.text = text;
    }

    @NonNull
    @Override
    public Holder getViewHolder(@NonNull View v) {
        return new StringItem.Holder(v);
    }

    @Override
    public int getType() {
        return R.id.stream_item;
    }

    @Override
    public int getLayoutRes() {
        return R.layout.string_item;
    }

    public static class Holder extends ButterKnifeViewHolder<StringItem> {

        @BindView(R.id.text)
        TextView textView;

        Holder(View view) {
            super(view);
        }

        @Override
        public void bindView(@NonNull StringItem stringItem, @NonNull List<Object> payloads) {
            textView.setText(stringItem.text);
        }

        @Override
        public void unbindView(@NonNull StringItem item) {
        }
    }
}
