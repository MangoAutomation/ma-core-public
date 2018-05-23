/**
 * Copyright (C) 2018 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.web.mvc.rest.v1.converters;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;

import org.springframework.core.ResolvableType;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractGenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.web.mvc.rest.v1.csv.CSVPojoWriter;
import com.serotonin.m2m2.web.mvc.rest.v1.model.QueryArrayStream;
import com.serotonin.m2m2.web.mvc.rest.v1.model.QueryDataPageStream;

import au.com.bytecode.opencsv.CSVWriter;

/**
 * @author Terry Packer
 * @author Jared Wiltshire
 */
public class CsvDataPageQueryStreamMessageConverter extends AbstractGenericHttpMessageConverter<QueryDataPageStream<?>>
{
    public static final MediaType MEDIA_TYPE = new MediaType("text", "csv");
    private final char separator, quote;

    public CsvDataPageQueryStreamMessageConverter() {
        this(CSVWriter.DEFAULT_SEPARATOR, CSVWriter.DEFAULT_QUOTE_CHARACTER);
    }

    public CsvDataPageQueryStreamMessageConverter(char separator, char quote) {
        super(MEDIA_TYPE);
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
        return QueryArrayStream.class.isAssignableFrom(resolvedType.getRawClass());
    }

    @Override
    public QueryDataPageStream<?> read(Type type, Class<?> contextClass, HttpInputMessage inputMessage)
            throws IOException, HttpMessageNotReadableException {

        return null;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    protected void writeInternal(QueryDataPageStream<?> stream, Type type, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {

        CSVPojoWriter out = new CSVPojoWriter(new CSVWriter(new OutputStreamWriter(outputMessage.getBody(), Common.UTF8_CS),separator,quote));
        stream.streamData(out);
        out.close();
    }

    @Override
    protected QueryDataPageStream<?> readInternal(Class<? extends QueryDataPageStream<?>> clazz, HttpInputMessage inputMessage)
            throws IOException, HttpMessageNotReadableException {

        return null; //Not implemented
    }

}