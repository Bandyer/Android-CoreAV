/*
 * Copyright (C) 2018 Bandyer S.r.l. All Rights Reserved.
 * See LICENSE.txt for licensing information
 */
package com.bandyer.demo_core_av.design.bottom_sheet

import android.content.Context
import android.view.View
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import com.google.android.material.bottomsheet.BottomSheetDialog

/**
 * @author kristiyan
 */
open class AbstractBottomSheetDialog(context: Context) : BottomSheetDialog(context) {

    protected fun configureBottomSheetBehavior(contentView: View) {
        val mBottomSheetBehavior: BottomSheetBehavior<*> = BottomSheetBehavior.from(contentView.parent as View) ?: return
        mBottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        mBottomSheetBehavior.setBottomSheetCallback(object : BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) { //showing the different states
                when (newState) {
                    BottomSheetBehavior.STATE_HIDDEN -> dismiss() //if you want the modal to be dismissed when user drags the bottomsheet down
                    else -> Unit
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) = Unit
        })
    }
}