/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.web.dwr.emport;

import java.util.concurrent.CopyOnWriteArrayList;

import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.db.dao.DataSourceDao;
import com.serotonin.m2m2.db.dao.EventDao;
import com.serotonin.m2m2.db.dao.EventHandlerDao;
import com.serotonin.m2m2.db.dao.MailingListDao;
import com.serotonin.m2m2.db.dao.PublisherDao;
import com.serotonin.m2m2.db.dao.UserDao;
import com.serotonin.m2m2.i18n.ProcessMessage;
import com.serotonin.m2m2.i18n.ProcessMessage.Level;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableJsonException;
import com.serotonin.m2m2.i18n.Translations;

public class ImportContext {
    private final UserDao userDao = UserDao.instance;
    private final DataSourceDao dataSourceDao = DataSourceDao.instance;
    private final DataPointDao dataPointDao = DataPointDao.instance;
    private final EventDao eventDao = EventDao.instance;
    private final MailingListDao mailingListDao = MailingListDao.instance;
    private final PublisherDao publisherDao = PublisherDao.instance;
    private final EventHandlerDao eventHandlerDao = EventHandlerDao.instance;

    private final JsonReader reader;
    private final ProcessResult result;
    private final Translations translations;

    public ImportContext(JsonReader reader, ProcessResult result, Translations translations) {
        this.reader = reader;
        this.result = result;
        this.translations = translations;

        result.setMessages(new CopyOnWriteArrayList<ProcessMessage>());
    }

    public JsonReader getReader() {
        return reader;
    }

    public ProcessResult getResult() {
        return result;
    }

    public Translations getTranslations() {
        return translations;
    }

    public UserDao getUserDao() {
        return userDao;
    }

    public DataSourceDao getDataSourceDao() {
        return dataSourceDao;
    }

    public DataPointDao getDataPointDao() {
        return dataPointDao;
    }

    public EventDao getEventDao() {
        return eventDao;
    }

    public MailingListDao getMailingListDao() {
        return mailingListDao;
    }

    public PublisherDao getPublisherDao() {
        return publisherDao;
    }
	/**
	 * @return
	 */
	public EventHandlerDao getEventHandlerDao() {
		return eventHandlerDao;
	}

    public void copyValidationMessages(ProcessResult voResponse, String key, String desc) {
        for (ProcessMessage msg : voResponse.getMessages())
            result.addGenericMessage(key, desc, msg.toString(translations));
    }

    public void addSuccessMessage(boolean isnew, String key, String desc) {
        if (isnew)
            result.addGenericMessage(Level.info, key, desc, translations.translate("emport.added"));
        else
            result.addGenericMessage(Level.info, key, desc, translations.translate("emport.saved"));
    }

    public String getJsonExceptionMessage(JsonException e) {
        String msg = "'" + e.getMessage() + "'";
        Throwable t = e;
        while ((t = t.getCause()) != null) {
            if (t instanceof TranslatableJsonException)
                msg += ", " + translations.translate("emport.causedBy") + " '"
                        + ((TranslatableJsonException) t).getMsg().translate(translations) + "'";
            else
                msg += ", " + translations.translate("emport.causedBy") + " '" + t.getMessage() + "'";
        }

        // Throwable t = e;
        // while (t.getCause() != null)
        // t = t.getCause();
        // String msg = msgPrefix + t.getMessage();

        return msg;
    }
}
