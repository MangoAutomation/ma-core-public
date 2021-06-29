/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.i18n;

import java.io.Serializable;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Matthew Lohbihler
 */
public class TranslatableMessage implements Serializable {

    private static final long serialVersionUID = 8466485929018594868L;

    public static String translate(Translations translations, String key) {
        return translations.translate(key);
    }

    public static String translate(Translations translations, String key, Object... args) {
        if (args == null || args.length == 0)
            return translate(translations, key);
        return new TranslatableMessage(key, args).translate(translations);
    }

    private final String key;
    private final Object[] args;

    public TranslatableMessage(String key) {
        this(key, (Object[]) null);
    }

    public TranslatableMessage(String key, Object... args) {
        if (key == null)
            throw new NullPointerException("key cannot be null");

        this.key = key;

        if (args != null) {
            this.args = new Object[args.length];
            for (int i = 0; i < args.length; i++) {
                if (args[i] == null)
                    this.args[i] = "";
                else if (args[i] instanceof TranslatableMessage)
                    this.args[i] = args[i];
                else if(args[i] instanceof Object[])
                    this.args[i] = Arrays.toString((Object[])args[i]);
                else
                    this.args[i] = args[i].toString();
            }
        }
        else
            this.args = new Object[0];
    }

    public String translate(Translations translations) {
        if (translations == null)
            return "?x?" + key + "?x?";
        return translateImpl(translations, this);
    }

    private static String translateImpl(Translations translations, TranslatableMessage lm) {
        // Resolve any args that are themselves localizable messages to strings.
        Object[] resolvedArgs = new Object[lm.args.length];
        for (int i = 0; i < resolvedArgs.length; i++) {
            if (lm.args[i] instanceof TranslatableMessage)
                resolvedArgs[i] = translateImpl(translations, (TranslatableMessage) lm.args[i]);
            else
                resolvedArgs[i] = lm.args[i];
        }

        String pattern = translations.translate(lm.key);
        return MessageFormat.format(pattern, resolvedArgs);
    }

    public String getKey() {
        return key;
    }

    public Object[] getArgs() {
        return args;
    }

    public String serialize() {
        return serializeImpl(this, false);
    }

    private static String serializeImpl(TranslatableMessage lm, boolean nested) {
        StringBuilder sb = new StringBuilder();
        if (nested)
            sb.append('[');
        sb.append(encodeString(lm.key));
        if (lm.args != null) {
            for (Object o : lm.args) {
                if (o instanceof TranslatableMessage)
                    sb.append(serializeImpl((TranslatableMessage) o, true));
                else
                    sb.append(encodeString((String) o));
            }
        }
        if (nested)
            sb.append(']');
        return sb.toString();
    }

    public static TranslatableMessage deserialize(String s) throws TranslatableMessageParseException {
        return deserializeImpl(new StringBuilder(s));
    }

    private static TranslatableMessage deserializeImpl(StringBuilder sb) throws TranslatableMessageParseException {
        int pos = 0;
        String key = null;
        List<Object> args = new ArrayList<Object>();
        while (true) {
            if (sb.length() == 0)
                throw new TranslatableMessageParseException("Invalid localizable message encoding");

            if (sb.charAt(0) == '[') {
                // nested message
                sb.deleteCharAt(0);
                args.add(deserializeImpl(sb));

                if (sb.length() == 0)
                    break;
            }
            else if (sb.charAt(0) == ']') {
                // end of nested message
                sb.deleteCharAt(0);
                break;
            }
            else {
                pos = sb.indexOf("|", pos);
                if (pos == -1)
                    throw new TranslatableMessageParseException("Invalid localizable message encoding");
                else if (pos == 0 || sb.charAt(pos - 1) != '\\') {
                    String str = decodeString(sb.substring(0, pos + 1));

                    if (key == null)
                        key = str;
                    else
                        args.add(str);

                    sb.delete(0, pos + 1);
                    if (sb.length() == 0)
                        break;

                    pos = 0;
                }
                else
                    pos++;
            }
        }

        Object[] a = new Object[args.size()];
        args.toArray(a);
        return new TranslatableMessage(key, a);
    }

    private static String encodeString(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 10);

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '|':
                    sb.append("\\|");
                    break;
                case '[':
                    sb.append("\\[");
                    break;
                case ']':
                    sb.append("\\]");
                    break;
                default:
                    sb.append(c);
            }
        }

        sb.append('|');

        return sb.toString();
    }

    private static String decodeString(String s) {
        StringBuilder sb = new StringBuilder(s.length());

        int i = 0;
        int l = s.length() - 1;
        while (i < l) {
            char c1 = s.charAt(i);
            if (c1 == '\\') {
                char c2 = s.charAt(i + 1);
                switch (c2) {
                    case '|':
                    case '[':
                    case ']':
                        c1 = c2;
                        i++;
                        break;
                }
            }

            sb.append(c1);
            i++;
        }

        return sb.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(args);
        result = prime * result + ((key == null) ? 0 : key.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final TranslatableMessage other = (TranslatableMessage) obj;
        if (!Arrays.equals(args, other.args))
            return false;
        if (key == null) {
            if (other.key != null)
                return false;
        }
        else if (!key.equals(other.key))
            return false;
        return true;
    }
}
