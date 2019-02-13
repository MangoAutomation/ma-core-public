/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.rt.event.type.definition;

import java.util.ArrayList;
import java.util.List;

import com.serotonin.m2m2.db.dao.UserDao;
import com.serotonin.m2m2.i18n.Translations;
import com.serotonin.m2m2.module.SystemEventTypeDefinition;
import com.serotonin.m2m2.rt.event.type.SystemEventType;
import com.serotonin.m2m2.vo.User;

/**
 * @author Terry Packer
 *
 */
public class UserLoginEventTypeDefinition extends SystemEventTypeDefinition {

    @Override
    public String getTypeName() {
       return SystemEventType.TYPE_USER_LOGIN;
    }

    @Override
    public String getDescriptionKey() {
        return "event.system.userLogin";
    }

    @Override
    public String getEventListLink(int ref1, int ref2, Translations translations) {
        return null;
    }

    @Override
    public boolean supportsReferenceId1() {
        return true;
    }

    @Override
    public boolean supportsReferenceId2() {
        return false;
    }
    
    @Override
    public List<SystemEventType> genegeneratePossibleEventTypesWithReferenceId1() {
        List<User> users = UserDao.getInstance().getActiveUsers();
        List<SystemEventType> types = new ArrayList<>(users.size());
        for(User user : users)
            types.add(new SystemEventType(SystemEventType.TYPE_USER_LOGIN, user.getId()));
        
        return types;
    }

}
