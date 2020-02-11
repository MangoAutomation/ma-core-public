/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.infiniteautomation.mango.db.query.pojo;

import net.jazdw.rql.parser.ASTNode;
import net.jazdw.rql.parser.ASTVisitor;

import com.infiniteautomation.mango.db.query.RQLOperation;
import com.infiniteautomation.mango.db.query.QueryComparison;

/**
 * @author Terry Packer
 *
 */
public class RQLToQueryComparisonVisitor implements ASTVisitor<QueryComparison, String>{

	/* (non-Javadoc)
	 * @see net.jazdw.rql.parser.SimpleASTVisitor#visit(net.jazdw.rql.parser.ASTNode)
	 */
	@Override
	public QueryComparison visit(ASTNode node, String searchAttribute) {
		
		QueryComparison comparison = null;
		switch (node.getName()) {
		case "and":
        case "or":
        	for (Object obj : node) {
                comparison = ((ASTNode) obj).accept(this, searchAttribute);
                if(comparison != null)
                	return comparison;
            }
        break;
		case "limit":
		case "sort":
			//Don't Care
	        break;
	    default:
	    	String attribute = (String)node.getArgument(0);
	    	if(attribute.equals(searchAttribute)){
	    		return new QueryComparison(attribute, RQLOperation.convertTo(node.getName()), node.getArguments().subList(1, node.getArgumentsSize()));
	    	}
	    	
	    	
		}
		
		return comparison;
	}
}
