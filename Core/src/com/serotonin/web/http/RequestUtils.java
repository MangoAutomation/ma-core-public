package com.serotonin.web.http;

import java.util.Enumeration;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

public class RequestUtils {
    public static String dumpRequest(HttpServletRequest request) {
        StringBuilder sb = new StringBuilder();

        sb.append("parameters: ").append(request.getParameterMap());

        sb.append(", headers: {");
        @SuppressWarnings("unchecked")
        Enumeration<String> headers = request.getHeaderNames();
        boolean first = true;
        while (headers.hasMoreElements()) {
            if (first)
                first = false;
            else
                sb.append(", ");
            String name = headers.nextElement();
            sb.append(name).append("=").append(request.getHeader(name));
        }
        sb.append("}");

        sb.append(", cookies: ");
        Cookie[] cookies = request.getCookies();
        if (cookies == null)
            sb.append("null");
        else {
            sb.append("{");
            first = true;
            for (Cookie cookie : cookies) {
                if (first)
                    first = false;
                else
                    sb.append(", ");
                sb.append(cookie.getName()).append("=").append(cookie.getValue());
            }
            sb.append("}");
        }

        sb.append(", remoteIP: ").append(request.getRemoteAddr());

        return sb.toString();
    }
}
