package com.serotonin.web.listener;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

/**
 * Add to a web application by adding the following to the web.xml:
 *   <listener><listener-class>com.serotonin.web.listener.SessionDataAccess</listener-class></listener>
 * 
 * Access the session map using the static method getSessionData(ServletContext)
 * 
 * @author Matthew Lohbihler
 */
public class SessionDataAccess implements HttpSessionListener {
    private static final String SESSION_DATA_KEY = "com.serotonin.web.listener.SessionDataAccess.SESSION_DATA_KEY";
    
    public void sessionCreated(HttpSessionEvent sessionEvent) {
        // Add the session to the session reference list.
        HttpSession session = sessionEvent.getSession();
        Map<String, HttpSession> sessionData = getSessionData(session.getServletContext());
        sessionData.put(session.getId(), session);
    }

    public void sessionDestroyed(HttpSessionEvent sessionEvent) {
        // Remove the session from the session reference list.
        HttpSession session = sessionEvent.getSession();
        Map<String, HttpSession> sessionData = getSessionData(session.getServletContext());
        sessionData.remove(session.getId());
    }
    
    @SuppressWarnings("unchecked")
    public static Map<String, HttpSession> getSessionData(ServletContext context) {
        Map<String, HttpSession> sessionData = (Map<String, HttpSession>)context.getAttribute(SESSION_DATA_KEY);
        if (sessionData == null) {
            sessionData = new HashMap<String, HttpSession>();
            context.setAttribute(SESSION_DATA_KEY, sessionData);
        }
        return sessionData;
    }
}
