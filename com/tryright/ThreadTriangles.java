/*
# Name: Peter Hoang
# Date: 03/26/2026
# Class: CSC 4180-01: Spring 2026
# Pledge: I have neither given nor received unauthorized aid on this program.
# Description: Program 4: Runs the triangle counter using multiple threads.
  Distributes the workload across threads and aggregates partial results.
*/
package com.tryright;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Runs the triangle counter using multiple threads and aggregates results.
 *
 * @author Peter Hoang
 * @version 1.0
 */
public class ThreadTriangles {
    private ThreadTriangles() { }

    private static final int EXIT_OK = 0;
    private static final int EXIT_ERROR = 1;

    /**
     * Entry point. Parses arguments and delegates to the threaded counter.
     *
     * @param args command-line arguments: filename and numThreads
     */
    public static void main(String[] args) {
        if (args == null || args.length != 2) {
            printUsage("Wrong number of arguments.");
            System.exit(EXIT_ERROR);
            return;
        }

        final String filename = args[0];
        final int numThreads = parseThreadCount(args[1]);

        validateFile(filename);

        PointStore ps = null;
        try {
            ps = Triangles.openStore(filename);
            final int n = ps.numPoints();

            if (n == 0) {
                System.out.println(0);
                System.exit(EXIT_OK);
                return;
            }

            long total = runWorkers(filename, n, numThreads);
            System.out.println(total);
            System.exit(EXIT_OK);

        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(EXIT_ERROR);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Error: interrupted");
            System.exit(EXIT_ERROR);

        } finally {
            if (ps != null) ps.close();
        }
    }

    /**
     * Parses the thread count from a command-line argument string.
     * Exits with an error if the value is not a positive integer.
     *
     * @param arg the string to parse as the thread count
     * @return the parsed positive thread count
     */
    private static int parseThreadCount(String arg) {
        final int numThreads;
        try {
            numThreads = Integer.parseInt(arg);
        } catch (NumberFormatException e) {
            printUsage("<numThreads> must be an integer.");
            System.exit(EXIT_ERROR);
            return -1; // unreachable
        }

        if (numThreads <= 0) {
            printUsage("<numThreads> must be positive.");
            System.exit(EXIT_ERROR);
            return -1; // unreachable
        }
        return numThreads;
    }

    /**
     * Validates that the given file exists and is readable.
     * Exits with an error if validation fails.
     *
     * @param filename the path to the file to validate
     */
    private static void validateFile(String filename) {
        final File file = new File(filename);
        if (!file.exists()) {
            System.err.println("Error: No such file or directory");
            System.exit(EXIT_ERROR);
        }
        if (!file.canRead()) {
            System.err.println("Error: Permission denied");
            System.exit(EXIT_ERROR);
        }
    }

    /**
     * Creates and runs worker threads, each counting right triangles
     * over a disjoint range of pivot points. Each thread opens its own
     * PointStore for thread safety.
     *
     * @param filename   the path to the point file
     * @param n          the total number of points
     * @param numThreads the requested number of threads
     * @return the total number of right triangles found by all workers
     * @throws InterruptedException if a worker thread is interrupted
     */
    private static long runWorkers(String filename, int n, int numThreads)
            throws InterruptedException {

        final int t = Math.min(numThreads, n);
        final long[] partialCounts = new long[t];
        final AtomicBoolean errorFlag = new AtomicBoolean(false);
        final AtomicReference<String> errorMsg = new AtomicReference<>(null);
        final Thread[] workers = new Thread[t];

        for (int i = 0; i < t; i++) {
            final int start = i * n / t;
            final int end = (i + 1) * n / t;
            final int threadIndex = i;

            workers[i] = new Thread(() -> {
                PointStore local = null;
                try {
                    local = Triangles.openStore(filename);
                    partialCounts[threadIndex] =
                            Triangles.countRightTrianglesRange(local, start, end);
                } catch (IOException e) {
                    errorFlag.set(true);
                    errorMsg.compareAndSet(null, e.getMessage());
                } finally {
                    if (local != null) local.close();
                }
            });

            workers[i].start();
        }

        for (int i = 0; i < t; i++) {
            workers[i].join();
        }

        if (errorFlag.get()) {
            String msg = errorMsg.get();
            if (msg == null) msg = "worker thread failed";
            System.err.println("Error: " + msg);
            System.exit(EXIT_ERROR);
        }

        long total = 0L;
        for (int i = 0; i < t; i++) {
            total += partialCounts[i];
        }
        return total;
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
        System.err.println("Usage: java com.tryright.ThreadTriangles <filename> <numThreads>");
        System.err.println("Parameter(s): <filename> <numThreads>");
    }
}
