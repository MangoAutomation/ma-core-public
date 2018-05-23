/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.converters;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.web.mvc.rest.v1.csv.CSVPojoReader;
import com.serotonin.m2m2.web.mvc.rest.v1.csv.CSVPojoWriter;
import com.serotonin.m2m2.web.mvc.rest.v1.model.AbstractRestModel;
import com.serotonin.m2m2.web.mvc.rest.v1.model.AbstractVoModel;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

/**
 * @author Terry Packer
 *
 */
public class CsvRowMessageConverter extends
AbstractHttpMessageConverter<AbstractRestModel<?>> {

    private final char separator, quote;

    public CsvRowMessageConverter() {
        this(CSVWriter.DEFAULT_SEPARATOR, CSVWriter.DEFAULT_QUOTE_CHARACTER);
    }

    public CsvRowMessageConverter(char separator, char quote) {
        super(Common.MediaTypes.CSV);
        this.separator = separator;
        this.quote = quote;
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        return AbstractRestModel.class.isAssignableFrom(clazz);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.springframework.http.converter.AbstractHttpMessageConverter#readInternal
     * (java.lang.Class, org.springframework.http.HttpInputMessage)
     */
    @SuppressWarnings({ "rawtypes"})
    @Override
    protected AbstractVoModel<?> readInternal(
            Class<? extends AbstractRestModel<?>> clazz,
                    HttpInputMessage inputMessage) throws IOException,
    HttpMessageNotReadableException {
        CSVPojoReader in = new CSVPojoReader(new CSVReader(new InputStreamReader(inputMessage.getBody(), Common.UTF8_CS),separator, quote));
        AbstractVoModel<?> record = (AbstractVoModel<?>) in.readNext();
        in.close();
        return record;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.springframework.http.converter.AbstractHttpMessageConverter#writeInternal
     * (java.lang.Object, org.springframework.http.HttpOutputMessage)
     */
    @SuppressWarnings({ "rawtypes" })
    @Override
    protected void writeInternal(AbstractRestModel<?> record,
            HttpOutputMessage outputMessage) throws IOException,
    HttpMessageNotWritableException {
        CSVPojoWriter out = new CSVPojoWriter(
                new CSVWriter(new OutputStreamWriter(outputMessage.getBody(),
                        Common.UTF8_CS), separator, quote));
        out.writeNext(record);
        out.close();
    }

}