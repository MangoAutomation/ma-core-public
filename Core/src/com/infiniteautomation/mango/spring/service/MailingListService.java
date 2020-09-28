/**
 * Copyright (C) 2018  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.infiniteautomation.mango.spring.db.MailingListTableDefinition;
import com.infiniteautomation.mango.spring.events.DaoEvent;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.MailingListDao;
import com.serotonin.m2m2.db.dao.UserDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.module.PermissionDefinition;
import com.serotonin.m2m2.module.definitions.permissions.MailingListCreatePermission;
import com.serotonin.m2m2.rt.event.AlarmLevels;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.mailingList.AddressEntry;
import com.serotonin.m2m2.vo.mailingList.MailingList;
import com.serotonin.m2m2.vo.mailingList.MailingListRecipient;
import com.serotonin.m2m2.vo.mailingList.PhoneEntry;
import com.serotonin.m2m2.vo.mailingList.RecipientListEntryType;
import com.serotonin.m2m2.vo.mailingList.UserEntry;
import com.serotonin.m2m2.vo.mailingList.UserPhoneEntry;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

/**
 * Mailing list service
 *
 * @author Terry Packer
 *
 */
@Service
public class MailingListService extends AbstractVOService<MailingList, MailingListTableDefinition, MailingListDao> {

    private final UserDao userDao;
    private final MailingListCreatePermission createPermission;
    private final Cache<Integer, MailingList> cache;

    @Autowired
    public MailingListService(MailingListDao dao, PermissionService permissionService, UserDao userDao, MailingListCreatePermission createPermission) {
        super(dao, permissionService);
        this.userDao = userDao;
        this.createPermission = createPermission;
        this.cache = Caffeine.newBuilder().build();
        this.dao.getAll().stream().forEach(ml -> this.cache.put(ml.getId(), ml));
    }

    /**
     *  Get any addresses for mailing lists that are mailed on alarm level up to and including 'alarmLevel'
     *
     * @param alarmLevel
     * @param time of gathering addresses used to determine if a list is inactive
     * @param types for types of entries to return
     * @return
     */
    public Set<String> getAlarmAddresses(AlarmLevels alarmLevel, long time, RecipientListEntryType... types) {
        PermissionHolder user = Common.getUser();
        this.permissionService.ensureAdminRole(user);

        List<MailingList> result = new ArrayList<>();

        cache.asMap().forEach((id, ml) -> {
            if(ml.getReceiveAlarmEmails().value() >= 0 && ml.getReceiveAlarmEmails().value() <= alarmLevel.value()) {
                result.add(ml);
            }
        });

        Set<String> addresses = new HashSet<>();
        for(MailingList list : result) {
            addresses.addAll(getActiveRecipients(list.getEntries(), time, types));
        }
        return addresses;
    }

    /**
     * Get a list of all active recipients for the desired types of entries while also
     *  populating the entries of the list.
     *
     * @param recipients
     * @param sendTime
     * @param types
     * @return
     */
    public Set<String> getActiveRecipients(List<MailingListRecipient> recipients, long sendTime, RecipientListEntryType... types){
        PermissionHolder user = Common.getUser();
        this.permissionService.ensureAdminRole(user);

        Set<String> addresses = new HashSet<String>();
        for (MailingListRecipient r : recipients) {
            if(ArrayUtils.contains(types, r.getRecipientType())) {
                switch(r.getRecipientType()) {
                    case ADDRESS:
                        addresses.add(r.getReferenceAddress());
                        break;
                    case MAILING_LIST:
                        //Reload this whole guy as he may have been serialized or changed
                        MailingList list = dao.get(r.getReferenceId());
                        if(list != null) {
                            if(list.getInactiveIntervals().contains(getIntervalIdAt(sendTime))) {
                                Set<String> activeFromList = getActiveRecipients(list.getEntries(), sendTime);
                                addresses.addAll(activeFromList);
                            }
                        }
                        break;
                    case PHONE_NUMBER:
                        addresses.add(r.getReferenceAddress());
                        break;
                    case USER:
                        User u = userDao.get(r.getReferenceId());
                        if(u == null || u.isDisabled()) {
                            break;
                        }else {
                            addresses.add(u.getEmail());
                        }
                        break;
                    case USER_PHONE_NUMBER:
                        User up = userDao.get(r.getReferenceId());
                        if(up == null || up.isDisabled()) {
                            break;
                        }else {
                            addresses.add(up.getPhone());
                        }
                        break;
                    default:
                        break;

                }
            }
        }
        return addresses;
    }

