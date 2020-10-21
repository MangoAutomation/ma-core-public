/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 *
 * @author Terry Packer
 */
package com.serotonin.m2m2.module.definitions.event.handlers;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import com.infiniteautomation.mango.spring.events.DaoEvent;
import com.infiniteautomation.mango.spring.service.EventHandlerService;
import com.infiniteautomation.mango.spring.service.MailingListService;
import com.infiniteautomation.mango.spring.service.MangoJavaScriptService;
import com.infiniteautomation.mango.spring.service.PermissionService;
import com.infiniteautomation.mango.util.script.ScriptPermissions;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.RoleDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.module.EventHandlerDefinition;
import com.serotonin.m2m2.rt.event.handlers.EmailHandlerRT;
import com.serotonin.m2m2.rt.event.handlers.EventHandlerRT;
import com.serotonin.m2m2.rt.script.ScriptError;
import com.serotonin.m2m2.vo.event.EmailEventHandlerVO;
import com.serotonin.m2m2.vo.mailingList.MailingListRecipient;
import com.serotonin.m2m2.vo.mailingList.RecipientListEntryType;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.m2m2.vo.role.Role;
import com.serotonin.m2m2.vo.role.RoleVO;

import freemarker.template.Template;

/**
 * @author Terry Packer
 *
 */
public class EmailEventHandlerDefinition extends EventHandlerDefinition<EmailEventHandlerVO> {

    public static final String TYPE_NAME = "EMAIL";
    public static final String DESC_KEY = "eventHandlers.type.email";
    public static final int EMAIL_SCRIPT_TYPE = 2;

    @Autowired
    EventHandlerService eventHandlerService;
    @Autowired
    RoleDao roleDao;
    @Autowired
    PermissionService service;
    @Autowired
    MailingListService mailingListService;

    @Override
    public String getEventHandlerTypeName() {
        return TYPE_NAME;
    }

    @Override
    public String getDescriptionKey() {
        return DESC_KEY;
    }

    @Override
    protected EmailEventHandlerVO createEventHandlerVO() {
        return new EmailEventHandlerVO();
    }

    @Override
    public void handleRoleEvent(EmailEventHandlerVO vo, DaoEvent<? extends RoleVO> event) {
        //Remove and re-serialize our handler's script roles if it has a role that was deleted
        if(vo.getScriptRoles().getRoles().contains(event.getVo().getRole())){
            switch(event.getType()) {
                case UPDATE:
                    break;
                case DELETE:
                    Set<Role> updated = new HashSet<>(vo.getScriptRoles().getRoles());
                    updated.remove(event.getVo().getRole());
                    Set<Role> allRoles = new HashSet<>();
                    for(Role role : updated) {
                        allRoles.add(role);
                        allRoles.addAll(roleDao.getFlatInheritance(role));
                    }
                    ScriptPermissions permission = new ScriptPermissions(allRoles, vo.getScriptRoles().getPermissionHolderName());
                    vo.setScriptRoles(permission);
                    eventHandlerService.update(vo.getId(), vo);
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void validate(ProcessResult result, EmailEventHandlerVO vo, PermissionHolder savingUser) {
        commonValidation(result, vo, savingUser);
        if(vo.getScriptRoles() != null) {
            service.validatePermissionHolderRoles(result, "scriptRoles", savingUser, null, vo.getScriptRoles().getRoles());
        }else {
            result.addContextualMessage("scriptRoles", "validate.permission.null");
        }
    }

    @Override
    public void validate(ProcessResult result, EmailEventHandlerVO existing,
            EmailEventHandlerVO vo, PermissionHolder savingUser) {
        commonValidation(result, vo, savingUser);
        if (vo.getScriptRoles() == null) {
            result.addContextualMessage("scriptRoles", "validate.permission.null");
        }else {
            Set<Role> roles = existing.getScriptRoles() == null ? null : existing.getScriptRoles().getRoles();
            service.validatePermissionHolderRoles(result, "scriptRoles", savingUser,
                    roles, vo.getScriptRoles().getRoles());
        }
    }

    private void commonValidation(ProcessResult result, EmailEventHandlerVO vo, PermissionHolder savingUser) {
        if(vo.getActiveRecipients() != null) {
            int pos = 0;
            for(MailingListRecipient b : vo.getActiveRecipients()) {
                mailingListService.validateRecipient("activeRecipients[" + pos + "]",
                        b,
                        result,
                        RecipientListEntryType.ADDRESS,
                        RecipientListEntryType.MAILING_LIST,
                        RecipientListEntryType.USER);
                pos++;
            }
        }

        if (vo.isSendEscalation()) {
            if (vo.getEscalationDelay() <= 0)
                result.addContextualMessage("escalationDelay", "eventHandlers.escalDelayError");
            if(!Common.TIME_PERIOD_CODES.isValidId(vo.getEscalationDelayType()))
                result.addContextualMessage("escalationDelayType", "validate.invalidValue");
            if(vo.getEscalationRecipients() != null) {
                int pos = 0;
                for(MailingListRecipient b : vo.getEscalationRecipients()) {
                    mailingListService.validateRecipient("escalationRecipients[" + pos + "]",
                            b,
                            result,
                            RecipientListEntryType.ADDRESS,
                            RecipientListEntryType.MAILING_LIST,
                            RecipientListEntryType.USER);
                    pos++;
                }
            }
        } else if(vo.isRepeatEscalations()) {
            vo.setRepeatEscalations(false);
        }

        if(StringUtils.isNotEmpty(vo.getCustomTemplate())) {
            try {
                new Template("customTemplate", new StringReader(vo.getCustomTemplate()), Common.freemarkerConfiguration);
            }catch(Exception e) {
                result.addContextualMessage("customTemplate", "common.default", e.getMessage());
            }
        }

        if(vo.getAdditionalContext() != null)
            validateScriptContext(vo.getAdditionalContext(), result);
        else {
            vo.setAdditionalContext(new ArrayList<>());
        }

        if(!StringUtils.isEmpty(vo.getScript())) {
            MangoJavaScriptService service = Common.getBean(MangoJavaScriptService.class);
            service.getPermissionService().runAs(vo.getScriptRoles(), () -> {
                try {
                    service.compile(vo.getScript(), true);
                } catch(ScriptError e) {
                    result.addContextualMessage("script", "eventHandlers.invalidActiveScriptError", e.getTranslatableMessage());
                }
            });
        }

        if(!EmailEventHandlerVO.SUBJECT_INCLUDE_CODES.isValidId(vo.getSubject()))
            result.addContextualMessage("subject", "validate.invalidValue");

    }

    @Override
    public EventHandlerRT<EmailEventHandlerVO> createRuntime(EmailEventHandlerVO vo){
        return new EmailHandlerRT(vo);
    }
}
