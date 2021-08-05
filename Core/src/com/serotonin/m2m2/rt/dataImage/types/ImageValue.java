/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.rt.dataImage.types;

import java.awt.Image;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serotonin.InvalidArgumentException;
import com.serotonin.ShouldNeverHappenException;
import com.serotonin.io.StreamUtils;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.DataTypes;
import com.serotonin.util.ArrayUtils;
import com.serotonin.util.image.ImageUtils;

/**
 * @author Matthew Lohbihler
 */
public class ImageValue extends DataValue implements Comparable<ImageValue> {
    private static final String FILENAME_PREFIX = "img";

    public static final int TYPE_JPG = 1;
    private static final Logger LOG = LoggerFactory.getLogger(ImageValue.class);
    private static final String[] TYPES = { "", "jpg" };

    private long id = Common.NEW_ID;
    private int type;
    private byte[] data;
    private byte[] digest = null;

    public ImageValue(long id, int type) {
        this.id = id;
        this.type = type;
    }

    public ImageValue(byte[] data, int type) {
        this.data = data;
        this.type = type;
    }

    public ImageValue(String filename) throws InvalidArgumentException {
        id = parseIdFromFilename(filename);
        if (id == -1)
            throw new InvalidArgumentException();

        int dot = filename.indexOf('.');
        if (dot == -1)
            throw new InvalidArgumentException();

        String ext = filename.substring(dot + 1);
        type = ArrayUtils.indexOf(TYPES, ext);
        if (type == -1)
            throw new InvalidArgumentException();
    }

    public String getFilename() {
        return FILENAME_PREFIX + id + '.' + TYPES[type];
    }

    public static long parseIdFromFilename(String filename) {
        if (!filename.startsWith(FILENAME_PREFIX))
            return -1;
        int dot = filename.indexOf('.');
        if (dot == -1)
            filename = filename.substring(FILENAME_PREFIX.length());
        else
            filename = filename.substring(FILENAME_PREFIX.length(), dot);

        try {
            return Long.parseLong(filename);
        }
        catch (NumberFormatException e) {
            return -1;
        }
    }

    @Override
    public String toString() {
        return getFilename();
    }

    public String getTypeExtension() {
        return TYPES[type];
    }

    public boolean isSaved() {
        return id != Common.NEW_ID;
    }

    public Image getImage() {
        try {
            if (data != null)
                return ImageUtils.createImage(data);
            return ImageUtils.loadImage(Common.getFiledataPath().resolve(getFilename()).toFile().getPath());
        }
        catch (InterruptedException e) {
            // no op
        }
        return null;
    }

    public byte[] getImageData() throws IOException {
        if (!isSaved())
            return data;

        FileInputStream in = null;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            in = new FileInputStream(Common.getFiledataPath().resolve(getFilename()).toFile().getPath());
            StreamUtils.transfer(in, out);
        }
        finally {
            if (in != null)
                in.close();
        }
        return out.toByteArray();
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
        digest = null;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (id ^ (id >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final ImageValue other = (ImageValue) obj;

        if (id != -1 && id == other.id)
            return true;

        if (data != null && other.data != null && Arrays.equals(data, other.data))
            return true;

        return false;
    }

    @Override
    public boolean hasDoubleRepresentation() {
        return false;
    }

    @Override
    public double getDoubleValue() {
        throw new RuntimeException(
                "ImageValue has no double value. Use hasDoubleRepresentation() to check before calling this method");
    }

    @Override
    public String getStringValue() {
        return getFilename();
    }

    @Override
    public boolean getBooleanValue() {
        throw new RuntimeException("ImageValue has no boolean value.");
    }

    @Override
    public Object getObjectValue() {
        throw new RuntimeException("ImageValue has no object value.");
    }

    @Override
    public int getIntegerValue() {
        throw new RuntimeException("ImageValue has no int value.");
    }

    @Override
    public Number numberValue() {
        throw new RuntimeException("ImageValue has no Number value.");
    }

    @Override
    public int getDataType() {
        return DataTypes.IMAGE;
    }

    @Override
    public int compareTo(ImageValue that) {
        return getFilename().compareTo(that.getFilename());
    }

    @Override
    public <T extends DataValue> int compareTo(T that) {
        return compareTo((ImageValue) that);
    }

    public byte[] getDigest() {
        if(digest == null) {
            try {
                MessageDigest digester = MessageDigest.getInstance("MD5");
                byte[] data;
                if(this.data != null)
                    data = this.data;
                else
                    data = getImageData();
                digester.update(data);
                digester.update((byte)(data.length>>24));
                digester.update((byte)(data.length>>16));
                digester.update((byte)(data.length>>8));
                digester.update((byte)data.length);
                digest = digester.digest();
            } catch (NoSuchAlgorithmException e) {
                LOG.error(e.getMessage(), e);
                throw new ShouldNeverHappenException("Digest type MD5 not supported");
            } catch(IOException e) {
                LOG.error("Failed to load image data for computing image value digest: " + e.getMessage(), e);
                return new byte[0];
            }
        }
        return digest;
    }

    public boolean equalDigests(byte[] digest) {
        return MessageDigest.isEqual(this.digest != null ? this.digest : getDigest(), digest);
    }
}
