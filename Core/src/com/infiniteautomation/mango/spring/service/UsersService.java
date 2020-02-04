/**
 * Copyright (C) 2018  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;

import javax.mail.internet.AddressException;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import com.google.common.base.Objects;
import com.infiniteautomation.mango.permission.UserRolesDetails;
import com.infiniteautomation.mango.spring.db.UserTableDefinition;
import com.infiniteautomation.mango.spring.service.PasswordService.PasswordInvalidException;
import com.infiniteautomation.mango.util.exception.NotFoundException;
import com.infiniteautomation.mango.util.exception.ValidationException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.RoleDao;
import com.serotonin.m2m2.db.dao.RoleDao.RoleDeletedDaoEvent;
import com.serotonin.m2m2.db.dao.SystemSettingsDao;
import com.serotonin.m2m2.db.dao.UserDao;
import com.serotonin.m2m2.email.MangoEmailContent;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.i18n.Translations;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.module.PermissionDefinition;
import com.serotonin.m2m2.module.definitions.permissions.UserCreatePermission;
import com.serotonin.m2m2.module.definitions.permissions.UserEditSelfPermission;
import com.serotonin.m2m2.rt.maint.work.EmailWorkItem;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.permission.PermissionException;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.m2m2.vo.role.Role;
import com.serotonin.m2m2.vo.role.RoleVO;
import com.serotonin.validation.StringValidation;

import freemarker.template.TemplateException;

/**
 * Service to access Users
 *
 * NOTES:
 *  Users are cached by username
 *
 *  by using any variation of the get(String, user) methods you are returned
 *   a cached user, any modifications to this will result in changes to a session user
 *   to avoid this use the get(Integer, user) variations
 *
 * @author Terry Packer
 *
 */
@Service
public class UsersService extends AbstractVOService<User, UserTableDefinition, UserDao> {

    private final RoleDao roleDao;
    private final SystemSettingsDao systemSettings;
    private final PasswordService passwordService;
    private final PermissionDefinition editSelfPermission;

    @Autowired
    public UsersService(UserDao dao, PermissionService permissionService,
            RoleDao roleDao, SystemSettingsDao systemSettings,
            PasswordService passwordService) {
        super(dao, permissionService);
        this.systemSettings = systemSettings;
        this.passwordService = passwordService;
        this.roleDao = roleDao;
        this.editSelfPermission = ModuleRegistry.getPermissionDefinition(UserEditSelfPermission.PERMISSION);
    }

    @Override
    public Set<Role> getCreatePermissionRoles() {
        return ModuleRegistry.getPermissionDefinition(UserCreatePermission.PERMISSION).getRoles();
    }

    @Override
    @EventListener
    protected void handleRoleDeletedEvent(RoleDeletedDaoEvent event) {
        for(User user : dao.getAll()) {
            if(user.getRoles().contains(event.getRole().getRole())) {
                Set<Role> updated = new HashSet<>(user.getRoles());
                updated.remove(event.getRole().getRole());
                user.setRoles(updated);
                dao.update(dao.get(user.getId()), user);
            }
        }
    }

    /*
     * Nice little hack since Users don't have an XID.
     */
    @Override
    public User get(String username)
            throws NotFoundException, PermissionException {
        User vo = dao.getByXid(username);
        if(vo == null)
            throw new NotFoundException();
        PermissionHolder user = Common.getUser();
        ensureReadPermission(user, vo);
        return vo;
    }

    /**
     *
     * Get a user by their email address
     *
     * @param emailAddress
     * @return
     */
    public User getUserByEmail(String emailAddress) throws NotFoundException, PermissionException {
        User vo =  dao.getUserByEmail(emailAddress);
        if(vo == null)
            throw new NotFoundException();
        ensureReadPermission(Common.getUser(), vo);
        return vo;
    }

    @Override
    public User insert(User vo)
            throws PermissionException, ValidationException {
        PermissionHolder user = Common.getUser();
        //Ensure they can create
        ensureCreatePermission(user, vo);

        //Ensure id is not set
        if(vo.getId() != Common.NEW_ID) {
            ProcessResult result = new ProcessResult();
            result.addContextualMessage("id", "validate.invalidValue");
            throw new ValidationException(result);
        }

        //Generate an Xid if necessary
        if(StringUtils.isEmpty(vo.getXid()))
            vo.setXid(dao.generateUniqueXid());

        ensureValid(vo, user);

        //After validation we can set the created date if necessary
        if(vo.getCreated() == null) {
            vo.setCreated(new Date());
        }

        dao.insert(vo);
        return vo;
    }

