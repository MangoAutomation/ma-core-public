/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.infiniteautomation.mango.webapp.servlets;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Locale;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import com.infiniteautomation.mango.spring.service.PermissionService;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.rt.dataImage.PointValueFacade;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.rt.dataImage.types.ImageValue;
import com.serotonin.util.image.BoxScaledImage;
import com.serotonin.util.image.ImageUtils;
import com.serotonin.util.image.JpegImageFormat;
import com.serotonin.util.image.PercentScaledImage;

@Component
@WebServlet(urlPatterns = {"/imageValue/*"})
public class ImageValueServlet extends HttpServlet {
    private static final long serialVersionUID = -1;

    public static final String servletPath = "imageValue/";
    public static final String historyPrefix = "hst";

    private static final Log LOG = LogFactory.getLog(ImageValueServlet.class);

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String imageInfo = request.getPathInfo();

        // The imageInfo contains the timestamp of the last point value and the data point id. The intention is to
        // create a name for the virtual image such that the browser will cache the data and only come here when the
        // data has change. The format of the name is:
        // /{last timestamp}_{data point id}.${value extension}

        try {
            // Remove the / and the extension
            int dot = imageInfo.indexOf('.');
            String extension = imageInfo.substring(dot+1, imageInfo.length()).toLowerCase(Locale.ROOT);
            imageInfo = imageInfo.substring(1, dot);


            // Split by underscore.
            String[] imageBits = imageInfo.split("_");

            // Get the data.
            String timestamp = imageBits[0];
            int dataPointId = Integer.parseInt(imageBits[1]);
            int scalePercent = getIntRequestParameter(request, "p", -1);
            int width = getIntRequestParameter(request, "w", -1);
            int height = getIntRequestParameter(request, "h", -1);

            PointValueFacade pointValueFacade = new PointValueFacade(dataPointId);

            //Permission checks
            Common.getBean(PermissionService.class).ensureDataPointReadPermission(Common.getUser(), dataPointId);

            PointValueTime pvt = null;
            if (timestamp.startsWith(historyPrefix)) {
                // Find the point with the given timestamp
                long time = Long.parseLong(timestamp.substring(historyPrefix.length()));
                pvt = pointValueFacade.getPointValueAt(time);
            }
            else
                // Use the latest value
                pvt = pointValueFacade.getPointValue();

            if (pvt == null || pvt.getValue() == null || !(pvt.getValue() instanceof ImageValue)) {
                LOG.warn("Invalid pvt: " + pvt);
                response.sendError(HttpStatus.SC_NOT_FOUND);
            }
            else {
                ImageValue imageValue = (ImageValue) pvt.getValue();
                byte[] data = imageValue.getImageData();


                if (scalePercent != -1) {
                    //Definitely going to be JPEG
                    response.setContentType(MediaType.IMAGE_JPEG_VALUE);
                    // Scale the image
                    PercentScaledImage scaler = new PercentScaledImage(((float) scalePercent) / 100);
                    data = ImageUtils.scaleImage(scaler, data, new JpegImageFormat(0.85f));
                }
                else if (width != -1 && height != -1) {
                    //Definitely going to be JPEG
                    response.setContentType(MediaType.IMAGE_JPEG_VALUE);
                    // Scale the image
                    BoxScaledImage scaler = new BoxScaledImage(width, height);
                    data = ImageUtils.scaleImage(scaler, data, new JpegImageFormat(0.85f));
                }else{
                    //Use the Image extension to se the Content Type
                    if("jpg".equals(extension))
                        response.setContentType(MediaType.IMAGE_JPEG_VALUE);
                    else if("png".equals(extension))
                        response.setContentType(MediaType.IMAGE_PNG_VALUE);
                    else if("gif".equals(extension))
                        response.setContentType(MediaType.IMAGE_GIF_VALUE);
                }

                response.getOutputStream().write(data);
            }
        }
        catch (FileNotFoundException e) {
            LOG.warn("", e);
        }
        catch (InterruptedException e) {
            LOG.warn("", e);
        }
        catch (StringIndexOutOfBoundsException e) {
            LOG.warn("", e);
        }
        catch (NumberFormatException e) {
            LOG.warn("", e);
        }
        catch (ArrayIndexOutOfBoundsException e) {
            LOG.warn("", e);
        }
        catch (IllegalArgumentException e) {
            LOG.warn("", e);
        }
    }

    protected int getIntRequestParameter(HttpServletRequest request, String paramName, int defaultValue) {
        String value = request.getParameter(paramName);
        try {
            if (value != null)
                return Integer.parseInt(value);
        }
        catch (NumberFormatException e) {
            // no op
        }
        return defaultValue;
    }
}
