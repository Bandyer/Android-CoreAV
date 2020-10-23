/*
 * Copyright (C) 2020 Bandyer S.r.l. All Rights Reserved.
 * See LICENSE.txt for licensing information
 */

package com.bandyer.demo_core_av

import androidx.appcompat.app.AppCompatActivity
import com.bandyer.core_av.capturer.Capturer
import com.bandyer.core_av.capturer.CapturerFactory

inline fun <reified T : Capturer<*, *>> capturer(context: AppCompatActivity, noinline block: CapturerFactory.() -> Unit): T = CapturerFactory(context, block).create(T::class)
