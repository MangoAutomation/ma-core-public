/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.vo;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.IllformedLocaleException;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTimeZone;
import org.passay.LengthRule;
import org.passay.PasswordData;
import org.passay.PasswordValidator;
import org.passay.RuleResult;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Joiner;
import com.infiniteautomation.mango.util.LazyInitializer;
import com.infiniteautomation.mango.util.datetime.NextTimePeriodAdjuster;
import com.serotonin.ShouldNeverHappenException;
import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.ObjectWriter;
import com.serotonin.json.spi.JsonProperty;
import com.serotonin.json.spi.JsonSerializable;
import com.serotonin.json.type.JsonObject;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.AbstractDao;
import com.serotonin.m2m2.db.dao.SystemSettingsDao;
import com.serotonin.m2m2.db.dao.UserDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.i18n.Translations;
import com.serotonin.m2m2.module.definitions.permissions.SuperadminPermissionDefinition;
import com.serotonin.m2m2.rt.dataImage.SetPointSource;
import com.serotonin.m2m2.rt.event.AlarmLevels;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;
import com.serotonin.m2m2.vo.permission.Permission;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.m2m2.vo.permission.Permissions;
import com.serotonin.m2m2.vo.publish.PublishedPointVO;
import com.serotonin.m2m2.vo.publish.PublisherVO;
import com.serotonin.m2m2.web.dwr.beans.DataExportDefinition;
import com.serotonin.m2m2.web.dwr.beans.EventExportDefinition;
import com.serotonin.m2m2.web.dwr.beans.TestingUtility;
import com.serotonin.m2m2.web.dwr.emport.ImportTask;
import com.serotonin.m2m2.web.mvc.spring.security.authentication.MangoUserDetailsService;
import com.serotonin.validation.StringValidation;

public class User extends AbstractVO<User> implements SetPointSource, JsonSerializable, UserDetails, PermissionHolder {

    public final static String PLAIN_TEXT_ALGORITHM = "PLAINTEXT";
    public final static String NONE_ALGORITHM = "NONE";
    public final static String BCRYPT_ALGORITHM = "BCRYPT";
    public final static String LOCKED_ALGORITHM = "LOCKED";
    public final static String SHA1_ALGORITHM = "SHA-1";

    @JsonProperty
    private String username;
    /**
     * This field actually stores a password hash with the hashing algorithm prefixed.
     * e.g. {BCRYPT}hash
     */
    @JsonProperty
    private String password;
    @JsonProperty
    private String email;
    @JsonProperty
    private String phone;
    @JsonProperty
    private boolean disabled;
    @JsonProperty
    private String homeUrl;
    private long lastLogin;
    //Receive emails for alarm levels >= this
    @JsonProperty
    private AlarmLevels receiveAlarmEmails = AlarmLevels.IGNORE;
    @JsonProperty
    private boolean receiveOwnAuditEvents;
    @JsonProperty
    private String timezone;
    @JsonProperty
    private boolean muted = true;
    //TODO More aptly named roles
    private String permissions = "user"; //Default group
    @JsonProperty
    private String locale;

    private int tokenVersion;
    private int passwordVersion;
    private long passwordChangeTimestamp;
    @JsonProperty
    private boolean sessionExpirationOverride;
    @JsonProperty
    private int sessionExpirationPeriods;
    @JsonProperty
    private String sessionExpirationPeriodType;
    @JsonProperty
    private String organization;
    @JsonProperty
    private String organizationalRole;
    private Date created;
    private Date emailVerified;
    @JsonProperty
    private JsonNode data;

