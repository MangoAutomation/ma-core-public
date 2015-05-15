/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.infiniteautomation.mango.db.query.pojo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.jazdw.rql.parser.ASTNode;
import net.jazdw.rql.parser.ASTVisitor;

import com.infiniteautomation.mango.db.query.ComparisonEnum;
import com.infiniteautomation.mango.db.query.QueryComparison;
import com.infiniteautomation.mango.db.query.SortOption;
import com.serotonin.ShouldNeverHappenException;

/**
 * 
 * Class to visit an AST Tree and filter a List of Objects based on the criteria.
 * 
 * Returns the filtered list
 * 
 * @author Terry Packer
 *
 */
public class RQLToObjectListQuery<T> implements ASTVisitor<List<T>, List<T>>{
	
	public RQLToObjectListQuery(){	}
	
	
	/* (non-Javadoc)
	 * @see net.jazdw.rql.parser.ASTVisitor#visit(net.jazdw.rql.parser.ASTNode, java.lang.Object)
	 */
	@Override
	public List<T> visit(ASTNode node, List<T> data) {
		
		QueryComparison comparison;
		
        switch (node.getName()) {
        
        case "and":
            List<T> matched = new ArrayList<>(data);
            for (Object obj : node) {
                matched = ((ASTNode) obj).accept(this, matched);
            }
            return matched;
        case "or":
            List<T> matched2 = new ArrayList<>();
            for (Object obj : node) {
                matched2.addAll(((ASTNode) obj).accept(this, data));
            }
            return matched2;
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
            return applySort(node, data);
        case "limit":
            return applyLimit(node.getArguments(), data);
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
	private List<T> applyLimit(List<Object> arguments, List<T> data) {
		
		if(arguments.size() > 0){
			int limit = (int)arguments.get(0);
			if(data.size() > limit)
				return data.subList(0, limit);
			else
				return data;
		}else
			return data;
				
	}


	/**
	 * @param node
	 * @param data
	 * @return
	 */
	private List<T> applySort(ASTNode node, List<T> data) {
		
		if(node.getArgumentsSize() == 0)
			return data;
		
		boolean descending = false;
		SortOption sort;
		SortOptionComparator<Object> compare;
		
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
            compare = new SortOptionComparator<Object>(sort);
            Collections.sort(data, compare);
		}
		
		return data;
		
	}


	/**
	 * @param comparison
	 * @param data
	 * @return
	 */
	private List<T> compare(QueryComparison comparison, List<T> data) {

		List<T> keep = new ArrayList<T>();
		for(T d : data){
			if(comparison.apply(d))
				keep.add(d);
		}
		return keep;
	}

}
