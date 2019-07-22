/*
 * Copyright (C) 2018 Bandyer S.r.l. All Rights Reserved.
 * See LICENSE.txt for licensing information
 */

package com.bandyer.demo_core_av.room;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bandyer.demo_core_av.BaseActivity;
import com.bandyer.demo_core_av.R;
import com.bandyer.demo_core_av.room.adapter_items.InformationItem;
import com.mikepenz.fastadapter.FastAdapter;
import com.mikepenz.fastadapter.IItem;
import com.mikepenz.fastadapter.adapters.ItemAdapter;

import java.util.ArrayList;

import butterknife.BindView;

/**
 * @author kristiyan
 **/
public class InformationActivity extends BaseActivity {

    public static final String INFO_HEADERS = "headers";
    public static final String INFO_ITEMS = "items";

    @BindView(R.id.list)
    RecyclerView list;

    FastAdapter adapter;


    public static void show(BaseActivity activity, ArrayList<String> headers, ArrayList<String> items) {
        Intent intent = new Intent(activity, InformationActivity.class);
        intent.putExtra(INFO_HEADERS, headers);
        intent.putExtra(INFO_ITEMS, items);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_information);
        list.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        ArrayList<String> headers = getIntent().getStringArrayListExtra(INFO_HEADERS);
        ArrayList<String> items = getIntent().getStringArrayListExtra(INFO_ITEMS);

        ItemAdapter<IItem> itemAdapter = new ItemAdapter<>();

        adapter = FastAdapter.with(itemAdapter);
        adapter.setHasStableIds(true);
        list.setAdapter(adapter);

        for (int pos = 0; pos < headers.size(); pos++) {
            String header = headers.get(pos);
            String item = items.get(pos);
            itemAdapter.add(new InformationItem(true, header));
            itemAdapter.add(new InformationItem(false, item));
        }
    }


}
