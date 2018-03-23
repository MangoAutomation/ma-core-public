/**
 * @copyright 2018 {@link http://infiniteautomation.com|Infinite Automation Systems, Inc.} All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.module;

import java.io.IOException;

import org.springframework.security.access.AccessDeniedException;

import com.fasterxml.jackson.databind.JsonNode;
import com.infiniteautomation.mango.rest.v2.exception.ValidationFailedRestException;
import com.infiniteautomation.mango.rest.v2.model.RestValidationResult;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.SystemSettingsDao;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.permission.PermissionException;
import com.serotonin.m2m2.vo.permission.Permissions;

import net.jazdw.rql.parser.ASTNode;

/**
 *
 * @author Terry Packer
 */
public abstract class ModuleQueryDefinition extends ModuleElementDefinition {
    
    /**
     * An internal identifier for this type of module query. Must be unique within an MA instance, and is recommended
     * to be unique inasmuch as possible across all modules.
     * 
     * @return the data source type name.
     */
    abstract public String getQueryTypeName();
    
    /**
     * Get the TypeName of the permission definition
     * @return
     */
    abstract protected String getPermissionTypeName();
    
    /**
     * Validate the inputs for the query
     * 
     * @param parameters
     */
    abstract protected void validateImpl(final User user, final JsonNode parameters, final RestValidationResult result);
    
    /**
     * Create an AST Node for this query
     * @param parameters
     * @return
     */
    abstract public ASTNode createQuery(User user, JsonNode parameters) throws IOException;
    
    public void validate(final User user, final JsonNode input, final RestValidationResult result) throws ValidationFailedRestException {
        validateImpl(user, input, result);
    }
    
    /**
     * Ensure that a User has read permission
     * @throws PermissionException
     */
    public void ensurePermission(User user) throws AccessDeniedException{
        PermissionDefinition def = ModuleRegistry.getPermissionDefinition(getPermissionTypeName());
        if(def == null)
            return;
        if(!Permissions.hasPermission(user, SystemSettingsDao.getValue(def.getPermissionTypeName())))
            throw new AccessDeniedException(new TranslatableMessage("permissions.accessDenied", user.getUsername(), new TranslatableMessage(def.getPermissionKey())).translate(Common.getTranslations()));
    }
    
    /**
     * Append an AND Restriction to a query
     * @param query - can be null
     * @param restriction
     * @return
     */
    protected static ASTNode addAndRestriction(ASTNode query, ASTNode restriction){
        //Root query node
        ASTNode root = null;
        
        if(query == null){
            root = restriction;
        }else if(query.getName().equalsIgnoreCase("and")){
            root = query.addArgument(restriction);
        }else{
            root = new ASTNode("and", restriction, query);
        }
        return root;
    }
    
    /**
     * Append an OR restriction to the query
     * @param query - can be null
     * @param restriction
     * @return
     */
    protected static ASTNode addOrRestriction(ASTNode query, ASTNode restriction){
        //Root query node
        ASTNode root = null;
        
        if(query == null){
            root = restriction;
        }else if(query.getName().equalsIgnoreCase("or")){
            root = query.addArgument(restriction);
        }else{
            root = new ASTNode("or", restriction, query);
        }
        return root;
    }
    
}