/**
 * @copyright 2018 {@link http://infiniteautomation.com|Infinite Automation Systems, Inc.} All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.module.definitions.query;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectReader;
import com.infiniteautomation.mango.rest.v2.model.RestValidationResult;
import com.serotonin.db.MappedRowCallback;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.module.ModuleQueryDefinition;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.User;

import net.jazdw.rql.parser.ASTNode;

/**
 *
 * @author Terry Packer
 */
public class EventsByTagQueryDefinition extends ModuleQueryDefinition {

    public static final String QUERY_TYPE_NAME = "EVENTS_BY_TAG";
    
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
     * @see com.serotonin.m2m2.module.ModuleQueryDefinition#validateImpl(com.fasterxml.jackson.databind.JsonNode)
     */
    @Override
    protected void validateImpl(final User user, final JsonNode parameters, final RestValidationResult result) {
        if(parameters.get("tags") == null)
            result.addRequiredError("tags");
        //TODO Validate that if limit | offset exist they are integers
        //TODO Validate that tags are Strings
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.module.ModuleQueryDefinition#createQuery(com.fasterxml.jackson.databind.JsonNode)
     */
    @Override
    public ASTNode createQuery(User user, JsonNode parameters) throws IOException {
        JsonNode tagsNode = parameters.get("tags");
        ObjectReader reader = Common.objectMapper.getObjectReader(Map.class);
        Map<String, String> tags = reader.readValue(tagsNode);
        //Lookup data points by tag
        List<Object> args = new ArrayList<>();
        args.add("typeRef1");
        DataPointDao.instance.dataPointsForTags(tags, user, new MappedRowCallback<DataPointVO>() {

            @Override
            public void row(DataPointVO item, int index) {
                //TODO Might as well filter by permissions here?  It happens in the query too
                args.add(Integer.toString(item.getId()));
            }
        });

        //Create Event Query for these Points
        ASTNode query = new ASTNode("in", args);
        query = addAndRestriction(query, new ASTNode("eq", "userId", user.getId()));
        query = addAndRestriction(query, new ASTNode("eq", "typeName", "DATA_POINT"));

        //TODO Should we force a limit if none is supplied?
        if(parameters.has("limit")) {
            int offset = 0;
            int limit = parameters.get("limit").asInt();
            if(parameters.has("offset"))
                offset = parameters.get("offset").asInt();
            query = addAndRestriction(query, new ASTNode("limit", limit, offset));
        }
        return query;
    }

}
