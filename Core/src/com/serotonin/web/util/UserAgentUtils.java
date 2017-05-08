package com.serotonin.web.util;

import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;

public class UserAgentUtils {
    private static final String HTTP_X_OPERAMINI_PHONE_UA = "X-Operamini-Phone-UA";
    private static final String HTTP_X_SKYFIRE_PHONE = "X-Skyfire-Phone";
    private static final String HTTP_USER_AGENT = "User-Agent";

    private static final Pattern mobile = Pattern.compile("(iphone|ipod|ipad|blackberry|android|palm|windows\\s+ce)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern desktop = Pattern.compile("(windows|linux|os\\s+[x9]|solaris|bsd)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern bot = Pattern.compile("(spider|crawl|slurp|bot)", Pattern.CASE_INSENSITIVE);

    public static String getUserAgent(HttpServletRequest request) {
        String ua = request.getHeader(HTTP_X_OPERAMINI_PHONE_UA);
        if (StringUtils.isBlank(ua))
            ua = request.getHeader(HTTP_X_SKYFIRE_PHONE);
        if (StringUtils.isBlank(ua))
            ua = request.getHeader(HTTP_USER_AGENT);
        if (StringUtils.isBlank(ua))
            ua = "";
        return ua;
    }

    public static boolean isDesktop(HttpServletRequest request) {
        return isDesktop(getUserAgent(request));
    }

    public static boolean isDesktop(String userAgent) {
        //
        // Anything that looks like a phone isn't a desktop.
        // Anything that looks like a desktop probably is.
        // Anything that looks like a bot should default to desktop.
        //
        return (!matches(mobile, userAgent) && matches(desktop, userAgent)) || matches(bot, userAgent);
    }

    private static boolean matches(Pattern p, String s) {
        return p.matcher(s).find();
    }

    public static boolean isMobile(HttpServletRequest request) {
        return isMobile(getUserAgent(request));
    }

    public static boolean isMobile(String userAgent) {
        return !isDesktop(userAgent);
    }
    //
    //    public static void main(String[] args) {
    //        String s = "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:19.0) Gecko/20100101 Firefox/19.0";
    //        String r = "(windows|linux|os\\s+[x9]|solaris|bsd)";
    //
    //        Pattern p = Pattern.compile(r, Pattern.DOTALL);
    //        System.out.println(com.serotonin.util.StringUtils.findGroup(p, s));
    //        System.out.println(p.matcher(s).matches());
    //
    //        System.out.println(isDesktop(s));
    //        System.out.println(isMobile(s));
    //
    //        //        System.out.println(s.matches());
    //        //        System.out.println(mobile.matcher(s).matches());
    //        //        System.out.println(desktop.matcher(s).matches());
    //        //        System.out.println(bot.matcher(s).matches());
    //    }
}
