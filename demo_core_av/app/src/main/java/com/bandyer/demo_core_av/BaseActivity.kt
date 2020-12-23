/*
 * Copyright (C) 2018 Bandyer S.r.l. All Rights Reserved.
 * See LICENSE.txt for licensing information
 */
package com.bandyer.demo_core_av

import android.annotation.SuppressLint
import android.app.Dialog
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.util.*

/**
 * @author kristiyan
 */
@SuppressLint("Registered")
open class BaseActivity : AppCompatActivity() {

    private var toBeDismissed: MutableList<Dialog>? = null

    fun autoDismiss(dialog: Dialog) {
        if (toBeDismissed == null) {
            toBeDismissed = ArrayList()
        }
        toBeDismissed!!.add(dialog)
    }

    override fun onPause() {
        super.onPause()
        if (toBeDismissed.isNullOrEmpty()) return
        toBeDismissed!!.forEach { dialog ->
            kotlin.runCatching { dialog.dismiss() }
        }
        toBeDismissed!!.clear()
    }

    fun showError(title: String?, reason: String?) {
        if(isFinishing) return
        autoDismiss(AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(reason)
                .setNegativeButton(R.string.button_ok) { dialog, which -> dialog.dismiss() }
                .show())
    }
}