/*
# Name: Peter Hoang
# Date: 03/26/2026
# Class: CSC 4180-01: Spring 2026
# Pledge: I have neither given nor received unauthorized aid on this program.
# Description: Program 4: Binary file implementation of PointStore using memory-mapped I/O.
  Reads points from binary files where each point is encoded as two 4-byte big-endian integers.
*/
package com.tryright;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Binary file implementation of PointStore using memory-mapped I/O.
 *
 * <p>Reads points from binary files where each point is encoded as two
 * 4-byte big-endian integers (x, then y). Uses memory-mapped I/O for
 * efficient random access to point data.</p>
 *
 * <p>File format: Each point occupies 8 bytes (2 integers x 4 bytes).
 * For example, point (1, 2) is encoded as:</p>
 * <pre>
 * 00 00 00 01 00 00 00 02
 * </pre>
 *
 * @author Peter Hoang
 * @version 1.0
 */
public final class BinPointStore implements PointStore {

    /**
     * Size of a Java int in bytes (4 bytes).
     */
    private static final int INTEGER_SIZE = 4;

    /**
     * Size of a point in bytes (2 integers = 8 bytes).
     */
    private static final int POINT_BYTES = INTEGER_SIZE * 2;

    /**
     * Random access file (kept open for the lifetime of the store).
     */
    private final RandomAccessFile raf;

    /**
     * File channel (kept open for the lifetime of the store).
     */
    private final FileChannel channel;

    /**
     * Memory-mapped buffer providing a single view of the entire file.
     * May be null if the file is empty.
     */
    private final MappedByteBuffer buffer;

    /**
     * Number of points in the file.
     */
    private final int n;

    /**
     * Constructs a BinPointStore by memory-mapping a binary point file.
     *
     * <p>Memory-maps the entire file into virtual address space. No data is
     * actually read or copied into user memory — the OS pages data in on
     * demand when accessed. This is the correct memory-mapped I/O approach.</p>
     *
     * @param filename the path to the binary file containing points
     * @throws IOException if the file cannot be read or the format is invalid
     */
    public BinPointStore(final String filename) throws IOException {
        if (filename == null || filename.isEmpty()) {
            throw new IOException("Invalid file format");
        }

        final File f = new File(filename);
        if (!f.exists())
            throw new IOException("No such file or directory");
        if (!f.canRead())
            throw new IOException("Permission denied");

        raf = new RandomAccessFile(f, "r");
        channel = raf.getChannel();

        final long size = channel.size();
        if (size % POINT_BYTES != 0) {
            close();
            throw new IOException("Invalid file format");
        }

        n = (int) (size / POINT_BYTES);

        // Memory-map the entire file. This does NOT read any data —
        // it establishes a virtual memory mapping so the OS can page
        // data in on demand when getX/getY access specific offsets.
        if (size > 0) {
            buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, size);
        } else {
            buffer = null;
        }
    }

    /**
     * Gets the x-coordinate at the specified index.
     *
     * <p>Uses memory-mapped I/O to efficiently read the value directly
     * from the file without loading all points into memory.</p>
     *
     * @param idx the index of the point
     * @return the x-coordinate at the given index
     * @throws IndexOutOfBoundsException if idx is out of range
     */
    @Override
    public int getX(final int idx) {
        if (idx < 0 || idx >= n) {
            throw new IndexOutOfBoundsException(
                    "Index " + idx + " out of bounds for " + n + " points");
        }
        final int position = idx * POINT_BYTES;
        return buffer.getInt(position);
    }

    /**
     * Gets the y-coordinate at the specified index.
     *
     * <p>Uses memory-mapped I/O to efficiently read the value directly
     * from the file without loading all points into memory.</p>
     *
     * @param idx the index of the point
     * @return the y-coordinate at the given index
     * @throws IndexOutOfBoundsException if idx is out of range
     */
    @Override
    public int getY(final int idx) {
        if (idx < 0 || idx >= n) {
            throw new IndexOutOfBoundsException(
                    "Index " + idx + " out of bounds for " + n + " points");
        }
        final int position = idx * POINT_BYTES + INTEGER_SIZE;
        return buffer.getInt(position);
    }

    /**
     * Returns the total number of points in the store.
     *
     * @return the number of points
     */
    @Override
    public int numPoints() {
        return n;
    }

    /**
     * Closes the point store and releases all resources.
     *
     * <p>Closes the file channel and random access file. The memory-mapped
     * buffer will be garbage collected automatically.</p>
     */
    @Override
    public void close() {
        try {
            if (channel != null)
                channel.close();
        } catch (IOException ignored) {
        }
        try {
            if (raf != null)
                raf.close();
        } catch (IOException ignored) {
        }
    }
}
