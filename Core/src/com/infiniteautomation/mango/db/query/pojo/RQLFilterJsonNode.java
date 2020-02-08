/*
 * Copyright (C) 2020 Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.db.query.pojo;

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
        JsonNode node = item.get(property);

        if (node.isTextual()) {
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
