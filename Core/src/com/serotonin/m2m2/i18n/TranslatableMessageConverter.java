/*
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.i18n;

import java.util.Locale;

import org.directwebremoting.WebContextFactory;
import org.directwebremoting.convert.StringConverter;
import org.directwebremoting.extend.MarshallException;
import org.directwebremoting.extend.OutboundContext;
import org.directwebremoting.extend.OutboundVariable;

import com.serotonin.m2m2.web.mvc.controller.ControllerUtils;

/**
 * @author Matthew Lohbihler
 */
public class TranslatableMessageConverter extends StringConverter {
    @Override
    public OutboundVariable convertOutbound(Object data, OutboundContext outctx) throws MarshallException {
        Locale locale = ControllerUtils.getLocale(WebContextFactory.get().getHttpServletRequest());
        TranslatableMessage message = (TranslatableMessage) data;
        String s = message.translate(Translations.getTranslations(locale));
        return super.convertOutbound(s, outctx);
    }
}
