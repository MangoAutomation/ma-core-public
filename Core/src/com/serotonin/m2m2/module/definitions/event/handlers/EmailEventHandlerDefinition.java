/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 *
 * @author Terry Packer
 */
package com.serotonin.m2m2.module.definitions.event.handlers;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import com.infiniteautomation.mango.spring.service.MangoJavaScriptService;
import com.infiniteautomation.mango.spring.service.PermissionService;
import com.infiniteautomation.mango.util.script.ScriptPermissions;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.RoleDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.module.EventHandlerDefinition;
import com.serotonin.m2m2.rt.script.ScriptError;
import com.serotonin.m2m2.vo.event.EmailEventHandlerVO;
import com.serotonin.m2m2.vo.mailingList.RecipientListEntryBean;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.m2m2.vo.role.Role;

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
    PermissionService service;

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
    public void saveRelationalData(EmailEventHandlerVO eh, boolean insert) {
        if (eh.getScriptRoles() != null) {
            RoleDao.getInstance().replaceRolesOnVoPermission(eh.getScriptRoles().getRoles(), eh,
                    PermissionService.SCRIPT, insert);
        }
    }

    @Override
    public void loadRelationalData(EmailEventHandlerVO eh) {
        eh.setScriptRoles(new ScriptPermissions(
                RoleDao.getInstance().getRoles(eh, PermissionService.SCRIPT)));
    }

    @Override
    public void deleteRelationalData(EmailEventHandlerVO vo) {
        RoleDao.getInstance().deleteRolesForVoPermission(vo, PermissionService.SCRIPT);
    }

    @Override
    public void validate(ProcessResult result, EmailEventHandlerVO vo, PermissionHolder savingUser) {
        commonValidation(result, vo, savingUser);
        if(vo.getScriptRoles() != null) {
            service.validateVoRoles(result, "scriptRoles", savingUser, false, null, vo.getScriptRoles().getRoles());
        }

    }

    @Override
    public void validate(ProcessResult result, EmailEventHandlerVO existing,
            EmailEventHandlerVO vo, PermissionHolder savingUser) {
        commonValidation(result, vo, savingUser);
        if (vo.getScriptRoles() == null) {
            result.addContextualMessage("scriptRoles", "validate.permission.null");
        }else {
            Set<Role> existingRoles = existing.getScriptRoles() == null ? null : existing.getScriptRoles().getRoles();
            service.validateVoRoles(result, "scriptRoles", savingUser, false,
                    existingRoles, vo.getScriptRoles().getRoles());
        }
    }

    private void commonValidation(ProcessResult result, EmailEventHandlerVO vo, PermissionHolder savingUser) {
        if(vo.getActiveRecipients() != null) {
            int pos = 0;
            for(RecipientListEntryBean b : vo.getActiveRecipients()) {
                validateRecipient("activeRecipients[" + pos + "]", b, result);
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
                for(RecipientListEntryBean b : vo.getEscalationRecipients()) {
                    validateRecipient("escalationRecipients[" + pos + "]", b, result);
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
}
