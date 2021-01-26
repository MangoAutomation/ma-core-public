/*
    Copyright (C) 2006-2007 Serotonin Software Technologies Inc.
 	@author Matthew Lohbihler
 */
package com.serotonin.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;

import com.serotonin.ModuleNotLoadedException;
import com.serotonin.ShouldNeverHappenException;

/**
 * @author Matthew Lohbihler
 */
public class SerializationHelper {
    public static void writeSafeUTF(ObjectOutputStream out, String utf) throws IOException {
        if (utf == null)
            out.writeBoolean(false);
        else {
            out.writeBoolean(true);
            out.writeUTF(utf);
        }
    }

    public static String readSafeUTF(ObjectInputStream in) throws IOException {
        boolean exists = in.readBoolean();
        if (exists)
            return in.readUTF();
        return null;
    }

    public static void writeSafeObject(ObjectOutputStream out, Object o) throws IOException {
        if (o == null)
            out.writeBoolean(false);
        else {
            out.writeBoolean(true);
            out.writeObject(o);
        }
    }

    public static <T> T readSafeObject(ObjectInputStream in) throws IOException {
        boolean exists = in.readBoolean();
        if (exists)
            return readObject(in);
        return null;
    }

    public static void writeObject(ObjectOutputStream out, Object o) throws IOException {
        out.writeObject(o);
    }

    @SuppressWarnings("unchecked")
    public static <T> T readObject(ObjectInputStream in) throws IOException {
        try {
            return (T) in.readObject();
        }
        catch (ClassNotFoundException e) {
            throw new IOException(e);
        }
    }

    public static Object readObject(InputStream is) throws ShouldNeverHappenException {
        if (is == null)
            return null;

        try {
            return new ObjectInputStream(is).readObject();
        }
        catch (Exception e) {
            throw new ShouldNeverHappenException(e);
        }
    }

    public static Object readObjectInContextFromArray(byte[] data) throws ShouldNeverHappenException {
        if (data == null)
            return null;
        return readObjectInContext(new ByteArrayInputStream(data));
    }

    public static Object readObjectInContext(InputStream is) throws ShouldNeverHappenException {
        if (is == null)
            return null;

        try (ClassLoaderObjectInputStream stream = new ClassLoaderObjectInputStream(is, Thread.currentThread().getContextClassLoader())) {
            return stream.readObject();
        } catch (Exception e) {
            throw new ShouldNeverHappenException(e);
        }
    }

    public static Object readObject(InputStream is, ClassLoader classLoader) throws ShouldNeverHappenException {
        if (is == null)
            return null;

        try (ClassLoaderObjectInputStream stream = new ClassLoaderObjectInputStream(is, classLoader)) {
            return stream.readObject();
        } catch (Exception e) {
            throw new ShouldNeverHappenException(e);
        }
    }

    public static Object readObjectFromArray(byte[] data) throws ShouldNeverHappenException {
        if (data == null)
            return null;
        return readObject(new ByteArrayInputStream(data));
    }

    public static ByteArrayInputStream writeObject(Object o) throws ShouldNeverHappenException {
        if (o == null)
            return null;
        return new ByteArrayInputStream(writeObjectToArray(o));
    }

    public static byte[] writeObjectToArray(Object o) throws ShouldNeverHappenException {
        if (o == null)
            return null;

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            new ObjectOutputStream(baos).writeObject(o);
            return baos.toByteArray();
        }
        catch (IOException e) {
            throw new ShouldNeverHappenException(e);
        }
    }

    static class ClassLoaderObjectInputStream extends ObjectInputStream {
        private final ClassLoader classLoader;

        public ClassLoaderObjectInputStream(InputStream in, ClassLoader classLoader) throws IOException {
            super(in);
            this.classLoader = classLoader;
        }

        @Override
        protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
            String name = desc.getName();
            try {
                return Class.forName(name, false, classLoader);
            }
            catch (ClassNotFoundException ex) {
                try {
                    return super.resolveClass(desc);
                }catch(Exception e) {
                    throw new ModuleNotLoadedException(name, e);
                }
            }
        }
    }
}
