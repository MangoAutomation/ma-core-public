/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.util;

import java.util.List;

import com.serotonin.m2m2.i18n.TranslatableMessage;

/**
 * @author Matthew Lohbihler
 */
public interface ChangeComparable<T> {
    int getId();

    String getTypeKey();

    void addProperties(List<TranslatableMessage> list);

    void addPropertyChanges(List<TranslatableMessage> list, T from);
}
