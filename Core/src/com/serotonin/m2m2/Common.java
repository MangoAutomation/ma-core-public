/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
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
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.AuthSchemes;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.joda.time.DateTimeZone;
import org.joda.time.Period;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCrypt;

import com.infiniteautomation.mango.io.serial.SerialPortManager;
import com.infiniteautomation.mango.monitor.MonitoredValues;
import com.serotonin.ShouldNeverHappenException;
import com.serotonin.db.pair.StringStringPair;
import com.serotonin.json.JsonContext;
import com.serotonin.m2m2.db.DatabaseProxy;
import com.serotonin.m2m2.db.dao.SystemSettingsDao;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.i18n.Translations;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.rt.EventManager;
import com.serotonin.m2m2.rt.RuntimeManager;
import com.serotonin.m2m2.rt.maint.BackgroundProcessing;
import com.serotonin.m2m2.rt.maint.work.WorkItem;
import com.serotonin.m2m2.shared.VersionData;
import com.serotonin.m2m2.util.BackgroundContext;
import com.serotonin.m2m2.util.DocumentationManifest;
import com.serotonin.m2m2.util.ExportCodes;
import com.serotonin.m2m2.util.license.InstanceLicense;
import com.serotonin.m2m2.util.license.LicenseFeature;
import com.serotonin.m2m2.view.DynamicImage;
import com.serotonin.m2m2.view.ImageSet;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.web.OverridingWebAppContext;
import com.serotonin.m2m2.web.comparators.StringStringPairComparator;
import com.serotonin.m2m2.web.mvc.spring.security.authentication.MangoPasswordAuthenticationProvider;
import com.serotonin.timer.CronTimerTrigger;
import com.serotonin.timer.OrderedRealTimeTimer;
import com.serotonin.timer.RealTimeTimer;
import com.serotonin.util.properties.ReloadingProperties;

import freemarker.template.Configuration;

public class Common {
    public static final String SESSION_USER = "sessionUser";
    public static final String SESSION_USER_EXCEPTION  = "MANGO_USER_LAST_EXCEPTION";
    
    public static OverridingWebAppContext owac;
    // Note the start time of the application.
    public static final long START_TIME = System.currentTimeMillis();

    public static String MA_HOME;
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
    public static SerialPortManager serialPortManager;

    public static String applicationLogo = "/images/logo.png";
    public static String applicationFavicon = "/images/favicon.ico";
    public static final List<String> moduleStyles = new ArrayList<String>();
    public static final List<String> moduleScripts = new ArrayList<String>();
    public static final List<String> moduleJspfs = new ArrayList<String>();

    public static final DocumentationManifest documentationManifest = new DocumentationManifest();
    public static final List<ImageSet> imageSets = new ArrayList<ImageSet>();
    public static final List<DynamicImage> dynamicImages = new ArrayList<DynamicImage>();

    public static final RealTimeTimer timer = new OrderedRealTimeTimer();
    public static final MonitoredValues MONITORED_VALUES = new MonitoredValues();
    public static final JsonContext JSON_CONTEXT = new JsonContext();

    //
    // License
    static InstanceLicense license;
    static boolean free = true;
    static boolean invalid = false;

    public static InstanceLicense license() {
        return license;
    }

    public static LicenseFeature licenseFeature(String name) {
        if (license != null)
            return license.getFeature(name);
        return null;
    }
    
    public static boolean isFree() {
    	return free;
    }
    
    public static boolean isInvalid() {
    	return invalid;
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
        return 3;
    }

    public static final int getMinorVersion() {
        return 0;
    }

    public static final int getMicroVersion() {
        return 1;
    }

    public static final int getDatabaseSchemaVersion() {
        return 17;
    }

    /**
     * @return The version of Java that the core is compiled for
     */
    public static final double getJavaSpecificationVersion(){
    	return 1.8;
    }
    
    public static String getWebPath(String path) {
        return MA_HOME + "/web" + (path.startsWith("/") ? path : "/" + path);
    }

