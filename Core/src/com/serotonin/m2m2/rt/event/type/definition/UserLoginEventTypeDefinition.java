/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.rt.event.type.definition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.serotonin.m2m2.db.dao.SystemSettingsDao;
import com.serotonin.m2m2.db.dao.UserDao;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.i18n.Translations;
import com.serotonin.m2m2.module.SystemEventTypeDefinition;
import com.serotonin.m2m2.rt.event.AlarmLevels;
import com.serotonin.m2m2.rt.event.type.SystemEventType;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.event.EventTypeVO;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

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
    public List<EventTypeVO> generatePossibleEventTypesWithReferenceId1(PermissionHolder user, String subtype) {
        if(!StringUtils.equals(SystemEventType.TYPE_USER_LOGIN, subtype))
            return Collections.emptyList();

        AlarmLevels level = AlarmLevels.fromValue(SystemSettingsDao.instance.getIntValue(SystemEventType.SYSTEM_SETTINGS_PREFIX + SystemEventType.TYPE_USER_LOGIN));
        if(user.hasAdminPermission()) {
            List<User> users = UserDao.getInstance().getActiveUsers();
            List<EventTypeVO> types = new ArrayList<>(users.size());
            for(User u : users)
                types.add(new EventTypeVO(new SystemEventType(SystemEventType.TYPE_USER_LOGIN, u.getId()), new TranslatableMessage("event.system.userLoginForUser", u.getName()), level));
            
            return types;            
        }else {
            List<EventTypeVO> types = new ArrayList<>(1);
            User u = UserDao.getInstance().getUser(user.getPermissionHolderId());
            types.add(new EventTypeVO(new SystemEventType(SystemEventType.TYPE_USER_LOGIN, u.getId()), new TranslatableMessage("event.system.userLoginForUser", u.getName()), level));
            return types;
        }

    }

}
