/*
 * Copyright (C) 2018 Bandyer S.r.l. All Rights Reserved.
 * See LICENSE.txt for licensing information
 */

package com.bandyer.demo_core_av.design.bottom_sheet.group_picker;

import java.util.List;

/**
 * @author kristiyan
 **/

public interface BottomGroupPickerDelegate<T> {
    void onClicked(List<T> itemsSelected);
}
