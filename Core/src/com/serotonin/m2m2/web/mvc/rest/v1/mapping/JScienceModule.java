/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.mapping;

import java.io.IOException;

import javax.measure.unit.ProductUnit;
import javax.measure.unit.Unit;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;
import com.serotonin.m2m2.util.UnitUtil;

/**
 * @author Terry Packer
 *
 */
@SuppressWarnings("rawtypes")
public class JScienceModule extends SimpleModule {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public JScienceModule() {
        super("JScience", new Version(0, 0, 1, "SNAPSHOT", "com.infiniteautomation",
				"jscience"));

        addSerializer(Unit.class, new UnitJsonSerializer());
        addDeserializer(Unit.class, new UnitJsonDeserializer());
        addSerializer(ProductUnit.class, new ProductUnitJsonSerializer());
        addDeserializer(ProductUnit.class, new ProductUnitJsonDeserializer());
        
    }

    private class UnitJsonSerializer extends StdScalarSerializer<Unit> {
        protected UnitJsonSerializer() {
            super(Unit.class);
        }

        @Override
        public void serialize(Unit unit, JsonGenerator jgen, SerializerProvider provider) throws IOException {
            if (unit == null) {
                jgen.writeNull();
            }
            else {
                // Format the unit using the standard UCUM representation.
                // The string produced for a given unit is always the same; it is not affected by the locale.
                // It can be used as a canonical string representation for exchanging units.
                String ucumFormattedUnit = UnitUtil.formatUcum(unit);

                jgen.writeString(ucumFormattedUnit);
            }
        }
    }

    private class UnitJsonDeserializer extends StdScalarDeserializer<Unit> {
        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		protected UnitJsonDeserializer() {
            super(Unit.class);
        }

		@Override
        public Unit deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
            JsonToken currentToken = jsonParser.getCurrentToken();

            if (currentToken == JsonToken.VALUE_STRING) {
            	return UnitUtil.parseUcum(jsonParser.getText());
            }
            throw deserializationContext.wrongTokenException(jsonParser,
                    JsonToken.VALUE_STRING,
                    "Expected unit value in String format");
        }
    }

    private class ProductUnitJsonSerializer extends StdScalarSerializer<ProductUnit> {
        protected ProductUnitJsonSerializer() {
            super(ProductUnit.class);
        }

        @Override
        public void serialize(ProductUnit unit, JsonGenerator jgen, SerializerProvider provider) throws IOException {
            if (unit == null) {
                jgen.writeNull();
            }
            else {
                // Format the unit using the standard UCUM representation.
                // The string produced for a given unit is always the same; it is not affected by the locale.
                // It can be used as a canonical string representation for exchanging units.
                String ucumFormattedUnit = UnitUtil.formatUcum(unit);

                jgen.writeString(ucumFormattedUnit);
            }
        }
    }

    private class ProductUnitJsonDeserializer extends StdScalarDeserializer<ProductUnit> {
        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		protected ProductUnitJsonDeserializer() {
            super(ProductUnit.class);
        }

		@Override
        public ProductUnit deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
            JsonToken currentToken = jsonParser.getCurrentToken();

            if (currentToken == JsonToken.VALUE_STRING) {
            	return (ProductUnit) UnitUtil.parseUcum(jsonParser.getText());
            }
            throw deserializationContext.wrongTokenException(jsonParser,
                    JsonToken.VALUE_STRING,
                    "Expected unit value in String format");
        }
    }
}
