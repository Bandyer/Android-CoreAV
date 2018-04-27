/*
 * Copyright (C) 2018 Bandyer S.r.l. All Rights Reserved.
 * See LICENSE.txt for licensing information
 */

package com.bandyer.demo_core_av_2;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;

/**
 * @author kristiyan
 **/

@SuppressLint("Registered")
public class BaseActivity extends AppCompatActivity {

    private List<Dialog> toBeDismissed;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        ButterKnife.bind(this);
    }

    public final void autoDismiss(final Dialog dialog) {
        if (toBeDismissed == null) {
            toBeDismissed = new ArrayList<>();
        }
        toBeDismissed.add(dialog);
    }

    @Override
    protected void onPause() {
        if (toBeDismissed != null && !toBeDismissed.isEmpty()) {
            for (final Dialog dialog : toBeDismissed) {
                try {
                    dialog.dismiss();
                } catch (final Throwable ignored) {
                }
            }
            toBeDismissed.clear();
        }
        super.onPause();
    }

    public void showError(@SuppressWarnings("SameParameterValue") String title, String reason) {
        autoDismiss((new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(reason)
                .setNegativeButton(R.string.button_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show()));
    }

}
