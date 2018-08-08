/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.web.dwr;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.directwebremoting.WebContext;
import org.directwebremoting.WebContextFactory;
import org.springframework.beans.propertyeditors.LocaleEditor;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

import com.infiniteautomation.mango.spring.dao.UserDao;
import com.serotonin.io.StreamUtils;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.EventDao;
import com.serotonin.m2m2.db.dao.MailingListDao;
import com.serotonin.m2m2.email.MangoEmailContent;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.i18n.Translations;
import com.serotonin.m2m2.rt.event.EventInstance;
import com.serotonin.m2m2.rt.maint.work.EmailWorkItem;
import com.serotonin.m2m2.util.DocumentationItem;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.web.dwr.beans.RecipientListEntryBean;
import com.serotonin.m2m2.web.dwr.util.DwrPermission;

public class MiscDwr extends BaseDwr {
    public static final Log LOG = LogFactory.getLog(MiscDwr.class);

    @DwrPermission(anonymous = true)
    public ProcessResult toggleSilence(int eventId) {
        ProcessResult response = new ProcessResult();
        response.addData("eventId", eventId);

        User user = Common.getHttpUser();
        if (user != null) {
            boolean result = Common.eventManager.toggleSilence(eventId, user.getId());
            resetLastAlarmLevelChange();
            response.addData("silenced", result);
        }
        else
            response.addData("silenced", false);

        return response;
    }

    @DwrPermission(anonymous = true)
    public ProcessResult silenceAll() {
        List<Integer> silenced = new ArrayList<Integer>();
        User user = Common.getHttpUser();
        if (user != null) {
            EventDao eventDao = EventDao.instance;
            for (EventInstance evt : eventDao.getPendingEvents(user.getId())) {
                if (!evt.isSilenced()) {
                    Common.eventManager.toggleSilence(evt.getId(), user.getId());
                    silenced.add(evt.getId());
                }
            }

            resetLastAlarmLevelChange();
        }

        ProcessResult response = new ProcessResult();
        response.addData("silenced", silenced);
        return response;
    }

    @DwrPermission(anonymous = true)
    public int acknowledgeEvent(int eventId) {
        User user = Common.getHttpUser();
        if (user != null) {
            Common.eventManager.acknowledgeEventById(eventId, Common.timer.currentTimeMillis(), user, null);
            resetLastAlarmLevelChange();
        }
        return eventId;
    }

    @DwrPermission(anonymous = true)
    public void acknowledgeAllPendingEvents() {
        User user = Common.getHttpUser();
        if (user != null) {
            EventDao eventDao = EventDao.instance;
            long now = Common.timer.currentTimeMillis();
            for (EventInstance evt : eventDao.getPendingEvents(user.getId()))
                Common.eventManager.acknowledgeEventById(evt.getId(), now, user, null);
            resetLastAlarmLevelChange();
        }
    }

    @DwrPermission(anonymous = true)
    public boolean toggleUserMuted() {
        User user = Common.getHttpUser();
        if (user != null) {
            user.setMuted(!user.isMuted());
            UserDao.instance.saveMuted(user.getId(), user.isMuted());
            return user.isMuted();
        }
        return false;
    }

    @DwrPermission(anonymous = true)
    public Map<String, Object> getDocumentationItem(String documentId) {
        Map<String, Object> result = new HashMap<String, Object>();

        DocumentationItem item = Common.documentationManifest.getItem(documentId);
        if (item == null)
            result.put("error", translate("dox.idNotFound", documentId));
        else {
            // Find the file appropriate for the locale.
            Locale locale = getLocale();
            File file = Common.documentationManifest.getDocumentationFile(item, locale.getLanguage(),
                    locale.getCountry(), locale.getVariant());

            // Read the content.
            try {
                Reader in = new InputStreamReader(new FileInputStream(file), Charset.forName("UTF-8"));
                StringWriter out = new StringWriter();
                StreamUtils.transfer(in, out);
                in.close();

                addDocumentationItem(result, item);
                result.put("content", out.toString());

                List<Map<String, Object>> related = new ArrayList<Map<String, Object>>();
                for (String relatedId : item.getRelated()) {
                    Map<String, Object> map = new HashMap<String, Object>();
                    related.add(map);
                    DocumentationItem relatedItem = Common.documentationManifest.getItem(relatedId);
                    if (relatedItem == null)
                        throw new RuntimeException("Related document '" + relatedId + "' not found");
                    addDocumentationItem(map, relatedItem);
                }

                result.put("relatedList", related);
            }
            catch (FileNotFoundException e) {
                result.put("error", translate("dox.fileNotFound", file.getPath()));
            }
            catch (IOException e) {
                result.put("error", translate("dox.readError", e.getClass().getName(), e.getMessage()));
            }
        }

        return result;
    }

