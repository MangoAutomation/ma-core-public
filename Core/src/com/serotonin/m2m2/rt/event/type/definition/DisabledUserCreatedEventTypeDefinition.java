/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.rt.event.type.definition;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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
public class DisabledUserCreatedEventTypeDefinition extends SystemEventTypeDefinition {

    @Override
    public String getTypeName() {
        return SystemEventType.TYPE_DISABLED_USER_CREATED;
    }

    @Override
    public String getDescriptionKey() {
        return "event.system.disabledUserCreated";
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
        if(!StringUtils.equals(SystemEventType.TYPE_DISABLED_USER_CREATED, subtype))
            return Collections.emptyList();

        AlarmLevels level = AlarmLevels.fromValue(SystemSettingsDao.instance.getIntValue(SystemEventType.SYSTEM_SETTINGS_PREFIX + SystemEventType.TYPE_DISABLED_USER_CREATED));

        List<User> users;
        if (user.hasAdminPermission()) {
            users = UserDao.getInstance().getActiveUsers();
        } else if (user instanceof User) {
            users = Collections.singletonList((User) user);
        } else {
            users = Collections.emptyList();
        }

        return users.stream()
                .map(u -> new EventTypeVO(new SystemEventType(SystemEventType.TYPE_DISABLED_USER_CREATED, u.getId()), new TranslatableMessage("event.system.disabledUserCreatedByUser", u.getName()), level))
                .collect(Collectors.toList());
    }

    
    @Override
    public AlarmLevels getDefaultAlarmLevel() {
        return AlarmLevels.INFORMATION;
    }
}