    @Override
    public User update(User existing, User vo)
            throws PermissionException, ValidationException {
        PermissionHolder user = Common.getUser();
        ensureEditPermission(user, existing);
        vo.setId(existing.getId());


        //Set the date created, it will be validated later
        if(vo.getCreated() == null) {
            vo.setCreated(existing.getCreated());
        }

        String newPassword = vo.getPassword();
        if (StringUtils.isBlank(newPassword)) {
            // just use the old password
            vo.setPassword(existing.getPassword());
        }

        // set the email verified date to null if the email was changed but the date was not
        Date emailVerified = vo.getEmailVerified();
        if (emailVerified == null || emailVerified.equals(existing.getEmailVerified())) {
            boolean emailChanged = !existing.getEmail().equals(vo.getEmail());
            if (emailChanged) {
                vo.setEmailVerified(null);
            } else {
                vo.setEmailVerified(existing.getEmailVerified());
            }
        }

        ensureValid(existing, vo, Common.getUser());
        dao.update(existing, vo);
        return vo;
    }

    @Override
    public User delete(User vo)
            throws PermissionException, NotFoundException {
        PermissionHolder user = Common.getUser();
        //You cannot delete yourself
        if (user instanceof User && ((User) user).getId() == vo.getId())
            throw new PermissionException(new TranslatableMessage("users.validate.badDelete"), user);

        //Only admin can delete
        user.ensureHasAdminRole();

        dao.delete(vo);
        return vo;
    }

    /**
     * Update the password for a user
     *
     * @param user
     * @param newPassword plain text password
     * @throws ValidationException if password is not valid
     */
    public void updatePassword(User user, String newPassword) throws ValidationException {
        // don't want to change the passed in user in case it comes from the cache (in which case another thread might use it)
        User copy = this.get(user.getId());
        copy.setPlainTextPassword(newPassword);
        ensureValid(user, Common.getUser());
        copy.hashPlainText();

        this.dao.updatePasswordHash(user, copy.getPassword());
    }

    /**
     * Lock a user's password
     * @param username
     * @param user
     * @throws PermissionException
     * @throws NotFoundException
     */
    public void lockPassword(String username)
            throws PermissionException, NotFoundException {
        PermissionHolder user = Common.getUser();
        permissionService.ensureAdminRole(user);
        User toLock = this.get(username);
        if (user instanceof User && ((User) user).getId() == toLock.getId())
            throw new PermissionException(new TranslatableMessage("users.validate.cannotLockOwnPassword"), user);
        dao.lockPassword(toLock);
    }

    /**
     * Get user permission information for all users, will filter
     *  based on what roles the calling user can 'see'
     * @return
     */
    public Set<UserRolesDetails> getPermissionDetailsForAllUsers() {
        return getPermissionDetailsForAllUsers(Collections.emptySet());
    }

    /**
     * Get User Permissions Information for all users, exclude provided roles in query, will filter
     *  based on what roles the calling user can 'see'
     * @param query - cannot be null
     * @return
     */
    public Set<UserRolesDetails> getPermissionDetailsForAllUsers(Collection<String> query) {
        PermissionHolder user = Common.getUser();
        Set<UserRolesDetails> details = new TreeSet<>();
        for (User u : dao.getActiveUsers()){
            UserRolesDetails deets = permissionService.getPermissionDetails(user, query, u);
            if(deets != null)
                details.add(deets);
        }
        return details;
    }

    /**
     * Get all roles that a user can 'see'
     * @return
     */
    public Set<Role> getUserRoles() {
        return getUserRoles(Collections.emptySet());
    }

    /**
     * Get All roles that a user can 'see', exclude any roles listed
     * @param exclude cannot be null
     * @return
     */
    public Set<Role> getUserRoles(Collection<String> exclude) {
        PermissionHolder user = Common.getUser();
        Set<Role> groups = new HashSet<>();
        groups.addAll(user.getRoles());

        if (user.hasAdminRole()) {
            for (RoleVO role : this.roleDao.getAll())
                groups.add(role.getRole());
        }
        for (String part : exclude) {
            groups.removeIf((r) -> {
                if(StringUtils.equals(part, r.getXid())) {
                    return true;
                }else {
                    return false;
                }
            });
        }

        return groups;
    }

