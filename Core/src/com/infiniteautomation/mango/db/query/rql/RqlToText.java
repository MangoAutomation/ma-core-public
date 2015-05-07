/**
 * Copyright (C) 2015 Infinite Automation Systems. All rights reserved.
 * http://infiniteautomation.com/
 */
package com.infiniteautomation.mango.db.query.rql;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.infiniteautomation.mango.db.query.ast.AndNode;
import com.infiniteautomation.mango.db.query.ast.ComparisonNode;
import com.infiniteautomation.mango.db.query.ast.FunctionNode;
import com.infiniteautomation.mango.db.query.ast.NoArgASTVisitor;
import com.infiniteautomation.mango.db.query.ast.Node;
import com.infiniteautomation.mango.db.query.ast.OrNode;

/**
 * @author Jared Wiltshire
 */
public class RqlToText extends NoArgASTVisitor<String> {
    /* (non-Javadoc)
     * @see com.infiniteautomation.mango.db.query.parser.ast.RQLVisitor#visit(com.infiniteautomation.mango.db.query.parser.ast.AndNode, java.lang.Object)
     */
    @Override
    public String visit(AndNode node) {
        List<String> components = new ArrayList<String>();
        
        String suffix = "";
        
        Iterator<Node> it = node.iterator();
        while (it.hasNext()) {
            Node childNode = it.next();
            
            if (childNode instanceof FunctionNode) {
                suffix += childNode.accept(this);
                continue;
            }
            
            components.add(childNode.accept(this));
        }
        
        return "(" + StringUtils.join(components, ") AND (") + ")" + suffix;
    }

    /* (non-Javadoc)
     * @see com.infiniteautomation.mango.db.query.parser.ast.RQLVisitor#visit(com.infiniteautomation.mango.db.query.parser.ast.OrNode, java.lang.Object)
     */
    @Override
    public String visit(OrNode node) {
        List<String> components = new ArrayList<String>();
        
        Iterator<Node> it = node.iterator();
        while (it.hasNext()) {
            Node childNode = it.next();
            
            if (childNode instanceof FunctionNode) {
                continue;
            }
            
            components.add(childNode.accept(this));
        }
        
        return "(" + StringUtils.join(components, ") OR (") + ")";
    }

    /* (non-Javadoc)
     * @see com.infiniteautomation.mango.db.query.parser.ast.RQLVisitor#visit(com.infiniteautomation.mango.db.query.parser.ast.ComparisonNode, java.lang.Object)
     */
    @Override
    public String visit(ComparisonNode node) {
        return node.toString();
    }

    /* (non-Javadoc)
     * @see com.infiniteautomation.mango.db.query.ast.NoArgASTVisitor#visit(com.infiniteautomation.mango.db.query.ast.FunctionNode)
     */
    @Override
    public String visit(FunctionNode node) {
        return node.toString();
    }
}
