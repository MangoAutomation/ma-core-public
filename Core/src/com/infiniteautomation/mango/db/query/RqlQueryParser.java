/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.infiniteautomation.mango.db.query;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Terry Packer
 *
 */
public abstract class RqlQueryParser {

	//TODO only works for single sort
    private static final Pattern SORT_PATTERN = Pattern.compile("sort\\(([\\+-]{1})(.*)\\)");
    private static final Pattern LIMIT_PATTERN = Pattern.compile("limit\\((\\d+?)(?:,(\\d+?))?\\)");
    //private static final Pattern LIKE_PATTERN = Pattern.compile("match\\((?*),(?*)?\\)");
	/**
	 * @param query
	 * @return
	 */
	protected QueryModel parseRQL(String query) {

		String[] parts = query == null ? new String[]{} : query.split("&");
		QueryModel model = new QueryModel();
		List<QueryComparison> orComparisons = new ArrayList<QueryComparison>();
		List<QueryComparison> andComparisons = new ArrayList<QueryComparison>();
		List<SortOption> sorts = new ArrayList<SortOption>();
		for(String part : parts){
			//TODO LOG.debug("part: " + part);
		
			if(part.startsWith("sort(")){
				//Sort starts with sort(
				Matcher matcher = SORT_PATTERN.matcher(part);
				if(matcher.matches()){
					//int groupCount = matcher.groupCount();
					boolean desc = false;
					if(matcher.group(1).equals("-"))
						desc = true;
					SortOption sort = new SortOption(matcher.group(2), desc);
					sorts.add(sort);
				}
			}else if(part.startsWith("limit(")){
				Matcher matcher = LIMIT_PATTERN.matcher(part);
				if(matcher.matches()){
					String limit = matcher.group(1);
					if((limit != null)&&(!limit.isEmpty())){
						model.setLimit(Integer.parseInt(limit));
					}
					//Have an offset to use
					String offset = matcher.group(2);
					if((offset != null)&&(!offset.isEmpty())){
						model.setOffset(Integer.parseInt(offset));
					}
				}
			}else if(part.startsWith("like(")){
				Pattern LIKE_PATTERN = Pattern.compile("like\\((.*),(.*)?\\)");
				Matcher matcher = LIKE_PATTERN.matcher(part);
				if(matcher.matches()){
					String attribute = matcher.group(1);
					String condition = matcher.group(2);
					QueryComparison comparison = new QueryComparison(attribute, QueryComparison.LIKE, condition);
					andComparisons.add(comparison);
				}
			}else{
				//Must be Comparison
				String [] currentComparisons;
				String[] comparisonParts;
				
				if(part.contains("=gte=")){
					//Greater than
					comparisonParts = part.split("=gte=");
					QueryComparison comparison = new QueryComparison(comparisonParts[0], QueryComparison.GREATER_THAN_EQUAL_TO, comparisonParts[1]);
					andComparisons.add(comparison);
				}else if(part.contains("=gt=")){
					comparisonParts = part.split("=gt=");
					QueryComparison comparison = new QueryComparison(comparisonParts[0], QueryComparison.GREATER_THAN, comparisonParts[1]);
					andComparisons.add(comparison);
				}else if(part.contains("=lte=")){
					comparisonParts = part.split("=lte=");
					QueryComparison comparison = new QueryComparison(comparisonParts[0], QueryComparison.LESS_THAN_EQUAL_TO, comparisonParts[1]);
					andComparisons.add(comparison);
				}else if(part.contains("=lt=")){
					comparisonParts = part.split("=lt=");
					QueryComparison comparison = new QueryComparison(comparisonParts[0], QueryComparison.LESS_THAN, comparisonParts[1]);
					andComparisons.add(comparison);
				}else{
					//Simple Equals
					if(part.contains("|")){
						//Going to use OR, 
						// Could be xid=volts|xid=temp or (xid=volts|xid=temp)
						//TODO this is ok for now but we need to re-work all of this to allow nesting methods
						part = part.replace("(", "");
						part = part.replace(")", "");
						currentComparisons = part.split("\\|");
						for(String currentComparison : currentComparisons){
							comparisonParts = currentComparison.split("=");
							//TODO Allow nesting so we would make a recursive call on the parts here
							QueryComparison comparison = new QueryComparison(comparisonParts[0], QueryComparison.EQUAL_TO, comparisonParts[1]);
							orComparisons.add(comparison);
						}
					}else{
						comparisonParts = part.split("=");
						if(comparisonParts.length == 2){
							//Must be simple equals
							QueryComparison comparison = new QueryComparison(comparisonParts[0], QueryComparison.EQUAL_TO, comparisonParts[1]);
							andComparisons.add(comparison);
						}
					}
				}
			}
		}
		
		model.setSort(sorts);
		model.setOrComparisons(orComparisons);
		model.setAndComparisons(andComparisons);
		
		return model;
	}
	
}
