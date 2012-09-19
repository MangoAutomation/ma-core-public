package com.serotonin.m2m2.web.servlet;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.serotonin.db.MappedRowCallback;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.db.dao.PointValueDao;
import com.serotonin.m2m2.i18n.Translations;
import com.serotonin.m2m2.rt.dataImage.AnnotatedPointValueTime;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.export.ExportCsvStreamer;
import com.serotonin.m2m2.vo.export.ExportDataValue;
import com.serotonin.m2m2.vo.export.ExportPointInfo;
import com.serotonin.m2m2.vo.permission.Permissions;
import com.serotonin.m2m2.web.dwr.beans.DataExportDefinition;

public class ChartExportServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        User user = Common.getUser(request);
        if (user == null)
            return;

        DataExportDefinition def = user.getDataExportDefinition();
        if (def == null)
            return;

        DataPointDao dataPointDao = new DataPointDao();
        PointValueDao pointValueDao = new PointValueDao();

        long from = def.getFrom() == null ? -1 : def.getFrom().getMillis();
        long to = def.getTo() == null ? System.currentTimeMillis() : def.getTo().getMillis();

        // Stream the content.
        response.setContentType("text/csv");

        final Translations translations = Common.getTranslations();
        final ExportCsvStreamer exportCreator = new ExportCsvStreamer(response.getWriter(), translations);

        final ExportDataValue edv = new ExportDataValue();
        MappedRowCallback<PointValueTime> callback = new MappedRowCallback<PointValueTime>() {
            @Override
            public void row(PointValueTime pvt, int rowIndex) {
                edv.setValue(pvt.getValue());
                edv.setTime(pvt.getTime());
                if (pvt instanceof AnnotatedPointValueTime)
                    edv.setAnnotation(((AnnotatedPointValueTime) pvt).getSourceMessage());
                else
                    edv.setAnnotation(null);
                exportCreator.pointData(edv);
            }
        };

        for (int pointId : def.getPointIds()) {
            DataPointVO dp = dataPointDao.getDataPoint(pointId);
            if (Permissions.hasDataPointReadPermission(user, dp)) {
                ExportPointInfo pointInfo = new ExportPointInfo();
                pointInfo.setPointName(dp.getName());
                pointInfo.setDeviceName(dp.getDeviceName());
                pointInfo.setTextRenderer(dp.getTextRenderer());
                exportCreator.startPoint(pointInfo);

                pointValueDao.getPointValuesBetween(pointId, from, to, callback);
            }
        }

        exportCreator.done();
    }
}
