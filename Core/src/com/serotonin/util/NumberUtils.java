package com.serotonin.util;

public class NumberUtils {
    public static String countDescription(long count) {
        StringBuilder sb = new StringBuilder();

        if (count < 0) {
            sb.append("-");
            count = -count;
        }

        if (count < 1000)
            sb.append(count);
        else {
            count /= 1000;
            if (count < 1000)
                sb.append(count).append(" K");
            else {
                count /= 1000;
                if (count < 1000)
                    sb.append(count).append(" M");
                else {
                    count /= 1000;
                    if (count < 1000)
                        sb.append(count).append(" G");
                    else {
                        count /= 1000;
                        sb.append(count).append(" T");
                    }
                }
            }
        }

        return sb.toString();
    }
}
