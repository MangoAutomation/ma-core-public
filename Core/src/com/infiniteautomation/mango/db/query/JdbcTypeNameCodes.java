/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.infiniteautomation.mango.db.query;

import java.lang.reflect.Field;
import java.sql.Types;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @author Terry Packer
 *
 */
public class JdbcTypeNameCodes {
	
	public static JdbcTypeNameCodes instance = new JdbcTypeNameCodes();
	private Map<Integer, String> codeMap;
	
	private JdbcTypeNameCodes() {

	    codeMap = new HashMap<Integer, String>();

	    for (Field field : Types.class.getFields()) {
	        try {
				codeMap.put((Integer)field.get(null), field.getName());
			} catch (IllegalArgumentException | IllegalAccessException e) {
				//Don't really care
			}
	    }
	}

	public String getTypeName(int sqlType){
		return codeMap.get(sqlType);
	}
	
	public int getType(String typeName){
		Iterator<Integer> it = codeMap.keySet().iterator();
		while(it.hasNext()){
			int type = it.next();
			if(codeMap.get(type).equalsIgnoreCase(typeName))
				return type; 
		}
		return -1;
	}
	
}
