/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.i18n;

import java.text.MessageFormat;
import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;

public class TranslatedMessageSource implements MessageSource {
    @Override
    public String getMessage(String code, Object[] args, String defaultMessage, Locale locale) {
        String t = Translations.getTranslations(locale).translateAllowNull(code);
        if (t == null)
            return defaultMessage;
        return MessageFormat.format(t, args);
    }

    @Override
    public String getMessage(String code, Object[] args, Locale locale) throws NoSuchMessageException {
        return TranslatableMessage.translate(Translations.getTranslations(locale), code, args);
    }

    @Override
    public String getMessage(MessageSourceResolvable resolvable, Locale locale) throws NoSuchMessageException {
        Translations translations = Translations.getTranslations(locale);
        for (String key : resolvable.getCodes()) {
            String t = translations.translateAllowNull(key);
            if (t != null)
                return MessageFormat.format(t, resolvable.getArguments());
        }

        return resolvable.getDefaultMessage();
    }
}
