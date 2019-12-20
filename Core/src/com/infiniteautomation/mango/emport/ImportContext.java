/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.infiniteautomation.mango.emport;

import java.util.concurrent.CopyOnWriteArrayList;

import com.infiniteautomation.mango.spring.service.MailingListService;
import com.infiniteautomation.mango.spring.service.UsersService;
import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.db.dao.DataSourceDao;
import com.serotonin.m2m2.db.dao.EventDao;
import com.serotonin.m2m2.db.dao.EventHandlerDao;
import com.serotonin.m2m2.db.dao.MailingListDao;
import com.serotonin.m2m2.db.dao.PublisherDao;
import com.serotonin.m2m2.i18n.ProcessMessage;
import com.serotonin.m2m2.i18n.ProcessMessage.Level;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableJsonException;
import com.serotonin.m2m2.i18n.Translations;
import com.serotonin.m2m2.vo.event.AbstractEventHandlerVO;
import com.serotonin.m2m2.vo.publish.PublishedPointVO;

public class ImportContext {
    
    private final UsersService usersService;
    private final MailingListService mailingListService;
    
    private final DataSourceDao<?> dataSourceDao = DataSourceDao.getInstance();
    private final DataPointDao dataPointDao = DataPointDao.getInstance();
    private final EventDao eventDao = EventDao.getInstance();
    private final MailingListDao mailingListDao = MailingListDao.getInstance();
    private final PublisherDao<? extends PublishedPointVO> publisherDao = PublisherDao.getInstance();
    private final EventHandlerDao<AbstractEventHandlerVO<?>> eventHandlerDao = EventHandlerDao.getInstance();

    private final JsonReader reader;
    private final ProcessResult result;
    private final Translations translations;

    public ImportContext(JsonReader reader, ProcessResult result, Translations translations) {
        this.usersService = Common.getBean(UsersService.class);
        this.mailingListService = Common.getBean(MailingListService.class);
        this.reader = reader;
        this.result = result;
        this.translations = translations;

        result.setMessages(new CopyOnWriteArrayList<ProcessMessage>());
    }

    public UsersService getUsersService() {
        return this.usersService;
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

    public DataSourceDao<?> getDataSourceDao() {
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

    public MailingListService getMailingListService() {
        return mailingListService;
    }
    
    public PublisherDao<?> getPublisherDao() {
        return publisherDao;
    }

	public EventHandlerDao<AbstractEventHandlerVO<?>> getEventHandlerDao() {
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
        return msg;
    }
}