    /**
     * Clean a list of recipients by removing any entries with dead references,
     *  i.e. a user was deleted while this list was serialized in the database
     * @param list
     */
    public void cleanRecipientList(List<MailingListRecipient> list){
        if(list == null)
            return;

        ListIterator<MailingListRecipient> it = list.listIterator();
        while(it.hasNext()) {
            MailingListRecipient recipient = it.next();
            switch(recipient.getRecipientType()){
                case ADDRESS:
                case PHONE_NUMBER:
                    if(StringUtils.isEmpty(recipient.getReferenceAddress())) {
                        it.remove();
                    }
                    break;
                case MAILING_LIST:
                    if(dao.getXidById(recipient.getReferenceId()) == null) {
                        it.remove();
                    }
                    break;
                case USER:
                case USER_PHONE_NUMBER:
                    if(userDao.getXidById(recipient.getReferenceId()) == null) {
                        it.remove();
                    }
                    break;
                default:
                    break;

            }
        }
    }

    @Override
    protected PermissionDefinition getCreatePermission() {
        return createPermission;
    }

    /**
     * Can this user edit this mailing list
     *
     * @param user
     * @param item
     * @return
     */
    @Override
    public boolean hasEditPermission(PermissionHolder user, MailingList item) {
        return permissionService.hasPermission(user, item.getEditPermission());
    }

    /**
     * All users can read mailing lists, however you must have READ permission to view the addresses
     *
     * @param user
     * @param item
     * @return
     */
    @Override
    public boolean hasReadPermission(PermissionHolder user, MailingList item) {
        return permissionService.hasPermission(user, item.getReadPermission());
    }

    /**
     * Can this user view the recipients on this list?
     * @param user
     * @param item
     * @return
     */
    public boolean hasRecipientViewPermission(PermissionHolder user, MailingList item) {
        if(permissionService.hasPermission(user, item.getReadPermission())) {
            return true;
        }else if(permissionService.hasPermission(user, item.getEditPermission())) {
            return true;
        }else {
            return false;
        }
    }

    @Override
    public ProcessResult validate(MailingList vo, PermissionHolder user) {
        ProcessResult result = commonValidation(vo, user);
        permissionService.validateVoRoles(result, "readPermission", user, false, null, vo.getReadPermission());
        permissionService.validateVoRoles(result, "editPermission", user, false, null, vo.getEditPermission());
        return result;
    }

    @Override
    public ProcessResult validate(MailingList existing, MailingList vo, PermissionHolder user) {
        ProcessResult result = commonValidation(vo, user);

        //Additional checks for existing list
        permissionService.validateVoRoles(result, "readPermission", user, false, existing.getReadPermission(), vo.getReadPermission());
        permissionService.validateVoRoles(result, "editPermission", user, false, existing.getEditPermission(), vo.getEditPermission());
        return result;
    }

    /**
     * Common validation logic for insert/update of Mailing lists
     * @param vo
     * @param user
     * @return
     */
    protected ProcessResult commonValidation(MailingList vo, PermissionHolder user) {
        ProcessResult result = super.validate(vo, user);

        if(vo.getReceiveAlarmEmails() == null) {
            result.addContextualMessage("receiveAlarmEmails", "validate.invalidValue");
        }

        if(vo.getEntries() == null || vo.getEntries().size() == 0) {
            result.addContextualMessage("recipients", "mailingLists.validate.entries");
        }else {
            int index = 0;
            for(MailingListRecipient recipient : vo.getEntries()) {
                validateRecipient(vo, "recipients[" + index + "]", recipient, result, RecipientListEntryType.values());
                index++;
            }
        }

        if(vo.getInactiveIntervals() != null) {
            if(vo.getInactiveIntervals().size() > 672)
                result.addContextualMessage("inactiveSchedule", "validate.invalidValue");
        }
        return result;
    }

