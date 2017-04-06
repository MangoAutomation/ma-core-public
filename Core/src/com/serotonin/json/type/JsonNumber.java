package com.serotonin.json.type;

import java.math.BigDecimal;
import java.math.BigInteger;

public class JsonNumber extends JsonValue {
    private final BigDecimal delegate;

    public JsonNumber(BigDecimal delegate) {
        this.delegate = delegate;
    }

    public byte byteValue() {
        return delegate.byteValue();
    }

    public short shortValue() {
        return delegate.shortValue();
    }

    public int intValue() {
        return delegate.intValue();
    }

    public long longValue() {
        return delegate.longValue();
    }

    public float floatValue() {
        return delegate.floatValue();
    }

    public double doubleValue() {
        return delegate.doubleValue();
    }

    public BigDecimal bigDecimalValue() {
        return delegate;
    }

    public BigInteger bigIntegerValue() {
        return delegate.toBigInteger();
    }

    @Override
    public String toString() {
        return delegate.toString();
    }
}
