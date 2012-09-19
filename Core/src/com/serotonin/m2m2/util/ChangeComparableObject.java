/*
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.util;

import java.util.List;

import com.serotonin.m2m2.i18n.TranslatableMessage;

/**
 * This interface is meant for comparable objects (for audit purposes) that are members of other comparable objects. It
 * does not use generics so to avoid having to spread generic definitions throughout the entire code base.
 * 
 * @author Matthew Lohbihler
 */
public interface ChangeComparableObject {
    void addProperties(List<TranslatableMessage> list);

    void addPropertyChanges(List<TranslatableMessage> list, Object o);
}
