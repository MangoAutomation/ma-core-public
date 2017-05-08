package com.serotonin.web.context;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * A class to maintain the web application context objects with a ThreadLocal
 * instance, making the objects available within the application without having
 * to pass them around.
 * 
 * @author mlohbihler
 */
public class WebContext {
    /**
     * The ThreadLocal instance that will contain the various WebContext objects.
     */
    private static ThreadLocal<WebContext> contextStore = new ThreadLocal<WebContext>();

    /**
     * Creates the WebContext instance for this thread and adds it to the store.
     * @param request the request object
     * @param response the response object
     */
    public static void set(HttpServletRequest request, HttpServletResponse response) {
        contextStore.set(new WebContext(request, response));
    }
    
    /**
     * Used within user code to access the context objects.
     * @return the WebContext object found for this thread.
     */
    public static WebContext get() {
        return contextStore.get();
    }
    
    /**
     * Removes the WebContext object from this thread once we are done with it.
     */
    public static void remove() {
        contextStore.remove();
    }
    
    /**
     * The servlet request object.
     */
    private HttpServletRequest request;
    
    /**
     * The servlet response object.
     */
    private HttpServletResponse response;
    
    // Cached objects
    
    /**
     * Constructor
     * @param request the request object
     * @param response thre response object
     */
    private WebContext(HttpServletRequest request, HttpServletResponse response) {
        this.request = request;
        this.response = response;
    }
    
    public HttpServletRequest getRequest() {
        return request;
    }
    
    public HttpServletResponse getResponse() {
        return response;
    }
    
    /**
     * Convenience method for getting the session.
     * @return the session associated with the request.
     */
    public HttpSession getSession() {
        return request.getSession();
    }
    
    /**
     * Convenience method for getting the servlet context.
     * @return the servlet context associated with the request.
     */
    public ServletContext getServletContext() {
        return request.getSession().getServletContext();
    }
}
