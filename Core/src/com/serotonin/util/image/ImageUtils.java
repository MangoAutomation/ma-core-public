/*
    Copyright (C) 2006-2007 Serotonin Software Technologies Inc.
 	@author Matthew Lohbihler
 */
package com.serotonin.util.image;

import java.awt.Color;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Iterator;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;

import com.serotonin.ShouldNeverHappenException;
import com.serotonin.io.StreamUtils;

/**
 * @author Matthew Lohbihler
 */
public class ImageUtils {
    /**
     * The byte array representing a transparent GIF of size 1x1.
     */
    public static byte[] TRANSPARENT_PIXEL_GIF = { 0x47, 0x49, 0x46, 0x38, 0x39, 0x61, 0x01, 0x00, 0x01, 0x00,
            (byte) 0x80, 0x00, 0x00, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            0x21, (byte) 0xf9, 0x04, 0x01, 0x0a, 0x00, 0x01, 0x00, 0x2c, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x01,
            0x00, 0x00, 0x02, 0x02, 0x4c, 0x01, 0x00, 0x3b, 0x00 };

    /**
     * The byte array representing a transparent PNG of size 1x1.
     */
    public static byte[] TRANSPARENT_PIXEL_PNG = { (byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 0x00, 0x00,
            0x00, 0x0d, 0x49, 0x48, 0x44, 0x52, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01, 0x08, 0x06, 0x00, 0x00,
            0x00, 0x1f, 0x15, (byte) 0xc4, (byte) 0x89, 0x00, 0x00, 0x00, 0x01, 0x73, 0x52, 0x47, 0x42, 0x00,
            (byte) 0xae, (byte) 0xce, 0x1c, (byte) 0xe9, 0x00, 0x00, 0x00, 0x04, 0x67, 0x41, 0x4d, 0x41, 0x00, 0x00,
            (byte) 0xb1, (byte) 0x8f, 0x0b, (byte) 0xfc, 0x61, 0x05, 0x00, 0x00, 0x00, 0x09, 0x70, 0x48, 0x59, 0x73,
            0x00, 0x00, 0x0e, (byte) 0xc3, 0x00, 0x00, 0x0e, (byte) 0xc3, 0x01, (byte) 0xc7, 0x6f, (byte) 0xa8, 0x64,
            0x00, 0x00, 0x00, 0x1a, 0x74, 0x45, 0x58, 0x74, 0x53, 0x6f, 0x66, 0x74, 0x77, 0x61, 0x72, 0x65, 0x00, 0x50,
            0x61, 0x69, 0x6e, 0x74, 0x2e, 0x4e, 0x45, 0x54, 0x20, 0x76, 0x33, 0x2e, 0x35, 0x2e, 0x31, 0x30, 0x30,
            (byte) 0xf4, 0x72, (byte) 0xa1, 0x00, 0x00, 0x00, 0x0d, 0x49, 0x44, 0x41, 0x54, 0x18, 0x57, 0x63,
            (byte) 0xf8, (byte) 0xff, (byte) 0xff, 0x3f, 0x03, 0x00, 0x08, (byte) 0xfc, 0x02, (byte) 0xfe, (byte) 0x88,
            0x5f, 0x06, (byte) 0xe0, 0x00, 0x00, 0x00, 0x00, 0x49, 0x45, 0x4e, 0x44, (byte) 0xae, 0x42, 0x60,
            (byte) 0x82 };

    /**
     * Loads an image from the given file name. Uses a media tracker to ensure the file is loaded fully.
     * 
     * @param filename
     *            the name of the file to load from
     * @return the image that was loaded
     * @throws InterruptedException
     *             if the media tracker was interrupted
     */
    public static Image loadImage(String filename) throws InterruptedException {
        return waitForImage(Toolkit.getDefaultToolkit().createImage(filename));
    }

    public static Image loadImage(File file) throws InterruptedException, MalformedURLException {
        return waitForImage(Toolkit.getDefaultToolkit().createImage(file.toURI().toURL()));
    }

    /**
     * Creates an image from the given image data.
     * 
     * @param data
     *            the pixel data
     * @return the image that was created
     */
    public static Image createImage(byte[] data) throws InterruptedException {
        return waitForImage(Toolkit.getDefaultToolkit().createImage(data));
    }

    public static byte[] encodeImage(BufferedImage image, BaseImageFormat encodingFormat) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        if (encodingFormat.supportsCompression()) {
            Iterator<ImageWriter> iter = ImageIO.getImageWritersByFormatName(encodingFormat.getType());
            ImageWriter writer = iter.next();
            if (writer == null)
                throw new ShouldNeverHappenException("No writers for image format type " + encodingFormat.getType());
            ImageWriteParam iwp = writer.getDefaultWriteParam();
            iwp.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            iwp.setCompressionQuality(encodingFormat.getCompressionQuality());

            ImageOutputStream ios = new MemoryCacheImageOutputStream(out);
            writer.setOutput(ios);
            IIOImage iioImage = new IIOImage(image, null, null);
            writer.write(null, iioImage, iwp);
            ios.close();
        }
        else
            ImageIO.write(image, encodingFormat.getType(), out);

        return out.toByteArray();
    }

