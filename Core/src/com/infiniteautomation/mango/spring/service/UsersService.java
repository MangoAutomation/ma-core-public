/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.spring.service;

import freemarker.template.TemplateException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IllformedLocaleException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import javax.mail.internet.AddressException;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.infiniteautomation.mango.spring.events.DaoEvent;
import com.infiniteautomation.mango.spring.events.DaoEventType;
import com.infiniteautomation.mango.spring.service.PasswordService.PasswordInvalidException;
import com.infiniteautomation.mango.util.exception.NotFoundException;
import com.infiniteautomation.mango.util.exception.ValidationException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.DatabaseProxy;
import com.serotonin.m2m2.db.dao.SystemSettingsDao;
import com.serotonin.m2m2.db.dao.UserDao;
import com.serotonin.m2m2.email.MangoEmailContent;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.i18n.Translations;
import com.serotonin.m2m2.module.PermissionDefinition;
import com.serotonin.m2m2.module.definitions.permissions.ChangeOwnUsernamePermissionDefinition;
import com.serotonin.m2m2.module.definitions.permissions.UserCreatePermission;
import com.serotonin.m2m2.module.definitions.permissions.UserEditSelfPermission;
import com.serotonin.m2m2.rt.maint.work.EmailWorkItem;
import com.serotonin.m2m2.vo.LinkedAccount;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.permission.PermissionException;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.m2m2.vo.role.Role;
import com.serotonin.m2m2.vo.role.RoleVO;
import com.serotonin.validation.StringValidation;

import static com.infiniteautomation.mango.spring.events.DaoEventType.UPDATE;

/**
 * Service to access Users
 * <p>
 * NOTES:
 * Users are cached by username based on the env property cache.users.enabled
 * <p>
 * by using any variation of the get(String, user) methods you are potentially returned
 * a cached user, any modifications to this will result in changes to a session user
 * to avoid this use the get(Integer, user) variations
 *
 * @author Terry Packer
 */
@SuppressWarnings("SpringEventListenerInspection")
@Service
public class UsersService extends AbstractVOService<User, UserDao> implements CachingService {

    private final SystemSettingsDao systemSettings;
    private final PasswordService passwordService;
    private final PermissionDefinition editSelfPermission;
    private final PermissionDefinition changeOwnUsernamePermission;
    private final PermissionDefinition createPermission;
    private final ApplicationEventPublisher eventPublisher;
    private final Cache<String, User> userByUsername;

