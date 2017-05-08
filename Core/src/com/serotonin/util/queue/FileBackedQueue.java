package com.serotonin.util.queue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class FileBackedQueue<E extends Serializable> {
    private static final String FILE_PREFIX = "fbqsave.";
    private static final String FILE_SUFFIX = ".bin";

    private final int blockSize;
    private final File baseDir;
    private final ObjectQueue<E> headQueue;
    private final ObjectQueue<E> tailQueue;
    private int fileHead;
    private int fileNext;
    private boolean putHead;

    public FileBackedQueue(int blockSize, File baseDir) {
        this.blockSize = blockSize;
        this.baseDir = baseDir;

        headQueue = new ObjectQueue<E>(blockSize << 1);
        tailQueue = new ObjectQueue<E>(blockSize);

        // Make sure the base dir exists.
        baseDir.mkdirs();

        // Look for files from which to load.
        boolean fileFound = false;
        File[] list = baseDir.listFiles();
        if (list != null) {
            for (File file : list) {
                String name = file.getName();
                if (name.startsWith(FILE_PREFIX) && name.endsWith(FILE_SUFFIX)) {
                    // Determine the index.
                    String idStr = name.substring(FILE_PREFIX.length(), name.length() - FILE_SUFFIX.length());
                    int id = Integer.parseInt(idStr);

                    if (!fileFound) {
                        // The first file found
                        fileHead = id;
                        fileNext = id;
                        fileFound = true;
                    }
                    else {
                        if (fileHead > id)
                            fileHead = id;
                        if (fileNext < id)
                            fileNext = id;
                    }
                }
            }

            putHead = !fileFound;
            if (fileFound)
                fileNext++;
        }
    }

    public synchronized int size() {
        return headQueue.size() + tailQueue.size() + (fileNext - fileHead) * blockSize;
    }

    public synchronized boolean offer(E e) {
        if (putHead) {
            // Still adding to the head, but is there room? Note that the head queue is allowed to grow to twice the
            // block size.
            if (headQueue.size() < (blockSize << 1))
                // Yes, there is room
                headQueue.push(e);
            else
                // No, no more room. Fail over to below
                putHead = false;
        }

        if (!putHead) {
            // Adding to the tail. Is there room?
            if (tailQueue.size() >= blockSize) {
                // No, no more room. Save the whole tail in a file and start the tail queue over.
                writeFile(tailQueue, fileNext++);
                tailQueue.clear();
            }

            tailQueue.push(e);
        }

        return true;
    }

    @SuppressWarnings("unchecked")
    public synchronized E poll() {
        // If there is anything in the head queue...
        if (headQueue.size() > 0)
            // ... return it.
            return headQueue.pop();

        // Are we putting into the head?
        if (putHead)
            return null;

        // No. Move some backlog into the head.
        if (fileHead < fileNext) {
            // There is at least one saved file. Load it in.
            File file = createFile(fileHead++);
            try {
                ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));
                int count = ois.readInt();
                while (count-- > 0)
                    headQueue.push((E) ois.readObject());
                ois.close();
            }
            catch (ClassNotFoundException ex) {
                throw new RuntimeException("While reading " + file.getPath(), ex);
            }
            catch (IOException ex) {
                throw new RuntimeException("While reading " + file.getPath(), ex);
            }
            file.delete();

            // If there are no more save files, reset the indices.
            if (fileHead == fileNext)
                fileHead = fileNext = 0;
        }
        else {
            // No saved files. Move the tail into the head and resume putting into the head.
            headQueue.push(tailQueue);
            tailQueue.clear();
            putHead = true;
        }

        if (headQueue.size() > 0)
            return headQueue.pop();

        return null;
    }

    /**
     * Save the current memory contents of the queue - both head and tail as appropriate to the file system.
     */
    public synchronized void save() {
        if (headQueue.size() > 0) {
            writeFile(headQueue, --fileHead);
            headQueue.clear();
            putHead = false;
        }

        if (tailQueue.size() > 0) {
            writeFile(tailQueue, fileNext++);
            tailQueue.clear();
        }
    }

    private File createFile(int id) {
        return new File(baseDir, FILE_PREFIX + id + FILE_SUFFIX);
    }

    private void writeFile(ObjectQueue<E> queue, int id) {
        try {
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(createFile(id)));
            oos.writeInt(queue.size());
            for (E t : queue)
                oos.writeObject(t);
            oos.close();
        }
        catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
