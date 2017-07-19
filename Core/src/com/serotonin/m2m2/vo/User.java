/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.vo;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.IllformedLocaleException;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTimeZone;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.serotonin.ShouldNeverHappenException;
import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.ObjectWriter;
import com.serotonin.json.spi.JsonProperty;
import com.serotonin.json.spi.JsonSerializable;
import com.serotonin.json.type.JsonObject;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.AbstractDao;
import com.serotonin.m2m2.db.dao.UserDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.i18n.Translations;
import com.serotonin.m2m2.module.definitions.permissions.SuperadminPermissionDefinition;
import com.serotonin.m2m2.rt.dataImage.SetPointSource;
import com.serotonin.m2m2.rt.event.AlarmLevels;
import com.serotonin.m2m2.rt.event.type.SystemEventType;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;
import com.serotonin.m2m2.vo.permission.Permissions;
import com.serotonin.m2m2.vo.publish.PublishedPointVO;
import com.serotonin.m2m2.vo.publish.PublisherVO;
import com.serotonin.m2m2.web.dwr.beans.DataExportDefinition;
import com.serotonin.m2m2.web.dwr.beans.EventExportDefinition;
import com.serotonin.m2m2.web.dwr.beans.TestingUtility;
import com.serotonin.m2m2.web.dwr.emport.ImportTask;
import com.serotonin.validation.StringValidation;

public class User extends AbstractVO<User> implements SetPointSource, HttpSessionBindingListener, JsonSerializable, UserDetails {
	
	@JsonProperty
    private String username;
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
    private int receiveAlarmEmails = AlarmLevels.IGNORE;
    @JsonProperty
    private boolean receiveOwnAuditEvents;
    @JsonProperty
    private String timezone;
    @JsonProperty
    private boolean muted = true;
    private String permissions = "user"; //Default group
    @JsonProperty
    private String locale;

    //
    // Session data. The user object is stored in session, and some other session-based information is cached here
    // for convenience.
    //
    private transient Map<String, Object> attributes = new HashMap<>();
    private transient DataPointVO editPoint;
    private transient DataSourceVO<?> editDataSource;
    private transient TestingUtility testingUtility;
    private transient PublisherVO<? extends PublishedPointVO> editPublisher;
    private transient ImportTask importTask;
    private transient DataExportDefinition dataExportDefinition;
    private transient EventExportDefinition eventExportDefinition;
    private transient TimeZone _tz;
    private transient DateTimeZone _dtz;
    private transient String remoteAddr; //remote address we are logged in from
    private boolean admin;
    //
    //Spring Security
    //
    private final Set<GrantedAuthority> authorities;
    
    public User() {
        this.name = "";
        this.timezone = "";
        this.locale = "";
        this.authorities = null;
    }
    
