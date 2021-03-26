/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.m2m2.module;

import com.serotonin.m2m2.i18n.TranslatableMessage;

public final class PermissionGroupImpl implements PermissionGroup {
    private final String name;
    private final TranslatableMessage title;
    private final TranslatableMessage description;

    public PermissionGroupImpl(String name, TranslatableMessage title, TranslatableMessage description) {
        this.name = name;
        this.title = title;
        this.description = description;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public TranslatableMessage getTitle() {
        return title;
    }

    @Override
    public TranslatableMessage getDescription() {
        return description;
    }
}
