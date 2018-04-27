/*
 * Copyright (C) 2018 Bandyer S.r.l. All Rights Reserved.
 * See LICENSE.txt for licensing information
 */

package com.bandyer.demo_core_av_2.design.bottom_sheet.group_picker;

import android.content.DialogInterface;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.SparseArray;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bandyer.demo_core_av_2.BaseActivity;
import com.bandyer.demo_core_av_2.R;
import com.bandyer.demo_core_av_2.design.adapter_items.StringItem;
import com.bandyer.demo_core_av_2.design.bottom_sheet.AbstractBottomSheetDialog;
import com.bandyer.demo_core_av_2.design.bottom_sheet.picker.BottomListPickerItem;
import com.mikepenz.fastadapter.IItem;
import com.mikepenz.fastadapter.commons.adapters.FastItemAdapter;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * @author kristiyan
 **/

public class BottomGroupPicker<T> extends AbstractBottomSheetDialog {

    private FastItemAdapter<IItem> fastAdapter;

    @BindView(R.id.title)
    TextView title;

    @BindView(R.id.list)
    RecyclerView list;

    private int columnIndex = 0;

    private int columnsNumber = 2;


    private SparseArray<IItem> allItems = new SparseArray<>();

    public BottomGroupPicker(@NonNull final BaseActivity context, final String titleName, int columnsNumber) {
        super(context);
        this.columnsNumber = columnsNumber;
        setContentView(context, R.layout.bottom_group_picker);
        ButterKnife.bind(this);

        fastAdapter = new FastItemAdapter<>();
        fastAdapter.withSelectable(true);

        GridLayoutManager layoutManager = new GridLayoutManager(context, columnsNumber, GridLayoutManager.VERTICAL, false);
        list.setLayoutManager(layoutManager);
        list.setAdapter(fastAdapter);
        title.setText(titleName);

        context.autoDismiss(this);
    }

    private void setContentView(@NonNull final BaseActivity context, @LayoutRes int layoutResource) {
        final DisplayMetrics metrics = new DisplayMetrics();
        context.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        context.autoDismiss(this);

        final View contentView = getLayoutInflater().inflate(layoutResource, null);
        contentView.setLayoutParams(new LinearLayout.LayoutParams(metrics.widthPixels, (int) (metrics.heightPixels * 0.5f)));
        setContentView(contentView);

        configureBottomSheetBehavior(contentView);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void show() {
        super.show();

        for (int i = 0; i < allItems.size(); i++) {
            fastAdapter.add(allItems.get(i));
        }

        fastAdapter.withEventHook(new BottomListPickerItem.GroupPickerItemClick());
    }

    public BottomGroupPicker<T> setDelegate(@NonNull final BottomGroupPickerDelegate<T> delegate) {
        setOnDismissListener(new OnDismissListener() {

            @Override
            @SuppressWarnings({"unchecked", "deprecation"})
            public void onDismiss(DialogInterface dialog) {
                List<T> items = new ArrayList<>();
                for (IItem item : fastAdapter.getSelectedItems()) {
                    BottomListPickerItem<T> bottomListPickerItem = (BottomListPickerItem<T>) item;
                    items.add(bottomListPickerItem.getItem());
                }
                delegate.onClicked(items);
            }
        });
        return this;
    }


    public BottomGroupPicker<T> addGroupItems(String title,
                                              @NonNull final List<T> items,
                                              final int defaultSelectedItemPosition) {

        allItems.put(columnIndex, new StringItem(title));

        for (int i = 0; i < items.size(); i++) {
            final T item = items.get(i);
            allItems.put(columnsNumber * (i + 1) + columnIndex, new BottomListPickerItem<T>()
                    .setItem(item)
                    .withSetSelected(i == defaultSelectedItemPosition)
                    .withTag(columnIndex));
        }

        columnIndex += 1;

        return this;
    }
}
