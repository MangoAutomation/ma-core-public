/*
 * Created on 26-Jul-2006
 */
package com.serotonin.web.content;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.MissingResourceException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

/**
 * @author Matthew Lohbihler
 */
public class ContentGenerator {
    private final HttpServletRequest request;
    private final String contentJsp;
    private final Map<String, Object> model;

    public ContentGenerator(HttpServletRequest request, String contentJsp, Map<String, Object> model) {
        this.request = request;
        this.contentJsp = contentJsp;
        this.model = model;
    }
    
    public String generateContent() throws ServletException, IOException {
        return generateContent(request, contentJsp, model);
    }
    
    public static String generateContent(HttpServletRequest request, String contentJsp, Map<String, Object> model)
            throws ServletException, IOException {
        MockServletResponse response = new MockServletResponse();
        Map<String, Object> oldValues = new HashMap<String, Object>();
        
        if (model != null) {
            for (String key : model.keySet()) {
                Object oldValue = request.getAttribute(key);
                if (oldValue != null)
                    oldValues.put(key, oldValue);
                
                request.setAttribute(key, model.get(key));
            }
        }
        
        try {
            request.getRequestDispatcher(contentJsp).forward(request, response);
        }
        catch (MissingResourceException e) {
            return "Resource "+ contentJsp +" not found";
        }
        finally {
            if (model != null) {
                for (String key : model.keySet())
                    request.removeAttribute(key);
                
                for (String key : oldValues.keySet())
                    request.setAttribute(key, oldValues.get(key));
            }
        }
        
        return response.getContent();
    }
}
