/*
# Name: Peter Hoang
# Date: 03/26/2026
# Class: CSC 4180-01: Spring 2026
# Pledge: I have neither given nor received unauthorized aid on this program.
# Description: Program 3: This program reads a set of 2D points from a file and counts how many right triangles can be formed using any three points.
A triangle is considered a right triangle if the dot product of two vectors sharing a common vertex is zero.
*/
package com.tryright;

import java.io.IOException;
import java.util.*;

/**
 * Counts right triangles from a set of 2D points using a PointStore
 * abstraction.
 * Supports both full-run mode and worker mode for ProcessTriangles.
 *
 * <p>Usage:</p>
 * <pre>
 * java com.tryright.Triangles &lt;filename&gt;
 * java com.tryright.Triangles &lt;filename&gt; &lt;start&gt; &lt;end&gt;
 * </pre>
 *
 * <p>If filename ends with ".dat", BinPointStore is used; otherwise TextPointStore
 * is used.</p>
 *
 * @author Peter Hoang
 * @version 1.0
 */
public class Triangles {

    private Triangles() { }

    private static final int EXIT_OK = 0;
    private static final int EXIT_ERROR = 1;

    /**
     * Entry point for the triangle counter.
     *
     * @param args command-line arguments: filename [start end]
     */
    public static void main(String[] args) {
        if (args == null || (args.length != 1 && args.length != 3)) {
            printUsage("Wrong number of arguments.");
            System.exit(EXIT_ERROR);
        }

        final String filename = args[0];

        int start = 0;
        int end = -1; // filled after we know n

        if (args.length == 3) {
            try {
                start = Integer.parseInt(args[1]);
                end = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                System.err.println("Error: start/end must be integers");
                System.exit(EXIT_ERROR);
            }
        }

        PointStore store = null;
        try {
            store = openStore(filename);
            final int n = store.numPoints();

            if (end == -1) {
                end = n;
            }

            if (start < 0 || end < 0 || start > end || end > n) {
                System.err.println("Error: invalid range");
                System.exit(EXIT_ERROR);
            }

            long answer = countRightTrianglesRange(store, start, end);
            System.out.println(answer);
            System.exit(EXIT_OK);

        } catch (IOException e) {
            // Your stores should throw messages WITHOUT "Error:" prefix
            // so this prints exactly: "Error: Permission denied", etc.
            System.err.println("Error: " + e.getMessage());
            System.exit(EXIT_ERROR);

        } finally {
            if (store != null) {
                store.close();
            }
        }
    }

    /**
     * Chooses store implementation by filename extension.
     *
     * @param filename the path to the point file
     * @return a PointStore backed by the given file
     * @throws IOException if the file cannot be read or is invalid
     */
    public static PointStore openStore(String filename) throws IOException {
        if (filename != null && filename.endsWith(".dat")) {
            return new BinPointStore(filename);
        }
        return new TextPointStore(filename);
    }

    /**
     * Prints an error message and usage instructions to stderr.
     *
     * @param message the error message to display, or null for no message
     */
    private static void printUsage(String message) {
        if (message != null && !message.isEmpty()) {
            System.err.println("Error: " + message);
        }
        System.err.println("Usage: java com.tryright.Triangles <filename>");
        System.err.println("Usage: java com.tryright.Triangles <filename> <start> <end>");
        System.err.println("Parameter(s): <filename> [<start> <end>]");
    }

    /**
     * Counts right triangles using only pivot points in [start, end).
     * Each worker owns a disjoint pivot range, so triangles are counted once
     * overall
     * (as long as ProcessTriangles splits pivot indices into disjoint ranges).
     *
     * @param store the point store to read coordinates from
     * @param start the inclusive start index of the pivot range
     * @param end   the exclusive end index of the pivot range
     * @return the number of right triangles found
     */
    public static long countRightTrianglesRange(PointStore store, int start, int end) {
        final int n = store.numPoints();
        long total = 0L;

        for (int i = start; i < end; i++) {
            final int ax = store.getX(i);
            final int ay = store.getY(i);

            // Key: packed reduced direction (dx,dy) as a long; Value: frequency
            final Map<Long, Integer> dirCounts = new HashMap<>(Math.max(16, n * 2));

            for (int j = 0; j < n; j++) {
                if (j == i)
                    continue;

                int dx = store.getX(j) - ax;
                int dy = store.getY(j) - ay;

                // If duplicates exist, skip zero vector
                if (dx == 0 && dy == 0)
                    continue;

                int g = gcd(Math.abs(dx), Math.abs(dy));
                dx /= g;
                dy /= g;

                long key = packDirection(dx, dy);
                dirCounts.put(key, dirCounts.getOrDefault(key, 0) + 1);
            }

            // Count perpendicular pairs once per pivot
            for (Map.Entry<Long, Integer> e : dirCounts.entrySet()) {
                long key = e.getKey();
                int cnt = e.getValue();

                int dx = unpackDx(key);
                int dy = unpackDy(key);

                long perpKey = packDirection(dy, -dx);
                Integer perpCnt = dirCounts.get(perpKey);
                if (perpCnt != null && key < perpKey) {
                    total += (long) cnt * (long) perpCnt;
                }
            }
        }

        return total;
    }

    /**
     * Counts all right triangles in the store.
     *
     * @param store the point store to read coordinates from
     * @return the total number of right triangles
     */
    public static long countRightTriangles(PointStore store) {
        return countRightTrianglesRange(store, 0, store.numPoints());
    }

    /**
     * Packs a reduced direction vector (dx, dy) into a single long value.
     * Normalizes the direction so that equivalent opposite directions map
     * to the same key.
     *
     * @param dx the x-component of the direction
     * @param dy the y-component of the direction
     * @return a packed long representing the canonical direction
     */
    private static long packDirection(int dx, int dy) {
        if (dx < 0 || (dx == 0 && dy < 0)) {
            dx = -dx;
            dy = -dy;
        }
        return (((long) dx) << 32) ^ (dy & 0xffffffffL);
    }

    /**
     * Extracts the dx component from a packed direction key.
     *
     * @param key the packed direction
     * @return the dx component
     */
    private static int unpackDx(long key) {
        return (int) (key >> 32);
    }

    /**
     * Extracts the dy component from a packed direction key.
     *
     * @param key the packed direction
     * @return the dy component
     */
    private static int unpackDy(long key) {
        return (int) key;
    }

    /**
     * Computes the greatest common divisor of two non-negative integers
     * using the Euclidean algorithm. Returns 1 if both are zero.
     *
     * @param a first non-negative integer
     * @param b second non-negative integer
     * @return the greatest common divisor
     */
    private static int gcd(int a, int b) {
        if (a == 0)
            return (b == 0) ? 1 : b;
        if (b == 0)
            return a;
        while (b != 0) {
            int t = a % b;
            a = b;
            b = t;
        }
        return a;
    }
}