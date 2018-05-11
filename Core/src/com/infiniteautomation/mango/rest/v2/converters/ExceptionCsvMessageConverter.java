/**
 * @copyright 2017 {@link http://infiniteautomation.com|Infinite Automation Systems, Inc.} All rights reserved.
 * @author Terry Packer
 */
package com.infiniteautomation.mango.rest.v2.converters;

import java.io.IOException;
import java.lang.reflect.Type;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.http.converter.json.AbstractJackson2HttpMessageConverter;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.fasterxml.jackson.dataformat.csv.CsvSchema.ColumnType;
import com.infiniteautomation.mango.rest.v2.exception.AbstractRestV2Exception;
import com.serotonin.ShouldNeverHappenException;

/**
 * Class to map Exceptions to CSV output, reading in not supported
 *
 *
 * @author Terry Packer
 */
public class ExceptionCsvMessageConverter extends AbstractJackson2HttpMessageConverter {

    final CsvMapper csvMapper;

    final CsvSchema exceptionSchema;
    final CsvSchema restExceptionSchema;


    public ExceptionCsvMessageConverter() {
        this(new CsvMapper());
    }

    public ExceptionCsvMessageConverter(CsvMapper csvMapper) {
        super(csvMapper, new MediaType("text", "csv"));
        this.csvMapper = csvMapper;

        CsvSchema.Builder builder = CsvSchema.builder();
        builder.setUseHeader(true);
        builder.addColumn("message", ColumnType.STRING);
        builder.addColumn("stackTrace", ColumnType.ARRAY);
        this.exceptionSchema = builder.build();

        builder = CsvSchema.builder();
        builder.setUseHeader(true);
        builder.addColumn("cause", ColumnType.STRING);
        builder.addColumn("mangoStatusCode", ColumnType.NUMBER);
        builder.addColumn("mangoStatusName", ColumnType.STRING);
        builder.addColumn("localizedMessage", ColumnType.STRING);
        this.restExceptionSchema = builder.build();

    }

    /* (non-Javadoc)
     * @see org.springframework.http.converter.json.AbstractJackson2HttpMessageConverter#canWrite(java.lang.Class, org.springframework.http.MediaType)
     */
    @Override
    public boolean canWrite(Class<?> clazz, MediaType mediaType) {
        if (!canWrite(mediaType))
            return false;

        if(Exception.class.isAssignableFrom(clazz))
            return true;
        else
            return false;
    }

    @Override
    protected void writeInternal(Object object, Type type, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {

        MediaType contentType = outputMessage.getHeaders().getContentType();
        JsonEncoding encoding = getJsonEncoding(contentType);
        JsonGenerator generator = this.objectMapper.getFactory().createGenerator(outputMessage.getBody(), encoding);
        try {
            CsvSchema schema;

            if(object instanceof AbstractRestV2Exception) {
                schema = this.restExceptionSchema;
                object = new CsvRestException((AbstractRestV2Exception)object);
            }else {
                schema = this.exceptionSchema;
            }
            writePrefix(generator, object);
            ObjectWriter objectWriter  = this.objectMapper.writer().with(schema);
            objectWriter.writeValue(generator, object);
            writeSuffix(generator, object);
            generator.flush();
        }
        catch (JsonProcessingException ex) {
            throw new HttpMessageNotWritableException("Could not write content: " + ex.getMessage(), ex);
        }
    }

    @Override
    public boolean canRead(Type type, Class<?> contextClass, MediaType mediaType) {
        return false;
    }

    @Override
    protected Object readInternal(Class<?> clazz, HttpInputMessage inputMessage)
            throws IOException, HttpMessageNotReadableException {
        throw new ShouldNeverHappenException("Reading exceptions not supported");
    }

    @Override
    public Object read(Type type, Class<?> contextClass, HttpInputMessage inputMessage)
            throws IOException, HttpMessageNotReadableException {
        throw new ShouldNeverHappenException("Reading exceptions not supported");
    }

    class CsvRestException {

        @JsonProperty
        String cause;
        @JsonProperty
        int mangoStatusCode;
        @JsonProperty
        String mangoStatusName;
        @JsonProperty
        String localizedMessage;

        public CsvRestException(AbstractRestV2Exception e) {
            this.cause = e.getCauseMessage();
            this.mangoStatusCode = e.getMangoStatusCode();
            this.mangoStatusName = e.getMangoStatusName();
            this.localizedMessage = e.getLocalizedMessage();
        }

        /**
         * @return the cause
         */
        public String getCause() {
            return cause;
        }

        /**
         * @param cause the cause to set
         */
        public void setCause(String cause) {
            this.cause = cause;
        }

        /**
         * @return the mangoStatusCode
         */
        public int getMangoStatusCode() {
            return mangoStatusCode;
        }

        /**
         * @param mangoStatusCode the mangoStatusCode to set
         */
        public void setMangoStatusCode(int mangoStatusCode) {
            this.mangoStatusCode = mangoStatusCode;
        }

        /**
         * @return the mangoStatusName
         */
        public String getMangoStatusName() {
            return mangoStatusName;
        }

        /**
         * @param mangoStatusName the mangoStatusName to set
         */
        public void setMangoStatusName(String mangoStatusName) {
            this.mangoStatusName = mangoStatusName;
        }

        /**
         * @return the localizedMessage
         */
        public String getLocalizedMessage() {
            return localizedMessage;
        }

        /**
         * @param localizedMessage the localizedMessage to set
         */
        public void setLocalizedMessage(String localizedMessage) {
            this.localizedMessage = localizedMessage;
        }

    }

}