/*
 * Copyright (C) 2020 Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.db.query.pojo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import com.fasterxml.jackson.databind.JsonNode;

import net.jazdw.rql.parser.ASTNode;

/**
 * @author Jared Wiltshire
 */
public class RQLFilterJsonNode extends RQLFilter<JsonNode> {

    /**
     * @param node
     */
    public RQLFilterJsonNode(ASTNode node) {
        super(node);
    }

    @Override
    protected Object getProperty(JsonNode item, String property) {
        String[] propertyNames = RQLFilterJavaBean.PROPERTY_SEPARATOR.split(property);
        for (String subProperty : propertyNames) {
            item = item.get(subProperty);
        }
        return toJavaNative(item);
    }

    public static Object toJavaNative(JsonNode node) {
        if (node.isArray()) {
            ArrayList<Object> list = new ArrayList<>(node.size());
            for (JsonNode child : node) {
                list.add(toJavaNative(child));
            }
            return list;
        } else if (node.isObject()) {
            Map<String, Object> map = new HashMap<>(node.size());
            Iterator<Entry<String, JsonNode>> it = node.fields();
            while (it.hasNext()) {
                Entry<String, JsonNode> entry = it.next();
                map.put(entry.getKey(), toJavaNative(entry.getValue()));
            }
            return map;
        } else if (node.isTextual()) {
            return node.textValue();
        } else if (node.isNumber()) {
            return node.numberValue();
        } else if (node.isBoolean()) {
            return node.booleanValue();
        } else if (node.isNull()) {
            return null;
        }

        throw new UnsupportedOperationException("Unsupported JsonNode type" + node.getClass());
    }
}
