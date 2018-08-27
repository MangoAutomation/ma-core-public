/**
 * @copyright 2018 {@link http://infiniteautomation.com|Infinite Automation Systems, Inc.} All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.module.definitions.query;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.infiniteautomation.mango.rest.v2.model.RestValidationResult;
import com.serotonin.db.MappedRowCallback;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.db.dao.SchemaDefinition;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.ModuleQueryDefinition;
import com.serotonin.m2m2.rt.event.type.EventType.EventTypeNames;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.permission.Permissions;

import net.jazdw.rql.parser.ASTNode;

/**
 *
 * @author Terry Packer
 */
public class DataPointEventsByTagQueryDefinition extends ModuleQueryDefinition {

    public static final String QUERY_TYPE_NAME = "DATA_POINT_EVENTS_BY_TAG";

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.module.ModuleQueryDefinition#getQueryTypeName()
     */
    @Override
    public String getQueryTypeName() {
        return QUERY_TYPE_NAME;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.module.ModuleQueryDefinition#getPermissionTypeName()
     */
    @Override
    protected String getPermissionTypeName() {
        return null; //Don't have any permissions for this query
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.module.ModuleQueryDefinition#getTableName()
     */
    @Override
    public String getTableName() {
        return SchemaDefinition.EVENTS_TABLE;
    }
    /* (non-Javadoc)
     * @see com.serotonin.m2m2.module.ModuleQueryDefinition#validateImpl(com.fasterxml.jackson.databind.JsonNode)
     */
    @Override
    protected void validateImpl(final User user, final JsonNode parameters, final RestValidationResult result) {
        if(parameters.get("tags") == null)
            result.addRequiredError("tags");
        else {
            //Ensure the format of tags is a Map<String,String>
            ObjectReader reader = this.readerFor(Map.class);
            try {
                reader.readValue(parameters.get("tags"));
            }catch(IOException e) {
                result.addInvalidValueError("tags");
            }
        }
        if(parameters.has("limit")) {
            if(!parameters.get("limit").isNumber())
                result.addInvalidValueError("limit");
        }

        if(parameters.has("offset")) {
            if(!parameters.get("offset").isNumber())
                result.addInvalidValueError("offset");
        }
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.module.ModuleQueryDefinition#createQuery(com.fasterxml.jackson.databind.JsonNode)
     */
    @Override
    public ASTNode createQuery(User user, JsonNode parameters) throws IOException {
        JsonNode tagsNode = parameters.get("tags");
        ObjectReader reader = this.readerFor(Map.class);
        Map<String, String> tags = reader.readValue(tagsNode);
        //Lookup data points by tag
        List<Object> args = new ArrayList<>();
        args.add("typeRef1");
        DataPointDao.getInstance().dataPointsForTags(tags, user, new MappedRowCallback<DataPointVO>() {
            @Override
            public void row(DataPointVO dp, int index) {
                if(Permissions.hasDataPointReadPermission(user, dp)){
                    args.add(Integer.toString(dp.getId()));
                }
            }
        });

        //Create Event Query for these Points
        if(args.size() > 0) {
            ASTNode query = new ASTNode("in", args);
            query = addAndRestriction(query, new ASTNode("eq", "userId", user.getId()));
            query = addAndRestriction(query, new ASTNode("eq", "typeName", EventTypeNames.DATA_POINT));

            if(parameters.has("limit")) {
                int offset = 0;
                int limit = parameters.get("limit").asInt();
                if(parameters.has("offset"))
                    offset = parameters.get("offset").asInt();
                query = addAndRestriction(query, new ASTNode("limit", limit, offset));
            }
            return query;
        }else {
            return new ASTNode("limit", 0, 0);
        }
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.module.ModuleQueryDefinition#getExplainInfo()
     */
    @Override
    public JsonNode getExplainInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("tags", new ParameterInfo("Map", true, null, new TranslatableMessage("common.default", "Tags used in query")));
        info.put("limit", new ParameterInfo("Number", false, null, new TranslatableMessage("common.default", "Limit of data points to return events for")));
        info.put("offset", new ParameterInfo("Number", false, 0, new TranslatableMessage("common.default", "Offset for limit")));
        return JsonNodeFactory.instance.pojoNode(info);
    }
}
