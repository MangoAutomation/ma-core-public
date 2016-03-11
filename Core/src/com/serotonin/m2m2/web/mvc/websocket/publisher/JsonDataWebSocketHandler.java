/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.websocket.publisher;

import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.json.JsonDataVO;
import com.serotonin.m2m2.vo.permission.Permissions;
import com.serotonin.m2m2.web.mvc.rest.v1.model.jsondata.JsonDataModel;
import com.serotonin.m2m2.web.mvc.websocket.DaoNotificationWebSocketHandler;

/**
 * @author Terry Packer
 *
 */
public class JsonDataWebSocketHandler extends DaoNotificationWebSocketHandler<JsonDataVO>{

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.web.mvc.websocket.DaoNotificationWebSocketHandler#hasPermission(com.serotonin.m2m2.vo.User, com.serotonin.m2m2.vo.AbstractVO)
     */
    @Override
    protected boolean hasPermission(User user, JsonDataVO vo) {
        return Permissions.hasPermission(user, vo.getReadPermission());
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.web.mvc.websocket.DaoNotificationWebSocketHandler#createModel(com.serotonin.m2m2.vo.AbstractVO)
     */
    @Override
    protected Object createModel(JsonDataVO vo) {
        return new JsonDataModel(vo);
    }

	
}
