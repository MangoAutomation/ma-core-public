package com.serotonin.propertyEditor;

import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.MessageCodesResolver;

@Deprecated
public class DefaultMessageCodesResolver implements MessageCodesResolver {
    public String[] resolveMessageCodes(String errorCode, String objectName, String field,
            @SuppressWarnings("rawtypes") Class fieldType) {
        if ("typeMismatch".equals(errorCode)) {
            if (fieldType == Double.TYPE)
                return new String[] { "badDecimalFormat" };
            if (fieldType == Integer.TYPE)
                return new String[] { "badIntegerFormat" };
        }
        if (StringUtils.isBlank(errorCode))
            return new String[0];
        return new String[] { errorCode };
    }

    public String[] resolveMessageCodes(String errorCode, String objectName) {
        return resolveMessageCodes(errorCode, objectName, null, null);
    }
}
