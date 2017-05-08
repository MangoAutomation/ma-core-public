package com.serotonin.web.spring;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;

import com.serotonin.web.http.RequestUtils;

abstract public class ExceptionLoggingController extends AbstractController {
    private static final Log LOG = LogFactory.getLog(ExceptionLoggingController.class);

    @Override
    final protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        try {
            return handleRequestImpl(request, response);
        }
        catch (Exception e) {
            // Ensure that exceptions get logged. Don't trust error pages for this.
            LOG.error("Exception in request: " + RequestUtils.dumpRequest(request), e);
            throw e;
        }
    }

    abstract protected ModelAndView handleRequestImpl(HttpServletRequest request, HttpServletResponse response)
            throws Exception;
}
