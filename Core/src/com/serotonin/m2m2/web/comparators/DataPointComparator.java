/*
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.web.comparators;

import com.serotonin.m2m2.i18n.Translations;
import com.serotonin.m2m2.vo.DataPointVO;

public class DataPointComparator extends BaseComparator<DataPointVO> {
    private static final int SORT_NAME = 1;
    private static final int SORT_DS_NAME = 2;
    private static final int SORT_ENABLED = 4;
    private static final int SORT_DATA_TYPE = 5;
    private static final int SORT_CONFIG = 6;

    private final Translations translations;

    public DataPointComparator(Translations translations, String sortField, boolean descending) {
        this.translations = translations;

        if ("name".equals(sortField))
            sortType = SORT_NAME;
        else if ("dsName".equals(sortField))
            sortType = SORT_DS_NAME;
        else if ("enabled".equals(sortField))
            sortType = SORT_ENABLED;
        else if ("dataType".equals(sortField))
            sortType = SORT_DATA_TYPE;
        else if ("config".equals(sortField))
            sortType = SORT_CONFIG;
        this.descending = descending;
    }

    public int compare(DataPointVO dp1, DataPointVO dp2) {
        int result = 0;
        if (sortType == SORT_NAME)
            result = dp1.getName().compareTo(dp2.getName());
        else if (sortType == SORT_DS_NAME)
            result = dp1.getDataSourceName().compareTo(dp2.getDataSourceName());
        else if (sortType == SORT_ENABLED)
            result = new Boolean(dp1.isEnabled()).compareTo(new Boolean(dp2.isEnabled()));
        else if (sortType == SORT_DATA_TYPE) {
            String s1 = dp1.getDataTypeMessage().translate(translations);
            String s2 = dp2.getDataTypeMessage().translate(translations);
            result = s1.compareTo(s2);
        }
        else if (sortType == SORT_CONFIG) {
            String s1 = dp1.getConfigurationDescription().translate(translations);
            String s2 = dp2.getConfigurationDescription().translate(translations);
            result = s1.compareTo(s2);
        }

        if (descending)
            return -result;
        return result;
    }
}
