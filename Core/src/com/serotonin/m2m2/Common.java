/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

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
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCrypt;

import com.github.zafarkhaja.semver.Version;
import com.infiniteautomation.mango.CompiledCoreVersion;
import com.infiniteautomation.mango.io.serial.SerialPortManager;
import com.infiniteautomation.mango.monitor.MonitoredValues;
import com.infiniteautomation.mango.spring.MangoRuntimeContextConfiguration;
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
import com.serotonin.m2m2.shared.ModuleUtils;
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
import com.serotonin.provider.Providers;
import com.serotonin.timer.AbstractTimer;
import com.serotonin.timer.CronTimerTrigger;
import com.serotonin.timer.OrderedRealTimeTimer;
import com.serotonin.util.properties.MangoProperties;

import freemarker.template.Configuration;

public class Common {
    public static final String SESSION_USER_EXCEPTION  = "MANGO_USER_LAST_EXCEPTION";

    public static OverridingWebAppContext owac;
    // Note the start time of the application.
    public static final long START_TIME = System.currentTimeMillis();

    /**
     * <p>Prefer {@link Common#MA_HOME_PATH}</p>
     *
     * <p>The Mango Automation installation directory. This is specified by the ma.home environment variable.</p>
     */
    @Deprecated
    public static String MA_HOME;

    /**
     * <p>The Mango Automation installation directory. This is specified by the ma.home environment variable.</p>
     */
    public static final Path MA_HOME_PATH;
    static {
        String maHomeProperty = System.getProperty("ma.home");
        MA_HOME_PATH = Paths.get(maHomeProperty != null ? maHomeProperty : ".").toAbsolutePath();
    }

    public static final String UTF8 = "UTF-8";
    public static final Charset UTF8_CS = Charset.forName(UTF8);
    public static final Charset ASCII_CS = Charset.forName("ASCII");

    public static final int NEW_ID = -1;

    public static MangoProperties envProps;
    public static Properties releaseProps;
    public static Configuration freemarkerConfiguration;
    public static DatabaseProxy databaseProxy;
    public static BackgroundProcessing backgroundProcessing;
    public static EventManager eventManager;
    public static RuntimeManager runtimeManager;
    public static SerialPortManager serialPortManager;

    //Used to determine the given size of all Task Queues
    //TODO Remove this and replace with varying size queues
    // depending on the type of task.  This was placed
    // here so we can release 3.0.0 with other features
    // and not have to worry about the various problems/testing
    // for all the different tasks.
    public static int defaultTaskQueueSize = 1;

    public static String applicationLogo = "/images/logo.png";
    public static String applicationFavicon = "/images/favicon.ico";
    public static final List<String> moduleStyles = new ArrayList<String>();
    public static final List<String> moduleScripts = new ArrayList<String>();
    public static final List<String> moduleJspfs = new ArrayList<String>();

    public static final DocumentationManifest documentationManifest = new DocumentationManifest();
    public static final List<ImageSet> imageSets = new ArrayList<ImageSet>();
    public static final List<DynamicImage> dynamicImages = new ArrayList<DynamicImage>();

    public static AbstractTimer timer = new OrderedRealTimeTimer();
    public static final MonitoredValues MONITORED_VALUES = new MonitoredValues();
    public static final JsonContext JSON_CONTEXT = new JsonContext();

    // epoch time in seconds of last upgrade/install of core or modules
    public static int lastUpgrade = 0;

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

    public static boolean isCoreSigned() {
        return releaseProps != null && Boolean.TRUE.toString().equals(releaseProps.getProperty("signed"));
    }

    /*
     * Updating the MA version: - Create a DBUpdate subclass for the old version number. This may not do anything in
     * particular to the schema, but is still required to update the system settings so that the database has the
     * correct version.
     */

    public static final Version getVersion() {
        return CoreVersion.INSTANCE.version;
    }

    public static final Version normalizeVersion(Version version) {
        return Version.forIntegers(version.getMajorVersion(), version.getMinorVersion(), version.getPatchVersion());
    }

    private enum CoreVersion {
        INSTANCE();
        final Version version;

        CoreVersion() {
            Version version = CompiledCoreVersion.VERSION;

            try {
                version = loadCoreVersionFromReleaseProperties(version);
            } catch (Throwable t) {}

            // check for possible license subversion
            if (version.getMajorVersion() != CompiledCoreVersion.VERSION.getMajorVersion()) {
                throw new RuntimeException("Version from release.properties does not match compiled major version " + CompiledCoreVersion.VERSION.getMajorVersion());
            }

            this.version = version;
        }
    }