    /**
     * For internal use to validate a mailing list
     * @param list
     * @param prefix
     * @param recipient
     * @param result
     * @param acceptableTypes
     */
    protected void validateRecipient(MailingList list, String prefix, MailingListRecipient recipient, ProcessResult result, RecipientListEntryType... acceptableTypes) {
        if(!ArrayUtils.contains(acceptableTypes, recipient.getRecipientType())) {
            result.addContextualMessage(prefix + ".recipientType", "mailingLists.validate.invalidEntryType", recipient.getRecipientType(), acceptableTypes);
        }else {
            switch(recipient.getRecipientType()) {
                case ADDRESS:
                    AddressEntry ee = (AddressEntry)recipient;
                    if (StringUtils.isBlank(ee.getAddress())) {
                        result.addContextualMessage(prefix, "validate.required");
                    }
                    break;
                case MAILING_LIST:
                    //If a mailing list then make sure it exists and there are no circular references
                    MailingList sublist = dao.get(recipient.getReferenceId());
                    if(sublist == null) {
                        result.addContextualMessage(prefix, "mailingLists.validate.listDoesNotExist");
                    }else {
                        Set<Integer> listIds = new HashSet<>();
                        if(list != null) {
                            listIds.add(list.getId());
                        }
                        //Check to see if the top level recipient is the same as me
                        if(listIds.add(sublist.getId())) {
                            result.addContextualMessage(prefix, "mailingLists.validate.listCannotContainItself");
                            return;
                        }
                        recursivelyCheckMailingListEntries(listIds, sublist, prefix, result);
                    }
                    break;
                case PHONE_NUMBER:
                    PhoneEntry pe = (PhoneEntry)recipient;
                    if (StringUtils.isBlank(pe.getPhone())) {
                        result.addContextualMessage(prefix, "validate.required");
                    }
                    break;
                case USER:
                    UserEntry ue = (UserEntry)recipient;
                    if(userDao.getXidById(ue.getUserId()) == null) {
                        result.addContextualMessage(prefix, "mailingLists.validate.userDoesNotExist");
                    }
                    break;
                case USER_PHONE_NUMBER:
                    UserPhoneEntry up = (UserPhoneEntry)recipient;
                    User userWithPhone = userDao.get(up.getUserId());
                    if(userWithPhone == null) {
                        result.addContextualMessage(prefix, "mailingLists.validate.userDoesNotExist");
                    }else if(StringUtils.isBlank(userWithPhone.getPhone())){
                        result.addContextualMessage(prefix, "mailingLists.validate.userDoesNotHavePhoneNumber");
                    }
                    break;
                default:
                    break;

            }
        }
    }

    /**
     * Validate a recipient
     * @param prefix - recipients[1] or inactiveRecipients[2]
     * @param recipient
     * @param result
     * @param acceptableTypes - allowed types
     */
    public void validateRecipient(String prefix, MailingListRecipient recipient, ProcessResult result, RecipientListEntryType... acceptableTypes) {
        validateRecipient(null, prefix, recipient, result, acceptableTypes);
    }

    /**
     * @param listIds
     * @param list
     */
    private void recursivelyCheckMailingListEntries(Set<Integer> listIds, MailingList list, String prefix, ProcessResult result) {
        for(MailingListRecipient recipient : list.getEntries()) {
            switch(recipient.getRecipientType()) {
                case MAILING_LIST:
                    if(!listIds.add(recipient.getReferenceId())) {
                        //Failed, we already have this list as reference
                        result.addContextualMessage(prefix, "mailingLists.validate.listCannotContainItself");
                        return;
                    }else {
                        MailingList sublist = dao.get(recipient.getReferenceId());
                        recursivelyCheckMailingListEntries(listIds, sublist, prefix, result);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * Get the interval at this time based on the server timezone
     * @param time
     * @return
     */
    private static int getIntervalIdAt(long time) {
        Instant i = Instant.ofEpochSecond(time);
        ZonedDateTime dt = ZonedDateTime.ofInstant(i, ZoneId.systemDefault());

        int interval = 0;
        interval += dt.getMinute() / 15;
        interval += dt.getHour() * 4;
        interval += (dt.getDayOfWeek().getValue() - 1) * 96;
        return interval;
    }

    /**
     * Keep our cache up to date by evicting changed roles
     * @param event
     */
    @EventListener
    protected void handleMailingListEvent(DaoEvent<? extends MailingList> event) {
        switch(event.getType()) {
            case DELETE:
                this.cache.invalidate(event.getVo().getId());
                break;
            case UPDATE:
            case CREATE:
                this.cache.put(event.getVo().getId(), event.getVo());
                break;
            default:
                break;
        }
    }
}
