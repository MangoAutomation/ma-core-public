/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.web.mvc.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.ParameterizableViewController;
import org.springframework.web.servlet.view.RedirectView;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.EventDao;
import com.serotonin.m2m2.db.dao.PublisherDao;
import com.serotonin.m2m2.i18n.Translations;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.module.PublisherDefinition;
import com.serotonin.m2m2.rt.event.EventInstance;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.permission.Permissions;
import com.serotonin.m2m2.vo.publish.PublishedPointVO;
import com.serotonin.m2m2.vo.publish.PublisherVO;
import com.serotonin.m2m2.web.dwr.beans.EventInstanceBean;
import com.serotonin.m2m2.web.taglib.Functions;

/**
 * @author Matthew Lohbihler
 */
public class PublisherEditController extends ParameterizableViewController {
    private String errorViewName;

    public void setErrorViewName(String errorViewName) {
        this.errorViewName = errorViewName;
    }

    public PublisherEditController(){
        super();
        setViewName("/WEB-INF/jsp/publisherEdit.jsp");
        setErrorViewName("/publishers.shtm");
    }


    @Override
    protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        User user = Common.getUser(request);
        Permissions.ensureHasAdminPermission(user);

        PublisherVO<? extends PublishedPointVO> publisherVO;

        // Get the id.
        String idStr = request.getParameter("pid");
        if (idStr == null) {
            // Adding a new data source? Get the type id.
            String typeId = request.getParameter("typeId");
            if (StringUtils.isBlank(typeId))
                return new ModelAndView(new RedirectView(errorViewName));

            // A new publisher
            PublisherDefinition def = ModuleRegistry.getPublisherDefinition(typeId);
            if (def == null)
                return new ModelAndView(new RedirectView(errorViewName));
            publisherVO = def.baseCreatePublisherVO();
            publisherVO.setXid(PublisherDao.getInstance().generateUniqueXid());
        }
        else {
            // An existing configuration.
            int id = Integer.parseInt(idStr);

            publisherVO = Common.runtimeManager.getPublisher(id);
            if (publisherVO == null)
                return new ModelAndView(new RedirectView(errorViewName));
        }

        // Set the id of the data source in the user object for the DWR.
        user.setEditPublisher(publisherVO);

        // Create the model.
        Map<String, Object> model = new HashMap<>();
        model.put("publisher", publisherVO);
        if (publisherVO.getId() != Common.NEW_ID) {
            List<EventInstance> events = EventDao.getInstance().getPendingEventsForPublisher(publisherVO.getId(), user.getId());
            List<EventInstanceBean> beans = new ArrayList<>();
            if (events != null) {
                Translations translations = ControllerUtils.getTranslations(request);
                for (EventInstance event : events)
                    beans.add(new EventInstanceBean(event.isActive(), event.getAlarmLevel(), Functions.getTime(event
                            .getActiveTimestamp()), event.getMessage().translate(translations)));
            }
            model.put("publisherEvents", beans);
        }

        publisherVO.addEditContext(model);

        return new ModelAndView(getViewName(), model);
    }
}
