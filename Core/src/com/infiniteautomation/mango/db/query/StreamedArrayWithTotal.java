/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.db.query;

/**
 * Model for typical RQL query results, an object with two members, the array of items and a total.
 * 
 * @author Jared Wiltshire
 */
public interface StreamedArrayWithTotal {
    StreamedArray getItems();
    int getTotal();
}
