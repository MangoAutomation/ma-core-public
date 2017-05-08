package com.serotonin.util;

import java.io.IOException;
import java.io.Writer;

public class XmlStreamUtils {
    private final Writer out;
    private boolean elementStarted = false;
    private final StringBuilder sb = new StringBuilder();

    public XmlStreamUtils(Writer out) {
        this.out = out;
    }

    public void writeDeclaration() throws IOException {
        out.write("<?xml version=\"1.0\"?>");
    }

    public void startElement(String tagName) throws IOException {
        if (elementStarted)
            out.append(">");
        out.append("<").append(tagName);
        elementStarted = true;
    }

    public void writeAttribute(String name, String value) throws IOException {
        if (!elementStarted)
            throw new IllegalStateException("");
        out.append(" ").append(name).append("=\"").append(encode(value)).append("\"");
    }

    public void writeText(String text) throws IOException {
        if (text != null) {
            if (elementStarted)
                out.append(">");
            out.append(encode(text));
            elementStarted = false;
        }
    }

    public void writeTextElement(String tagName, String text) throws IOException {
        startElement(tagName);
        writeText(text);
        endElement(tagName);
    }

    public void endElement(String tagName) throws IOException {
        if (elementStarted)
            out.append("/>");
        else
            out.append("</").append(tagName).append(">");
        elementStarted = false;
    }

    private String encode(String s) {
        if (s == null)
            return "";

        boolean encoded = false;
        String replacement;

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            replacement = null;

            if (c < 0x9 || (c > 0x9 && c < 0xa) || (c > 0xa && c < 0xd) || (c > 0xd && c < 0x20))
                replacement = "?";

            switch (c) {
            case '<':
                replacement = "&lt;";
                break;
            case '>':
                replacement = "&gt;";
                break;
            case '&':
                replacement = "&amp;";
                break;
            case '"':
                replacement = "&quot;";
                break;
            }

            if (replacement != null) {
                if (!encoded) {
                    sb.setLength(0);
                    sb.append(s.substring(0, i));
                    encoded = true;
                }
                sb.append(replacement);
            }
            else if (encoded)
                sb.append(c);
        }

        if (!encoded)
            return s;
        return sb.toString();
    }
}
