/*
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.web.dwr;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.directwebremoting.WebContextFactory;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.IllegalFieldValueException;

import com.serotonin.ShouldNeverHappenException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.ILifecycle;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.db.dao.EventDao;
import com.serotonin.m2m2.db.dao.SystemSettingsDao;
import com.serotonin.m2m2.db.dao.UserDao;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.i18n.Translations;
import com.serotonin.m2m2.module.LongPollDefinition;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.rt.dataImage.DataPointRT;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.rt.dataImage.SetPointSource;
import com.serotonin.m2m2.rt.dataImage.types.DataValue;
import com.serotonin.m2m2.rt.dataImage.types.ImageValue;
import com.serotonin.m2m2.rt.event.EventInstance;
import com.serotonin.m2m2.util.DateUtils;
import com.serotonin.m2m2.view.chart.ChartRenderer;
import com.serotonin.m2m2.vo.DataPointExtendedNameComparator;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.UserComment;
import com.serotonin.m2m2.vo.permission.Permissions;
import com.serotonin.m2m2.web.dwr.beans.BasePointState;
import com.serotonin.m2m2.web.dwr.beans.DataPointBean;
import com.serotonin.m2m2.web.dwr.beans.PointDetailsState;
import com.serotonin.m2m2.web.dwr.longPoll.LongPollData;
import com.serotonin.m2m2.web.dwr.longPoll.LongPollHandler;
import com.serotonin.m2m2.web.dwr.longPoll.LongPollRequest;
import com.serotonin.m2m2.web.dwr.longPoll.LongPollState;
import com.serotonin.m2m2.web.dwr.util.DwrPermission;
import com.serotonin.m2m2.web.mvc.controller.ControllerUtils;
import com.serotonin.m2m2.web.taglib.Functions;
import com.serotonin.provider.Providers;
import com.serotonin.web.content.ContentGenerator;

abstract public class BaseDwr {
    public static final String MODEL_ATTR_EVENTS = "events";
    public static final String MODEL_ATTR_HAS_UNACKED_EVENT = "hasUnacknowledgedEvent";
    public static final String MODEL_ATTR_TRANSLATIONS = "bundle";

    protected static EventDao EVENT_DAO;

    public static void initialize() {
        EVENT_DAO = new EventDao();
    }

    public BaseDwr() {
        // Cache the long poll handlers.
        for (LongPollDefinition def : ModuleRegistry.getDefinitions(LongPollDefinition.class))
            longPollHandlers.add(def.getHandler());
    }

    /**
     * Base method for preparing information in a state object and returning a point value.
     * 
     * @param componentId
     *            a unique id for the browser side component. Required for set point snippets.
     * @param state
     * @param point
     * @param status
     * @param model
     * @return
     */
    protected static PointValueTime prepareBasePointState(String componentId, BasePointState state,
            DataPointVO pointVO, DataPointRT point, Map<String, Object> model) {
        model.clear();
        model.put("componentId", componentId);
        model.put("point", pointVO);
        model.put("pointRT", point);
        model.put(MODEL_ATTR_TRANSLATIONS, getTranslations());

        PointValueTime pointValue = null;
        if (point == null)
            model.put("disabled", "true");
        else {
            pointValue = point.getPointValue();
            if (pointValue != null)
                model.put("pointValue", pointValue);
        }

        return pointValue;
    }

    protected static void setEvents(DataPointVO pointVO, User user, Map<String, Object> model) {
        int userId = 0;
        if (user != null)
            userId = user.getId();
        List<EventInstance> events = EVENT_DAO.getPendingEventsForDataPoint(pointVO.getId(), userId);
        if (events != null) {
            model.put(MODEL_ATTR_EVENTS, events);
            for (EventInstance event : events) {
                if (!event.isAcknowledged())
                    model.put(MODEL_ATTR_HAS_UNACKED_EVENT, true);
            }
        }
    }

    protected static void setPrettyText(HttpServletRequest request, PointDetailsState state, DataPointVO pointVO,
            Map<String, Object> model, PointValueTime pointValue) {
        if (pointValue != null && pointValue.getValue() instanceof ImageValue) {
            if (!ObjectUtils.equals(pointVO.lastValue(), pointValue)) {
                state.setValue(generateContent(request, "imageValueThumbnail.jsp", model));
                state.setTime(Functions.getTime(pointValue));
                pointVO.updateLastValue(pointValue);
            }
        }
        else {
            String prettyText = Functions.getHtmlText(pointVO, pointValue);
            model.put("text", prettyText);
            if (!ObjectUtils.equals(pointVO.lastValue(), pointValue)) {
                state.setValue(prettyText);
                if (pointValue != null)
                    state.setTime(Functions.getTime(pointValue));
                pointVO.updateLastValue(pointValue);
            }
        }
    }

    protected static void setChange(DataPointVO pointVO, BasePointState state, DataPointRT point,
            HttpServletRequest request, Map<String, Object> model, User user) {
        if (Permissions.hasDataPointSetPermission(user, pointVO))
            setChange(pointVO, state, point, request, model);
    }

    protected static void setChange(DataPointVO pointVO, BasePointState state, DataPointRT point,
            HttpServletRequest request, Map<String, Object> model) {
        if (pointVO.getPointLocator().isSettable()) {
            if (point == null)
                state.setChange(translate("common.pointDisabled"));
            else {
                String snippet = pointVO.getTextRenderer().getChangeSnippetFilename();
                state.setChange(generateContent(request, snippet, model));
            }
        }
    }

    protected static void setChart(DataPointVO point, BasePointState state, HttpServletRequest request,
            Map<String, Object> model) {
        ChartRenderer chartRenderer = point.getChartRenderer();
        if (chartRenderer != null) {
            chartRenderer.addDataToModel(model, point);
            String snippet = chartRenderer.getChartSnippetFilename();
            state.setChart(generateContent(request, snippet, model));
        }
    }

    protected static void setMessages(BasePointState state, HttpServletRequest request, String snippet,
            Map<String, Object> model) {
        state.setMessages(generateContent(request, snippet, model).trim());
    }

    /**
     * Allows the setting of a given data point. Used by the watch list and point details pages. Views implement their
     * own version to accommodate anonymous users.
     * 
     * @param pointId
     * @param valueStr
     * @return
     */
    @DwrPermission(user = true)
    public int setPoint(int pointId, int componentId, String valueStr) {
        User user = Common.getUser();
        DataPointVO point = new DataPointDao().getDataPoint(pointId);

        // Check permissions.
        Permissions.ensureDataPointSetPermission(user, point);

        setPointImpl(point, valueStr, user);
        return componentId;
    }

    protected void setPointImpl(DataPointVO point, String valueStr, SetPointSource source) {
        if (point == null)
            return;

        if (valueStr == null)
            Common.runtimeManager.relinquish(point.getId());
        else {
            // Convert the string value into an object.
            DataValue value = DataValue.stringToValue(valueStr, point.getPointLocator().getDataTypeId());
            Common.runtimeManager.setDataPointValue(point.getId(), value, source);
        }
    }

    @DwrPermission(user = true)
    public void forcePointRead(int pointId) {
        User user = Common.getUser();
        DataPointVO point = new DataPointDao().getDataPoint(pointId);

        // Check permissions.
        Permissions.ensureDataPointReadPermission(user, point);

        Common.runtimeManager.forcePointRead(pointId);
    }

    /**
     * Logs a user comment after validation.
     * 
     * @param eventId
     * @param comment
     * @return
     */
    @DwrPermission(user = true)
    public UserComment addUserComment(int typeId, int referenceId, String comment) {
        if (StringUtils.isBlank(comment))
            return null;

        User user = Common.getUser();
        UserComment c = new UserComment();
        c.setComment(comment);
        c.setTs(System.currentTimeMillis());
        c.setUserId(user.getId());
        c.setUsername(user.getUsername());

        if (typeId == UserComment.TYPE_EVENT)
            EVENT_DAO.insertEventComment(referenceId, c);
        else if (typeId == UserComment.TYPE_POINT)
            new UserDao().insertUserComment(UserComment.TYPE_POINT, referenceId, c);
        else
            throw new ShouldNeverHappenException("Invalid comment type: " + typeId);

        return c;
    }

    protected List<DataPointBean> getReadablePoints() {
        User user = Common.getUser();

        List<DataPointVO> points = new DataPointDao().getDataPoints(DataPointExtendedNameComparator.instance, false);
        if (!Permissions.hasAdmin(user)) {
            List<DataPointVO> userPoints = new ArrayList<DataPointVO>();
            for (DataPointVO dp : points) {
                if (Permissions.hasDataPointReadPermission(user, dp))
                    userPoints.add(dp);
            }
            points = userPoints;
        }

        List<DataPointBean> result = new ArrayList<DataPointBean>();
        for (DataPointVO dp : points)
            result.add(new DataPointBean(dp));

        return result;
    }

    @DwrPermission(anonymous = true)
    public Map<String, Object> getDateRangeDefaults(int periodType, int period) {
        Map<String, Object> result = new HashMap<String, Object>();

        DateTimeZone dtz = Common.getUserDateTimeZone(Common.getUser());

        // Default the specific date fields.
        DateTime dt = new DateTime(dtz);
        result.put("toYear", dt.getYear());
        result.put("toMonth", dt.getMonthOfYear());
        result.put("toDay", dt.getDayOfMonth());
        result.put("toHour", dt.getHourOfDay());
        result.put("toMinute", dt.getMinuteOfHour());
        result.put("toSecond", 0);

        dt = DateUtils.minus(dt, periodType, period);
        result.put("fromYear", dt.getYear());
        result.put("fromMonth", dt.getMonthOfYear());
        result.put("fromDay", dt.getDayOfMonth());
        result.put("fromHour", dt.getHourOfDay());
        result.put("fromMinute", dt.getMinuteOfHour());
        result.put("fromSecond", 0);

        return result;
    }

    protected static String translate(String key, Object... args) {
        if (args == null || args.length == 0)
            return getTranslations().translate(key);
        return new TranslatableMessage(key, args).translate(getTranslations());
    }

    protected static String translate(TranslatableMessage message) {
        return message.translate(getTranslations());
    }

    protected static Translations getTranslations() {
        return Translations.getTranslations(getLocale());
    }

    protected static Locale getLocale() {
        return ControllerUtils.getLocale(WebContextFactory.get().getHttpServletRequest());
    }

    public static String generateContent(HttpServletRequest request, String snippet, Map<String, Object> model) {
        if (!snippet.startsWith("/"))
            snippet = "/WEB-INF/snippet/" + snippet;

        try {
            return ContentGenerator.generateContent(request, snippet, model);
        }
        catch (ServletException e) {
            throw new ShouldNeverHappenException(e);
        }
        catch (IOException e) {
            throw new ShouldNeverHappenException(e);
        }
    }

    protected List<User> getShareUsers(User excludeUser) {
        List<User> users = new ArrayList<User>();
        for (User u : new UserDao().getUsers()) {
            if (u.getId() != excludeUser.getId())
                users.add(u);
        }
        return users;
    }

    //
    //
    // Long Poll
    //
    private static final String LONG_POLL_DATA_KEY = "LONG_POLL_DATA";
    private static final String LONG_POLL_DATA_TIMEOUT_KEY = "LONG_POLL_DATA_TIMEOUT";
    private final List<LongPollHandler> longPollHandlers = new ArrayList<LongPollHandler>();

    @DwrPermission(anonymous = true)
    public Map<String, Object> initializeLongPoll(int pollSessionId, LongPollRequest request) {
        LongPollData data = getLongPollData(pollSessionId, true);
        data.setRequest(request);
        return doLongPoll(pollSessionId);
    }

    @DwrPermission(anonymous = true)
    public Map<String, Object> doLongPoll(int pollSessionId) {
        Map<String, Object> response = new HashMap<String, Object>();
        HttpServletRequest httpRequest = WebContextFactory.get().getHttpServletRequest();
        User user = Common.getUser(httpRequest);
        EventDao eventDao = new EventDao();

        LongPollData data = getLongPollData(pollSessionId, false);
        data.updateTimestamp();

        LongPollRequest pollRequest = data.getRequest();

        long expireTime = System.currentTimeMillis() + 60000; // One minute
        LongPollState state = data.getState();
        int waitTime = SystemSettingsDao.getIntValue(SystemSettingsDao.UI_PERFORMANCE);

        // For users that log in on multiple machines (or browsers), reset the last alarm timestamp so that it always
        // gets reset with at least each new poll. For now this beats writing user-specific event change tracking code.
        state.setLastAlarmLevelChange(0);

        while (!pollRequest.isTerminated() && System.currentTimeMillis() < expireTime) {
            if (Providers.get(ILifecycle.class).isTerminated()) {
                pollRequest.setTerminated(true);
                break;
            }

            if (pollRequest.isMaxAlarm() && user != null) {
                // Check the max alarm. First check if the events have changed since the last time this request checked.
                long lastEMUpdate = Common.eventManager.getLastAlarmTimestamp();
                if (state.getLastAlarmLevelChange() < lastEMUpdate) {
                    state.setLastAlarmLevelChange(lastEMUpdate);

                    // The events have changed. See if the user's particular max alarm level has changed.
                    int maxAlarmLevel = eventDao.getHighestUnsilencedAlarmLevel(user.getId());
                    if (maxAlarmLevel != state.getMaxAlarmLevel()) {
                        response.put("highestUnsilencedAlarmLevel", maxAlarmLevel);
                        state.setMaxAlarmLevel(maxAlarmLevel);
                    }
                }
            }

            if (pollRequest.isPointDetails() && user != null) {
                PointDetailsState newState = DataPointDetailsDwr.getPointData();
                PointDetailsState responseState;
                PointDetailsState oldState = state.getPointDetailsState();

                if (oldState == null)
                    responseState = newState;
                else {
                    responseState = newState.clone();
                    responseState.removeEqualValue(oldState);
                }

                if (!responseState.isEmpty()) {
                    response.put("pointDetailsState", responseState);
                    state.setPointDetailsState(newState);
                }
            }

            if (pollRequest.isPendingAlarms() && user != null) {
                // Create the list of most current pending alarm content.
                Map<String, Object> model = new HashMap<String, Object>();
                model.put("events", eventDao.getPendingEvents(user.getId()));
                model.put("pendingEvents", true);
                model.put("noContentWhenEmpty", true);
                String currentContent = generateContent(httpRequest, "eventList.jsp", model);
                currentContent = com.serotonin.util.StringUtils.trimWhitespace(currentContent);

                if (!StringUtils.equals(currentContent, state.getPendingAlarmsContent())) {
                    response.put("pendingAlarmsContent", currentContent);
                    state.setPendingAlarmsContent(currentContent);
                }
            }

            // Module handlers
            for (int i = 0; i < longPollHandlers.size(); i++) {
                LongPollHandler handler = longPollHandlers.get(i);
                handler.handleLongPoll(data, response, user);
            }

            if (!response.isEmpty())
                break;

            synchronized (pollRequest) {
                try {
                    pollRequest.wait(waitTime);
                }
                catch (InterruptedException e) {
                    // no op
                }
            }
        }

        if (pollRequest.isTerminated())
            response.put("terminated", true);

        return response;
    }

    @DwrPermission(anonymous = true)
    public void terminateLongPoll(int pollSessionId) {
        terminateLongPollImpl(getLongPollData(pollSessionId, false));
    }

    @DwrPermission(anonymous = true)
    public static void terminateLongPollImpl(LongPollData longPollData) {
        LongPollRequest request = longPollData.getRequest();
        if (request == null)
            return;

        request.setTerminated(true);
        notifyLongPollImpl(request);
    }

    @DwrPermission(anonymous = true)
    public void notifyLongPoll(int pollSessionId) {
        notifyLongPollImpl(getLongPollData(pollSessionId, false).getRequest());
    }

    protected static void notifyLongPollImpl(LongPollRequest request) {
        synchronized (request) {
            request.notifyAll();
        }
    }

    protected LongPollData getLongPollData(int pollSessionId, boolean refreshState) {
        List<LongPollData> dataList = getLongPollData();

        LongPollData data = getDataFromList(dataList, pollSessionId);
        if (data == null) {
            synchronized (dataList) {
                data = getDataFromList(dataList, pollSessionId);
                if (data == null) {
                    data = new LongPollData(pollSessionId);
                    refreshState = true;
                    dataList.add(data);
                }
            }
        }

        if (refreshState)
            data.setState(new LongPollState());

        return data;
    }

    private LongPollData getDataFromList(List<LongPollData> dataList, int pollSessionId) {
        for (LongPollData data : dataList) {
            if (data.getPollSessionId() == pollSessionId)
                return data;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private List<LongPollData> getLongPollData() {
        HttpSession session = WebContextFactory.get().getSession();

        List<LongPollData> data = (List<LongPollData>) session.getAttribute(LONG_POLL_DATA_KEY);
        if (data == null) {
            synchronized (session) {
                data = (List<LongPollData>) session.getAttribute(LONG_POLL_DATA_KEY);
                if (data == null) {
                    data = new ArrayList<LongPollData>();
                    session.setAttribute(LONG_POLL_DATA_KEY, data);
                }
            }
        }

        // Check for old data objects.
        Long lastTimeoutCheck = (Long) session.getAttribute(LONG_POLL_DATA_TIMEOUT_KEY);
        if (lastTimeoutCheck == null)
            lastTimeoutCheck = 0L;
        long cutoff = System.currentTimeMillis() - (1000 * 60 * 5); // Five minutes.
        if (lastTimeoutCheck < cutoff) {
            synchronized (data) {
                Iterator<LongPollData> iter = data.iterator();
                while (iter.hasNext()) {
                    LongPollData lpd = iter.next();
                    if (lpd.getTimestamp() < cutoff)
                        iter.remove();
                }
            }

            session.setAttribute(LONG_POLL_DATA_TIMEOUT_KEY, System.currentTimeMillis());
        }

        return data;
    }

    protected void resetLastAlarmLevelChange() {
        List<LongPollData> data = getLongPollData();

        synchronized (data) {
            // Check if this user has a current long poll request (very likely)
            for (LongPollData lpd : data) {
                LongPollState state = lpd.getState();
                // Reset the last alarm level change time so that the alarm level gets rechecked.
                state.setLastAlarmLevelChange(0);
                // Notify the long poll thread so that any change
                notifyLongPollImpl(lpd.getRequest());
            }
        }
    }

    //
    //
    // Charts and data in a time frame
    //
    protected DateTime createDateTime(int year, int month, int day, int hour, int minute, int second, boolean none,
            DateTimeZone dtz) {
        DateTime dt = null;
        try {
            if (!none)
                dt = new DateTime(year, month, day, hour, minute, second, 0, dtz);
        }
        catch (IllegalFieldValueException e) {
            dt = new DateTime(dtz);
        }
        return dt;
    }
}
