/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.m2m2.module;

import com.serotonin.m2m2.i18n.TranslatableMessage;

/**
 * Logical group for permissions, used for sorting into groups on the UI.
 */
public interface PermissionGroup {
    String getName();
    TranslatableMessage getTitle();
    TranslatableMessage getDescription();
}
