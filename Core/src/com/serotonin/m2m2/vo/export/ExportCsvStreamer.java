/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.vo.export;

import java.io.PrintWriter;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.web.util.UriComponentsBuilder;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.DataTypes;
import com.serotonin.m2m2.i18n.Translations;
import com.serotonin.m2m2.view.export.CsvWriter;
import com.serotonin.m2m2.view.text.TextRenderer;

/**
 * @author Matthew Lohbihler
 */
public class ExportCsvStreamer implements ExportDataStreamHandler {
    private final PrintWriter out;
    private final Translations translations;

    public final static int columns = 8;
    // Working fields
    private TextRenderer textRenderer;
    private int dataPointId;
    private final String[] data = new String[columns];
    public static final DateTimeFormatter dtf = DateTimeFormat.forPattern("yyyy/MM/dd HH:mm:ss.SSS");
    private final CsvWriter csvWriter = new CsvWriter();
    private final UriComponentsBuilder imageServletBuilder;   
    
    public ExportCsvStreamer(String host, int port, PrintWriter out, Translations translations) {
        this.out = out;
        this.translations = translations;
        
        //Setup the Image URL Builder
        imageServletBuilder  = UriComponentsBuilder.fromPath("/imageValue/hst{ts}_{id}.jpg");
		if(Common.envProps.getBoolean("ssl.on", false))
			imageServletBuilder.scheme("https");
		else
			imageServletBuilder.scheme("http");
		imageServletBuilder.host(host);
		imageServletBuilder.port(port);

        // Write the headers.
        data[0] = Common.translate("emport.dataPoint.xid");
    	data[1] = Common.translate("pointEdit.props.deviceName");
    	data[2] = Common.translate("common.pointName");
    	data[3] = Common.translate("common.time");
        data[4] = Common.translate("common.value");
        data[5] = Common.translate("common.rendered");
        data[6] = Common.translate("common.annotation");
        data[7] = Common.translate("common.modify");

        out.write(csvWriter.encodeRow(data));
        
        data[7] = "";
    }

    @Override
    public void startPoint(ExportPointInfo pointInfo) {
        data[0] = pointInfo.getXid();
        data[1] = pointInfo.getDeviceName();
        data[2] = pointInfo.getPointName();
        textRenderer = pointInfo.getTextRenderer();
        dataPointId = pointInfo.getDataPointId();
    }

    @Override
    public void pointData(ExportDataValue rdv) {
        data[3] = dtf.print(new DateTime(rdv.getTime()));

        if (rdv.getValue() == null)
            data[4] = data[5] = null;
        else {
        	if(rdv.getValue().getDataType() == DataTypes.IMAGE)
        		data[4] = imageServletBuilder.buildAndExpand(rdv.getTime(), dataPointId).toUri().toString();
        	else
        		data[4] = rdv.getValue().toString();
            data[5] = textRenderer.getText(rdv.getValue(), TextRenderer.HINT_FULL);
        }

        if (rdv.getAnnotation() == null)
            data[6] = "";
        else
            data[6] = rdv.getAnnotation().translate(translations);
        
        out.write(csvWriter.encodeRow(data));
    }

    @Override
    public void done() {
        out.flush();
        out.close();
    }
}