    //
    // Session data. The user object is stored in session, and some other session-based information is cached here
    // for convenience.
    //
    private final transient ConcurrentMap<String, Object> attributes = new ConcurrentHashMap<>();
    private transient DataPointVO editPoint;
    private transient DataSourceVO<?> editDataSource;
    // TODO Mango 3.6 remove
    private transient TestingUtility testingUtility;
    private transient PublisherVO<? extends PublishedPointVO> editPublisher;
    private transient ImportTask importTask;
    private transient DataExportDefinition dataExportDefinition;
    private transient EventExportDefinition eventExportDefinition;
    private transient final LazyInitializer<TimeZone> _tz = new LazyInitializer<>();
    private transient final LazyInitializer<DateTimeZone> _dtz = new LazyInitializer<>();
    private transient final LazyInitializer<Locale> localeObject = new LazyInitializer<>();
    //TODO More aptly named rolesSet
    private transient final LazyInitializer<Set<String>> permissionsSet = new LazyInitializer<>();
    private transient final LazyInitializer<Set<Permission>> grantedPermissions = new LazyInitializer<>();

    private transient volatile boolean admin;

    //
    //Spring Security
    //
    private transient final LazyInitializer<Set<GrantedAuthority>> authorities = new LazyInitializer<>();

    public User() {
        this.name = "";
        this.timezone = "";
        this.locale = "";

        this.tokenVersion = 1;
        this.passwordVersion = 1;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.vo.AbstractVO#getXid()
     */
    @Override
    public String getXid() {
        return "N/A";
    }
    /**
     * Used for various display purposes.
     */
    public String getDescription() {
        return username + " (" + id + ")";
    }

    public boolean isFirstLogin() {
        return lastLogin == 0;
    }

    //
    //
    // SetPointSource implementation
    //
    @Override
    public int getSetPointSourceId() {
        return id;
    }

    @Override
    public String getSetPointSourceType() {
        return "USER";
    }

    @Override
    public TranslatableMessage getSetPointSourceMessage() {
        return new TranslatableMessage("annotation.user", username);
    }

    @Override
    public void raiseRecursionFailureEvent() {
        throw new ShouldNeverHappenException("");
    }

    //
    //
    // Attributes
    //
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    public void removeAttribute(String key) {
        attributes.remove(key);
    }

    public Object getAttribute(String key) {
        return attributes.get(key);
    }

    public <T> T getAttribute(String key, Class<T> requiredClass) {
        return requiredClass.cast(attributes.get(key));
    }

    // Convenience method for JSPs
    public boolean isDataSourcePermission() {
        return Permissions.hasDataSourcePermission(this);
    }

    //
    // Testing utility management
    // TODO Mango 3.6 remove
    public <T extends TestingUtility> T getTestingUtility(Class<T> requiredClass) {
        TestingUtility tu = testingUtility;

        if (tu != null) {
            try {
                return requiredClass.cast(tu);
            }
            catch (ClassCastException e) {
                tu.cancel();
                testingUtility = null;
            }
        }
        return null;
    }

    public void setTestingUtility(TestingUtility testingUtility) {
        TestingUtility tu = this.testingUtility;
        if (tu != null)
            tu.cancel();
        this.testingUtility = testingUtility;
    }

    public void cancelTestingUtility() {
        setTestingUtility(null);
    }

    // Properties

    /**
     * This method should not be used for permission checks, use hasAdminPermission().
     *
     * Why? This method does not check if the user is disabled.
     *
     * @return
     */
    @Deprecated
    public boolean isAdmin() {
        return admin;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    @Override
    @JsonIgnore
    public String getPassword() {
        return password;
    }

    /**
     * If the password field doesn't have an algorithm encoded in it, assume it's SHA-1 (legacy JSON) and update it
     *
     * @return true if the password field was updated, i.e. there was no algorithm in the password field
     */
    public boolean assumeSha1Algorithm() {
        if (this.password != null && !this.password.isEmpty()) {
            Matcher m = Common.EXTRACT_ALGORITHM_HASH.matcher(this.password);
            if (!m.matches()) {
                this.password = "{" + SHA1_ALGORITHM + "}" + this.password;
                return true;
            }
        }
        return false;
    }

    /**
     * Checks the password field, if the algorithm is PLAINTEXT then it will hash it using the default
     * hash algorithm (BCRYPT)
     *
     * @return true if the password field was updated, i.e. the password was plain text
     */
    public boolean hashPlainText() {
        Matcher m = Common.EXTRACT_ALGORITHM_HASH.matcher(this.password);
        if (m.matches()) {
            String algorithm = m.group(1);
            String plainText = m.group(2);

            // if the algorithm is set to PLAINTEXT we are going to hash the password
            if (PLAIN_TEXT_ALGORITHM.equals(algorithm)) {
                this.password = Common.encrypt(plainText);
                return true;
            }
        }
        return false;
    }

    /**
     * Sets a new plaintext password, i.e. prepends it with {PLAINTEXT} so it will be hashed upon saving
     * (after validation).
     *
     * @param password
     */
    public void setPlainTextPassword(String password) {
        this.setPasswordHash(PLAIN_TEXT_ALGORITHM, password);
    }

    /**
     * Sets the password field to the given algorithm and hash. It is saved in the form {HASH_ALGORITHM}passwordHash
     *
     * @param algorithm
     * @param hash
     */
    public void setPasswordHash(String algorithm, String hash) {
        this.password = "{" + algorithm + "}" + hash;
    }

    /**
     * Set the password field, this is actually a password hash, or a plaintext string that will be hashed upon
     * database insertion/update. It takes the form {HASH_ALGORITHM}passwordHash
     *
     * You shouldn't use this directly in most cases (except in tests). Use setPlainTextPassword() or setPasswordHash() instead.
     *
     * @param password
     */
    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public DataPointVO getEditPoint() {
        return editPoint;
    }

    public void setEditPoint(DataPointVO editPoint) {
        this.editPoint = editPoint;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    @Deprecated
    public String getPermissions() {
        return permissions;
    }

    public void setPermissions(String permissions) {
        this.permissions = permissions;
        this.authorities.reset();
        this.permissionsSet.reset();
        this.admin = this.getPermissionsSet().contains(SuperadminPermissionDefinition.GROUP_NAME);
    }

    public DataSourceVO<?> getEditDataSource() {
        return editDataSource;
    }

    public void setEditDataSource(DataSourceVO<?> editDataSource) {
        this.editDataSource = editDataSource;
    }

    public String getHomeUrl() {
        return homeUrl;
    }

    public void setHomeUrl(String homeUrl) {
        this.homeUrl = homeUrl;
    }

    public long getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(long lastLogin) {
        this.lastLogin = lastLogin;
    }

    public PublisherVO<? extends PublishedPointVO> getEditPublisher() {
        return editPublisher;
    }

    public void setEditPublisher(PublisherVO<? extends PublishedPointVO> editPublisher) {
        this.editPublisher = editPublisher;
    }

    public ImportTask getImportTask() {
        return importTask;
    }

    public void setImportTask(ImportTask importTask) {
        this.importTask = importTask;
    }

    public boolean isMuted() {
        return muted;
    }

    public void setMuted(boolean muted) {
        this.muted = muted;
    }

    public AlarmLevels getReceiveAlarmEmails() {
        return receiveAlarmEmails;
    }

    public void setReceiveAlarmEmails(AlarmLevels receiveAlarmEmails) {
        this.receiveAlarmEmails = receiveAlarmEmails;
    }

    public boolean isReceiveOwnAuditEvents() {
        return receiveOwnAuditEvents;
    }

    public void setReceiveOwnAuditEvents(boolean receiveOwnAuditEvents) {
        this.receiveOwnAuditEvents = receiveOwnAuditEvents;
    }

    public DataExportDefinition getDataExportDefinition() {
        return dataExportDefinition;
    }

    public void setDataExportDefinition(DataExportDefinition dataExportDefinition) {
        this.dataExportDefinition = dataExportDefinition;
    }

    public EventExportDefinition getEventExportDefinition() {
        return eventExportDefinition;
    }

    public void setEventExportDefinition(EventExportDefinition eventExportDefinition) {
        this.eventExportDefinition = eventExportDefinition;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
        this._tz.reset();
        this._dtz.reset();
    }

    public TimeZone getTimeZoneInstance() {
        return this._tz.get(() -> {
            TimeZone tz = null;
            if (!StringUtils.isEmpty(timezone))
                tz = TimeZone.getTimeZone(timezone);
            if (tz == null)
                tz = TimeZone.getDefault();
            return tz;
        });
    }

    public DateTimeZone getDateTimeZoneInstance() {
        return this._dtz.get(() -> {
            DateTimeZone dtz = null;
            if (!StringUtils.isEmpty(timezone))
                dtz = DateTimeZone.forID(timezone);
            if (dtz == null)
                dtz = DateTimeZone.getDefault();
            return dtz;
        });
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
        this.localeObject.reset();
    }

    public void setPasswordChangeTimestamp(long timestamp){
        this.passwordChangeTimestamp = timestamp;
    }

    public long getPasswordChangeTimestamp() {
        return this.passwordChangeTimestamp;
    }

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }

    public String getOrganizationalRole() {
        return organizationalRole;
    }

    public void setOrganizationalRole(String organizationalRole) {
        this.organizationalRole = organizationalRole;
    }

    public Date getCreated() {
        return created;
    }

    public Long getCreatedTs() {
        return created == null ? null : created.getTime();
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public Date getEmailVerified() {
        return emailVerified;
    }

    public Long getEmailVerifiedTs() {
        return emailVerified == null ? null : emailVerified.getTime();
    }

    public void setEmailVerified(Date emailVerified) {
        this.emailVerified = emailVerified;
    }

    public JsonNode getData() {
        return data;
    }

    public void setData(JsonNode data) {
        this.data = data;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return this.authorities.get(() -> {
            return Collections.unmodifiableSet(MangoUserDetailsService.getGrantedAuthorities(permissions));
        });
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; //Don't have this feature
    }

    @Override
    public boolean isAccountNonLocked() {
        return true; //Don't have this feature
    }

    @Override
    public boolean isCredentialsNonExpired() {
        if(SystemSettingsDao.instance.getBooleanValue(SystemSettingsDao.PASSWORD_EXPIRATION_ENABLED)) {
            int resetPeriodType = SystemSettingsDao.instance.getIntValue(SystemSettingsDao.PASSWORD_EXPIRATION_PERIOD_TYPE);
            int resetPeriods = SystemSettingsDao.instance.getIntValue(SystemSettingsDao.PASSWORD_EXPIRATION_PERIODS);
            NextTimePeriodAdjuster adjuster = new NextTimePeriodAdjuster(resetPeriodType, resetPeriods);
            ZoneId zoneId = this.getTimeZoneInstance().toZoneId();
            ZonedDateTime lastChange = ZonedDateTime.ofInstant(Instant.ofEpochMilli(this.passwordChangeTimestamp), zoneId);
            ZonedDateTime now = ZonedDateTime.ofInstant(Instant.ofEpochMilli(Common.timer.currentTimeMillis()), zoneId);
            ZonedDateTime nextChange = (ZonedDateTime) adjuster.adjustInto(lastChange);
            if(nextChange.isAfter(now))
                return true;
            return false;
        }
        return true;
    }

    @Override
    public boolean isEnabled() {
        return !this.disabled;
    }

    public boolean isSessionExpirationOverride() {
        return sessionExpirationOverride;
    }

    public void setSessionExpirationOverride(boolean sessionExpirationOverride) {
        this.sessionExpirationOverride = sessionExpirationOverride;
    }

    public int getSessionExpirationPeriods() {
        return sessionExpirationPeriods;
    }

    public void setSessionExpirationPeriods(int sessionExpirationPeriods) {
        this.sessionExpirationPeriods = sessionExpirationPeriods;
    }

    public String getSessionExpirationPeriodType() {
        return sessionExpirationPeriodType;
    }

    public void setSessionExpirationPeriodType(String sessionExpirationPeriodType) {
        this.sessionExpirationPeriodType = sessionExpirationPeriodType;
    }

    public Set<Permission> getGrantedPermissions() {
        return grantedPermissions.get(() -> {
            return Collections.unmodifiableSet(Permissions.getGrantedPermissions(this));
        });
    }

    public void resetGrantedPermissions() {
        this.grantedPermissions.reset();
    }

    @Override
    public void validate(ProcessResult response) {
        //TODO Pass in saving user during validation
        User savingUser = Common.getHttpUser();
        if(savingUser == null)
            savingUser = Common.getBackgroundContextUser();

        //get the existing user if there is one (don't use username as it can change)
        User existing;
        if(id != Common.NEW_ID) {
            existing = UserDao.getInstance().get(id);
        }else {
            existing = null;
        }

        if (StringUtils.isBlank(username))
            response.addMessage("username", new TranslatableMessage("validate.required"));
        else if(!UserDao.getInstance().isUsernameUnique(username, id))
            response.addMessage("username", new TranslatableMessage("users.validate.usernameInUse"));

        if (StringUtils.isBlank(email))
            response.addMessage("email", new TranslatableMessage("validate.required"));
        else if(!UserDao.getInstance().isEmailUnique(email, id))
            response.addMessage("email", new TranslatableMessage("users.validate.emailUnique"));

        if (StringUtils.isBlank(password)) {
            response.addMessage("password", new TranslatableMessage("validate.required"));
        } else {
            Matcher m = Common.EXTRACT_ALGORITHM_HASH.matcher(password);
            if (!m.matches()) {
                response.addMessage("password", new TranslatableMessage("validate.illegalValue"));
            } else {
                String algorithm = m.group(1);
                String hashOrPassword = m.group(2);

                if ((PLAIN_TEXT_ALGORITHM.equals(algorithm) || NONE_ALGORITHM.equals(algorithm)) && StringUtils.isBlank(hashOrPassword)) {
                    response.addMessage("password", new TranslatableMessage("validate.required"));
                }

                //Validate against our rules
                if (PLAIN_TEXT_ALGORITHM.equals(algorithm) || NONE_ALGORITHM.equals(algorithm)){

                    //Can't use same one 2x
                    if(existing != null && Common.checkPassword(hashOrPassword, existing.getPassword(), false))
                        response.addMessage("password", new TranslatableMessage("users.validate.cannotUseSamePasswordTwice"));

                    PasswordValidator validator = new PasswordValidator(Arrays.asList(
                            new LengthRule(8, 255)));
                    RuleResult result = validator.validate(new PasswordData(hashOrPassword));
                    if(!result.isValid()) {
                        response.addContextualMessage("password", "common.default", Joiner.on(",").join(validator.getMessages(result)));
                    }
                }
            }
        }

        if (StringUtils.isBlank(name))
            response.addMessage("name", new TranslatableMessage("validate.required"));

        // Check field lengths
        if (StringValidation.isLengthGreaterThan(username, 40))
            response.addMessage("username", new TranslatableMessage("validate.notLongerThan", 40));
        if (StringValidation.isLengthGreaterThan(email, 255))
            response.addMessage("email", new TranslatableMessage("validate.notLongerThan", 255));
        if (StringValidation.isLengthGreaterThan(phone, 40))
            response.addMessage("phone", new TranslatableMessage("validate.notLongerThan", 40));
        if (StringValidation.isLengthGreaterThan(name, 255))
            response.addMessage("name", new TranslatableMessage("validate.notLongerThan", 255));

        if(receiveAlarmEmails == null) {
            response.addMessage("receiveAlarmEmails", new TranslatableMessage("validate.required"));
        }

        if(locale == null) {
            response.addMessage("locale", new TranslatableMessage("validate.required"));
        }else if (StringValidation.isLengthGreaterThan(locale, 50)) {
            response.addMessage("locale", new TranslatableMessage("validate.notLongerThan", 50));
        }

        if (StringValidation.isLengthGreaterThan(timezone, 50)) {
            response.addMessage("timezone", new TranslatableMessage("validate.notLongerThan", 50));
        }

        //Validate Permissions (Can't be blank)
        if (!StringUtils.isEmpty(this.permissions)) {
            for (String s : this.permissions.split(",")) {
                if(StringUtils.isBlank(s)){
                    response.addMessage("permissions", new TranslatableMessage("validate.cannotContainEmptyString"));
                    break;
                }
            }
        }

        if(existing != null) {
            if(existing.isSessionExpirationOverride() != sessionExpirationOverride && (savingUser == null || !savingUser.hasAdminPermission())) {
                response.addContextualMessage("sessionExpirationOverride", "permission.exception.mustBeAdmin");
            }

            if(existing.getSessionExpirationPeriods() != sessionExpirationPeriods && (savingUser == null || !savingUser.hasAdminPermission())) {
                response.addContextualMessage("sessionExpirationPeriods", "permission.exception.mustBeAdmin");
            }

            if(!StringUtils.equals(existing.getSessionExpirationPeriodType(), sessionExpirationPeriodType) && (savingUser == null || !savingUser.hasAdminPermission())) {
                response.addContextualMessage("sessionExpirationPeriodType", "permission.exception.mustBeAdmin");
            }
        }else {
            if(sessionExpirationOverride) {
                if(savingUser == null || !savingUser.hasAdminPermission()) {
                    response.addContextualMessage("sessionExpirationOverride", "permission.exception.mustBeAdmin");
                }
            }
        }
        
        if(sessionExpirationOverride) {
            if (-1 == Common.TIME_PERIOD_CODES.getId(sessionExpirationPeriodType, Common.TimePeriods.MILLISECONDS))
                response.addContextualMessage("sessionExpirationPeriodType", "validate.invalidValueWithAcceptable", Common.TIME_PERIOD_CODES.getCodeList());
            if(sessionExpirationPeriods <= 0)
                response.addContextualMessage("sessionExpirationPeriods", "validate.greaterThanZero");
        }

        if(StringUtils.isNotEmpty(organization)) {
            if (StringValidation.isLengthGreaterThan(organization, 80)) {
                response.addMessage("organization", new TranslatableMessage("validate.notLongerThan", 80));
            }
        }

        if(StringUtils.isNotEmpty(organizationalRole)) {
            if (StringValidation.isLengthGreaterThan(organizationalRole, 80)) {
                response.addMessage("organizationalRole", new TranslatableMessage("validate.notLongerThan", 80));
            }
        }
    }

    @Override
    public String toString() {
        return "User [id=" + id + ", username=" + username + ", password=<redacted>" + ", email=" + email + ", phone="
                + phone + ", disabled=" + disabled + ", homeUrl=" + homeUrl + ", lastLogin="
                + lastLogin + ", receiveAlarmEmails=" + receiveAlarmEmails + ", receiveOwnAuditEvents="
                + receiveOwnAuditEvents + ", timezone=" + timezone + ", name=" + name + ", locale=" + locale + ", permissions=" + permissions + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + id;
        return result;
    }

    /**
     * User is considered to be equal if its id is equal.
     * This implementation is relied upon by MangoSessionRegistry
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final User other = (User) obj;
        if (id != other.id)
            return false;
        return true;
    }

    @Override
    public void jsonWrite(ObjectWriter writer) throws IOException,
    JsonException {
        writer.writeEntry("name", name);
        writer.writeEntry("permissions", permissions);
    }

    @Override
    public void jsonRead(JsonReader reader, JsonObject jsonObject) throws JsonException {
        name = jsonObject.getString("name");
        if(name == null)
            name = username;
        String text = jsonObject.getString("permissions");
        if(text != null)
            setPermissions(text);
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.vo.AbstractVO#getDao()
     */
    @Override
    protected AbstractDao<User> getDao() {
        return UserDao.getInstance();
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.vo.AbstractVO#getTypeKey()
     */
    @Override
    public String getTypeKey() {
        return "event.audit.user";
    }

    public Locale getLocaleObject() {
        return this.localeObject.get(() -> {
            if (locale == null || locale.isEmpty()) {
                return Common.getLocale();
            }

            try {
                return new Locale.Builder().setLanguageTag(locale).build();
            } catch (IllformedLocaleException e) {
                return Common.getLocale();
            }
        });
    }

    /**
     * Get the translations for a User's locale
     * @return
     */
    public Translations getTranslations(){
        return Translations.getTranslations(getLocaleObject());
    }

    /**
     *
     */
    private static final long serialVersionUID = -1L;

    public int getTokenVersion() {
        return tokenVersion;
    }

    public void setTokenVersion(int tokenVersion) {
        this.tokenVersion = tokenVersion;
    }

    public int getPasswordVersion() {
        return passwordVersion;
    }

    public void setPasswordVersion(int passwordVersion) {
        this.passwordVersion = passwordVersion;
    }

    public boolean isPasswordLocked() {
        return this.password != null && this.password.startsWith(UserDao.LOCKED_PASSWORD);
    }

    /**
     * @param obj
     * @return true if the import/export fields are all equal
     */
    public boolean equalsImportExport(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        User other = (User) obj;
        if (disabled != other.disabled)
            return false;
        if (email == null) {
            if (other.email != null)
                return false;
        } else if (!email.equals(other.email))
            return false;
        if (homeUrl == null) {
            if (other.homeUrl != null)
                return false;
        } else if (!homeUrl.equals(other.homeUrl))
            return false;
        if (locale == null) {
            if (other.locale != null)
                return false;
        } else if (!locale.equals(other.locale))
            return false;
        if (muted != other.muted)
            return false;
        if (password == null) {
            if (other.password != null)
                return false;
        } else if (!password.equals(other.password))
            return false;
        if (passwordVersion != other.passwordVersion)
            return false;
        if (permissions == null) {
            if (other.permissions != null)
                return false;
        } else if (!permissions.equals(other.permissions))
            return false;
        if (phone == null) {
            if (other.phone != null)
                return false;
        } else if (!phone.equals(other.phone))
            return false;
        if (receiveAlarmEmails != other.receiveAlarmEmails)
            return false;
        if (receiveOwnAuditEvents != other.receiveOwnAuditEvents)
            return false;
        if (timezone == null) {
            if (other.timezone != null)
                return false;
        } else if (!timezone.equals(other.timezone))
            return false;
        if (username == null) {
            if (other.username != null)
                return false;
        } else if (!username.equals(other.username))
            return false;
        return true;
    }

    @Override
    public Set<String> getPermissionsSet() {
        return permissionsSet.get(() -> {
            HashSet<String> groups = new HashSet<>(Permissions.explodePermissionGroups(this.permissions));
            groups.add(Permissions.USER_DEFAULT);
            return Collections.unmodifiableSet(groups);
        });
    }

    public void setPermissionsSet(Set<String> permissionsSet) {
        HashSet<String> groups = new HashSet<>(permissionsSet);
        groups.remove(Permissions.USER_DEFAULT);
        this.setPermissions(Permissions.implodePermissionGroups(groups));
    }

    @Override
    public String getPermissionHolderName() {
        return this.username;
    }

    @Override
    public boolean isPermissionHolderDisabled() {
        return this.disabled;
    }

}
