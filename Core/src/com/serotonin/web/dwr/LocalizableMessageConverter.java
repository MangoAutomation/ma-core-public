/*
    Copyright (C) 2006-2007 Serotonin Software Technologies Inc.
 	@author Matthew Lohbihler
 */
package com.serotonin.web.dwr;

import org.directwebremoting.WebContext;
import org.directwebremoting.WebContextFactory;
import org.directwebremoting.convert.StringConverter;
import org.directwebremoting.extend.MarshallException;
import org.directwebremoting.extend.OutboundContext;
import org.directwebremoting.extend.OutboundVariable;

import com.serotonin.web.i18n.I18NUtils;
import com.serotonin.web.i18n.LocalizableMessage;

/**
 * @author Matthew Lohbihler
 */
public class LocalizableMessageConverter extends StringConverter {
    @Override
    public OutboundVariable convertOutbound(Object data, OutboundContext outctx) throws MarshallException {
        WebContext webctx = WebContextFactory.get();
        LocalizableMessage lm = (LocalizableMessage)data;
        String s = lm.getLocalizedMessage(I18NUtils.getBundle(webctx.getHttpServletRequest()));
        return super.convertOutbound(s, outctx);
    }
}
