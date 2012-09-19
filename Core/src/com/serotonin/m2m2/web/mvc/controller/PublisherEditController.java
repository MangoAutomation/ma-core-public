/*
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.web.mvc.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.ParameterizableViewController;

import com.serotonin.ShouldNeverHappenException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.EventDao;
import com.serotonin.m2m2.db.dao.PublisherDao;
import com.serotonin.m2m2.i18n.Translations;
import com.serotonin.m2m2.module.ModuleRegistry;
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
    @Override
    protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        User user = Common.getUser(request);
        Permissions.ensureAdmin(user);

        PublisherVO<? extends PublishedPointVO> publisherVO;

        // Get the id.
        String idStr = request.getParameter("pid");
        if (idStr == null) {
            // Adding a new data source? Get the type id.
            String typeId = request.getParameter("typeId");

            // A new publisher
            publisherVO = ModuleRegistry.getPublisherDefinition(typeId).baseCreatePublisherVO();
            publisherVO.setXid(new PublisherDao().generateUniqueXid());
        }
        else {
            // An existing configuration.
            int id = Integer.parseInt(idStr);

            publisherVO = Common.runtimeManager.getPublisher(id);
            if (publisherVO == null)
                throw new ShouldNeverHappenException("Publisher not found with id " + id);
        }

        // Set the id of the data source in the user object for the DWR.
        user.setEditPublisher(publisherVO);

        // Create the model.
        Map<String, Object> model = new HashMap<String, Object>();
        model.put("publisher", publisherVO);
        if (publisherVO.getId() != Common.NEW_ID) {
            List<EventInstance> events = new EventDao().getPendingEventsForPublisher(publisherVO.getId(), user.getId());
            List<EventInstanceBean> beans = new ArrayList<EventInstanceBean>();
            if (events != null) {
                Translations translations = ControllerUtils.getTranslations(request);
                for (EventInstance event : events)
                    beans.add(new EventInstanceBean(event.isActive(), event.getAlarmLevel(), Functions.getTime(event
                            .getActiveTimestamp()), event.getMessage().translate(translations)));
            }
            model.put("publisherEvents", beans);
        }

        return new ModelAndView(getViewName(), model);
    }
}
