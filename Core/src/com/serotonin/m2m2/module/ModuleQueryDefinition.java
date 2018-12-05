/**
 * @copyright 2018 {@link http://infiniteautomation.com|Infinite Automation Systems, Inc.} All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.module;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.access.AccessDeniedException;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.infiniteautomation.mango.spring.MangoRuntimeContextConfiguration;
import com.infiniteautomation.mango.util.exception.ValidationException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.SystemSettingsDao;
import com.serotonin.m2m2.i18n.ProcessResult;
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
     * Get the name of the table that the query from createQuery will be used on
     * @return
     */
    abstract public String getTableName();

    /**
     * Validate the inputs for the query
     *
     * @param parameters
     */
    abstract protected void validateImpl(final User user, final JsonNode parameters, final ProcessResult result);

    /**
     * Return information about the parameters and types for the query
     * @return
     */
    abstract public JsonNode getExplainInfo();

    /**
     * Create an AST Node for this query.
     * @param parameters
     * @return
     */
    abstract public ASTNode createQuery(User user, JsonNode parameters) throws IOException;

    protected ObjectReader readerFor(Class<?> clazz) {
        return Common.getBean(ObjectMapper.class, MangoRuntimeContextConfiguration.REST_OBJECT_MAPPER_NAME).readerFor(clazz);
    }

    public void validate(final User user, final String tableName, final JsonNode input, final ProcessResult result) throws ValidationException {
        if(!StringUtils.equals(getTableName(), tableName)) {
            //This definition doesn't match the table it is going to run on, abort
            result.addContextualMessage("queryTypeName", "validate.invalidValue");
            return;
        }

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
        if(!Permissions.hasPermission(user, SystemSettingsDao.instance.getValue(def.getPermissionTypeName())))
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

    public class ParameterInfo {

        final String type;
        final boolean required;
        final Object defaultValue;
        final TranslatableMessage description;

        public ParameterInfo(String type, boolean required, Object defaultValue, TranslatableMessage description) {
            this.type = type;
            this.required = required;
            this.defaultValue = defaultValue;
            this.description = description;
        }

        public boolean isRequired() {
            return required;
        }
        public String getType() {
            return type;
        }
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public Object getDefaultValue() {
            return defaultValue;
        }
        public TranslatableMessage getDescription() {
            return description;
        }
    }

}