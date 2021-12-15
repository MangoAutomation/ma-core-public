package com.serotonin.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

/**
 * A wrapper around a file that allows readers to read the file while it is still being written by a writer.
 */
public class ActiveFile {
    final File file;
    volatile boolean active;

    public ActiveFile(File file) {
        this.file = file;
        active = true;
    }

    /**
     * Convenience constructor for cases where the file is known to be done, but an ActiveFile instance is required.
     *
     */
    public ActiveFile(File file, boolean active) {
        this.file = file;
        this.active = active;
    }

    public void close() {
        active = false;
        synchronized (this) {
            notifyAll();
        }
    }

    public File getFile() {
        return file;
    }

    public Reader createReader() {
        return new ActiveFileReader();
    }

    class ActiveFileReader extends Reader {
        private FileReader reader;

        @Override
        public int read(char[] cbuf, int off, int len) throws IOException {
            while (true) {
                boolean localActive = active;

                if (reader == null) {
                    if (file.exists())
                        reader = new FileReader(file);
                    else {
                        if (!localActive)
                            throw new FileNotFoundException(file.getPath());
                    }
                }

                if (reader != null) {
                    int count = reader.read(cbuf, off, len);
                    if (count != -1 || !localActive)
                        return count;
                }

                synchronized (this) {
                    try {
                        wait(20);
                    }
                    catch (InterruptedException e) {
                        // Ignore
                    }
                }
            }
        }

        @Override
        public void close() throws IOException {
            if (reader != null)
                reader.close();
        }
    }
}
