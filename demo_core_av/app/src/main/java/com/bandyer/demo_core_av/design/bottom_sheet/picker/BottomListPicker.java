/*
 * Copyright (C) 2018 Bandyer S.r.l. All Rights Reserved.
 * See LICENSE.txt for licensing information
 */

package com.bandyer.demo_core_av.design.bottom_sheet.picker;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bandyer.demo_core_av.BaseActivity;
import com.bandyer.demo_core_av.R;
import com.bandyer.demo_core_av.design.bottom_sheet.AbstractBottomSheetDialog;
import com.mikepenz.fastadapter.commons.adapters.FastItemAdapter;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * @author kristiyan
 **/

public class BottomListPicker<T> extends AbstractBottomSheetDialog {

    private FastItemAdapter<BottomListPickerItem<T>> fastAdapter;

    @BindView(R.id.title)
    TextView title;

    @BindView(R.id.list)
    RecyclerView list;

    public BottomListPicker(@NonNull final BaseActivity context, final String titleName) {
        super(context);
        setContentView(context, R.layout.bottom_list_picker);
        ButterKnife.bind(this);

        fastAdapter = new FastItemAdapter<>();
        fastAdapter.withSelectable(true);
        list.setLayoutManager(new LinearLayoutManager(context));
        list.setAdapter(fastAdapter);
        title.setText(titleName);
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

    public BottomListPicker addItem(final BottomListPickerItem<T> item) {
        fastAdapter.add(item);
        return this;
    }

    @SuppressWarnings("unchecked")
    public BottomListPicker setItems(@NonNull final List<T> items, final int defaultSelectedItemPosition, @NonNull final BottomListPickerItem.Delegate<T> delegate) {
        fastAdapter.withEventHook(new BottomListPickerItem.PickerItemClick<>(delegate));

        for (int i = 0; i < items.size(); i++) {
            final T item = items.get(i);
            fastAdapter.add(new BottomListPickerItem<T>()
                    .setItem(item)
                    .withSetSelected(i == defaultSelectedItemPosition));
        }
        list.scrollToPosition(defaultSelectedItemPosition);
        return this;
    }
}