    /**
     * Create the UsersService
     *
     * @param databaseProxy - added as a dependency to ensure the database is fully created before initializing the cache
     *                         as the admin user is created in the PostConstruct method of that bean
     */
    @Autowired
    public UsersService(UserDao dao,
                        ServiceDependencies dependencies,
                        SystemSettingsDao systemSettings,
                        PasswordService passwordService,
                        @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") UserEditSelfPermission editSelfPermission,
                        @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") ChangeOwnUsernamePermissionDefinition changeOwnUsernamePermission,
                        @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") UserCreatePermission createPermission,
                        ApplicationEventPublisher eventPublisher,
                        DatabaseProxy databaseProxy) {
        super(dao, dependencies);
        this.systemSettings = systemSettings;
        this.passwordService = passwordService;
        this.editSelfPermission = editSelfPermission;
        this.changeOwnUsernamePermission = changeOwnUsernamePermission;
        this.createPermission = createPermission;
        this.eventPublisher = eventPublisher;
        if(env.getProperty("cache.users.enabled", Boolean.class, true)) {
            this.userByUsername = Caffeine.newBuilder().build();
            populateUserByUsernameCache();
        }else {
            this.userByUsername = null;
        }

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

            // TODO Mango 4.0 this is only weakly consistent, such that
            //  changes to the map during iteration may not be reflected
            //  in the user here
            if(userByUsername != null) {
                userByUsername.asMap().forEach((username, user) -> {
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

                        // publish the event using the same user for originalVo, we aren't changing the XID
                        // so it shouldn't matter
                        DaoEvent<User> userUpdatedEvent = new DaoEvent<>(this.dao, UPDATE, user, user);
                        this.eventPublisher.publishEvent(userUpdatedEvent);
                    }
                });
            }
        }
    }

    @EventListener
    protected void userUpdated(DaoEvent<? extends User> event) {
        // if this is a user update event from the handleRoleEvent method, ignore it, our cache is up to date
        if (event.getType() == DaoEventType.UPDATE && event.getVo() == event.getOriginalVo()) {
            return;
        }

        if(userByUsername != null) {
            if (event.getType() == DaoEventType.UPDATE) {
                String originalUsername = event.getOriginalVo().getUsername().toLowerCase(Locale.ROOT);
                String username = event.getVo().getUsername().toLowerCase(Locale.ROOT);
                if (!username.equals(originalUsername)) {
                    this.userByUsername.invalidate(originalUsername);
                }
                this.userByUsername.put(username, event.getVo());
            } else if (event.getType() == DaoEventType.DELETE) {
                String originalUsername = event.getVo().getUsername().toLowerCase(Locale.ROOT);
                this.userByUsername.invalidate(originalUsername);
            } else if (event.getType() == DaoEventType.CREATE) {
                String username = event.getVo().getUsername().toLowerCase(Locale.ROOT);
                this.userByUsername.put(username, event.getVo());
            }
        }
    }

    /*
     * Nice little hack since Users don't have an XID.
     */
    @Override
    public User get(String username) throws NotFoundException, PermissionException {
        Assert.notNull(username, "Username required");
        if(this.userByUsername != null) {
            String usernameLower = username.toLowerCase(Locale.ROOT);
            User vo = userByUsername.get(usernameLower, dao::getByXid);
            if (vo == null) {
                throw new NotFoundException();
            }
            PermissionHolder currentUser = Common.getUser();
            ensureReadPermission(currentUser, vo);
            return vo;
        }else{
            return super.get(username);
        }
    }

    /**
     * Get a user by their email address
     *
     * @param emailAddress the email address of the user to find
     * @return the user corresponding to the email address
     * @throws NotFoundException if the email address was not found in the database
     * @throws PermissionException if the current user does not have permission to read this user
     */
    public User getUserByEmail(String emailAddress) throws NotFoundException, PermissionException {
        User vo = dao.getUserByEmail(emailAddress);
        if (vo == null)
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
        if (vo.getId() != Common.NEW_ID) {
            ProcessResult result = new ProcessResult();
            result.addContextualMessage("id", "validate.invalidValue");
            throw new ValidationException(result);
        }

        //Generate a username if necessary
        if (StringUtils.isEmpty(vo.getUsername()))
            vo.setUsername(dao.generateUniqueXid());

        ensureValid(vo);

        //After validation we can set the created date if necessary
        if (vo.getCreated() == null) {
            vo.setCreated(new Date());
        }

        //After validation we can set password changed date
        vo.setPasswordChangeTimestamp(Common.timer.currentTimeMillis());

        dao.insert(vo);
        return vo;
    }

    @Override
    protected User update(User existing, User vo) throws PermissionException, ValidationException {
        PermissionHolder currentUser = Common.getUser();
        ensureEditPermission(currentUser, existing);
        vo.setId(existing.getId());

        //Set the date created, it will be validated later
        if (vo.getCreated() == null) {
            vo.setCreated(existing.getCreated());
        }

        String newPassword = vo.getPassword();
        if (StringUtils.isBlank(newPassword)) {
            // just use the old password
            vo.setPassword(existing.getPassword());
        }

        // set the email verified date to null if the email was changed but the date was not
        Date emailVerified = vo.getEmailVerifiedDate();
        if (emailVerified == null || emailVerified.equals(existing.getEmailVerifiedDate())) {
            boolean emailChanged = !existing.getEmail().equals(vo.getEmail());
            if (emailChanged) {
                vo.setEmailVerifiedDate(null);
            } else {
                vo.setEmailVerifiedDate(existing.getEmailVerifiedDate());
            }
        }

        ensureValid(existing, vo);
        dao.update(existing, vo);
        return vo;
    }

    @Override
    protected User delete(User vo) throws PermissionException, NotFoundException {
        PermissionHolder currentUser = Common.getUser();
        ensureEditPermission(currentUser, vo);

        //You cannot delete yourself
        if (currentUser.getUser() != null && currentUser.getUser().getId() == vo.getId())
            throw new PermissionException(new TranslatableMessage("users.validate.badDelete"), currentUser);

        dao.delete(vo);
        return vo;
    }

    /**
     * Update the password for a user
     *
     * @param user        user to update password for
     * @param newPassword plain text password
     * @throws ValidationException if password is not valid
     * @throws PermissionException if the current user does not have permission to edit this user
     */
    public void updatePassword(User user, String newPassword) throws PermissionException, ValidationException {
        ensureEditPermission(Common.getUser(), user);

        // don't want to change the passed in user in case it comes from the cache (in which case another thread might use it)
        User copy = this.get(user.getId());
        copy.setPlainTextPassword(newPassword);
        ensureValid(user, copy);
        copy.hashPlainText();

        this.dao.updatePasswordHash(user, copy.getPassword());
    }

    /**
     * Lock a user's password
     *
     * @param username the username of the user to lock
     * @throws NotFoundException if the username was not found in the database
     * @throws PermissionException if the current user does not have permission to edit this user
     */
    public void lockPassword(String username) throws PermissionException, NotFoundException {
        User toLock = this.get(username);
        PermissionHolder currentUser = Common.getUser();
        ensureEditPermission(currentUser, toLock);

        if (currentUser.getUser() != null && currentUser.getUser().getId() == toLock.getId())
            throw new PermissionException(new TranslatableMessage("users.validate.cannotLockOwnPassword"), currentUser);
        dao.lockPassword(toLock);
    }

    @Override
    public ProcessResult validate(User vo) {
        PermissionHolder holder = Common.getUser();
        ProcessResult result = commonValidation(vo, holder);

        if (vo.isSessionExpirationOverride()) {
            if (-1 == Common.TIME_PERIOD_CODES.getId(vo.getSessionExpirationPeriodType(), Common.TimePeriods.MILLISECONDS)) {
                result.addContextualMessage("sessionExpirationPeriodType", "validate.invalidValueWithAcceptable", Common.TIME_PERIOD_CODES.getCodeList());
            }
            if (vo.getSessionExpirationPeriods() <= 0) {
                result.addContextualMessage("sessionExpirationPeriods", "validate.greaterThanZero");
            }
        }

        // validate roles
        permissionService.validatePermissionHolderRoles(result, "roles", holder,
                vo.getRoles());

        // validate permissions
        permissionService.validatePermission(result, "readPermission", holder,
                vo.getReadPermission());
        permissionService.validatePermission(result, "editPermission", holder,
                vo.getEditPermission());

        return result;
    }

    @Override
    public ProcessResult validate(User existing, User vo) {
        PermissionHolder holder = Common.getUser();
        ProcessResult result = commonValidation(vo, holder);

        boolean hasExplicitEditPermission = hasExplicitEditPermission(holder, existing);

        // validate roles
        permissionService.validatePermissionHolderRoles(result, "roles", holder,
                vo.getRoles());

        // validate permissions
        if (!existing.getReadPermission().equals(vo.getReadPermission())) {
            if (!hasExplicitEditPermission) {
                result.addContextualMessage("readPermission", "validate.mustHaveExplicitEditPermission");
            }
            permissionService.validatePermission(result, "readPermission", holder,
                    existing.getReadPermission(), vo.getReadPermission());
        }
        if (!existing.getEditPermission().equals(vo.getEditPermission())) {
            if (!hasExplicitEditPermission) {
                result.addContextualMessage("editPermission", "validate.mustHaveExplicitEditPermission");
            }
            permissionService.validatePermission(result, "editPermission", holder,
                    existing.getEditPermission(), vo.getEditPermission());
        }

        if (!StringUtils.isBlank(vo.getPassword())) {
            Matcher m = Common.EXTRACT_ALGORITHM_HASH.matcher(vo.getPassword());
            if (m.matches()) {
                String hashOrPassword = m.group(2);
                //Can't use same one 2x
                if (Common.checkPassword(hashOrPassword, existing.getPassword(), false)) {
                    result.addMessage("password", new TranslatableMessage("users.validate.cannotUseSamePasswordTwice"));
                }
            }
        }

        // Validation for when the user is modifying themselves
        if (isSelf(holder, existing)) {
            // A user can never disable themselves
            if (vo.isDisabled()) {
                result.addContextualMessage("disabled", "users.validate.adminDisable");
            }

            // cannot remove any role from ourselves (unless superadmin)
            // checking for added roles is done via validatePermissionHolderRoles() above
            Set<Role> heldRoles = holder.getRoles();
            Set<Role> newRoles = vo.getRoles();
            if (heldRoles.contains(PermissionHolder.SUPERADMIN_ROLE)) {
                // cannot remove superadmin from self
                if (!newRoles.contains(PermissionHolder.SUPERADMIN_ROLE)) {
                    result.addContextualMessage("roles", "users.validate.cannotRemoveSuperadminRole");
                }
            } else {
                if (!newRoles.containsAll(heldRoles)) {
                    result.addContextualMessage("roles", "validate.role.modifyOwnRoles");
                }
            }

            // only allow changing own username if they have the "permissions.user.changeUsername" permission
            if (!StringUtils.equals(existing.getUsername(), vo.getUsername())) {
                if (!permissionService.hasPermission(holder, changeOwnUsernamePermission.getPermission())) {
                    result.addMessage("username", new TranslatableMessage("users.validate.cannotChangeOwnUsername"));
                }
            }

            // validate fields that you can only modify if you have explicit edit permission
            if (!hasExplicitEditPermission) {
                if (!Objects.equals(vo.getEmailVerifiedDate(), existing.getEmailVerifiedDate())) {
                    result.addContextualMessage("emailVerified", "validate.invalidValue");
                }

                if (!Objects.equals(vo.getCreated(), existing.getCreated())) {
                    result.addContextualMessage("created", "validate.invalidValue");
                }

                if (existing.isSessionExpirationOverride() != vo.isSessionExpirationOverride()) {
                    result.addContextualMessage("sessionExpirationOverride", "permission.exception.mustBeAdmin");
                }

                if (existing.getSessionExpirationPeriods() != vo.getSessionExpirationPeriods()) {
                    result.addContextualMessage("sessionExpirationPeriods", "permission.exception.mustBeAdmin");
                }

                if (!StringUtils.equals(existing.getSessionExpirationPeriodType(), vo.getSessionExpirationPeriodType())) {
                    result.addContextualMessage("sessionExpirationPeriodType", "permission.exception.mustBeAdmin");
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
        else if (!UserDao.getInstance().isEmailUnique(vo.getEmail(), vo.getId()))
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
        } else if (StringValidation.isLengthGreaterThan(vo.getName(), 255)) {
            response.addMessage("name", new TranslatableMessage("validate.notLongerThan", 255));
        }

        // Check field lengths
        if (StringValidation.isLengthGreaterThan(vo.getUsername(), 40))
            response.addMessage("username", new TranslatableMessage("validate.notLongerThan", 40));
        if (StringValidation.isLengthGreaterThan(vo.getEmail(), 255))
            response.addMessage("email", new TranslatableMessage("validate.notLongerThan", 255));
        if (StringValidation.isLengthGreaterThan(vo.getPhone(), 40))
            response.addMessage("phone", new TranslatableMessage("validate.notLongerThan", 40));


        if (vo.getReceiveAlarmEmails() == null) {
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
                //noinspection ResultOfMethodCallIgnored
                ZoneId.of(timezone);
            } catch (DateTimeException e) {
                response.addMessage("timezone", new TranslatableMessage("validate.invalidValue"));
            }
        }

        //Can't set email verified
        if (vo.getEmailVerifiedDate() != null && !permissionService.hasAdminRole(holder)) {
            response.addContextualMessage("emailVerified", "validate.invalidValue");
        }

        if (StringUtils.isNotEmpty(vo.getOrganization())) {
            if (StringValidation.isLengthGreaterThan(vo.getOrganization(), 80)) {
                response.addMessage("organization", new TranslatableMessage("validate.notLongerThan", 80));
            }
        }

        if (StringUtils.isNotEmpty(vo.getOrganizationalRole())) {
            if (StringValidation.isLengthGreaterThan(vo.getOrganizationalRole(), 80)) {
                response.addMessage("organizationalRole", new TranslatableMessage("validate.notLongerThan", 80));
            }
        }

        // Every user must have the user role, must be directly assigned otherwise if role inheritance changes the user may lose the role
        if (vo.getRoles() != null && !vo.getRoles().contains(PermissionHolder.USER_ROLE)) {
            Set<Role> updated = new HashSet<>(vo.getRoles());
            updated.add(PermissionHolder.USER_ROLE);
            vo.setRoles(Collections.unmodifiableSet(updated));
        }

        return response;
    }

    public boolean isSelf(PermissionHolder holder, User vo) {
        return holder.getUser() != null && holder.getUser().getId() == vo.getId();
    }

    public boolean canEditSelf(PermissionHolder holder) {
        return permissionService.hasPermission(holder, editSelfPermission.getPermission());
    }

    public boolean hasExplicitEditPermission(PermissionHolder holder, User vo) {
        return permissionService.hasPermission(holder, vo.getEditPermission());
    }

    public boolean hasExplicitReadPermission(PermissionHolder holder, User vo) {
        return permissionService.hasPermission(holder, vo.getReadPermission());
    }

    @Override
    public boolean hasEditPermission(PermissionHolder holder, User vo) {
        return isSelf(holder, vo) && canEditSelf(holder) ||
                hasExplicitEditPermission(holder, vo);
    }

    @Override
    public boolean hasReadPermission(PermissionHolder user, User vo) {
        return isSelf(user, vo) || hasExplicitReadPermission(user, vo);
    }

    @Override
    public boolean hasDeletePermission(PermissionHolder user, User vo) {
        return !isSelf(user, vo) && hasExplicitEditPermission(user, vo);
    }

    /**
     * @param username username of user to approve
     * @param sendEmail send email to user notifying them
     * @return the approved user
     * @throws NotFoundException if the username was not found in the database
     * @throws PermissionException if the current user does not have permission to edit this user
     * @throws TemplateException if there was an error in the email template
     * @throws IOException error generating email content
     * @throws AddressException email address was invalid
     */
    public User approveUser(String username, boolean sendEmail) throws PermissionException, NotFoundException, TemplateException, IOException, AddressException {
        User existing = this.get(username);
        User approved = (User) existing.copy();
        approved.setDisabled(false);
        update(existing, approved);

        if (sendEmail) {
            Translations translations = existing.getTranslations();
            Map<String, Object> model = new HashMap<>();
            TranslatableMessage subject = new TranslatableMessage("ftl.userApproved.subject", this.systemSettings.getValue(SystemSettingsDao.INSTANCE_DESCRIPTION));
            MangoEmailContent content = new MangoEmailContent("accountApproved", model, translations, subject.translate(translations), StandardCharsets.UTF_8);
            EmailWorkItem.queueEmail(existing.getEmail(), content);
        }

        return approved;
    }

    @Override
    public void clearCaches() {
        permissionService.ensureAdminRole(Common.getUser());
        if(userByUsername != null) {
            //This cache must always retain all users in it
            userByUsername.invalidateAll();
            populateUserByUsernameCache();
        }
    }

    private void populateUserByUsernameCache() {
        this.dao.getAll().forEach(u -> this.userByUsername.put(u.getUsername().toLowerCase(Locale.ROOT), u));
    }

    public User getByIdViaCache(int id) {
        return dao.doInTransaction((tx) -> {
            String username = dao.getXidById(id);
            if (username == null) {
                throw new NotFoundException();
            }
            User user = get(username);
            if (user.getId() != id) {
                throw new IllegalStateException("User was updated while retrieving from cache");
            }
            return user;
        });
    }

    public Optional<User> getUserForLinkedAccount(LinkedAccount linkedAccount) {
        Optional<User> optional = dao.getUserForLinkedAccount(linkedAccount);
        optional.ifPresent(u -> ensureReadPermission(Common.getUser(), u));
        return optional;
    }

    public void insertUserForLinkedAccount(User user, LinkedAccount linkedAccount) {
        ensureCreatePermission(Common.getUser(), user);
        dao.doInTransaction(tx -> {
            insert(user);
            dao.linkAccount(user.getId(), linkedAccount);
        });
    }

    public void linkAccount(int userId, LinkedAccount linkedAccount) {
        permissionService.ensureAdminRole(Common.getUser());
        dao.linkAccount(userId, linkedAccount);
    }

    public void updateLinkedAccounts(int userId, Iterable<? extends LinkedAccount> accounts) {
        permissionService.ensureAdminRole(Common.getUser());
        dao.updateLinkedAccounts(userId, accounts);
    }

    public List<LinkedAccount> getLinkedAccounts(User user) {
        ensureReadPermission(Common.getUser(), user);
        return dao.getLinkedAccounts(user.getId());
    }

    /**
     * Get all enabled users
     *  - Superadmin access only
     */
    public List<User> getEnabledUsers() {
        permissionService.ensureAdminRole(Common.getUser());
        if(userByUsername != null) {
            List<User> result = new ArrayList<>();

            //TODO This is only weakly consistent in that the underlying
            // map's changes may not be reflected while iterating
            // This method should be removed when we modify the EventManager
            //  to be able to cache users for notification
            userByUsername.asMap().forEach((username, u) -> {
                if (u.isEnabled()) {
                    result.add(u);
                }
            });
            return result;
        }else {
            return dao.getEnabledUsers();
        }
    }
}
