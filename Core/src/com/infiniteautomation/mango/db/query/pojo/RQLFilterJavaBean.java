/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.db.query.pojo;

import java.lang.reflect.InvocationTargetException;
import java.util.regex.Pattern;

import org.apache.commons.beanutils.PropertyUtils;

import com.serotonin.m2m2.i18n.Translations;

import net.jazdw.rql.parser.ASTNode;

/**
 * Class that filters Java beans / POJO objects
 *
 * @author Jared Wiltshire
 */
public class RQLFilterJavaBean<T> extends RQLFilter<T> {

    public static final Pattern PROPERTY_SEPARATOR = Pattern.compile("\\.");

    /**
     */
    public RQLFilterJavaBean(ASTNode node, Translations translations) {
        super(node, translations);
    }

    @Override
    protected Object getProperty(Object item, String property) {
        if (property == null) {
            return item;
        }
        String[] propertyNames = PROPERTY_SEPARATOR.split(property);
        for (String subProperty : propertyNames) {
            item = getItemProperty(item, subProperty);
        }
        return item;
    }

    protected Object getItemProperty(Object item, String property) {
        if (property.isEmpty()) {
            return item;
        }

        try {
            return PropertyUtils.getProperty(item, property);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new IllegalArgumentException("Error getting value for property " + property, e);
        }
    }

}
