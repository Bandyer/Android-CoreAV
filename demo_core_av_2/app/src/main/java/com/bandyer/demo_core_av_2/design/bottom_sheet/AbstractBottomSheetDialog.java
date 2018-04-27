/*
 * Copyright (C) 2018 Bandyer S.r.l. All Rights Reserved.
 * See LICENSE.txt for licensing information
 */

package com.bandyer.demo_core_av_2.design.bottom_sheet;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.StyleRes;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialog;
import android.view.View;

/**
 * @author kristiyan
 **/

public class AbstractBottomSheetDialog extends BottomSheetDialog {

    public AbstractBottomSheetDialog(@NonNull final Context context) {
        super(context);
    }

    public AbstractBottomSheetDialog(@NonNull final Context context, @StyleRes final int theme) {
        super(context, theme);
    }

    protected AbstractBottomSheetDialog(@NonNull final Context context, final boolean cancelable, final OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);
    }

    protected void configureBottomSheetBehavior(final View contentView) {
        final BottomSheetBehavior mBottomSheetBehavior = BottomSheetBehavior.from((View) contentView.getParent());

        if (mBottomSheetBehavior != null) {
            mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            mBottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {

                @Override
                public void onStateChanged(@NonNull final View bottomSheet, final int newState) {
                    //showing the different states
                    switch (newState) {
                        case BottomSheetBehavior.STATE_HIDDEN:
                            dismiss(); //if you want the modal to be dismissed when user drags the bottomsheet down
                            break;
                        case BottomSheetBehavior.STATE_EXPANDED:
                            break;
                        case BottomSheetBehavior.STATE_COLLAPSED:
                            break;
                        case BottomSheetBehavior.STATE_DRAGGING:
                            break;
                        case BottomSheetBehavior.STATE_SETTLING:
                            break;
                    }
                }

                @Override
                public void onSlide(@NonNull final View bottomSheet, final float slideOffset) {
                }
            });
        }
    }
}
