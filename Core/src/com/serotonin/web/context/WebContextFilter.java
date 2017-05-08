package com.serotonin.web.context;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * A filter for getting WebContext objects added to the thread for accessing by user code.
 * 
 * @author mlohbihler
 */
public class WebContextFilter implements Filter {
    public void init(FilterConfig arg0) {
        // no op
    }

    public void destroy() {
        // no op
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
            ServletException {
        try {
            // Added the web context to the thread.
            WebContext.set((HttpServletRequest) request, (HttpServletResponse) response);
            // Continue with the chain.
            chain.doFilter(request, response);
        }
        finally {
            // Make sure the web context gets removed when we are done with it.
            WebContext.remove();
        }
    }
}
