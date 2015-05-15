/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.infiniteautomation.mango.db.query.pojo;

import java.util.ArrayList;
import java.util.List;

import net.jazdw.rql.parser.ASTNode;
import net.jazdw.rql.parser.ASTVisitor;

import com.infiniteautomation.mango.db.query.ComparisonEnum;
import com.infiniteautomation.mango.db.query.QueryComparison;
import com.infiniteautomation.mango.db.query.SortOption;
import com.serotonin.ShouldNeverHappenException;

/**
 * This class is a work in progress.  It is not being used and needs some work.
 * 
 * For example the and/or logic isn't complete.
 * 
 * @author Terry Packer
 *
 */
public class RQLToObjectQuery<T> implements ASTVisitor<Boolean, T>{
	
	private List<SortOptionComparator<T>> compares = new ArrayList<SortOptionComparator<T>>();
	private int limit;
	
	public RQLToObjectQuery(){	}
	
	
	/* (non-Javadoc)
	 * @see net.jazdw.rql.parser.ASTVisitor#visit(net.jazdw.rql.parser.ASTNode, java.lang.Object)
	 */
	@Override
	public Boolean visit(ASTNode node, T data) {
		
		QueryComparison comparison;
		
        switch (node.getName()) {
        case "and":
        case "or":
            return visitAndOr(node, data);
        case "eq":
        	comparison = new QueryComparison((String)node.getArgument(0), ComparisonEnum.EQUAL_TO, node.getArguments().subList(1, node.getArgumentsSize()));
        	return compare(comparison, data);
        case "gt":
        	comparison = new QueryComparison((String)node.getArgument(0), ComparisonEnum.GREATER_THAN, node.getArguments().subList(1, node.getArgumentsSize()));
        	return compare(comparison, data);
        case "ge":
        	comparison = new QueryComparison((String)node.getArgument(0), ComparisonEnum.GREATER_THAN_EQUAL_TO, node.getArguments().subList(1, node.getArgumentsSize()));
        	return compare(comparison, data);
        case "lt":
        	comparison = new QueryComparison((String)node.getArgument(0), ComparisonEnum.LESS_THAN, node.getArguments().subList(1, node.getArgumentsSize()));
        	return compare(comparison, data);
        case "le":
        	comparison = new QueryComparison((String)node.getArgument(0), ComparisonEnum.LESS_THAN_EQUAL_TO, node.getArguments().subList(1, node.getArgumentsSize()));
        	return compare(comparison, data);
        case "ne":
        	comparison = new QueryComparison((String)node.getArgument(0), ComparisonEnum.NOT_EQUAL_TO, node.getArguments().subList(1, node.getArgumentsSize()));
        	return compare(comparison, data);
        case "match":
        case "like":
        	comparison = new QueryComparison((String)node.getArgument(0), ComparisonEnum.LIKE, node.getArguments().subList(1, node.getArgumentsSize()));
        	return compare(comparison, data);
        case "in":
        	comparison = new QueryComparison((String)node.getArgument(0), ComparisonEnum.IN, node.getArguments().subList(1, node.getArgumentsSize()));
        	return compare(comparison, data);
        case "sort":
            applySort(node, data);
            return true; //We are sorting after the visitation
        case "limit":
            applyLimit(node.getArguments(), data);
            return true;
        default:
        	throw new ShouldNeverHappenException("Unsupported comparison: " + node.getName());
        }
		
        //
		
	}

	/**
	 * @param arguments
	 * @param data
	 * @return
	 */
	private void applyLimit(List<Object> arguments, T data) {
		
		if(arguments.size() > 0)
			this.limit = (int)arguments.get(0);

				
	}


	/**
	 * We just keep a reference to our comparators to sort later
	 * 
	 * @param node
	 * @param data
	 * @return
	 */
	private void applySort(ASTNode node, T data) {
		
		if(node.getArgumentsSize() == 0)
			return;
		
		boolean descending = false;
		SortOption sort;
		SortOptionComparator<T> compare;
		
		for (Object arg : node) {
            
            String prop = (String) arg;
            if (prop.startsWith("-")) {
            	descending = true;
                prop = prop.substring(1);
            } else if (prop.startsWith("+")) {
                prop = prop.substring(1);
                descending = false;
            }
            
            sort = new SortOption(prop, descending);
            compare = new SortOptionComparator<T>(sort);
            this.compares.add(compare);
		}
		
		return;
		
	}


	/**
	 * @param comparison
	 * @param data
	 * @return
	 */
	private Boolean compare(QueryComparison comparison, T data) {
		return comparison.apply(data);
	}

	/**
	 * @param node
	 * @param param
	 * @return
	 */
	private Boolean visitAndOr(ASTNode node, T data) {
		
		Boolean keep = true;
		
		for (Object obj : node) {
            if (obj instanceof ASTNode) {
            	
            	switch(((ASTNode) obj).getName()){
            	case "limit":
            	case "sort":
                	((ASTNode) obj).accept(this, data);
            		break;
            	default:
            		if(node.getName().equalsIgnoreCase("or")){
            			//Combine results
            			if(((ASTNode) obj).accept(this, data))
            				keep = true;
            				
            		}else if(node.getName().equalsIgnoreCase("and")){
            			//Keep only common ones
            			if(!((ASTNode) obj).accept(this, data))
            				keep = false;
            		}
            	}
            }else {
                throw new ShouldNeverHappenException("AND/OR terms should only have ASTNode arguments");
            }
		}
		
		return keep;
	}


	public List<SortOptionComparator<T>> getCompares() {
		return compares;
	}


	public void setCompares(List<SortOptionComparator<T>> compares) {
		this.compares = compares;
	}


	public int getLimit() {
		return limit;
	}


	public void setLimit(int limit) {
		this.limit = limit;
	}

	
	
}
