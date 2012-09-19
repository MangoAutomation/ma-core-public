package com.serotonin.m2m2.web.servlet;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.EventDao;
import com.serotonin.m2m2.i18n.Translations;
import com.serotonin.m2m2.rt.event.EventInstance;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.export.EventCsvStreamer;
import com.serotonin.m2m2.web.dwr.beans.EventExportDefinition;

public class EventExportServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        User user = Common.getUser(request);
        if (user == null)
            return;

        EventExportDefinition def = user.getEventExportDefinition();
        if (def == null)
            return;

        final Translations translations = Common.getTranslations();
        List<EventInstance> events = new EventDao().search(def.getEventId(), def.getEventType(), def.getStatus(),
                def.getAlarmLevel(), def.getKeywords(), def.getDateFrom(), def.getDateTo(), user.getId(), translations,
                0, Integer.MAX_VALUE, null);

        // Stream the content.
        response.setContentType("text/csv");
        new EventCsvStreamer(response.getWriter(), events, translations);
    }
}