    private static final Version loadCoreVersionFromReleaseProperties(Version version) {
        if (releaseProps != null) {
            String versionStr = releaseProps.getProperty(ModuleUtils.Constants.PROP_VERSION);
            String buildNumberStr = releaseProps.getProperty(ModuleUtils.Constants.BUILD_NUMBER);

            if (versionStr != null) {
                try {
                    version = Version.valueOf(versionStr);
                } catch (com.github.zafarkhaja.semver.ParseException e) { }
            }

            if (buildNumberStr != null) {
                version = version.setBuildMetadata(buildNumberStr);
            }
        }

        return version;
    }

    public static final int getDatabaseSchemaVersion() {
        return 26;
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

    public static String getOverrideWebPath(String path) {
        return MA_HOME + "/overrides/web" + (path.startsWith("/") ? path : "/" + path);
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
        int START = 12;
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
        ROLLUP_CODES.addElement(Rollups.START, "START", "common.rollup.start");
    }

    public static ExportCodes WORK_ITEM_CODES = new ExportCodes();
    static {
        WORK_ITEM_CODES.addElement(WorkItem.PRIORITY_HIGH, "PRIORITY_HIGH");
        WORK_ITEM_CODES.addElement(WorkItem.PRIORITY_MEDIUM, "PRIORITY_MEDIUM");
        WORK_ITEM_CODES.addElement(WorkItem.PRIORITY_LOW, "PRIORITY_LOW");
    }

    public static ExportCodes VERSION_STATE_CODES = new ExportCodes();
    static{
        VERSION_STATE_CODES.addElement(UpgradeVersionState.DEVELOPMENT, "DEVELOPMENT", "systemSettings.upgradeState.development");
        VERSION_STATE_CODES.addElement(UpgradeVersionState.ALPHA, "ALPHA", "systemSettings.upgradeState.alpha");
        VERSION_STATE_CODES.addElement(UpgradeVersionState.BETA, "BETA", "systemSettings.upgradeState.beta");
        VERSION_STATE_CODES.addElement(UpgradeVersionState.RELEASE_CANDIDATE, "RELEASE_CANDIDATE", "systemSettings.upgradeState.releaseCanditate");
        VERSION_STATE_CODES.addElement(UpgradeVersionState.PRODUCTION, "PRODUCTION", "systemSettings.upgradeState.production");
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
            String name = SystemSettingsDao.instance.getValue(SystemSettingsDao.FILEDATA_PATH);
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
        return envProps.getString("security.hashAlgorithm", User.BCRYPT_ALGORITHM);
    }

    public static String encrypt(String plaintext, String alg) {
        try {
            if (User.NONE_ALGORITHM.equals(alg))
                return plaintext;

            if (User.BCRYPT_ALGORITHM.equals(alg)) {
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

    public static boolean verifyProperties(InputStream in, boolean signed, Map<String, String> properties) {
        return Providers.get(IMangoLifecycle.class).verifyProperties(in, signed, properties);
    }

    public static boolean checkPassword(String plaintext, String hashed) {
        return checkPassword(plaintext, hashed, false);
    }

    public static final Pattern EXTRACT_ALGORITHM_HASH = Pattern.compile("^\\{(.+?)\\}(.*)");

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

            if (User.BCRYPT_ALGORITHM.equals(algorithm)) {
                return BCrypt.checkpw(password, hash);
            } else if (User.NONE_ALGORITHM.equals(algorithm)) {
                return hash.equals(password);
            } else if (User.LOCKED_ALGORITHM.equals(algorithm)) {
                return false;
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
        RequestConfig defaultRequestConfig = RequestConfig.custom().setCookieSpec(CookieSpecs.DEFAULT)
                .setExpectContinueEnabled(true)
                .setTargetPreferredAuthSchemes(Arrays.asList(AuthSchemes.NTLM, AuthSchemes.DIGEST))
                .setProxyPreferredAuthSchemes(Arrays.asList(AuthSchemes.BASIC)).setSocketTimeout(timeout)
                .setConnectTimeout(timeout).build();

        if (SystemSettingsDao.instance.getBooleanValue(SystemSettingsDao.HTTP_CLIENT_USE_PROXY)) {
            String proxyHost = SystemSettingsDao.instance.getValue(SystemSettingsDao.HTTP_CLIENT_PROXY_SERVER);
            int proxyPort = SystemSettingsDao.instance.getIntValue(SystemSettingsDao.HTTP_CLIENT_PROXY_PORT);
            String username = SystemSettingsDao.instance.getValue(SystemSettingsDao.HTTP_CLIENT_PROXY_USERNAME);
            String password = SystemSettingsDao.instance.getValue(SystemSettingsDao.HTTP_CLIENT_PROXY_PASSWORD);

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
    }

    //
    //
    // i18n
    //
    private static Object i18nLock = new Object();
    private static String systemLanguage;
    private static Translations systemTranslations;
    private static Locale systemLocale;
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
        return systemLocale;
    }

    private static void ensureI18n() {
        if (systemLanguage == null) {
            synchronized (i18nLock) {
                if (systemLanguage == null) {
                    systemLanguage = SystemSettingsDao.instance.getValue(SystemSettingsDao.LANGUAGE);
                    systemLocale = parseLocale(systemLanguage);
                    if (systemLocale == null)
                        throw new IllegalArgumentException("Locale for given language not found: " + systemLanguage);
                    systemTranslations = Translations.getTranslations(systemLocale);
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
        systemLanguage = null;
        systemLocale = null;
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

    public static Locale parseLocale(String str) {
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

    /**
     * Get the HTTP/HTTPS Cookie Name based on scheme and port
     * @return
     */
    public static String getCookieName() {
        if (Common.envProps.getBoolean("sessionCookie.useGuid", true)) {
            return Providers.get(ICoreLicense.class).getGuid();
        }

        String cookieName = Common.envProps.getString("sessionCookie.name");
        if (cookieName != null && !cookieName.isEmpty()) {
            return cookieName;
        }

        if(Common.envProps.getBoolean("ssl.on", false)) {
            return "MANGO" + Common.envProps.getInt("ssl.port", 443);
        }else {
            return "MANGO" + Common.envProps.getInt("web.port", 8080);
        }
    }

    /* Spring application contexts */

    /**
     * Gets the spring root web application context, only set after the context is refreshed (started).
     * If its not null, its safe to use.
     *
     * @return
     */
    public static ApplicationContext getRootWebContext() {
        return MangoRuntimeContextConfiguration.getRootWebContext();
    }

    /**
     * Gets the spring runtime application context, only set after the context is refreshed (started).
     * If its not null, its safe to use.
     *
     * @return
     */
    public static ApplicationContext getRuntimeContext() {
        return MangoRuntimeContextConfiguration.getRuntimeContext();
    }

    /**
     * Get a bean from the runtime context.
     * @param clazz
     * @return
     */
    public static <T> T getBean(Class<T> clazz) {
        return getRuntimeContext().getBean(clazz);
    }

    /**
     * Get a bean from the runtime context.
     * @param clazz
     * @param name
     * @return
     */
    public static <T> T getBean(Class<T> clazz, String name) {
        return getRuntimeContext().getBean(name, clazz);
    }

    // TODO Mango 3.5 - Dont use in modules until Mango 3.5
    public static final class MediaTypes {
        private MediaTypes() {}

        public static final class VersionedMediaType extends MediaType {
            private static final long serialVersionUID = 1L;
            public static final String VERSION_PARAMETER = "version";
            private final Set<String> compatibleVersions = new HashSet<>();

            public VersionedMediaType(String type, String subtype, String version, String... compatibleVersions) {
                super(type, subtype, Collections.singletonMap(VERSION_PARAMETER, version));

                this.compatibleVersions.add(version);
                for (String v : compatibleVersions) {
                    this.compatibleVersions.add(v);
                }
            }

            @Override
            public boolean isCompatibleWith(MediaType other) {
                boolean compatible = super.isCompatibleWith(other);

                if (!compatible) {
                    return false;
                }

                String otherVersion = other.getParameter(VERSION_PARAMETER);
                return this.compatibleVersions.contains(otherVersion);
            }
        }

        // use these for produces/consumes annotations
        public static final String CSV_VALUE = "text/csv";
        public static final String SEROTONIN_JSON_VALUE = "application/vnd.infinite-automation-systems.mango.serotonin-json";
        public static final String SEROTONIN_JSON_OLD_VALUE = "application/sero-json";

        // TODO Mango 3.5 remove null from compatible versions, only allow explicit version=1
        public static final MediaType CSV_V1 = new VersionedMediaType("text", "csv", "1", (String) null);
        public static final MediaType CSV_V2 = new VersionedMediaType("text", "csv", "2", (String) null);
        public static final MediaType SEROTONIN_JSON = new MediaType("application", "vnd.infinite-automation-systems.mango.serotonin-json");

        // TODO Mango 3.5 remove this mime type - should be vendor prefixed
        public static final MediaType SEROTONIN_JSON_OLD = new MediaType("application", "sero-json");
    }

}