    public User(Set<GrantedAuthority> authorities){
        this.authorities = Collections.unmodifiableSet(authorities);
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

    public void remoteAttribute(String key) {
        attributes.remove(key);
    }

    public Object getAttribute(String key) {
        return attributes.get(key);
    }

    public <T> T getAttribute(String key, Class<T> requiredClass) {
        return requiredClass.cast(attributes.get(key));
    }

    //
    //
    // HttpSessionBindingListener implementation
    //
    @Override
    public void valueBound(HttpSessionBindingEvent evt) {
        // User is bound to a session when logged in. Notify the event manager.
        SystemEventType.raiseEvent(new SystemEventType(SystemEventType.TYPE_USER_LOGIN, id),
                Common.timer.currentTimeMillis(), true, new TranslatableMessage("event.login", username, remoteAddr));
    }

    @Override
    public void valueUnbound(HttpSessionBindingEvent evt) {
        // User is unbound from a session when logged out or the session expires.
        SystemEventType.returnToNormal(new SystemEventType(SystemEventType.TYPE_USER_LOGIN, id),
                Common.timer.currentTimeMillis());

        // Terminate any testing utility
        if (testingUtility != null)
            testingUtility.cancel();
    }

    // Convenience method for JSPs
    public boolean isDataSourcePermission() {
        return Permissions.hasDataSourcePermission(this);
    }

    //
    // Testing utility management
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

    @JsonIgnore
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

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

    public String getPermissions() {
        return permissions;
    }

    public void setPermissions(String permissions) {
        this.permissions = permissions;
        //Set the admin flag if necessary
        this.admin = Permissions.permissionContains(SuperadminPermissionDefinition.GROUP_NAME, permissions);
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

    public int getReceiveAlarmEmails() {
        return receiveAlarmEmails;
    }

    public void setReceiveAlarmEmails(int receiveAlarmEmails) {
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
        _tz = null;
        _dtz = null;
    }

    public TimeZone getTimeZoneInstance() {
        if (_tz == null) {
            if (!StringUtils.isEmpty(timezone))
                _tz = TimeZone.getTimeZone(timezone);
            if (_tz == null)
                _tz = TimeZone.getDefault();
        }
        return _tz;
    }

    public DateTimeZone getDateTimeZoneInstance() {
        if (_dtz == null) {
            if (!StringUtils.isEmpty(timezone))
                _dtz = DateTimeZone.forID(timezone);
            if (_dtz == null)
                _dtz = DateTimeZone.forID(TimeZone.getDefault().getID());
        }
        return _dtz;
    }
    
    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    /**
     * @return the ipAddress
     */
    public String getRemoteAddr() {
        return remoteAddr;
    }

    /**
     * @param ipAddress
     *            the ipAddress to set
     */
    public void setRemoteAddr(String ipAddress) {
        this.remoteAddr = ipAddress;
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.security.core.userdetails.UserDetails#getAuthorities()
     */
    @Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
    	return this.authorities;
    }
    
	/* (non-Javadoc)
	 * @see org.springframework.security.core.userdetails.UserDetails#isAccountNonExpired()
	 */
	@Override
	public boolean isAccountNonExpired() {
		return true; //Don't have this feature
	}

	/* (non-Javadoc)
	 * @see org.springframework.security.core.userdetails.UserDetails#isAccountNonLocked()
	 */
	@Override
	public boolean isAccountNonLocked() {
		return true; //Don't have this feature
	}

	/* (non-Javadoc)
	 * @see org.springframework.security.core.userdetails.UserDetails#isCredentialsNonExpired()
	 */
	@Override
	public boolean isCredentialsNonExpired() {
		return true; //Don't have this feature
	}
	
	/* (non-Javadoc)
	 * @see org.springframework.security.core.userdetails.UserDetails#isEnabled()
	 */
	@Override
	public boolean isEnabled() {
		return !this.disabled;
	}
    
    public void validate(ProcessResult response) {
        if (StringUtils.isBlank(username))
            response.addMessage("username", new TranslatableMessage("validate.required"));
        if (StringUtils.isBlank(email))
            response.addMessage("email", new TranslatableMessage("validate.required"));
        if (id == Common.NEW_ID && StringUtils.isBlank(password))
            response.addMessage("password", new TranslatableMessage("validate.required"));
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
        if (StringValidation.isLengthGreaterThan(locale, 50))
            response.addMessage("locale", new TranslatableMessage("validate.notLongerThan", 50));
        if (StringValidation.isLengthGreaterThan(timezone, 50))
            response.addMessage("timezone", new TranslatableMessage("validate.notLongerThan", 50));
        
        //Validate Permissions (Can't be blank)
        if (!StringUtils.isEmpty(this.permissions)) {
        	for (String s : this.permissions.split(",")) {
            	if(StringUtils.isBlank(s)){
            		response.addMessage("permissions", new TranslatableMessage("validate.cannotContainEmptyString"));
            		break;
            	}
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

	/* (non-Javadoc)
	 * @see com.serotonin.json.spi.JsonSerializable#jsonWrite(com.serotonin.json.ObjectWriter)
	 */
	@Override
	public void jsonWrite(ObjectWriter writer) throws IOException,
			JsonException {
        writer.writeEntry("name", name);
		writer.writeEntry("receiveAlarmEmails", AlarmLevels.CODES.getCode(receiveAlarmEmails));
		writer.writeEntry("permissions", permissions);
	}

	/* (non-Javadoc)
	 * @see com.serotonin.json.spi.JsonSerializable#jsonRead(com.serotonin.json.JsonReader, com.serotonin.json.type.JsonObject)
	 */
	@Override
	public void jsonRead(JsonReader reader, JsonObject jsonObject)
			throws JsonException {
        name = jsonObject.getString("name");
        if(name == null)
        	name = username;
		String text = jsonObject.getString("receiveAlarmEmails");
		if(text != null){
			receiveAlarmEmails = AlarmLevels.CODES.getId(text);
		}
		text = jsonObject.getString("permissions");
		if(text != null)
			setPermissions(text);
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.vo.AbstractVO#getDao()
	 */
	@Override
	protected AbstractDao<User> getDao() {
		return UserDao.instance;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.vo.AbstractVO#getTypeKey()
	 */
	@Override
	public String getTypeKey() {
		return "event.audit.user";
	}
	
	public Locale getLocaleObject() {
	    if (locale == null || locale.isEmpty()) {
	        return Common.getLocale();
	    }
	    
	    try {
	        return new Locale.Builder().setLanguageTag(locale).build();
	    } catch (IllformedLocaleException e) {
	        return Common.getLocale();
	    }
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
}
