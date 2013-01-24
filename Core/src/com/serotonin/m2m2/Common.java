/*
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2;

import java.io.File;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.directwebremoting.WebContext;
import org.directwebremoting.WebContextFactory;
import org.joda.time.DateTimeZone;
import org.joda.time.Period;

import com.serotonin.ShouldNeverHappenException;
import com.serotonin.db.pair.StringStringPair;
import com.serotonin.epoll.InputStreamEPoll;
import com.serotonin.json.JsonContext;
import com.serotonin.m2m2.db.DatabaseProxy;
import com.serotonin.m2m2.db.dao.SystemSettingsDao;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.i18n.Translations;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.rt.EventManager;
import com.serotonin.m2m2.rt.RuntimeManager;
import com.serotonin.m2m2.rt.maint.BackgroundProcessing;
import com.serotonin.m2m2.shared.VersionData;
import com.serotonin.m2m2.util.BackgroundContext;
import com.serotonin.m2m2.util.CommPortConfigException;
import com.serotonin.m2m2.util.DocumentationManifest;
import com.serotonin.m2m2.util.ExportCodes;
import com.serotonin.m2m2.util.license.InstanceLicense;
import com.serotonin.m2m2.util.license.LicenseFeature;
import com.serotonin.m2m2.view.DynamicImage;
import com.serotonin.m2m2.view.ImageSet;
import com.serotonin.m2m2.vo.CommPortProxy;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.web.comparators.StringStringPairComparator;
import com.serotonin.monitor.MonitoredValues;
import com.serotonin.timer.CronTimerTrigger;
import com.serotonin.timer.RealTimeTimer;
import com.serotonin.util.StringUtils;
import com.serotonin.util.properties.ReloadingProperties;

import freemarker.template.Configuration;
import gnu.io.CommPortIdentifier;

public class Common {
    private static final String SESSION_USER = "sessionUser";

    public static String M2M2_HOME;
    public static final String UTF8 = "UTF-8";
    public static final Charset UTF8_CS = Charset.forName(UTF8);
    public static final Charset ASCII_CS = Charset.forName("ASCII");

    public static final int NEW_ID = -1;

    public static ReloadingProperties envProps;
    public static Configuration freemarkerConfiguration;
    public static DatabaseProxy databaseProxy;
    public static BackgroundProcessing backgroundProcessing;
    public static EventManager eventManager;
    public static RuntimeManager runtimeManager;
    public static String applicationLogo = "images/logo.png";
    public static final List<String> applicationStyles = new ArrayList<String>();
    public static final List<String> applicationScripts = new ArrayList<String>();
    public static final DocumentationManifest documentationManifest = new DocumentationManifest();
    public static final List<ImageSet> imageSets = new ArrayList<ImageSet>();
    public static final List<DynamicImage> dynamicImages = new ArrayList<DynamicImage>();

    public static final RealTimeTimer timer = new RealTimeTimer();
    public static final MonitoredValues MONITORED_VALUES = new MonitoredValues();
    public static final JsonContext JSON_CONTEXT = new JsonContext();

    //
    // License
    static InstanceLicense license;

    public static InstanceLicense license() {
        return license;
    }

    public static LicenseFeature licenseFeature(String name) {
        if (license != null)
            return license.getFeature(name);
        return null;
    }

    //
    // E-poll
    private static InputStreamEPoll inputStreamEPoll;

    public static InputStreamEPoll getInputStreamEPoll() {
        return getInputStreamEPoll(true);
    }

    public static InputStreamEPoll getInputStreamEPoll(boolean create) {
        if (inputStreamEPoll == null && create) {
            synchronized (SESSION_USER) {
                if (inputStreamEPoll == null) {
                    inputStreamEPoll = new InputStreamEPoll();
                    new Thread(inputStreamEPoll).start();
                }
            }
        }

        return inputStreamEPoll;
    }

    /*
     * Updating the MA version: - Create a DBUpdate subclass for the old version number. This may not do anything in
     * particular to the schema, but is still required to update the system settings so that the database has the
     * correct version.
     */
    public static final VersionData getVersion() {
        return new VersionData(getMajorVersion(), getMinorVersion(), getMicroVersion());
    }

    public static final int getMajorVersion() {
        return 2;
    }

    public static final int getMinorVersion() {
        return 0;
    }

    public static final int getMicroVersion() {
        return 5;
    }

    public static final int getDatabaseSchemaVersion() {
        return 6;
    }

    public static String getWebPath(String path) {
        return M2M2_HOME + "/web" + (path.startsWith("/") ? path : "/" + path);
    }

    public static File getLogsDir() {
        File file = new File(M2M2_HOME, "logs");
        file.mkdirs();
        return file;
    }

    public interface ContextKeys {
        String IMAGE_SETS = "IMAGE_SETS";
        String DYNAMIC_IMAGES = "DYNAMIC_IMAGES";
    }

    public interface TimePeriods {
        int MILLISECONDS = 8;
        int SECONDS = 1;
        int MINUTES = 2;
        int HOURS = 3;
        int DAYS = 4;
        int WEEKS = 5;
        int MONTHS = 6;
        int YEARS = 7;
    }

    public static ExportCodes TIME_PERIOD_CODES = new ExportCodes();
    static {
        TIME_PERIOD_CODES.addElement(TimePeriods.MILLISECONDS, "MILLISECONDS");
        TIME_PERIOD_CODES.addElement(TimePeriods.SECONDS, "SECONDS");
        TIME_PERIOD_CODES.addElement(TimePeriods.MINUTES, "MINUTES");
        TIME_PERIOD_CODES.addElement(TimePeriods.HOURS, "HOURS");
        TIME_PERIOD_CODES.addElement(TimePeriods.DAYS, "DAYS");
        TIME_PERIOD_CODES.addElement(TimePeriods.WEEKS, "WEEKS");
        TIME_PERIOD_CODES.addElement(TimePeriods.MONTHS, "MONTHS");
        TIME_PERIOD_CODES.addElement(TimePeriods.YEARS, "YEARS");
    }

    /**
     * Returns the length of time in milliseconds that the
     * 
     * @param timePeriod
     * @param numberOfPeriods
     * @return
     */
    public static long getMillis(int periodType, int periods) {
        return getPeriod(periodType, periods).toDurationFrom(null).getMillis();
    }

    public static Period getPeriod(int periodType, int periods) {
        switch (periodType) {
        case TimePeriods.MILLISECONDS:
            return Period.millis(periods);
        case TimePeriods.SECONDS:
            return Period.seconds(periods);
        case TimePeriods.MINUTES:
            return Period.minutes(periods);
        case TimePeriods.HOURS:
            return Period.hours(periods);
        case TimePeriods.DAYS:
            return Period.days(periods);
        case TimePeriods.WEEKS:
            return Period.weeks(periods);
        case TimePeriods.MONTHS:
            return Period.months(periods);
        case TimePeriods.YEARS:
            return Period.years(periods);
        default:
            throw new ShouldNeverHappenException("Unsupported time period: " + periodType);
        }
    }

    public static TranslatableMessage getPeriodDescription(int periodType, int periods) {
        String periodKey;
        switch (periodType) {
        case TimePeriods.MILLISECONDS:
            periodKey = "common.tp.milliseconds";
            break;
        case TimePeriods.SECONDS:
            periodKey = "common.tp.seconds";
            break;
        case TimePeriods.MINUTES:
            periodKey = "common.tp.minutes";
            break;
        case TimePeriods.HOURS:
            periodKey = "common.tp.hours";
            break;
        case TimePeriods.DAYS:
            periodKey = "common.tp.days";
            break;
        case TimePeriods.WEEKS:
            periodKey = "common.tp.weeks";
            break;
        case TimePeriods.MONTHS:
            periodKey = "common.tp.months";
            break;
        case TimePeriods.YEARS:
            periodKey = "common.tp.years";
            break;
        default:
            throw new ShouldNeverHappenException("Unsupported time period: " + periodType);
        }

        return new TranslatableMessage("common.tp.description", periods, new TranslatableMessage(periodKey));
    }

    //
    // Session user
    public static User getUser() {
        WebContext webContext = WebContextFactory.get();
        if (webContext == null) {
            // If there is no web context, check if there is a background context
            BackgroundContext backgroundContext = BackgroundContext.get();
            if (backgroundContext == null)
                return null;
            return backgroundContext.getUser();
        }
        return getUser(webContext.getHttpServletRequest());
    }

    public static User getUser(HttpServletRequest request) {
        // Check first to see if the user object is in the request.
        User user = (User) request.getAttribute(SESSION_USER);
        if (user != null)
            return user;

        // If not, get it from the session.
        user = (User) request.getSession().getAttribute(SESSION_USER);

        if (user != null) {
            // Add the user to the request. This prevents race conditions in which long-ish lasting requests have the
            // user object swiped from them by a quicker (logout) request.
            request.setAttribute(SESSION_USER, user);
        }
        return user;
    }

    public static void setUser(HttpServletRequest request, User user) {
        request.getSession().setAttribute(SESSION_USER, user);
    }

    public static TimeZone getUserTimeZone(User user) {
        if (user != null)
            return user.getTimeZoneInstance();
        return TimeZone.getDefault();
    }

    public static DateTimeZone getUserDateTimeZone(User user) {
        if (user != null)
            return user.getDateTimeZoneInstance();
        return DateTimeZone.forID(TimeZone.getDefault().getID());
    }

    //
    // Background process description. Used for audit logs when the system automatically makes changes to data, such as
    // safe mode disabling stuff.
    public static String getBackgroundProcessDescription() {
        BackgroundContext backgroundContext = BackgroundContext.get();
        if (backgroundContext == null)
            return null;
        return backgroundContext.getProcessDescriptionKey();
    }

    //
    // Image sets and dynamic images
    public static List<String> getImageSetIds() {
        List<String> result = new ArrayList<String>();
        for (ImageSet s : imageSets)
            result.add(s.getId());
        return result;
    }

    public static ImageSet getImageSet(String id) {
        for (ImageSet imageSet : imageSets) {
            if (imageSet.getId().equals(id))
                return imageSet;
        }
        return null;
    }

    public static List<String> getDynamicImageIds() {
        List<String> result = new ArrayList<String>();
        for (DynamicImage d : dynamicImages)
            result.add(d.getId());
        return result;
    }

    public static DynamicImage getDynamicImage(String id) {
        for (DynamicImage dynamicImage : dynamicImages) {
            if (dynamicImage.getId().equals(id))
                return dynamicImage;
        }
        return null;
    }

    private static String lazyFiledataPath = null;

    public static String getFiledataPath() {
        if (lazyFiledataPath == null) {
            String name = SystemSettingsDao.getValue(SystemSettingsDao.FILEDATA_PATH);
            if (name.startsWith("~"))
                name = getWebPath(name.substring(1));

            File file = new File(name);
            if (!file.exists())
                file.mkdirs();

            lazyFiledataPath = name;
        }
        return lazyFiledataPath;
    }

    public static CronTimerTrigger getCronTrigger(int periodType, int delaySeconds) {
        int delayMinutes = 0;
        if (delaySeconds >= 60) {
            delayMinutes = delaySeconds / 60;
            delaySeconds %= 60;

            if (delayMinutes >= 60)
                delayMinutes = 59;
        }

        try {
            switch (periodType) {
            case TimePeriods.MILLISECONDS:
                throw new ShouldNeverHappenException("Can't create a cron trigger for milliseconds");
            case TimePeriods.SECONDS:
                return new CronTimerTrigger("* * * * * ?");
            case TimePeriods.MINUTES:
                return new CronTimerTrigger(delaySeconds + " * * * * ?");
            case TimePeriods.HOURS:
                return new CronTimerTrigger(delaySeconds + " " + delayMinutes + " * * * ?");
            case TimePeriods.DAYS:
                return new CronTimerTrigger(delaySeconds + " " + delayMinutes + " 0 * * ?");
            case TimePeriods.WEEKS:
                return new CronTimerTrigger(delaySeconds + " " + delayMinutes + " 0 ? * MON");
            case TimePeriods.MONTHS:
                return new CronTimerTrigger(delaySeconds + " " + delayMinutes + " 0 1 * ?");
            case TimePeriods.YEARS:
                return new CronTimerTrigger(delaySeconds + " " + delayMinutes + " 0 1 JAN ?");
            default:
                throw new ShouldNeverHappenException("Invalid cron period type: " + periodType);
            }
        }
        catch (ParseException e) {
            throw new ShouldNeverHappenException(e);
        }
    }

    //
    // Misc
    public static List<CommPortProxy> getCommPorts() throws CommPortConfigException {
        try {
            List<CommPortProxy> ports = new LinkedList<CommPortProxy>();
            Enumeration<?> portEnum = CommPortIdentifier.getPortIdentifiers();
            CommPortIdentifier cpid;
            while (portEnum.hasMoreElements()) {
                cpid = (CommPortIdentifier) portEnum.nextElement();
                if (cpid.getPortType() == CommPortIdentifier.PORT_SERIAL)
                    ports.add(new CommPortProxy(cpid));
            }
            return ports;
        }
        catch (UnsatisfiedLinkError e) {
            throw new CommPortConfigException(e.getMessage());
        }
        catch (NoClassDefFoundError e) {
            throw new CommPortConfigException(
                    "Comm configuration error. Check that rxtx DLL or libraries have been correctly installed.");
        }
    }

    public synchronized static String encrypt(String plaintext) {
        try {
            String alg = envProps.getString("security.hashAlgorithm", "SHA");
            if ("NONE".equals(alg))
                return plaintext;

            MessageDigest md = MessageDigest.getInstance(alg);
            if (md == null)
                throw new ShouldNeverHappenException("MessageDigest algorithm " + alg
                        + " not found. Set the 'security.hashAlgorithm' property in env.properties appropriately. "
                        + "Use 'NONE' for no hashing.");
            md.update(plaintext.getBytes(UTF8_CS));
            byte raw[] = md.digest();
            String hash = new String(Base64.encodeBase64(raw));
            return hash;
        }
        catch (NoSuchAlgorithmException e) {
            // Should never happen, so just wrap in a runtime exception and rethrow
            throw new ShouldNeverHappenException(e);
        }
    }

    //
    // HttpClient
    public static HttpClient getHttpClient() {
        return getHttpClient(30000); // 30 seconds.
    }

    public static HttpClient getHttpClient(int timeout) {
        DefaultHttpClient client = new DefaultHttpClient();
        client.getParams().setParameter("http.socket.timeout", timeout);
        client.getParams().setParameter("http.connection.timeout", timeout);
        client.getParams().setParameter("http.connection-manager.timeout", timeout);
        client.getParams().setParameter("http.protocol.head-body-timeout", timeout);

        if (SystemSettingsDao.getBooleanValue(SystemSettingsDao.HTTP_CLIENT_USE_PROXY)) {
            String proxyHost = SystemSettingsDao.getValue(SystemSettingsDao.HTTP_CLIENT_PROXY_SERVER);
            int proxyPort = SystemSettingsDao.getIntValue(SystemSettingsDao.HTTP_CLIENT_PROXY_PORT);
            String username = SystemSettingsDao.getValue(SystemSettingsDao.HTTP_CLIENT_PROXY_USERNAME, "");
            String password = SystemSettingsDao.getValue(SystemSettingsDao.HTTP_CLIENT_PROXY_PASSWORD, "");

            client.getCredentialsProvider().setCredentials(new AuthScope(proxyHost, proxyPort),
                    new UsernamePasswordCredentials(username, password));
        }

        return client;
    }

    //
    //
    // i18n
    //
    private static Object i18nLock = new Object();
    private static String systemLanguage;
    private static Translations systemTranslations;
    private static List<StringStringPair> languages;

    public static String translate(String key) {
        ensureI18n();
        return systemTranslations.translate(key);
    }

    public static Translations getTranslations() {
        ensureI18n();
        return systemTranslations;
    }

    private static void ensureI18n() {
        if (systemLanguage == null) {
            synchronized (i18nLock) {
                if (systemLanguage == null) {
                    systemLanguage = SystemSettingsDao.getValue(SystemSettingsDao.LANGUAGE);
                    Locale locale = parseLocale(systemLanguage);
                    if (locale == null)
                        throw new IllegalArgumentException("Locale for given language not found: " + systemLanguage);
                    systemTranslations = Translations.getTranslations(locale);
                }
            }
        }
    }

    public static String getMessage(String key, Object... args) {
        String pattern = translate(key);
        return MessageFormat.format(pattern, args);
    }

    public static void setSystemLanguage(String language) {
        if (parseLocale(language) == null)
            throw new IllegalArgumentException("Locale for given language not found: " + language);
        new SystemSettingsDao().setValue(SystemSettingsDao.LANGUAGE, language);
        systemLanguage = null;
        systemTranslations = null;
    }

    public static List<StringStringPair> getLanguages() {
        if (languages == null) {
            languages = new ArrayList<StringStringPair>();

            for (String localeStr : ModuleRegistry.getLocales()) {
                Locale locale = parseLocale(localeStr);
                if (locale != null)
                    languages.add(new StringStringPair(localeStr, Translations.getTranslations(locale).translate(
                            "locale.name")));
            }

            StringStringPairComparator.sort(languages);
        }

        return languages;
    }

    private static Locale parseLocale(String str) {
        String[] parts = str.split("_");
        Locale locale = null;
        if (parts.length == 1)
            locale = new Locale(parts[0]);
        else if (parts.length == 2)
            locale = new Locale(parts[0], parts[1]);
        else if (parts.length == 3)
            locale = new Locale(parts[0], parts[1], parts[2]);
        return locale;
    }

    public static String generateXid(String prefix) {
        return prefix + StringUtils.generateRandomString(6, "0123456789");
    }
}
