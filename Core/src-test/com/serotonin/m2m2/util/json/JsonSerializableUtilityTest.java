/**
 * @copyright 2018 {@link http://infiniteautomation.com|Infinite Automation Systems, Inc.} All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.util.json;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.serotonin.json.JsonException;
import com.serotonin.m2m2.util.JsonSerializableUtility;
import com.serotonin.m2m2.util.json.JsonSerializableTestObject.JsonSerializableTestEnum;

/**
 *
 * @author Terry Packer
 */
public class JsonSerializableUtilityTest {


    @Test
    public void testNoChange() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, JsonException, IOException {
        
        JsonSerializableTestObject o1 = new JsonSerializableTestObject();
        JsonSerializableTestObject o2 = new JsonSerializableTestObject();
        
        JsonSerializableUtility util = new JsonSerializableUtility();
        Map<String, Object> changes = util.findChanges(o1, o2);
        if(changes.size() > 0){
            Assert.fail("Should be no changes.");
        }
        
    }
    
    @Test
    public void testInnerEnum() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, JsonException, IOException {
        
        JsonSerializableTestObject o1 = new JsonSerializableTestObject();
        o1.setWriterInnerEnum(JsonSerializableTestEnum.ONE);
        
        JsonSerializableTestObject o2 = new JsonSerializableTestObject();
        o2.setWriterInnerEnum(JsonSerializableTestEnum.TWO);
        
        JsonSerializableUtility util = new JsonSerializableUtility();
        Map<String, Object> changes = util.findChanges(o1, o2);
        if(changes.size() != 1)
            Assert.fail("Should be a change of innerEnum");
        if(changes.get("innerEnum") != JsonSerializableTestEnum.TWO)
            Assert.fail("Should be a change of innerEnum to TWO");
    }

    @Test
    public void testAnnotatedMapOrder() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, JsonException, IOException {
        
        Map<String, Object> annotatedMap1 = new HashMap<>();
        annotatedMap1.put("int", new Integer(1));
        annotatedMap1.put("string", new String("one"));
        
        Map<String, Object> annotatedMap2 = new HashMap<>();
        annotatedMap2.put("string", new String("one"));
        annotatedMap2.put("int", new Integer(1));
        
        JsonSerializableTestObject o1 = new JsonSerializableTestObject();
        o1.setAnnotatedMap(annotatedMap1);
        
        JsonSerializableTestObject o2 = new JsonSerializableTestObject();
        o2.setAnnotatedMap(annotatedMap2);
        
        JsonSerializableUtility util = new JsonSerializableUtility();
        Map<String, Object> changes = util.findChanges(o1, o2);
        if(changes.size() != 0)
            Assert.fail("Map order should not matter");
        
    }
    
    @Test
    public void testAnnotatedMap() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, JsonException, IOException {
        
        Map<String, Object> annotatedMap1 = new HashMap<>();
        annotatedMap1.put("int", new Integer(1));
        annotatedMap1.put("string", new String("one"));
        
        Map<String, Object> annotatedMap2 = new HashMap<>();
        annotatedMap2.put("int", new Integer(1));
        annotatedMap2.put("string", new String("one"));
        
        JsonSerializableTestObject o1 = new JsonSerializableTestObject();
        o1.setAnnotatedMap(annotatedMap1);
        
        JsonSerializableTestObject o2 = new JsonSerializableTestObject();
        o2.setAnnotatedMap(annotatedMap2);

        JsonSerializableUtility util = new JsonSerializableUtility();
        Map<String, Object> changes = util.findChanges(o1, o2);
        
        if(changes.size() != 0)
            Assert.fail("annotatedMap should not have changed.");
        
        //Make change
        annotatedMap2.put("string", new String("two"));
        annotatedMap2.put("int", new Integer(2));
        changes = util.findChanges(o1, o2);
        if(changes.size() != 1)
            Assert.fail("annotatedMap should have changed");
        @SuppressWarnings("unchecked")
        Map<String, Object> changedMap = (Map<String, Object>) changes.get("annotatedMap");
        Assert.assertNotNull("annotatedMap change should have found", changedMap);
        Assert.assertEquals("two", changedMap.get("string"));
        Assert.assertEquals((int)2, (int)changedMap.get("int"));
    }
    
    //TODO Test writerMap
    
    @Test
    public void testAnnotatedList() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, JsonException, IOException {
        
        List<Object> annotatedList1 = new ArrayList<>();
        annotatedList1.add(new Integer(1));
        annotatedList1.add(new Integer(2));
        
        List<Object> annotatedList2 = new ArrayList<>();
        annotatedList2.add(new Integer(1));
        annotatedList2.add(new Integer(2));

        
        JsonSerializableTestObject o1 = new JsonSerializableTestObject();
        o1.setAnnotatedList(annotatedList1);
        
        JsonSerializableTestObject o2 = new JsonSerializableTestObject();
        o2.setAnnotatedList(annotatedList2);
        
        JsonSerializableUtility util = new JsonSerializableUtility();
        Map<String, Object> changes = util.findChanges(o1, o2);
        
        //No Changes
        if(changes.size() != 0)
            Assert.fail("List should not have changed");
        
        //Modify the List
        annotatedList2.remove(1);
        changes = util.findChanges(o1, o2);
        if(changes.size() != 1)
            Assert.fail("List should have changed");
        @SuppressWarnings("unchecked")
        List<Object> changedList = (List<Object>) changes.get("annotatedList");
        Assert.assertNotNull("List should have changed", changedList);
        Assert.assertEquals((int)1, changedList.size());
        Assert.assertEquals((int)1, changedList.get(0));
    }
    
    @Test
    public void testAnnotatedListOrder() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, JsonException, IOException {
        
        List<Object> annotatedList1 = new ArrayList<>();
        annotatedList1.add(new Integer(1));
        annotatedList1.add(new Integer(2));
        
        List<Object> annotatedList2 = new ArrayList<>();
        annotatedList2.add(new Integer(1));
        annotatedList2.add(new Integer(2));

        
        JsonSerializableTestObject o1 = new JsonSerializableTestObject();
        o1.setAnnotatedList(annotatedList1);
        
        JsonSerializableTestObject o2 = new JsonSerializableTestObject();
        o2.setAnnotatedList(annotatedList2);
        
        JsonSerializableUtility util = new JsonSerializableUtility();
        Map<String, Object> changes = util.findChanges(o1, o2);
        
        //No Changes
        if(changes.size() != 0)
            Assert.fail("List should not have changed");
        
        //Modify the List
        annotatedList2.clear();
        annotatedList2.add(new Integer(2));
        annotatedList2.add(new Integer(1));
        changes = util.findChanges(o1, o2);
        if(changes.size() != 1)
            Assert.fail("List should have changed");
        @SuppressWarnings("unchecked")
        List<Object> changedList = (List<Object>) changes.get("annotatedList");
        Assert.assertNotNull("List should have changed", changedList);
        Assert.assertEquals((int)2, changedList.size());
        Assert.assertEquals((int)2, changedList.get(0));
        Assert.assertEquals((int)1, changedList.get(1));
    }
    
    //TODO Test writer list
    //TODO test rollup enum
    
    //TODO Test List of JsonSerializable objects...
    
}
