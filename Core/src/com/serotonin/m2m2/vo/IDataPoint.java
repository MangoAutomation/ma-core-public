package com.serotonin.m2m2.vo;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.infiniteautomation.mango.permission.MangoPermission;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.SystemSettingsDao;
import com.serotonin.m2m2.module.definitions.settings.DataPointTagsDisplaySettingDefinition;

/**
 * Interface that represents both full data point VOs and summary objects.
 *
 * @author Matthew Lohbihler
 */
public interface IDataPoint {

    int getId();

    String getXid();

    String getName();

    int getDataSourceId();

    String getDeviceName();

    /**
     * Returns a map of the tag keys and values. Will not contain "name" or "device" keys.
     * @return
     */
    Map<String, String> getTags();

    /**
     * Roles that can read this point's value and configuration
     * @return
     */
    MangoPermission getReadPermission();

    /**
     * Roles that can edit the point's configuration
     * @return
     */
    MangoPermission getEditPermission();

    /**
     * Roles that can set the point's value
     * @return
     */
    MangoPermission getSetPermission();

    final static String DASH = " — ";
    final static String OPEN_BRACKET = " [";
    final static String CLOSE_BRACKET = "]";

    /**
     * Return a nicely formatted representation of the Data Point for use in events and displays
     * name - device name [tag1=tag1value, tag2=tag2value]
     * @return
     */
    default String getExtendedName() {
        StringBuilder b = new StringBuilder();
        b.append(getName());
        b.append(DASH);
        b.append(getDeviceName());

        Map<String, String> tags = getTags();
        if(tags != null) {
            String toDisplay = SystemSettingsDao.instance.getValue(DataPointTagsDisplaySettingDefinition.DEFAULT_DISPLAY_TAGS);

            Map<String, String> tagsToUse = tags;
            if(!StringUtils.isEmpty(toDisplay)) {
                String[] displayTags = Common.COMMA_SPLITTER.split(toDisplay);
                tagsToUse = new LinkedHashMap<>(displayTags.length);
                for(String tag : displayTags) {
                    if (!tag.isEmpty()) {
                        String value = tags.get(tag);
                        if(value != null) {
                            tagsToUse.put(tag, value);
                        }
                    }
                }
            }

            if(!tagsToUse.isEmpty()) {
                b.append(OPEN_BRACKET);
                b.append(tagsToUse.entrySet().stream()
                        .filter(e -> e.getValue() != null)
                        .map(e -> e.getKey() + "=" + e.getValue())
                        .collect(Collectors.joining(", ")));
                b.append(CLOSE_BRACKET);
            }

        }

        return b.toString();
    }
}
