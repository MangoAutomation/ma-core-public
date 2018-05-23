/**
 * Copyright (C) 2018 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.web.mvc.rest.v1.converters;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.core.ResolvableType;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractGenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.web.mvc.rest.v1.csv.CSVPojoReader;
import com.serotonin.m2m2.web.mvc.rest.v1.csv.CSVPojoWriter;
import com.serotonin.m2m2.web.mvc.rest.v1.model.AbstractRestModel;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

/**
 * @author Terry Packer
 * @author Jared Wiltshire
 */
public class CsvMessageConverter extends AbstractGenericHttpMessageConverter<List<AbstractRestModel<?>>> {
    private final char separator, quote;

    public CsvMessageConverter() {
        this(CSVWriter.DEFAULT_SEPARATOR, CSVWriter.DEFAULT_QUOTE_CHARACTER);
    }

    public CsvMessageConverter(char separator, char quote) {
        super(Common.MediaTypes.CSV_V1);
        this.separator = separator;
        this.quote = quote;
    }

    @Override
    public boolean canRead(Type type, Class<?> contextClass, MediaType mediaType) {
        return supports(type) && canRead(mediaType);
    }

    @Override
    public boolean canWrite(Type type, Class<?> clazz, MediaType mediaType) {
        return supports(type) && canWrite(mediaType);
    }

    @Override
    public List<AbstractRestModel<?>> read(Type type, Class<?> contextClass, HttpInputMessage inputMessage)
            throws IOException, HttpMessageNotReadableException {

        return readInternal(null, inputMessage);
    }

    private boolean supports(Type type) {
        ResolvableType resolvedType = ResolvableType.forType(type);
        ResolvableType supportedType = ResolvableType.forClassWithGenerics(List.class, AbstractRestModel.class);

        return supportedType.isAssignableFrom(resolvedType);
    }

    @Override
    protected List<AbstractRestModel<?>> readInternal(Class<? extends List<AbstractRestModel<?>>> clazz, HttpInputMessage inputMessage)
            throws IOException, HttpMessageNotReadableException {

        MediaType contentType = inputMessage.getHeaders().getContentType();
        Charset charset = this.charsetForContentType(contentType);

        List<AbstractRestModel<?>> records;
        try (CSVPojoReader<AbstractRestModel<?>> in = new CSVPojoReader<AbstractRestModel<?>>(new CSVReader(new InputStreamReader(inputMessage.getBody(), charset), separator, quote))) {
            records = in.readAll();
        }
        return records;
    }

    @Override
    protected void writeInternal(List<AbstractRestModel<?>> records, Type type, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {

        MediaType contentType = outputMessage.getHeaders().getContentType();
        Charset charset = this.charsetForContentType(contentType);

        if (records.size() > 0) {
            try (CSVPojoWriter<AbstractRestModel<?>> out = new CSVPojoWriter<AbstractRestModel<?>>(new CSVWriter(new OutputStreamWriter(outputMessage.getBody(), charset), separator, quote))) {
                out.writeAll(records);
            }
        }
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