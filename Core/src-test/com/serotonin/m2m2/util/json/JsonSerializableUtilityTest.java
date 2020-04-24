/**
 * @copyright 2018 {@link http://infiniteautomation.com|Infinite Automation Systems, Inc.} All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.util.json;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.serotonin.json.JsonException;
import com.serotonin.m2m2.MockMangoProperties;
import com.serotonin.m2m2.util.JsonSerializableUtility;
import com.serotonin.m2m2.util.json.JsonSerializableTestObject.JsonSerializableTestEnum;
import com.serotonin.provider.Providers;
import com.serotonin.util.properties.MangoProperties;

/**
 *
 * @author Terry Packer
 */
public class JsonSerializableUtilityTest {

    @BeforeClass
    public static void staticSetup() throws IOException{
        //Setup Mango properties Provider as we indirectly access Common
        Providers.add(MangoProperties.class, new MockMangoProperties());
    }

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

        Map<String, Object> annotatedMap1 = new LinkedHashMap<>();
        annotatedMap1.put("int", Integer.valueOf(1));
        annotatedMap1.put("string", new String("one"));

        Map<String, Object> annotatedMap2 = new LinkedHashMap<>();
        annotatedMap2.put("string", new String("one"));
        annotatedMap2.put("int", Integer.valueOf(1));

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
        annotatedMap1.put("int", Integer.valueOf(1));
        annotatedMap1.put("string", new String("one"));

