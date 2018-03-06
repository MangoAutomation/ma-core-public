/**
 * @copyright 2018 {@link http://infiniteautomation.com|Infinite Automation Systems, Inc.} All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.util.json;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.ObjectWriter;
import com.serotonin.json.spi.JsonProperty;
import com.serotonin.json.spi.JsonSerializable;
import com.serotonin.json.type.JsonObject;
import com.serotonin.m2m2.Common.Rollups;

/**
 *
 * @author Terry Packer
 */
public class JsonSerializableTestObject implements JsonSerializable{

    
    public enum JsonSerializableTestEnum {
        ONE("common.default1"),
        TWO("common.default2"),
        THREE("common.default2");
        
        private final String key;
        JsonSerializableTestEnum(String key){
            this.key = key;
        }
        public String getKey() {
            return this.key;
        }
    }

    @JsonProperty
    private JsonSerializableTestEnum innerAnnotatedEnum = JsonSerializableTestEnum.ONE;
    @JsonProperty
    private Rollups rollup;
    @JsonProperty
    private String string = "testString";
    @JsonProperty
    private Map<String, Object> annotatedMap;
    @JsonProperty
    private List<Object> annotatedList;
    
    private JsonSerializableTestEnum writerInnerEnum = JsonSerializableTestEnum.ONE;
    private Map<String, Object> writerMap;
    private List<Object> writerList;
    
    /* (non-Javadoc)
     * @see com.serotonin.json.spi.JsonSerializable#jsonWrite(com.serotonin.json.ObjectWriter)
     */
    @Override
    public void jsonWrite(ObjectWriter writer) throws IOException, JsonException {
        writer.writeEntry("innerEnum", writerInnerEnum);
        writer.writeEntry("writerMap", writerMap);
        writer.writeEntry("writerList", writerList);
    }

    /* (non-Javadoc)
     * @see com.serotonin.json.spi.JsonSerializable#jsonRead(com.serotonin.json.JsonReader, com.serotonin.json.type.JsonObject)
     */
    @Override
    public void jsonRead(JsonReader reader, JsonObject jsonObject) throws JsonException {
        //Don't care for Testing Auditing
    }

    /**
     * @return the innerAnnotatedEnum
     */
    public JsonSerializableTestEnum getInnerAnnotatedEnum() {
        return innerAnnotatedEnum;
    }

    /**
     * @param innerAnnotatedEnum the innerAnnotatedEnum to set
     */
    public void setInnerAnnotatedEnum(JsonSerializableTestEnum innerAnnotatedEnum) {
        this.innerAnnotatedEnum = innerAnnotatedEnum;
    }

    /**
     * @return the string
     */
    public String getString() {
        return string;
    }

    /**
     * @param string the string to set
     */
    public void setString(String string) {
        this.string = string;
    }

    /**
     * @return the annotatedMap
     */
    public Map<String, Object> getAnnotatedMap() {
        return annotatedMap;
    }

    /**
     * @param annotatedMap the annotatedMap to set
     */
    public void setAnnotatedMap(Map<String, Object> annotatedMap) {
        this.annotatedMap = annotatedMap;
    }

    /**
     * @return the annotatedList
     */
    public List<Object> getAnnotatedList() {
        return annotatedList;
    }

    /**
     * @param annotatedList the annotatedList to set
     */
    public void setAnnotatedList(List<Object> annotatedList) {
        this.annotatedList = annotatedList;
    }

    /**
     * @return the innerEnum
     */
    public JsonSerializableTestEnum getWriterInnerEnum() {
        return writerInnerEnum;
    }

    /**
     * @param innerEnum the innerEnum to set
     */
    public void setWriterInnerEnum(JsonSerializableTestEnum innerEnum) {
        this.writerInnerEnum = innerEnum;
    }

    /**
     * @return the writerMap
     */
    public Map<String, Object> getWriterMap() {
        return writerMap;
    }

    /**
     * @param writerMap the writerMap to set
     */
    public void setWriterMap(Map<String, Object> writerMap) {
        this.writerMap = writerMap;
    }

    /**
     * @return the writerList
     */
    public List<Object> getWriterList() {
        return writerList;
    }

    /**
     * @param writerList the writerList to set
     */
    public void setWriterList(List<Object> writerList) {
        this.writerList = writerList;
    }

}
