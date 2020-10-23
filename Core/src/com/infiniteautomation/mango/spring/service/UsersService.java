/**
 * Copyright (C) 2018  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IllformedLocaleException;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;

import javax.mail.internet.AddressException;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.infiniteautomation.mango.spring.db.UserTableDefinition;
import com.infiniteautomation.mango.spring.events.DaoEvent;
import com.infiniteautomation.mango.spring.events.DaoEventType;
import com.infiniteautomation.mango.spring.service.PasswordService.PasswordInvalidException;
import com.infiniteautomation.mango.util.exception.NotFoundException;
import com.infiniteautomation.mango.util.exception.ValidationException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.SystemSettingsDao;
import com.serotonin.m2m2.db.dao.UserDao;
import com.serotonin.m2m2.email.MangoEmailContent;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.i18n.Translations;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.module.PermissionDefinition;
import com.serotonin.m2m2.module.definitions.permissions.ChangeOwnUsernamePermissionDefinition;
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

import static com.infiniteautomation.mango.spring.events.DaoEventType.UPDATE;

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
public class UsersService extends AbstractVOService<User, UserTableDefinition, UserDao> implements CachingService {

    private final SystemSettingsDao systemSettings;
    private final PasswordService passwordService;
    private final PermissionDefinition editSelfPermission;
    private final PermissionDefinition changeOwnUsernamePermission;
    private final UserCreatePermission createPermission;
    private final ApplicationEventPublisher eventPublisher;
    private final LoadingCache<String, User> userByUsername;

    @Autowired
    public UsersService(UserDao dao, PermissionService permissionService,
            SystemSettingsDao systemSettings,
            PasswordService passwordService,
            UserCreatePermission createPermission,
            ApplicationEventPublisher eventPublisher) {
        super(dao, permissionService);
        this.systemSettings = systemSettings;
        this.passwordService = passwordService;
        this.editSelfPermission = ModuleRegistry.getPermissionDefinition(UserEditSelfPermission.PERMISSION);
        this.changeOwnUsernamePermission = ModuleRegistry.getPermissionDefinition(ChangeOwnUsernamePermissionDefinition.PERMISSION);
        this.createPermission = createPermission;
        this.eventPublisher = eventPublisher;

        this.userByUsername = Caffeine.newBuilder()
                .maximumSize(Common.envProps.getLong("cache.users.size", 1000))
                .build(this.dao::getByXid);
    }

    @Override
    protected PermissionDefinition getCreatePermission() {
        return createPermission;
    }

    @EventListener
    protected void handleRoleEvent(DaoEvent<? extends RoleVO> event) {
        if (event.getType() == DaoEventType.DELETE || event.getType() == DaoEventType.UPDATE) {
            Role originalRole = event.getType() == DaoEventType.UPDATE ?
                    event.getOriginalVo().getRole() :
                        event.getVo().getRole();

                    // TODO Mango 4.0 this is only weakly consistent
                    for (User user : this.userByUsername.asMap().values()) {
                        if (user.getRoles().contains(originalRole)) {
                            Set<Role> updatedRoles = new HashSet<>(user.getRoles());
                            if (event.getType() == DaoEventType.DELETE) {
                                //Remove this role
                                updatedRoles.remove(originalRole);
                            } else if (event.getType() == DaoEventType.UPDATE) {
                                //Replace this role
                                updatedRoles.remove(originalRole);
                                updatedRoles.add(event.getVo().getRole());
                            }
                            user.setRoles(Collections.unmodifiableSet(updatedRoles));
                            publishUserUpdated(user);
                        }
                    }
        }
    }

    @EventListener
    protected void userUpdated(DaoEvent<? extends User> event) {
        if (event.getType() == DaoEventType.UPDATE) {
            String originalUsername = event.getOriginalVo().getUsername().toLowerCase(Locale.ROOT);
            String username = event.getVo().getUsername().toLowerCase(Locale.ROOT);
            this.userByUsername.invalidate(originalUsername);
            this.userByUsername.put(username, event.getVo());
        } else if (event.getType() == DaoEventType.DELETE) {
            String originalUsername = event.getVo().getUsername().toLowerCase(Locale.ROOT);
            this.userByUsername.invalidate(originalUsername);
        }
    }

    private void publishUserUpdated(User user) {
        User existing = dao.get(user.getId());
        if (existing == null) {
            // this is a temporary work around for when user is deleted while iterating cache entries
            User fromCache = this.userByUsername.getIfPresent(user.getUsername());
            if (fromCache != null) {
                throw new IllegalStateException("User '" + user.getUsername() + "' was found in the user cache, but is not present in the database");
            } else {
                return;
            }
        }

        // Notify WebSockets and invalidate cache via listener userUpdated()
        DaoEvent<User> userUpdatedEvent = new DaoEvent<>(this.dao, UPDATE, user, existing);
        this.eventPublisher.publishEvent(userUpdatedEvent);
    }

    /*
     * Nice little hack since Users don't have an XID.
     */
    @Override
    public User get(String username) throws NotFoundException, PermissionException {
        Assert.notNull(username, "Username required");

        User vo = userByUsername.get(username.toLowerCase(Locale.ROOT));
        if(vo == null)
            throw new NotFoundException();

        PermissionHolder currentUser = Common.getUser();
        ensureReadPermission(currentUser, vo);
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

        PermissionHolder currentUser = Common.getUser();
        ensureReadPermission(currentUser, vo);
        return vo;
    }

    @Override
    public User insert(User vo) throws PermissionException, ValidationException {
        PermissionHolder currentUser = Common.getUser();
        //Ensure they can create
        ensureCreatePermission(currentUser, vo);

        //Ensure id is not set
        if(vo.getId() != Common.NEW_ID) {
            ProcessResult result = new ProcessResult();
            result.addContextualMessage("id", "validate.invalidValue");
            throw new ValidationException(result);
        }

        //Generate an Xid if necessary
        if(StringUtils.isEmpty(vo.getXid()))
            vo.setXid(dao.generateUniqueXid());

        ensureValid(vo, currentUser);

        //After validation we can set the created date if necessary
        if(vo.getCreated() == null) {
            vo.setCreated(new Date());
        }

        //After validation we can set password changed date
        vo.setPasswordChangeTimestamp(Common.timer.currentTimeMillis());

        dao.insert(vo);
        return vo;
    }

    @Override
    public User update(User existing, User vo) throws PermissionException, ValidationException {
        PermissionHolder currentUser = Common.getUser();
        ensureEditPermission(currentUser, existing);
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

        ensureValid(existing, vo, currentUser);
        dao.update(existing, vo);
        return vo;
    }

    @Override
    public User delete(User vo) throws PermissionException, NotFoundException {
        PermissionHolder currentUser = Common.getUser();
        ensureEditPermission(currentUser, vo);

        //You cannot delete yourself
        if (currentUser instanceof User && ((User) currentUser).getId() == vo.getId())
            throw new PermissionException(new TranslatableMessage("users.validate.badDelete"), currentUser);

        dao.delete(vo);
        return vo;
    }

    /**
     * Update the password for a user
     *
     * @param user user to update password for
     * @param newPassword plain text password
     * @throws ValidationException if password is not valid
     * @throws PermissionException if the current user does not have permission to edit this user
     */
    public void updatePassword(User user, String newPassword) throws PermissionException, ValidationException {
        ensureEditPermission(Common.getUser(), user);

        // don't want to change the passed in user in case it comes from the cache (in which case another thread might use it)
        User copy = this.get(user.getId());
        copy.setPlainTextPassword(newPassword);
        ensureValid(user, copy, Common.getUser());
        copy.hashPlainText();

        this.dao.updatePasswordHash(user, copy.getPassword());
    }

    /**
     * Lock a user's password
     * @param username
     * @throws PermissionException
     * @throws NotFoundException
     */
    public void lockPassword(String username) throws PermissionException, NotFoundException {
        User toLock = this.get(username);
        PermissionHolder currentUser = Common.getUser();
        ensureEditPermission(currentUser, toLock);

        if (currentUser instanceof User && ((User) currentUser).getId() == toLock.getId())
            throw new PermissionException(new TranslatableMessage("users.validate.cannotLockOwnPassword"), currentUser);
        dao.lockPassword(toLock);
    }

    @Override
    public ProcessResult validate(User vo, PermissionHolder holder) {
        ProcessResult result = commonValidation(vo, holder);
        //Must not have a date created set if we are non admin
        if(vo.getCreated() != null && !permissionService.hasAdminRole(holder)) {
            result.addContextualMessage("created", "validate.invalidValue");
        }

        if(vo.isSessionExpirationOverride()) {
            if(!permissionService.hasAdminRole(holder)) {
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
        permissionService.validatePermissionHolderRoles(result, "roles", holder, null, vo.getRoles());
        return result;
    }

    @Override
    public ProcessResult validate(User existing, User vo, PermissionHolder holder) {
        ProcessResult result = commonValidation(vo, holder);

        //Must not have a different date created set if we are non admin
        if(vo.getCreated() != null && !permissionService.hasAdminRole(holder)) {
            if(vo.getCreated().getTime() != existing.getCreated().getTime()) {
                result.addContextualMessage("created", "validate.invalidValue");
            }
        }

        //Validate roles
        boolean savingSelf = holder instanceof User && ((User)holder).getId() == existing.getId();
        permissionService.validatePermissionHolderRoles(result, "roles", holder, existing.getRoles(), vo.getRoles());

        //Things we cannot do to ourselves
        if (savingSelf) {
            //Cannot disable
            if (vo.isDisabled()) {
                result.addContextualMessage("disabled", "users.validate.adminDisable");
            }

            Set<Role> heldRoles = holder.getRoles();
            if (!heldRoles.contains(PermissionHolder.SUPERADMIN_ROLE)) {
                //Cannot add role you don't have
                Set<Role> inherited = permissionService.getAllInheritedRoles(holder);
                if (!inherited.containsAll(vo.getRoles())) {
                    result.addContextualMessage("roles", "validate.role.invalidModification", PermissionService.implodeRoles(inherited));
                }
                //Cannot change roles as non superadmin
                // TODO Mango 4.0
                // Should we allow users to add explicit roles to themselves that they currently inherit?
                // Can they remove an explicit role from themselves if they still inherit it?

                if (!Objects.equals(existing.getRoles(), vo.getRoles())) {
                    result.addContextualMessage("roles", "validate.role.modifyOwnRoles");
                }
            }else {
                //Cannot remove superadmin from ourself
                if(!vo.getRoles().contains(PermissionHolder.SUPERADMIN_ROLE)) {
                    result.addContextualMessage("roles","users.validate.cannotRemoveSuperadminRole");
                }
            }
        }

        if(!Objects.equals(vo.getEmailVerified(), existing.getEmailVerified()) && !permissionService.hasAdminRole(holder)) {
            result.addContextualMessage("emailVerified", "validate.invalidValue");
        }

        if(!Objects.equals(vo.getCreated(), existing.getCreated()) && !permissionService.hasAdminRole(holder)) {
            result.addContextualMessage("created", "validate.invalidValue");
        }

        if(existing.isSessionExpirationOverride() != vo.isSessionExpirationOverride() && !permissionService.hasAdminRole(holder)) {
            result.addContextualMessage("sessionExpirationOverride", "permission.exception.mustBeAdmin");
        }

        if(existing.getSessionExpirationPeriods() != vo.getSessionExpirationPeriods() && !permissionService.hasAdminRole(holder)) {
            result.addContextualMessage("sessionExpirationPeriods", "permission.exception.mustBeAdmin");
        }

        if(!StringUtils.equals(existing.getSessionExpirationPeriodType(), vo.getSessionExpirationPeriodType()) && !permissionService.hasAdminRole(holder)) {
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

        //Ensure they can change the username if they try
        if(!StringUtils.equals(existing.getUsername(), vo.getUsername())) {
            if(!permissionService.hasPermission(holder, changeOwnUsernamePermission.getPermission())) {
                result.addMessage("username", new TranslatableMessage("users.validate.cannotChangeOwnUsername"));
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

                //Validate against our rules
                if (User.PLAIN_TEXT_ALGORITHM.equals(algorithm) || User.NONE_ALGORITHM.equals(algorithm)) {
                    if (StringUtils.isBlank(hashOrPassword)) {
                        response.addMessage("password", new TranslatableMessage("validate.required"));
                    }

                    try {
                        passwordService.validatePassword(hashOrPassword);
                    } catch (PasswordInvalidException e) {
                        for (TranslatableMessage message : e.getMessages()) {
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

        String locale = vo.getLocale();
        if (StringUtils.isNotEmpty(locale)) {
            if (StringValidation.isLengthGreaterThan(locale, 50)) {
                response.addMessage("locale", new TranslatableMessage("validate.notLongerThan", 50));
            }

            try {
                new Locale.Builder().setLanguageTag(locale).build();
            } catch (IllformedLocaleException e) {
                response.addMessage("locale", new TranslatableMessage("validate.invalidValue"));
            }
        }

        String timezone = vo.getTimezone();
        if (StringUtils.isNotEmpty(vo.getTimezone())) {
            if (StringValidation.isLengthGreaterThan(vo.getTimezone(), 50)) {
                response.addMessage("timezone", new TranslatableMessage("validate.notLongerThan", 50));
            }

            try {
                ZoneId.of(timezone);
            } catch (DateTimeException  e) {
                response.addMessage("timezone", new TranslatableMessage("validate.invalidValue"));
            }
        }

        //Can't set email verified
        if(vo.getEmailVerified() != null && !permissionService.hasAdminRole(holder)) {
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
        if (permissionService.hasAdminRole(holder)) {
            return true;
        }

        return holder instanceof User && ((User) holder).getId() == vo.getId() &&
                permissionService.hasPermission(holder, editSelfPermission.getPermission());
    }

    @Override
    public boolean hasReadPermission(PermissionHolder user, User vo) {
        if (permissionService.hasAdminRole(user)) {
            return true;
        }

        return user instanceof User && ((User) user).getId() == vo.getId();
    }

    /**
     * @param username
     * @param sendEmail
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

    @Override
    public void clearCaches() {
        PermissionHolder currentUser = Common.getUser();
        permissionService.ensureAdminRole(currentUser);
        userByUsername.invalidateAll();
    }

}
