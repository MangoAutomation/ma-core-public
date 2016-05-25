/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.websocket.publisher;

import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.publish.PublisherVO;
import com.serotonin.m2m2.web.mvc.websocket.DaoNotificationWebSocketHandler;

/**
 * @author Terry Packer
 *
 */
public class PublisherWebSocketHandler extends DaoNotificationWebSocketHandler<PublisherVO<?>>{

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.web.mvc.websocket.DaoNotificationWebSocketHandler#hasPermission(com.serotonin.m2m2.vo.User, java.lang.Object)
	 */
	@Override
	protected boolean hasPermission(User user, PublisherVO<?> vo) {
		if(user.isAdmin())
			return true;
		else 
			return true; //TODO Implement permissions for publishers... Permissions.hasPublisherPermission(user, vo);
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.web.mvc.websocket.DaoNotificationWebSocketHandler#createModel(java.lang.Object)
	 */
	@Override
	protected Object createModel(PublisherVO<?> vo) {
		return vo.asModel();
	}

}