        Map<String, Object> annotatedMap2 = new HashMap<>();
        annotatedMap2.put("int", Integer.valueOf(1));
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
        annotatedMap2.put("int", Integer.valueOf(2));
        changes = util.findChanges(o1, o2);
        if(changes.size() != 1)
            Assert.fail("annotatedMap should have changed");
        @SuppressWarnings("unchecked")
        Map<String, Object> changedMap = (Map<String, Object>) changes.get("annotatedMap");
        Assert.assertNotNull("annotatedMap change should have found", changedMap);
        Assert.assertEquals("two", changedMap.get("string"));
        Assert.assertEquals(2, (int)changedMap.get("int"));
    }

    //TODO Test writerMap

    @Test
    public void testwriterListLength() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, JsonException, IOException {

        List<Object> annotatedList1 = new ArrayList<>();
        annotatedList1.add(Integer.valueOf(1));
        annotatedList1.add(Integer.valueOf(2));

        List<Object> annotatedList2 = new ArrayList<>();
        annotatedList2.add(Integer.valueOf(1));
        annotatedList2.add(Integer.valueOf(2));


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
        Assert.assertEquals(1, changedList.size());
        Assert.assertEquals(1, changedList.get(0));
    }

    @Test
    public void testAnnotatedListOrder() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, JsonException, IOException {

        List<Object> annotatedList1 = new ArrayList<>();
        annotatedList1.add(Integer.valueOf(1));
        annotatedList1.add(Integer.valueOf(2));

        List<Object> annotatedList2 = new ArrayList<>();
        annotatedList2.add(Integer.valueOf(1));
        annotatedList2.add(Integer.valueOf(2));


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
        annotatedList2.add(Integer.valueOf(2));
        annotatedList2.add(Integer.valueOf(1));
        changes = util.findChanges(o1, o2);
        if(changes.size() != 1)
            Assert.fail("List should have changed");
        @SuppressWarnings("unchecked")
        List<Object> changedList = (List<Object>) changes.get("annotatedList");
        Assert.assertNotNull("List should have changed", changedList);
        Assert.assertEquals(2, changedList.size());
        Assert.assertEquals(2, changedList.get(0));
        Assert.assertEquals(1, changedList.get(1));
    }

    @Test
    public void testAnnotatedList() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, JsonException, IOException {

        List<Object> annotatedList1 = new ArrayList<>();
        annotatedList1.add(Integer.valueOf(1));
        annotatedList1.add(Integer.valueOf(2));

        List<Object> annotatedList2 = new ArrayList<>();
        annotatedList2.add(Integer.valueOf(2));
        annotatedList2.add(Integer.valueOf(3));


        JsonSerializableTestObject o1 = new JsonSerializableTestObject();
        o1.setAnnotatedList(annotatedList1);

        JsonSerializableTestObject o2 = new JsonSerializableTestObject();
        o2.setAnnotatedList(annotatedList2);

        JsonSerializableUtility util = new JsonSerializableUtility();
        Map<String, Object> changes = util.findChanges(o1, o2);

        if(changes.size() != 1)
            Assert.fail("List should have changed");
        @SuppressWarnings("unchecked")
        List<Object> changedList = (List<Object>) changes.get("annotatedList");
        Assert.assertNotNull("List should have changed", changedList);
        Assert.assertEquals(2, changedList.size());
        Assert.assertEquals(2, changedList.get(0));
        Assert.assertEquals(3, changedList.get(1));
    }

    @Test
    public void testWriterListLength() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, JsonException, IOException {

        List<Object> writerList1 = new ArrayList<>();
        writerList1.add(Integer.valueOf(1));
        writerList1.add(Integer.valueOf(2));

        List<Object> writerList2 = new ArrayList<>();
        writerList2.add(Integer.valueOf(1));
        writerList2.add(Integer.valueOf(2));


        JsonSerializableTestObject o1 = new JsonSerializableTestObject();
        o1.setWriterList(writerList1);

        JsonSerializableTestObject o2 = new JsonSerializableTestObject();
        o2.setWriterList(writerList2);

        JsonSerializableUtility util = new JsonSerializableUtility();
        Map<String, Object> changes = util.findChanges(o1, o2);

        //No Changes
        if(changes.size() != 0)
            Assert.fail("List should not have changed");

        //Modify the List
        writerList2.remove(1);
        changes = util.findChanges(o1, o2);
        if(changes.size() != 1)
            Assert.fail("List should have changed");
        @SuppressWarnings("unchecked")
        List<Object> changedList = (List<Object>) changes.get("writerList");
        Assert.assertNotNull("List should have changed", changedList);
        Assert.assertEquals(1, changedList.size());
        Assert.assertEquals(1, changedList.get(0));
    }

    @Test
    public void testWriterListOrder() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, JsonException, IOException {

        List<Object> writerList1 = new ArrayList<>();
        writerList1.add(Integer.valueOf(1));
        writerList1.add(Integer.valueOf(2));

        List<Object> writerList2 = new ArrayList<>();
        writerList2.add(Integer.valueOf(1));
        writerList2.add(Integer.valueOf(2));


        JsonSerializableTestObject o1 = new JsonSerializableTestObject();
        o1.setWriterList(writerList1);

        JsonSerializableTestObject o2 = new JsonSerializableTestObject();
        o2.setWriterList(writerList2);

        JsonSerializableUtility util = new JsonSerializableUtility();
        Map<String, Object> changes = util.findChanges(o1, o2);

        //No Changes
        if(changes.size() != 0)
            Assert.fail("List should not have changed");

        //Modify the List
        writerList2.clear();
        writerList2.add(Integer.valueOf(2));
        writerList2.add(Integer.valueOf(1));
        changes = util.findChanges(o1, o2);
        if(changes.size() != 1)
            Assert.fail("List should have changed");
        @SuppressWarnings("unchecked")
        List<Object> changedList = (List<Object>) changes.get("writerList");
        Assert.assertNotNull("List should have changed", changedList);
        Assert.assertEquals(2, changedList.size());
        Assert.assertEquals(2, changedList.get(0));
        Assert.assertEquals(1, changedList.get(1));
    }

    @Test
    public void testWriterList() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, JsonException, IOException {

        List<Object> writerList1 = new ArrayList<>();
        writerList1.add(Integer.valueOf(1));
        writerList1.add(Integer.valueOf(2));

        List<Object> writerList2 = new ArrayList<>();
        writerList2.add(Integer.valueOf(2));
        writerList2.add(Integer.valueOf(3));


        JsonSerializableTestObject o1 = new JsonSerializableTestObject();
        o1.setWriterList(writerList1);

        JsonSerializableTestObject o2 = new JsonSerializableTestObject();
        o2.setWriterList(writerList2);

        JsonSerializableUtility util = new JsonSerializableUtility();
        Map<String, Object> changes = util.findChanges(o1, o2);

        if(changes.size() != 1)
            Assert.fail("List should have changed");
        @SuppressWarnings("unchecked")
        List<Object> changedList = (List<Object>) changes.get("writerList");
        Assert.assertNotNull("List should have changed", changedList);
        Assert.assertEquals(2, changedList.size());
        Assert.assertEquals(2, changedList.get(0));
        Assert.assertEquals(3, changedList.get(1));
    }

    @Test
    public void testRollupEnum() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, JsonException, IOException {

        JsonSerializableTestObject o1 = new JsonSerializableTestObject();

        JsonSerializableTestObject o2 = new JsonSerializableTestObject();

        JsonSerializableUtility util = new JsonSerializableUtility();
        Map<String, Object> changes = util.findChanges(o1, o2);

        if(changes.size() != 0)
            Assert.fail("Day Of Week should not have changed");

        o2.setDayOfWeekEnum(DayOfWeek.FRIDAY);
        changes = util.findChanges(o1, o2);
        if(changes.size() != 1)
            Assert.fail("Day Of Week should have changed");
        DayOfWeek dow = (DayOfWeek) changes.get("dayOfWeekEnum");
        Assert.assertEquals(DayOfWeek.FRIDAY, dow);
    }

    @Test
    public void testListJsonSerializable() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, JsonException, IOException {

        List<Object> writerList1 = new ArrayList<>();
        writerList1.add(new JsonSerializableTestObject());
        writerList1.add(new JsonSerializableTestObject());

        List<Object> writerList2 = new ArrayList<>();
        writerList2.add(new JsonSerializableTestObject());
        writerList2.add(new JsonSerializableTestObject());


        JsonSerializableTestObject o1 = new JsonSerializableTestObject();
        o1.setWriterList(writerList1);

        JsonSerializableTestObject o2 = new JsonSerializableTestObject();
        o2.setWriterList(writerList2);

        JsonSerializableUtility util = new JsonSerializableUtility();
        Map<String, Object> changes = util.findChanges(o1, o2);

        if(changes.size() != 0)
            Assert.fail("List should not have changed");

        ((JsonSerializableTestObject)o2.getWriterList().get(0)).setDayOfWeekEnum(DayOfWeek.SUNDAY);
        changes = util.findChanges(o1, o2);
        if(changes.size() != 1)
            Assert.fail("List should have changed");
        @SuppressWarnings("unchecked")
        List<JsonSerializableTestObject> changedList = (List<JsonSerializableTestObject>) changes.get("writerList");
        Assert.assertNotNull("List should have changed", changedList);
        Assert.assertEquals(2, changedList.size());
        Assert.assertEquals(DayOfWeek.SUNDAY, changedList.get(0).getDayOfWeekEnum());
        Assert.assertEquals(DayOfWeek.MONDAY, changedList.get(1).getDayOfWeekEnum());
    }

    @Test
    public void testIntArray() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, JsonException, IOException {

        JsonSerializableTestObject o1 = new JsonSerializableTestObject();
        JsonSerializableTestObject o2 = new JsonSerializableTestObject();

        JsonSerializableUtility util = new JsonSerializableUtility();
        Map<String, Object> changes = util.findChanges(o1, o2);

        if(changes.size() != 0)
            Assert.fail("Int array should not have changed");

        o2.setIntArray(new int[] {1,2,3});
        changes = util.findChanges(o1, o2);
        if(changes.size() != 1)
            Assert.fail("Int array should have changed");

        int[] changed = (int[]) changes.get("intArray");
        Assert.assertNotNull("Int array should have changed", changed);
        Assert.assertEquals(3, changed.length);
        Assert.assertEquals(1, changed[0]);
        Assert.assertEquals(2, changed[1]);
        Assert.assertEquals(3, changed[2]);

    }

    @Test
    public void testIntArrayOrder() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, JsonException, IOException {

        JsonSerializableTestObject o1 = new JsonSerializableTestObject();
        JsonSerializableTestObject o2 = new JsonSerializableTestObject();

        JsonSerializableUtility util = new JsonSerializableUtility();
        Map<String, Object> changes = util.findChanges(o1, o2);

        if(changes.size() != 0)
            Assert.fail("Int array should not have changed");

        o1.setIntArray(new int[] {3,2,1});
        o2.setIntArray(new int[] {1,2,3});

        changes = util.findChanges(o1, o2);
        if(changes.size() != 1)
            Assert.fail("Int array should have changed");

        int[] changed = (int[]) changes.get("intArray");
        Assert.assertNotNull("Int array should have changed", changed);
        Assert.assertEquals(3, changed.length);
        Assert.assertEquals(1, changed[0]);
        Assert.assertEquals(2, changed[1]);
        Assert.assertEquals(3, changed[2]);
    }

    @Test
    public void testObjectArray() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, JsonException, IOException {

        JsonSerializableTestObject o1 = new JsonSerializableTestObject();
        JsonSerializableTestObject o2 = new JsonSerializableTestObject();

        JsonSerializableUtility util = new JsonSerializableUtility();
        Map<String, Object> changes = util.findChanges(o1, o2);

        if(changes.size() != 0)
            Assert.fail("Object array should not have changed");

        o1.setObjectArray(new Object[] {Integer.valueOf(0), Integer.valueOf(1)});
        o2.setObjectArray(new Object[] {Integer.valueOf(2), Integer.valueOf(3)});
        changes = util.findChanges(o1, o2);
        if(changes.size() != 1)
            Assert.fail("Object array should have changed");

        Object[] changed = (Object[]) changes.get("objectArray");
        Assert.assertNotNull("Object array should have changed", changed);
        Assert.assertEquals(2, changed.length);
        Assert.assertEquals(2, changed[0]);
        Assert.assertEquals(3, changed[1]);
    }

    @Test
    public void testIntegerArray() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, JsonException, IOException {

        JsonSerializableTestObject o1 = new JsonSerializableTestObject();
        JsonSerializableTestObject o2 = new JsonSerializableTestObject();

        JsonSerializableUtility util = new JsonSerializableUtility();
        Map<String, Object> changes = util.findChanges(o1, o2);

        if(changes.size() != 0)
            Assert.fail("Integer array should not have changed");

        o1.setIntegerArray(new Integer[] {Integer.valueOf(0), Integer.valueOf(1)});
        o2.setIntegerArray(new Integer[] {Integer.valueOf(2), Integer.valueOf(3)});
        changes = util.findChanges(o1, o2);
        if(changes.size() != 1)
            Assert.fail("Integer array should have changed");

        Object[] changed = (Object[]) changes.get("integerArray");
        Assert.assertNotNull("Integer array should have changed", changed);
        Assert.assertEquals(2, changed.length);
        Assert.assertEquals(2, changed[0]);
        Assert.assertEquals(3, changed[1]);
    }
}
