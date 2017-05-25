/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.i18n;

import java.util.Locale;

import org.directwebremoting.convert.StringConverter;
import org.directwebremoting.extend.MarshallException;
import org.directwebremoting.extend.OutboundContext;
import org.directwebremoting.extend.OutboundVariable;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.vo.User;

/**
 * @author Matthew Lohbihler
 */
public class TranslatableMessageConverter extends StringConverter {
    @Override
    public OutboundVariable convertOutbound(Object data, OutboundContext outctx) throws MarshallException {
		User user = Common.getHttpUser();
		Locale locale = Locale.forLanguageTag(user.getLocale());
		if(locale == null)
			locale = Common.getLocale();
        TranslatableMessage message = (TranslatableMessage) data;
        String s = message.translate(Translations.getTranslations(locale));
        return super.convertOutbound(s, outctx);
    }
}
