/**
 * @copyright 2018 {@link http://infiniteautomation.com|Infinite Automation Systems, Inc.} All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Container to describe how to use a Module defined query
 * @author Terry Packer
 */
public class ModuleQueryExplainModel {

    /**
     * Query type from definition
     */
    private String queryType; 
    /**
     * Describe the parameter types and info
     */
    private JsonNode parameterInfo;
    
    
    public ModuleQueryExplainModel() { }
    
    /**
     * @param tableName
     * @param queryType
     * @param parameterInfo
     */
    public ModuleQueryExplainModel(String queryType, JsonNode parameterInfo) {
        this.queryType = queryType;
        this.parameterInfo = parameterInfo;
    }
    
    /**
     * @return the queryType
     */
    public String getQueryType() {
        return queryType;
    }
    /**
     * @param queryType the queryType to set
     */
    public void setQueryType(String queryType) {
        this.queryType = queryType;
    }
    /**
     * @return the parameterInfo
     */
    public JsonNode getParameterInfo() {
        return parameterInfo;
    }
    /**
     * @param parameterInfo the parameterInfo to set
     */
    public void setParameterInfo(JsonNode parameterInfo) {
        this.parameterInfo = parameterInfo;
    }
    
    
    
}