    @Override
    public ProcessResult validate(User vo, PermissionHolder holder) {
        ProcessResult result = commonValidation(vo, holder);
        //Must not have a date created set if we are non admin
        if(vo.getCreated() != null && !holder.hasAdminRole()) {
            result.addContextualMessage("created", "validate.invalidValue");
        }

        if(vo.isSessionExpirationOverride()) {
            if(!holder.hasAdminRole()) {
                result.addContextualMessage("sessionExpirationOverride", "permission.exception.mustBeAdmin");
            }else {
                if (-1 == Common.TIME_PERIOD_CODES.getId(vo.getSessionExpirationPeriodType(), Common.TimePeriods.MILLISECONDS)) {
                    result.addContextualMessage("sessionExpirationPeriodType", "validate.invalidValueWithAcceptable", Common.TIME_PERIOD_CODES.getCodeList());
                }
                if(vo.getSessionExpirationPeriods() <= 0) {
                    result.addContextualMessage("sessionExpirationPeriods", "validate.greaterThanZero");
                }
            }
        }

        //Validate roles
        permissionService.validateVoRoles(result, "roles", holder, false, null, vo.getRoles());
        return result;
    }

    @Override
    public ProcessResult validate(User existing, User vo, PermissionHolder holder) {
        ProcessResult result = commonValidation(vo, holder);

        //Must not have a different date created set if we are non admin
        if(vo.getCreated() != null && !holder.hasAdminRole()) {
            if(vo.getCreated().getTime() != existing.getCreated().getTime()) {
                result.addContextualMessage("created", "validate.invalidValue");
            }
        }

        //Validate roles
        boolean savingSelf = false;
        if(holder instanceof User) {
            savingSelf = ((User)holder).getId() == existing.getId();
        }
        permissionService.validateVoRoles(result, "roles", holder, savingSelf, existing.getRoles(), vo.getRoles());

        //Things we cannot do to ourselves
        if (holder instanceof User && ((User) holder).getId() == existing.getId()) {

            //Cannot disable
            if(vo.isDisabled()) {
                result.addContextualMessage("disabled", "users.validate.adminDisable");
            }else {
                //If we are disabled this check will throw an exception, we are invalid anyway so
                // don't check
                //Cannot remove admin permission
                if(existing.hasAdminRole())
                    if(!vo.hasAdminRole())
                        result.addContextualMessage("roles", "users.validate.adminInvalid");
            }
        }

        //Things we cannot do as non-admin
        if (!holder.hasAdminRole()) {
            if (!vo.getRoles().equals(existing.getRoles())) {
                result.addContextualMessage("roles", "users.validate.cannotChangePermissions");
            }
        }

        if(!Objects.equal(vo.getEmailVerified(), existing.getEmailVerified()) && !holder.hasAdminRole()) {
            result.addContextualMessage("emailVerified", "validate.invalidValue");
        }

        if(!Objects.equal(vo.getCreated(), existing.getCreated()) && !holder.hasAdminRole()) {
            result.addContextualMessage("created", "validate.invalidValue");
        }

        if(existing.isSessionExpirationOverride() != vo.isSessionExpirationOverride() && !holder.hasAdminRole()) {
            result.addContextualMessage("sessionExpirationOverride", "permission.exception.mustBeAdmin");
        }

        if(existing.getSessionExpirationPeriods() != vo.getSessionExpirationPeriods() && !holder.hasAdminRole()) {
            result.addContextualMessage("sessionExpirationPeriods", "permission.exception.mustBeAdmin");
        }

        if(!StringUtils.equals(existing.getSessionExpirationPeriodType(), vo.getSessionExpirationPeriodType()) && !holder.hasAdminRole()) {
            result.addContextualMessage("sessionExpirationPeriodType", "permission.exception.mustBeAdmin");
        }

        if (!StringUtils.isBlank(vo.getPassword())) {
            Matcher m = Common.EXTRACT_ALGORITHM_HASH.matcher(vo.getPassword());
            if (m.matches()) {
                String hashOrPassword = m.group(2);
                //Can't use same one 2x
                if(Common.checkPassword(hashOrPassword, existing.getPassword(), false)) {
                    result.addMessage("password", new TranslatableMessage("users.validate.cannotUseSamePasswordTwice"));
                }
            }
        }
        return result;
    }