    private void addDocumentationItem(Map<String, Object> map, DocumentationItem di) {
        map.put("id", di.getId());
        map.put("title", translate(di.getKey()));
    }

    @DwrPermission(anonymous = true)
    public void jsError(String desc, String page, String line, String browserName, String browserVersion,
            String osName, String location) {
        LOG.warn("Javascript error\r\n" + "   Description: " + desc + "\r\n" + "   Page: " + page + "\r\n"
                + "   Line: " + line + "\r\n" + "   Browser name: " + browserName + "\r\n" + "   Browser version: "
                + browserVersion + "\r\n" + "   osName: " + osName + "\r\n" + "   location: " + location);
    }

    @DwrPermission(user = true)
    public ProcessResult sendTestEmail(List<RecipientListEntryBean> recipientList, String prefix, String message) {
        ProcessResult response = new ProcessResult();

        String[] toAddrs = MailingListDao.instance.getRecipientAddresses(recipientList, null).toArray(new String[0]);
        if (toAddrs.length == 0)
            response.addGenericMessage("js.email.noRecipForEmail");
        else {
            try {
                Translations translations = Common.getTranslations();
                Map<String, Object> model = new HashMap<String, Object>();
                model.put("user", Common.getHttpUser());
                model.put("message", new TranslatableMessage("common.default", message));
                MangoEmailContent cnt = new MangoEmailContent("testEmail", model, translations,
                        translations.translate("ftl.testEmail"), Common.UTF8);
                EmailWorkItem.queueEmail(toAddrs, cnt);
            }
            catch (Exception e) {
                response.addGenericMessage("common.default", e.getMessage());
            }
        }

        response.addData("prefix", prefix);

        return response;
    }

    @DwrPermission(anonymous = true)
    public void setLocale(String locale) {
        WebContext webContext = WebContextFactory.get();

        LocaleResolver localeResolver = new SessionLocaleResolver();

        LocaleEditor localeEditor = new LocaleEditor();
        localeEditor.setAsText(locale);

        localeResolver.setLocale(webContext.getHttpServletRequest(), webContext.getHttpServletResponse(),
                (Locale) localeEditor.getValue());
    }

    @DwrPermission(user = true)
    public void setHomeUrl(String url) {
        // Remove the scheme, domain, and context if there.
        HttpServletRequest request = WebContextFactory.get().getHttpServletRequest();

        // Remove the scheme.
        url = url.substring(request.getScheme().length() + 3);

        // Remove the domain.
        url = url.substring(request.getServerName().length());

        // Remove the port
        if (url.charAt(0) == ':')
            url = url.substring(Integer.toString(request.getServerPort()).length() + 1);

        //        // Remove the context
        //        url = url.substring(request.getContextPath().length());
        //
        //        // Remove any leading /
        //        if (url.charAt(0) == '/')
        //            url = url.substring(1);

        // Save the result
        User user = Common.getHttpUser();
        UserDao.instance.saveHomeUrl(user.getId(), url);
        user.setHomeUrl(url);
    }

    @DwrPermission(user = true)
    public void deleteHomeUrl() {
        User user = Common.getHttpUser();
        UserDao.instance.saveHomeUrl(user.getId(), null);
        user.setHomeUrl(null);
    }

    @DwrPermission(user = true)
    public String getHomeUrl() {
        String url = Common.getHttpUser().getHomeUrl();
        if (StringUtils.isBlank(url))
            url = "help.shtm";
        return url;
    }
}
