/*
 * Copyright (C) 2018 Bandyer S.r.l. All Rights Reserved.
 * See LICENSE.txt for licensing information
 */

package com.bandyer.demo_core_av_2.design.bottom_sheet.picker;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bandyer.demo_core_av_2.R;
import com.mikepenz.fastadapter.FastAdapter;
import com.mikepenz.fastadapter.items.AbstractItem;
import com.mikepenz.fastadapter.listeners.ClickEventHook;

import net.igenius.customcheckbox.CustomCheckBox;

import java.util.List;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;


/**
 * @author kristiyan
 **/

public class BottomListPickerItem<T> extends AbstractItem<BottomListPickerItem, BottomListPickerItem.Holder<T>> {

    private T item;

    @NonNull
    @Override
    public Holder<T> getViewHolder(@NonNull View v) {
        return new BottomListPickerItem.Holder<>(v);
    }

    @Override
    public int getType() {
        return R.id.picker_item;
    }

    @Override
    public int getLayoutRes() {
        return R.layout.bottom_list_picker_item;
    }

    public interface Delegate<T> {
        void onClicked(int adapterPosition, T item);
    }

    public BottomListPickerItem<T> setItem(T item) {
        this.item = item;
        return this;
    }

    public T getItem() {
        return item;
    }

    @Override
    public void bindView(@NonNull Holder<T> holder, @NonNull List<Object> payloads) {
        super.bindView(holder, payloads);
        holder.checked.setChecked(isSelected());
    }

    public static class Holder<T> extends FastAdapter.ViewHolder<BottomListPickerItem<T>> {

        @BindView(R.id.picker_item)
        LinearLayout picker_item;

        @BindView(R.id.item)
        TextView item;

        @BindView(R.id.checked)
        CustomCheckBox checked;

        Holder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }

        @Override
        public void bindView(@NonNull BottomListPickerItem<T> listItem, @NonNull List<Object> payloads) {
            item.setText(listItem.item.toString());
        }

        @Override
        public void unbindView(@NonNull BottomListPickerItem<T> item) {

        }

    }

    public static class PickerItemClick<T> extends ClickEventHook<BottomListPickerItem<T>> {

        BottomListPickerItem.Delegate<T> delegate;

        PickerItemClick(BottomListPickerItem.Delegate<T> delegate) {
            this.delegate = delegate;
        }

        @Override
        public View onBind(@NonNull RecyclerView.ViewHolder viewHolder) {
            if (viewHolder instanceof BottomListPickerItem.Holder) {
                return ((BottomListPickerItem.Holder) viewHolder).picker_item;
            }
            return null;
        }

        @Override
        @SuppressWarnings("deprecation")
        public void onClick(@NonNull View v, int position, @NonNull FastAdapter<BottomListPickerItem<T>> fastAdapter, @NonNull BottomListPickerItem<T> item) {
            if (item.isSelected())
                return;

            Set<Integer> selections = fastAdapter.getSelections();

            if (selections.isEmpty()) {
                fastAdapter.select(position);
                delegate.onClicked(position, item.getItem());
                return;
            }

            int selectedPosition = selections.iterator().next();
            fastAdapter.deselect(selectedPosition);
            fastAdapter.notifyItemChanged(selectedPosition);

            fastAdapter.select(position);
            delegate.onClicked(position, item.getItem());
        }
    }

    public static class GroupPickerItemClick<T> extends ClickEventHook<BottomListPickerItem<T>> {

        @Override
        public View onBind(@NonNull RecyclerView.ViewHolder viewHolder) {
            if (viewHolder instanceof BottomListPickerItem.Holder) {
                return ((BottomListPickerItem.Holder) viewHolder).picker_item;
            }
            return null;
        }

        @Override
        @SuppressWarnings("deprecation")
        public void onClick(@NonNull View v, int position, @NonNull FastAdapter<BottomListPickerItem<T>> fastAdapter, @NonNull BottomListPickerItem<T> item) {
            if (item.isSelected())
                return;

            Set<Integer> selections = fastAdapter.getSelections();

            if (selections.isEmpty()) {
                fastAdapter.select(position);
                return;
            }

            for (Integer selectedPosition : selections) {
                if (item.getTag().equals(fastAdapter.getItem(selectedPosition).getTag())) {
                    fastAdapter.deselect(selectedPosition);
                    fastAdapter.notifyItemChanged(selectedPosition);
                }
            }

            fastAdapter.select(position);
        }
    }
}

