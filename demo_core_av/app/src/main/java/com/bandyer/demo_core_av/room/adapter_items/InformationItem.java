/*
 * Copyright (C) 2018 Bandyer S.r.l. All Rights Reserved.
 * See LICENSE.txt for licensing information
 */

package com.bandyer.demo_core_av.room.adapter_items;

import androidx.annotation.NonNull;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.TextView;

import com.bandyer.demo_core_av.R;
import com.mikepenz.fastadapter.items.AbstractItem;
import com.yuyh.jsonviewer.library.JsonRecyclerView;

import java.util.List;

import butterknife.BindView;

/**
 * @author kristiyan
 **/
public class InformationItem extends AbstractItem<InformationItem, InformationItem.ViewHolder> {

    private boolean isHeader = false;
    private String text;

    public InformationItem(boolean isHeader, String text) {
        this.isHeader = isHeader;
        this.text = text;
    }

    @Override
    public int getType() {
        return R.id.info;
    }

    @Override
    public int getLayoutRes() {
        return R.layout.item_information;
    }

    @NonNull
    @Override
    public ViewHolder getViewHolder(@NonNull View v) {
        return new ViewHolder(v);
    }

    protected static class ViewHolder extends ButterKnifeViewHolder<InformationItem> {

        @BindView(R.id.item_header)
        TextView itemHeader;

        @BindView(R.id.container_body)
        HorizontalScrollView containerBody;

        @BindView(R.id.item_body)
        JsonRecyclerView itemBody;

        ViewHolder(View v) {
            super(v);
        }

        @Override
        public void bindView(@NonNull InformationItem item, @NonNull List<Object> payloads) {
            itemHeader.setText(item.text);
            itemHeader.setVisibility(item.isHeader ? View.VISIBLE : View.GONE);
            containerBody.setVisibility(item.isHeader ? View.GONE : View.VISIBLE);
            if (!item.isHeader && !item.text.equals("null"))
                itemBody.bindJson(item.text);
        }

        @Override
        public void unbindView(@NonNull InformationItem item) {
            itemHeader.setText(null);
        }
    }
}
