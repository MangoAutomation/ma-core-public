package com.serotonin.web.spring;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.servlet.ModelAndView;

/**
 * @author Matthew Lohbihler
 */
public class CustomModelController extends ExceptionLoggingController {
    private static final Log LOG = LogFactory.getLog(CustomModelController.class);

    private String removePrefix = "/jsp/";
    private String removeSuffix = "";

    @Override
    protected void initServletContext(ServletContext servletContext) {
        super.initServletContext(servletContext);

    }

    public void setRemovePrefix(String removePrefix) {
        if (StringUtils.isBlank(removePrefix))
            this.removePrefix = null;
        else
            this.removePrefix = removePrefix;
    }

    public void setRemoveSuffix(String removeSuffix) {
        if (StringUtils.isBlank(removeSuffix))
            this.removeSuffix = null;
        else
            this.removeSuffix = removeSuffix;
    }

    @Override
    final protected ModelAndView handleRequestImpl(HttpServletRequest request, HttpServletResponse response) {
        Map<String, Object> model = new HashMap<String, Object>();
        handleRequestInternal(request, response, model);

        String path = request.getRequestURI();
        String context = request.getContextPath();

        if (!StringUtils.isBlank(context) && path.startsWith(context))
            path = path.substring(context.length());

        if (removePrefix != null && path.startsWith(removePrefix))
            path = path.substring(removePrefix.length());

        if (removeSuffix != null && path.endsWith(removeSuffix))
            path = path.substring(0, path.length() - removeSuffix.length());

        ModelAndView mv = new ModelAndView(path);
        mv.getModelMap().addAllAttributes(model);
        return mv;
    }

    /**
     * Override this method to do neat things.
     */
    protected void handleRequestInternal(@SuppressWarnings("unused") HttpServletRequest request,
            @SuppressWarnings("unused") HttpServletResponse response,
            @SuppressWarnings("unused") Map<String, Object> model) {
        // no op
    }

    protected boolean isPost(HttpServletRequest request) {
        return request.getMethod().equals(METHOD_POST);
    }
}