    public static void scaleImage(BaseScaledImage scaler, String inputFilename, String outputFilename,
            BaseImageFormat encodingFormat) throws IOException, InterruptedException {
        ByteArrayOutputStream imageData = new ByteArrayOutputStream();
        FileInputStream in = new FileInputStream(inputFilename);
        StreamUtils.transfer(in, imageData);
        in.close();

        byte[] scaledData = scaleImage(scaler, imageData.toByteArray(), encodingFormat);

        FileOutputStream out = new FileOutputStream(outputFilename);
        StreamUtils.transfer(new ByteArrayInputStream(scaledData), out);
        out.close();
    }

    public static byte[] scaleImage(BaseScaledImage scaler, byte[] imageData, BaseImageFormat encodingFormat)
            throws IOException, InterruptedException {
        return scaleImage(scaler, createImage(imageData), encodingFormat);
    }

    public static byte[] scaleImage(BaseScaledImage scaler, Image image, BaseImageFormat encodingFormat)
            throws IOException {
        scaler.scaleImage(image);
        byte[] result = encodeImage(scaler.getScaledImage(), encodingFormat);
        scaler.flush();
        image.flush();
        return result;
    }

    //
    // /
    // / Tinting
    // /
    //
    public static BufferedImage tintImage(String filename, float r, float g, float b) throws InterruptedException {
        return tintImage(loadImage(filename), r, g, b);
    }

    public static BufferedImage tintImage(Image original, float r, float g, float b) {
        BufferedImage tinted = createBufferedImage(original);
        WritableRaster raster = tinted.getRaster();

        // Tint is RGBA
        float[] tint = { r, g, b, 1 };
        float[] buf = new float[4];
        int x, y, i;
        for (y = 0; y < tinted.getHeight(); y++) {
            for (x = 0; x < tinted.getWidth(); x++) {
                raster.getPixel(x, y, buf);
                for (i = 0; i < buf.length; i++)
                    buf[i] = buf[i] * tint[i];
                raster.setPixel(x, y, buf);
            }
        }

        return tinted;
    }

    /**
     * Waits for the image to be loaded.
     * 
     * @param image
     *            the image to wait for
     * @return the image after loading
     * @throws InterruptedException
     *             if the media tracker was interrupted
     */
    private static Image waitForImage(Image image) throws InterruptedException {
        MediaTracker mediaTracker = new MediaTracker(new Container());
        mediaTracker.addImage(image, 0);
        mediaTracker.waitForID(0);
        return image;
    }

    public static BufferedImage createBufferedImage(Image image) {
        return createBufferedImage(image, BufferedImage.TYPE_INT_ARGB);
    }

    public static BufferedImage createBufferedImage(Image image, int imageType) {
        BufferedImage bi = new BufferedImage(image.getWidth(null), image.getHeight(null), imageType);
        bi.getGraphics().drawImage(image, 0, 0, null);
        return bi;
    }

    public static void main(String[] args) throws Exception {
        BufferedImage bi = ImageUtils
                .createBufferedImage(loadImage("swatchBackground.jpg"), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = bi.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g.setColor(Color.RED);
        g.fillRect(1, 1, 18, 18);
        g.dispose();

        // Encode it into GIF
        byte[] data = ImageUtils.encodeImage(bi, new JpegImageFormat(1f));
        FileOutputStream out = new FileOutputStream("output.jpg");

        // byte[] data = ImageUtils.encodeImage(bi, new SimpleImageFormat("GIF"));
        // FileOutputStream out = new FileOutputStream("output.gif");

        // byte[] data = ImageUtils.encodeImage(bi, new SimpleImageFormat("PNG"));
        // FileOutputStream out = new FileOutputStream("output.png");

        StreamUtils.transfer(new ByteArrayInputStream(data), out);
        out.close();

        // scaleImage(new PercentScaledImage(.25f), "swatchBackground.gif", "scaled.gif", new SimpleImageFormat("GIF"));
        // scaleImage(new PercentScaledImage(.25f), "test.jpg", "scaled.jpg", new JpegImageFormat(0.85f));
        // scaleImage(new PercentScaledImage(2f), "scaled.jpg", "upscaled.jpg", new JpegImageFormat(0.85f));
        // scaleImage(new BoxScaledImage(1000, 1000, false), "rotateWithDad.jpg", "rotateWithDadBox.jpg", new
        // JpegImageFormat(0.85f));
        // scaleImage(new ShortSideScaledImage(100, false), "withDad.jpg", "withDadShort.jpg", new
        // JpegImageFormat(0.85f));
        // scaleImage(new ShortSideScaledImage(100, false), "rotateWithDad.jpg", "rotateWithDadShort.jpg", new
        // JpegImageFormat(0.85f));
        // scaleImage(new ShortSideScaledImage(100, false), "IMG_0495.JPG", "rockShort.jpg", new
        // JpegImageFormat(0.85f));
        // scaleImage(new ShortSideScaledImage(100, false), "thumb_background.jpg", "bkgdShort.jpg", new
        // JpegImageFormat(0.85f));
    }
}