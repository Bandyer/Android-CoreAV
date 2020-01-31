/*
 * Copyright (C) 2018 Bandyer S.r.l. All Rights Reserved.
 * See LICENSE.txt for licensing information
 */
package com.bandyer.demo_core_av.design.bottom_sheet.group_picker

/**
 * @author kristiyan
 */
interface BottomGroupPickerDelegate<T> {
    fun onClicked(itemsSelected: List<T>)
}