package com.serotonin.json.util;

public class Utils {
    public static boolean isEmpty(CharSequence cs) {
        return cs == null || cs.length() == 0;
    }

    public static boolean isBlank(CharSequence cs) {
        int strLen;
        if (cs == null || (strLen = cs.length()) == 0)
            return true;
        for (int i = 0; i < strLen; i++)
            if (!Character.isWhitespace(cs.charAt(i)))
                return false;

        return true;
    }

    public static boolean equals(CharSequence cs1, CharSequence cs2) {
        return cs1 != null ? cs1.equals(cs2) : cs2 == null;
    }

    public static int indexOf(Object array[], Object objectToFind) {
        return indexOf(array, objectToFind, 0);
    }

    public static int indexOf(Object array[], Object objectToFind, int startIndex) {
        if (array == null)
            return -1;
        if (startIndex < 0)
            startIndex = 0;
        if (objectToFind == null) {
            for (int i = startIndex; i < array.length; i++)
                if (array[i] == null)
                    return i;

        }
        else if (((Object) (array)).getClass().getComponentType().isInstance(objectToFind)) {
            for (int i = startIndex; i < array.length; i++)
                if (objectToFind.equals(array[i]))
                    return i;

        }
        return -1;
    }

    public static boolean contains(Object array[], Object objectToFind) {
        return indexOf(array, objectToFind) != -1;
    }
}
