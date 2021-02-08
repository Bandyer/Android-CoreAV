/*
 * Copyright (C) 2020 Bandyer S.r.l. All Rights Reserved.
 * See LICENSE.txt for licensing information
 */

package com.bandyer.demo_core_av.design.bottom_sheet

import android.util.DisplayMetrics
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import com.bandyer.demo_core_av.BaseActivity
import com.bandyer.demo_core_av.R
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputLayout

class QualityPicker(context: BaseActivity) : AbstractBottomSheetDialog(context) {


    @Suppress("UNCHECKED_CAST")
    fun onChanged(delegate: (width: Int, height: Int, fps: Int, optimize: Boolean) -> Unit): QualityPicker {
        setOnDismissListener {
            val width = findViewById<TextInputLayout>(R.id.quality_width)!!.editText!!.text.toString().takeIf {  it.isNotBlank() }?.toInt() ?: 640
            val height = findViewById<TextInputLayout>(R.id.quality_height)!!.editText!!.text.toString().takeIf {  it.isNotBlank() }?.toInt() ?: 480
            val fps = findViewById<TextInputLayout>(R.id.quality_fps)!!.editText!!.text.toString().takeIf {  it.isNotBlank() }?.toInt() ?: 30
            val optimize = findViewById<SwitchMaterial>(R.id.enable_optimize_quality)!!.isChecked
            delegate(width, height, fps, optimize)
        }
        return this
    }


    init {
        val metrics = DisplayMetrics()
        context.windowManager.defaultDisplay.getMetrics(metrics)
        context.autoDismiss(this)
        val contentView = layoutInflater.inflate(R.layout.change_quality_dialog, null) as ConstraintLayout
        contentView.layoutParams = LinearLayout.LayoutParams(metrics.widthPixels, (metrics.heightPixels * 0.5f).toInt())
        setContentView(contentView)
        configureBottomSheetBehavior(contentView)
        context.autoDismiss(this)
    }
}