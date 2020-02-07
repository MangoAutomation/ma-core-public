/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.infiniteautomation.mango.db.query.pojo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.infiniteautomation.mango.db.query.ComparisonEnum;
import com.infiniteautomation.mango.db.query.QueryComparison;
import com.infiniteautomation.mango.db.query.SortOption;

import net.jazdw.rql.parser.ASTNode;
import net.jazdw.rql.parser.ASTVisitor;

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

    protected boolean limited = false;
    protected int unlimitedSize = 0;

    @Override
    public List<T> visit(ASTNode node, List<T> data) {
        QueryComparison comparison;
        List<T> result;

        switch (node.getName()) {
            case "and":
                for (Object obj : node) {
                    data = ((ASTNode) obj).accept(this, data);
                }
                result = data;
                break;
            case "or":
                Set<T> matched = new LinkedHashSet<>();
                for (Object obj : node) {
                    matched.addAll(((ASTNode) obj).accept(this, data));
                }
                result = new ArrayList<T>(matched);
                break;
            case "sort":
                result = applySort(node, data);
                break;
            case "limit":
                result = applyLimit(node.getArguments(), data);
                break;
            default:
                comparison = createComparison(node);
                result = compare(comparison, data);
                break;
        }

        if (!limited) {
            unlimitedSize = result.size();
        }

        return result;
    }

    /**
     * Override as necessary for custom comparisons
     * @param field
     * @param comparison
     * @param args
     * @return
     */
    protected QueryComparison createComparison(String field, ComparisonEnum comparison, List<Object> args) {
        return new QueryComparison(field, comparison, args);
    }

    /**
     * @param arguments
     * @param data
     * @return
     */
    protected List<T> applyLimit(List<Object> arguments, List<T> data) {
        if(arguments.size() > 0){
            this.limited = true;
            this.unlimitedSize = data.size();

            if(arguments.size() == 1){
                int limit = (int)arguments.get(0);
                if(data.size() > limit)
                    return data.subList(0, limit);
                else
                    return data;
            }else{
                //Do limit and offset
                int limit = (int)arguments.get(0);
                int offset = (int)arguments.get(1);
                int end = offset + limit;

                //Compute end location
                if(end>data.size())
                    end = data.size();

                return data.subList(offset, end);
            }
        }else{
            return data;
        }
    }

    /**
     * @param node
     * @param data
     * @return
     */
    protected List<T> applySort(ASTNode node, List<T> data) {

        if(node.getArgumentsSize() == 0)
            return data;
        List<T> sorted = new ArrayList<T>(data);
        boolean descending = false;
        SortOption sort;
        SortOptionComparator<Object> compare;

        for (Object arg : node) {

            String prop = (String) arg;
            if (prop.startsWith("-")) {
                descending = true;
                prop = prop.substring(1);
            }else if (prop.startsWith("+")) {
                prop = prop.substring(1);
                descending = false;
            } else {
                descending = false;
            }

            sort = new SortOption(prop, descending);
            compare = new SortOptionComparator<Object>(sort);
            Collections.sort(sorted, compare);
        }

        return sorted;

    }

    /**
     * @param comparison
     * @param data
     * @return
     */
    protected List<T> compare(QueryComparison comparison, List<T> data) {

        List<T> keep = new ArrayList<T>();
        for(T d : data){
            if(comparison.apply(d))
                keep.add(d);
        }
        return keep;
    }

    public int getUnlimitedSize() {
        return unlimitedSize;
    }

    @SuppressWarnings("unchecked")
    private QueryComparison createComparison(ASTNode node) {
        ComparisonEnum comparison = ComparisonEnum.convertTo(node.getName());

        List<Object> args = node.getArguments();
        String field = (String) args.get(0);
        List<Object> remainingArgs = args.subList(1, args.size());

        if (comparison == ComparisonEnum.IN) {
            if (remainingArgs.get(0) instanceof List) {
                remainingArgs = (List<Object>) remainingArgs.get(0);
            }
        }

        return new QueryComparison(field, comparison, remainingArgs);
    }
}