    protected ProcessResult commonValidation(User vo, PermissionHolder holder) {
        ProcessResult response = new ProcessResult();
        if (StringUtils.isBlank(vo.getUsername()))
            response.addMessage("username", new TranslatableMessage("validate.required"));
        if (!UserDao.getInstance().isUsernameUnique(vo.getUsername(), vo.getId()))
            response.addMessage("username", new TranslatableMessage("users.validate.usernameInUse"));

        if (StringUtils.isBlank(vo.getEmail()))
            response.addMessage("email", new TranslatableMessage("validate.required"));
        else if(!UserDao.getInstance().isEmailUnique(vo.getEmail(), vo.getId()))
            response.addMessage("email", new TranslatableMessage("users.validate.emailUnique"));

        if (StringUtils.isBlank(vo.getPassword())) {
            response.addMessage("password", new TranslatableMessage("validate.required"));
        } else {
            Matcher m = Common.EXTRACT_ALGORITHM_HASH.matcher(vo.getPassword());
            if (!m.matches()) {
                response.addMessage("password", new TranslatableMessage("validate.illegalValue"));
            } else {
                String algorithm = m.group(1);
                String hashOrPassword = m.group(2);

                if ((User.PLAIN_TEXT_ALGORITHM.equals(algorithm) || User.NONE_ALGORITHM.equals(algorithm)) && StringUtils.isBlank(hashOrPassword)) {
                    response.addMessage("password", new TranslatableMessage("validate.required"));
                }

                //Validate against our rules
                if (User.PLAIN_TEXT_ALGORITHM.equals(algorithm) || User.NONE_ALGORITHM.equals(algorithm)){
                    try {
                        passwordService.validatePassword(hashOrPassword);
                    }catch (PasswordInvalidException e) {
                        for(TranslatableMessage message : e.getMessages()) {
                            response.addMessage("password", message);
                        }
                    }
                }
            }
        }

        if (StringUtils.isBlank(vo.getName())) {
            response.addMessage("name", new TranslatableMessage("validate.required"));
        }else if (StringValidation.isLengthGreaterThan(vo.getName(), 255)) {
            response.addMessage("name", new TranslatableMessage("validate.notLongerThan", 255));
        }

        // Check field lengths
        if (StringValidation.isLengthGreaterThan(vo.getUsername(), 40))
            response.addMessage("username", new TranslatableMessage("validate.notLongerThan", 40));
        if (StringValidation.isLengthGreaterThan(vo.getEmail(), 255))
            response.addMessage("email", new TranslatableMessage("validate.notLongerThan", 255));
        if (StringValidation.isLengthGreaterThan(vo.getPhone(), 40))
            response.addMessage("phone", new TranslatableMessage("validate.notLongerThan", 40));


        if(vo.getReceiveAlarmEmails() == null) {
            response.addMessage("receiveAlarmEmails", new TranslatableMessage("validate.required"));
        }

        if(vo.getLocale() == null) {
            response.addMessage("locale", new TranslatableMessage("validate.required"));
        }else if (StringValidation.isLengthGreaterThan(vo.getLocale(), 50)) {
            response.addMessage("locale", new TranslatableMessage("validate.notLongerThan", 50));
        }

        if (StringValidation.isLengthGreaterThan(vo.getTimezone(), 50)) {
            response.addMessage("timezone", new TranslatableMessage("validate.notLongerThan", 50));
        }

        //Can't set email verified
        if(vo.getEmailVerified() != null && !holder.hasAdminRole()) {
            response.addContextualMessage("emailVerified", "validate.invalidValue");
        }

        if(StringUtils.isNotEmpty(vo.getOrganization())) {
            if (StringValidation.isLengthGreaterThan(vo.getOrganization(), 80)) {
                response.addMessage("organization", new TranslatableMessage("validate.notLongerThan", 80));
            }
        }

        if(StringUtils.isNotEmpty(vo.getOrganizationalRole())) {
            if (StringValidation.isLengthGreaterThan(vo.getOrganizationalRole(), 80)) {
                response.addMessage("organizationalRole", new TranslatableMessage("validate.notLongerThan", 80));
            }
        }

        return response;
    }

    @Override
    public boolean hasEditPermission(PermissionHolder holder, User vo) {
        if(holder.hasAdminRole()) {
            return true;
        }else if (holder instanceof User && ((User) holder).getId()  == vo.getId() && permissionService.hasAnyRole(holder, editSelfPermission.getRoles()))
            return true;
        else
            return false;
    }

    @Override
    public boolean hasReadPermission(PermissionHolder user, User vo) {
        if(user.hasAdminRole())
            return true;
        else if (user instanceof User && ((User) user).getId()  == vo.getId())
            return true;
        else
            return false;
    }

    /**
     * @param username
     * @param sendEmail
     * @param user
     * @return
     * @throws IOException
     * @throws TemplateException
     * @throws AddressException
     */
    public User approveUser(String username, boolean sendEmail) throws PermissionException, NotFoundException, TemplateException, IOException, AddressException {
        User existing = this.get(username);
        User approved = (User) existing.copy();
        approved.setDisabled(false);
        update(existing, approved);

        Translations translations = existing.getTranslations();
        Map<String, Object> model = new HashMap<>();
        TranslatableMessage subject = new TranslatableMessage("ftl.userApproved.subject", this.systemSettings.getValue(SystemSettingsDao.INSTANCE_DESCRIPTION));
        MangoEmailContent content = new MangoEmailContent("accountApproved", model, translations, subject.translate(translations), StandardCharsets.UTF_8);
        EmailWorkItem.queueEmail(existing.getEmail(), content);

        return approved;
    }

}
