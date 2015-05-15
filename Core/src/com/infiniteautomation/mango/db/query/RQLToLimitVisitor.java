/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.infiniteautomation.mango.db.query;

import net.jazdw.rql.parser.ASTNode;
import net.jazdw.rql.parser.SimpleASTVisitor;

/**
 * Visit the query tree and return the limit
 * 
 * @author Terry Packer
 *
 */
public class RQLToLimitVisitor implements SimpleASTVisitor<Integer>{

	/* (non-Javadoc)
	 * @see net.jazdw.rql.parser.SimpleASTVisitor#visit(net.jazdw.rql.parser.ASTNode)
	 */
	@Override
	public Integer visit(ASTNode node) {
		Integer limit = null;
		switch (node.getName()) {
		case "and":
        case "or":
        	for (Object obj : node) {
                limit = ((ASTNode) obj).accept(this);
            }
        break;
		case "limit":
			if(node.getArgumentsSize() > 0){
				limit = (Integer)node.getArgument(0);
			}
		}
		
		return limit;
	}

}
