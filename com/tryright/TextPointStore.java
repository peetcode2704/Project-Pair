/*
# Name: Peter Hoang
# Date: 03/26/2026
# Class: CSC 4180-01: Spring 2026
# Pledge: I have neither given nor received unauthorized aid on this program.
# Description: Program 4: Text file implementation of PointStore.
  Reads points from a text file where the first line is the count, followed by x y pairs.
*/
package com.tryright;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;

/**
 * Text file implementation of PointStore.
 *
 * <p>Reads points from a text file where the first line is the count of
 * points, followed by one "x y" pair per line. All values are stored
 * in memory as arrays.</p>
 *
 * @author Peter Hoang
 * @version 1.0
 */
public final class TextPointStore implements PointStore {

    private int[] xs;
    private int[] ys;

    /**
     * Construct a TextPointStore from a text-encoded file.
     *
     * @param filename path to the text file
     * @throws IOException if the file cannot be read or the format is invalid
     */
    public TextPointStore(String filename) throws IOException {
        if (filename == null || filename.isEmpty()) {
            throw new IOException("Invalid file format");
        }

        final File file = new File(filename);

        if (!file.exists()) {
            throw new IOException("No such file or directory");
        }
        if (!file.canRead()) {
            throw new IOException("Permission denied");
        }

        try (Scanner sc = new Scanner(file)) {
            if (!sc.hasNextInt()) {
                throw new IOException("Invalid file format");
            }

            final int n = sc.nextInt();
            if (n < 0) {
                throw new IOException("Invalid file format");
            }

            xs = new int[n];
            ys = new int[n];

            for (int i = 0; i < n; i++) {
                if (!sc.hasNextInt()) {
                    throw new IOException("Fewer coordinates than specified");
                }
                xs[i] = sc.nextInt();

                if (!sc.hasNextInt()) {
                    throw new IOException("Fewer coordinates than specified");
                }
                ys[i] = sc.nextInt();
            }

            if (sc.hasNextInt()) {
                throw new IOException("More coordinates than specified");
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getX(int idx) {
        if (idx < 0 || idx >= numPoints()) {
            throw new IndexOutOfBoundsException("Index " + idx + " out of bounds for " + numPoints() + " points");
        }
        return xs[idx];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getY(int idx) {
        if (idx < 0 || idx >= numPoints()) {
            throw new IndexOutOfBoundsException("Index " + idx + " out of bounds for " + numPoints() + " points");
        }
        return ys[idx];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int numPoints() {
        return (xs == null) ? 0 : xs.length;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        xs = null;
        ys = null;
    }
}
