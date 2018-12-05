/**
 * @copyright 2018 {@link http://infiniteautomation.com|Infinite Automation Systems, Inc.} All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.serotonin.ShouldNeverHappenException;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.module.ModuleQueryDefinition;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.vo.User;

import net.jazdw.rql.parser.ASTNode;

/**
 * Base class for Module Defined Query Models
 * @author Terry Packer
 */
public class ModuleQueryModel {

    @JsonIgnore
    protected ModuleQueryDefinition definition;
    /**
     * The queryTypeName from the definition
     */
    protected String queryType;
    
    /**
     * The inputs for the query
     */
    protected JsonNode parameters;
    
    
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
     * @return the parameters
     */
    public JsonNode getParameters() {
        return parameters;
    }

    /**
     * @param parameters the parameters to set
     */
    public void setParameters(JsonNode parameters) {
        this.parameters = parameters;
    }

    public ASTNode createQuery(User user) throws IOException {
        if(definition == null)
            throw new ShouldNeverHappenException("Ensure valid first!");
        return definition.createQuery(user, parameters);
    }
    /**
     * To be called prior to creating the query
     */
    public void ensureValid(User user, String tableName) {
        ProcessResult result = new ProcessResult();
        if(queryType == null)
            result.addContextualMessage("queryType", "validate.required");
        this.definition = ModuleRegistry.getModuleQueryDefinition(queryType);
        if(this.definition == null) {
            result.addContextualMessage("definition", "validate.invalidValue");
            result.ensureValid();
        }
        definition.ensurePermission(user);
        definition.validate(user, tableName, parameters, result);
        result.ensureValid();
    }
    
    /**
     * @return the definition
     */
    public ModuleQueryDefinition getDefinition() {
        return definition;
    }

    /**
     * @param definition the definition to set
     */
    public void setDefinition(ModuleQueryDefinition definition) {
        this.definition = definition;
    }
    
}
