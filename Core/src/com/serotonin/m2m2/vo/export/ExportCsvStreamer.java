/*
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.vo.export;

import java.io.PrintWriter;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.serotonin.m2m2.i18n.Translations;
import com.serotonin.m2m2.view.export.CsvWriter;
import com.serotonin.m2m2.view.text.TextRenderer;

/**
 * @author Matthew Lohbihler
 */
public class ExportCsvStreamer implements ExportDataStreamHandler {
    private final PrintWriter out;
    private final Translations translations;

    // Working fields
    private TextRenderer textRenderer;
    private final String[] data = new String[5];
    private final DateTimeFormatter dtf = DateTimeFormat.forPattern("yyyy/MM/dd HH:mm:ss.SSS");
    private final CsvWriter csvWriter = new CsvWriter();

    public ExportCsvStreamer(PrintWriter out, Translations translations) {
        this.out = out;
        this.translations = translations;

        // Write the headers.
        data[0] = translations.translate("common.pointName");
        data[1] = translations.translate("common.time");
        data[2] = translations.translate("common.value");
        data[3] = translations.translate("common.rendered");
        data[4] = translations.translate("common.annotation");
        out.write(csvWriter.encodeRow(data));
    }

    @Override
    public void startPoint(ExportPointInfo pointInfo) {
        data[0] = pointInfo.getExtendedName();
        textRenderer = pointInfo.getTextRenderer();
    }

    @Override
    public void pointData(ExportDataValue rdv) {
        data[1] = dtf.print(new DateTime(rdv.getTime()));

        if (rdv.getValue() == null)
            data[2] = data[3] = null;
        else {
            data[2] = rdv.getValue().toString();
            data[3] = textRenderer.getText(rdv.getValue(), TextRenderer.HINT_FULL);
        }

        if (rdv.getAnnotation() == null)
            data[4] = "";
        else
            data[4] = rdv.getAnnotation().translate(translations);

        out.write(csvWriter.encodeRow(data));
    }

    @Override
    public void done() {
        out.flush();
        out.close();
    }
}
