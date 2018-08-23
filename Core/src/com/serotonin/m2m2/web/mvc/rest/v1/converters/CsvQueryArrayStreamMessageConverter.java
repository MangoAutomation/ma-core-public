/**
 * Copyright (C) 2018 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.web.mvc.rest.v1.converters;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.springframework.core.ResolvableType;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractGenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

import com.serotonin.m2m2.web.MediaTypes;
import com.serotonin.m2m2.web.mvc.rest.v1.csv.CSVPojoWriter;
import com.serotonin.m2m2.web.mvc.rest.v1.model.QueryArrayStream;

import au.com.bytecode.opencsv.CSVWriter;

/**
 * @author Terry Packer
 * @author Jared Wiltshire
 */
public class CsvQueryArrayStreamMessageConverter extends AbstractGenericHttpMessageConverter<QueryArrayStream<?>>
{
    private final char separator, quote;

    public CsvQueryArrayStreamMessageConverter() {
        this(CSVWriter.DEFAULT_SEPARATOR, CSVWriter.DEFAULT_QUOTE_CHARACTER);
    }

    public CsvQueryArrayStreamMessageConverter(char separator, char quote) {
        super(MediaTypes.CSV_V1);
        this.separator = separator;
        this.quote = quote;
    }

    @Override
    public boolean canRead(Type type, Class<?> contextClass, MediaType mediaType) {
        return false;
    }

    @Override
    public boolean canWrite(Type type, Class<?> clazz, MediaType mediaType) {
        return supports(type) && canWrite(mediaType);
    }

    private boolean supports(Type type) {
        ResolvableType resolvedType = ResolvableType.forType(type);

        return resolvedType.getRawClass() != null && QueryArrayStream.class.isAssignableFrom(resolvedType.getRawClass());
    }

    @Override
    public QueryArrayStream<?> read(Type type, Class<?> contextClass, HttpInputMessage inputMessage)
            throws IOException, HttpMessageNotReadableException {

        return null;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    protected void writeInternal(QueryArrayStream<?> stream, Type type, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {

        MediaType contentType = outputMessage.getHeaders().getContentType();
        Charset charset = this.charsetForContentType(contentType);

        try (CSVPojoWriter out = new CSVPojoWriter(new CSVWriter(new OutputStreamWriter(outputMessage.getBody(), charset),separator,quote))) {
            stream.streamData(out);
        }
    }

    @Override
    protected QueryArrayStream<?> readInternal(Class<? extends QueryArrayStream<?>> clazz, HttpInputMessage inputMessage)
            throws IOException, HttpMessageNotReadableException {

        return null; //Not implemented
    }

    private Charset charsetForContentType(MediaType contentType) {
        if (contentType != null) {
            Charset contentTypeCharset = contentType.getCharset();
            if (contentTypeCharset != null) {
                return contentTypeCharset;
            }
        }
        return StandardCharsets.UTF_8;
    }
}