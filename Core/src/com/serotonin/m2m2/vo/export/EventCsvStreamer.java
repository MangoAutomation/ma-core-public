/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.vo.export;

import java.io.PrintWriter;
import java.util.List;

import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.i18n.Translations;
import com.serotonin.m2m2.rt.event.EventInstance;
import com.serotonin.m2m2.view.export.CsvWriter;

/**
 * @author Matthew Lohbihler
 */
public class EventCsvStreamer {
    public EventCsvStreamer(PrintWriter out, List<EventInstance> events, Translations translations) {
        CsvWriter csvWriter = new CsvWriter();
        String[] data = new String[7];

        // Write the headers.
        data[0] = translations.translate("reports.eventList.id");
        data[1] = translations.translate("common.alarmLevel");
        data[2] = translations.translate("common.activeTime");
        data[3] = translations.translate("reports.eventList.message");
        data[4] = translations.translate("reports.eventList.status");
        data[5] = translations.translate("reports.eventList.ackTime");
        data[6] = translations.translate("reports.eventList.ackUser");

        out.write(csvWriter.encodeRow(data));

        for (EventInstance event : events) {
            data[0] = Integer.toString(event.getId());
            data[1] = event.getAlarmLevel().getDescription().translate(translations);
            data[2] = event.getFullPrettyActiveTimestamp();
            data[3] = event.getMessage().translate(translations);

            if (event.isActive())
                data[4] = translations.translate("common.active");
            else if (!event.isRtnApplicable())
                data[4] = "";
            else
                data[4] = event.getFullPrettyRtnTimestamp() + " - " + event.getRtnMessage().translate(translations);

            if (event.isAcknowledged()) {
                data[5] = event.getFullPrettyAcknowledgedTimestamp();

                TranslatableMessage ack = event.getExportAckMessage();
                if (ack == null)
                    data[6] = "";
                else
                    data[6] = ack.translate(translations);
            }
            else {
                data[5] = "";
                data[6] = "";
            }

            out.write(csvWriter.encodeRow(data));
        }

        out.flush();
        out.close();
    }
}
