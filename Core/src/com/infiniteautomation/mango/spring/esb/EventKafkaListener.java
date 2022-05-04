/*
 * Copyright (C) 2022 Radix IoT LLC. All rights reserved.
 *
 *
 */

package com.infiniteautomation.mango.spring.esb;

import java.util.HashMap;
import java.util.List;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import com.infiniteautomation.mango.spring.service.MailingListService;
import com.infiniteautomation.mango.spring.service.PermissionService;
import com.infiniteautomation.mango.spring.service.UsersService;
import com.radixiot.pi.grpc.MangoEventRaised;
import com.radixiot.pi.grpc.MangoEventRtn;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.event.AlarmLevels;
import com.serotonin.m2m2.rt.event.EventInstance;
import com.serotonin.m2m2.rt.event.UserEventListener;
import com.serotonin.m2m2.rt.event.UserEventMulticaster;
import com.serotonin.m2m2.rt.event.type.SystemEventType;

@Component
public class EventKafkaListener {

    private final UsersService usersService;
    private final PermissionService permissionService;
    private final MailingListService mailingListService;

    private UserEventListener userEventMulticaster = null;

    public EventKafkaListener(UsersService usersService, PermissionService permissionService, MailingListService mailingListService) {
        this.usersService = usersService;
        this.permissionService = permissionService;
        this.mailingListService = mailingListService;
    }

    @KafkaListener(topics = MangoEventsRaisedTopic.TOPIC, groupId = "mango")
    public void eventRaisedListener(Message<MangoEventRaised> message) {
        List<Integer> userIdsToNotify = List.of(1);
        UserEventListener multicaster = userEventMulticaster;

        MangoEventRaised event = message.getPayload();
        EventInstance evt = new EventInstance(new SystemEventType(SystemEventType.TYPE_SYSTEM_STARTUP), event.getActiveTimestamp(), false,
                AlarmLevels.fromValue(event.getAlarmLevel().getNumber()), new TranslatableMessage("literal", event.getMessage()), new HashMap<>());

        if(multicaster != null ) {
            multicaster.raised(evt);
        }
    }

    @KafkaListener(topics = MangoEventsRtnTopic.TOPIC, groupId = "mango")
    public void eventRtnListener(Message<MangoEventRtn> message) {
        UserEventListener multicaster = userEventMulticaster;
        MangoEventRtn event = message.getPayload();
        EventInstance evt = new EventInstance(new SystemEventType(SystemEventType.TYPE_SYSTEM_STARTUP), event.getActiveTimestamp(), false,
                AlarmLevels.fromValue(event.getAlarmLevel().getNumber()), new TranslatableMessage("literal", event.getMessage()), new HashMap<>());
        if(multicaster != null ) {
            multicaster.returnToNormal(evt);
        }
    }

    public synchronized void addUserEventListener(UserEventListener l) {
        userEventMulticaster = UserEventMulticaster.add(userEventMulticaster, l);
    }

    public synchronized void removeUserEventListener(UserEventListener l) {
        userEventMulticaster = UserEventMulticaster.remove(userEventMulticaster, l);
    }

    public UserEventListener getUserEventMulticaster() {
        return userEventMulticaster;
    }

}