    public static File getLogsDir() {
        File file = new File(MA_HOME, "logs");
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

    //pointProperties.jsp probably depends on this and ROLLUP_CODES maintaining this ordering
    // meaning strictly ascending and the same add order to the rollup codes
    public interface Rollups {
    	int NONE = 0;
    	int AVERAGE = 1;
    	int DELTA = 2;
    	int MINIMUM = 3;
    	int MAXIMUM = 4;
    	int ACCUMULATOR = 5;
    	int SUM = 6;
    	int FIRST = 7;
    	int LAST = 8;
    	int COUNT = 9;
    	int INTEGRAL = 10;
    	int ALL = 11;
    }
    public static ExportCodes ROLLUP_CODES = new ExportCodes();
    static{
    	ROLLUP_CODES.addElement(Rollups.NONE, "NONE", "common.rollup.none");
    	ROLLUP_CODES.addElement(Rollups.AVERAGE, "AVERAGE", "common.rollup.average");
    	ROLLUP_CODES.addElement(Rollups.DELTA, "DELTA", "common.rollup.delta");
    	ROLLUP_CODES.addElement(Rollups.MINIMUM, "MINIMUM", "common.rollup.minimum");
    	ROLLUP_CODES.addElement(Rollups.MAXIMUM, "MAXIMUM", "common.rollup.maximum");
    	ROLLUP_CODES.addElement(Rollups.ACCUMULATOR, "ACCUMULATOR", "common.rollup.accumulator");
    	ROLLUP_CODES.addElement(Rollups.SUM, "SUM", "common.rollup.sum");
    	ROLLUP_CODES.addElement(Rollups.FIRST, "FIRST", "common.rollup.first");
    	ROLLUP_CODES.addElement(Rollups.LAST, "LAST", "common.rollup.last");
    	ROLLUP_CODES.addElement(Rollups.COUNT, "COUNT", "common.rollup.count");
    	ROLLUP_CODES.addElement(Rollups.INTEGRAL, "INTEGRAL", "common.rollup.integral");
    	ROLLUP_CODES.addElement(Rollups.ALL, "ALL", "common.rollup.all");
    }
    
	public static ExportCodes WORK_ITEM_CODES = new ExportCodes();
    static {
    	WORK_ITEM_CODES.addElement(WorkItem.PRIORITY_HIGH, "PRIORITY_HIGH");
    	WORK_ITEM_CODES.addElement(WorkItem.PRIORITY_MEDIUM, "PRIORITY_MEDIUM");
    	WORK_ITEM_CODES.addElement(WorkItem.PRIORITY_LOW, "PRIORITY_LOW");
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

    // Get the user of the current HTTP request or the background context
    @Deprecated
    public static User getUser() {
        User user = getHttpUser();
        if (user == null) {
            user = getBackgroundContextUser();
        }
        return user;
    }
    
    @Deprecated
    public static User getUser(HttpServletRequest request) {
        return getUser();
    }

    public static User getHttpUser() {
        // Check for the User via Spring Security, this will exist for every HTTP request
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            Object principle = auth.getPrincipal();
            // auth could be some token which does not have a User as its principle such as AnonymousAuthenticationToken
            if (principle instanceof User) {
                return (User) principle;
            }
        }
        return null;
    }
    
    public static void setHttpUser(HttpServletRequest request, User user) {
        User existingUser = getHttpUser();
        if (existingUser == null || existingUser.getId() != user.getId()) {
            throw new IllegalArgumentException("Can only update the current HTTP user");
        }
        
        setHttpAuthentication(request, MangoPasswordAuthenticationProvider.createAuthenticatedToken(user));
    }
    
    public static void setHttpAuthentication(HttpServletRequest request, Authentication authentication) {
        // Update the Spring Security context
        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        HttpSession session = request.getSession(false);
        if (session != null) {
            if (authentication == null) {
                session.removeAttribute(SESSION_USER);
            } else {
                Object principal = authentication.getPrincipal();
                if (principal instanceof User) {
                    // For legacy pages also update the session
                    session.setAttribute(SESSION_USER, (User) principal);
                }
            }
        }
    }

    public static User getBackgroundContextUser() {
        BackgroundContext backgroundContext = BackgroundContext.get();
        if (backgroundContext != null) {
            return backgroundContext.getUser();
        }
        return null;
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
    
    /**
     * Uses BCrypt by default
     * @param plaintext
     * @return
     */
    public static String encrypt(String plaintext) {
        String alg = getHashAlgorithm();
        String hash = encrypt(plaintext, alg);
        return "{" + alg + "}" + hash;
    }
    
    public static String getHashAlgorithm() {
        return envProps.getString("security.hashAlgorithm", "BCRYPT");
    }
    
    public static String encrypt(String plaintext, String alg) {
        try {
            if ("NONE".equals(alg))
                return plaintext;
            
            if ("BCRYPT".equals(alg)) {
                return BCrypt.hashpw(plaintext, BCrypt.gensalt());
            }
            
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
    
    public static boolean checkPassword(String plaintext, String hashed) {
        return checkPassword(plaintext, hashed, false);
    }
    
    public static final Pattern EXTRACT_ALGORITHM_HASH = Pattern.compile("^\\{(.*?)\\}(.*)");
    
    public static boolean checkPassword(String password, String storedHash, boolean passwordEncrypted) {
        try {
            if (password == null || storedHash == null)
                return false;
            
            if (passwordEncrypted) {
                return storedHash.equals(password);
            }
            
            Matcher m = EXTRACT_ALGORITHM_HASH.matcher(storedHash);
            if (!m.matches()) {
                return false;
            }
            String algorithm = m.group(1);
            String hash = m.group(2);
            
            if ("BCRYPT".equals(algorithm)) {
                return BCrypt.checkpw(password, hash);
            } else if ("NONE".equals(algorithm)) {
               return hash.equals(password);
            } else {
                return hash.equals(encrypt(password, algorithm));
            }
            
        } catch (Throwable t) {
            return false;
        }
    }
    
    public static String extractHashAlgorithm(String hash) {
        Matcher m = EXTRACT_ALGORITHM_HASH.matcher(hash);
        if (!m.matches()) {
            return null;
        }
        return m.group(1);
    }

    //
    // HttpClient
    public static HttpClient getHttpClient() {
        return getHttpClient(30000); // 30 seconds.
    }

    public static HttpClient getHttpClient(int timeout) {
        // Create global request configuration
        RequestConfig defaultRequestConfig = RequestConfig.custom().setCookieSpec(CookieSpecs.BEST_MATCH)
                .setExpectContinueEnabled(true).setStaleConnectionCheckEnabled(true)
                .setTargetPreferredAuthSchemes(Arrays.asList(AuthSchemes.NTLM, AuthSchemes.DIGEST))
                .setProxyPreferredAuthSchemes(Arrays.asList(AuthSchemes.BASIC)).setSocketTimeout(timeout)
                .setConnectTimeout(timeout).build();

        if (SystemSettingsDao.getBooleanValue(SystemSettingsDao.HTTP_CLIENT_USE_PROXY)) {
            String proxyHost = SystemSettingsDao.getValue(SystemSettingsDao.HTTP_CLIENT_PROXY_SERVER);
            int proxyPort = SystemSettingsDao.getIntValue(SystemSettingsDao.HTTP_CLIENT_PROXY_PORT);
            String username = SystemSettingsDao.getValue(SystemSettingsDao.HTTP_CLIENT_PROXY_USERNAME, "");
            String password = SystemSettingsDao.getValue(SystemSettingsDao.HTTP_CLIENT_PROXY_PASSWORD, "");

            CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(new AuthScope(proxyHost, proxyPort), new UsernamePasswordCredentials(
                    username, password));
            // Create an HttpClient with the given custom dependencies and configuration.
            CloseableHttpClient httpclient = HttpClients.custom().setProxy(new HttpHost(proxyHost, proxyPort))
                    .setDefaultRequestConfig(defaultRequestConfig).setDefaultCredentialsProvider(credentialsProvider)
                    .build();
            return httpclient;
        }
        else {
            // Create an HttpClient with the given custom dependencies and configuration.
            CloseableHttpClient httpclient = HttpClients.custom().setDefaultRequestConfig(defaultRequestConfig).build();
            return httpclient;
        }
        //LEGACY CODE LEFT HERE UNTIL Testing of above code is confirmed as working
        //        DefaultHttpClient client = new DefaultHttpClient();
        //        client.getParams().setParameter("http.socket.timeout", timeout);
        //        client.getParams().setParameter("http.connection.timeout", timeout);
        //        client.getParams().setParameter("http.connection-manager.timeout", timeout);
        //        client.getParams().setParameter("http.protocol.head-body-timeout", timeout);
        //
        //        if (SystemSettingsDao.getBooleanValue(SystemSettingsDao.HTTP_CLIENT_USE_PROXY)) {
        //            String proxyHost = SystemSettingsDao.getValue(SystemSettingsDao.HTTP_CLIENT_PROXY_SERVER);
        //            int proxyPort = SystemSettingsDao.getIntValue(SystemSettingsDao.HTTP_CLIENT_PROXY_PORT);
        //            String username = SystemSettingsDao.getValue(SystemSettingsDao.HTTP_CLIENT_PROXY_USERNAME, "");
        //            String password = SystemSettingsDao.getValue(SystemSettingsDao.HTTP_CLIENT_PROXY_PASSWORD, "");
        //
        //            client.getCredentialsProvider().setCredentials(new AuthScope(proxyHost, proxyPort),
        //                    new UsernamePasswordCredentials(username, password));
        //            
        //        }
        //
        //        return client;
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

    public static Locale getLocale() {
        ensureI18n();
        return parseLocale(systemLanguage);
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
        return prefix + UUID.randomUUID();
    }
}
