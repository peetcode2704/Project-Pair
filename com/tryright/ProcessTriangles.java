/*
# Name: Peter Hoang
# Date: 02/01/2026
# Class: CSC 4180-01: Spring 2026
# Pledge: I have neither given nor received unauthorized aid on this program.
# Description: Program 1: This program serves as a manager that distributes the workload of
  counting right triangles across multiple system processes to utilize multicore CPUs.
*/
package com.tryright;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Runs the triangle counter using multiple processes and aggregates worker results.
 * Uses Triangles.openStore() (".dat" =&gt; BinPointStore, otherwise TextPointStore).
 *
 * @author Peter Hoang
 * @version 1.0
 */
public class ProcessTriangles {

    private ProcessTriangles() { }

    private static final int EXIT_OK = 0;
    private static final int EXIT_ERROR = 1;

    /**
     * Entry point. Parses arguments and delegates to the process-based counter.
     *
     * @param args command-line arguments: filename and numProcesses
     */
    public static void main(String[] args) {
        if (args == null || args.length != 2) {
            printUsage("Wrong number of arguments.");
            System.exit(EXIT_ERROR);
            return;
        }

        final String filename = args[0];
        final int requestedProcesses = parseProcessCount(args[1]);

        validateFile(filename);

        final int n = getPointCount(filename);

        if (n == 0) {
            System.out.println(0);
            System.exit(EXIT_OK);
            return;
        }

        long total = launchAndCollect(filename, n, requestedProcesses);
        System.out.println(total);
        System.exit(EXIT_OK);
    }

    /**
     * Parses the process count from a command-line argument string.
     * Exits with an error if the value is not a positive integer.
     *
     * @param arg the string to parse as the process count
     * @return the parsed positive process count
     */
    private static int parseProcessCount(String arg) {
        final int count;
        try {
            count = Integer.parseInt(arg);
        } catch (NumberFormatException e) {
            printUsage("<numProcesses> must be an integer.");
            System.exit(EXIT_ERROR);
            return -1; // unreachable
        }

        if (count <= 0) {
            printUsage("<numProcesses> must be positive.");
            System.exit(EXIT_ERROR);
            return -1; // unreachable
        }
        return count;
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
     * Opens the point store and returns the number of points.
     * Exits with an error if the store cannot be opened.
     *
     * @param filename the path to the point file
     * @return the number of points in the file
     */
    private static int getPointCount(String filename) {
        PointStore ps = null;
        try {
            ps = Triangles.openStore(filename);
            return ps.numPoints();
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(EXIT_ERROR);
            return -1; // unreachable
        } finally {
            if (ps != null) ps.close();
        }
    }

    /**
     * Launches worker processes, each counting right triangles over a
     * disjoint range of pivot points, and collects their results.
     * Exits with an error if any worker fails.
     *
     * @param filename           the path to the point file
     * @param n                  the total number of points
     * @param requestedProcesses the requested number of worker processes
     * @return the total number of right triangles found by all workers
     */
    private static long launchAndCollect(String filename, int n, int requestedProcesses) {
        final int p = Math.min(requestedProcesses, n);
        final List<Process> processes = new ArrayList<>(p);
        final List<BufferedReader> readers = new ArrayList<>(p);

        try {
            launchWorkers(filename, n, p, processes, readers);
            return collectResults(p, processes, readers);

        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(EXIT_ERROR);
            return -1; // unreachable

        } catch (InterruptedException e) {
            System.err.println("Error: interrupted");
            System.exit(EXIT_ERROR);
            return -1; // unreachable

        } finally {
            closeReaders(readers);
            destroyProcesses(processes);
        }
    }

    /**
     * Spawns worker child processes, each responsible for a disjoint
     * range of pivot points.
     *
     * @param filename  the path to the point file
     * @param n         the total number of points
     * @param p         the number of worker processes to launch
     * @param processes list to receive the launched Process objects
     * @param readers   list to receive the stdout readers for each process
     * @throws IOException if a process cannot be started
     */
    private static void launchWorkers(String filename, int n, int p,
            List<Process> processes, List<BufferedReader> readers) throws IOException {

        final String classPath = System.getProperty("java.class.path");

        for (int i = 0; i < p; i++) {
            final int start = i * n / p;
            final int end = (i + 1) * n / p;

            ProcessBuilder pb = new ProcessBuilder(
                    "java",
                    "-cp", classPath,
                    "com.tryright.Triangles",
                    filename,
                    String.valueOf(start),
                    String.valueOf(end)
            );

            pb.redirectError(ProcessBuilder.Redirect.INHERIT);

            Process proc = pb.start();
            processes.add(proc);
            readers.add(new BufferedReader(new InputStreamReader(proc.getInputStream())));
        }
    }

    /**
     * Collects and sums the results from all worker processes.
     * Exits with an error if any worker produces invalid output or exits
     * with a non-zero code.
     *
     * @param p         the number of worker processes
     * @param processes the list of launched processes
     * @param readers   the list of stdout readers
     * @return the total right triangle count from all workers
     * @throws InterruptedException if waiting for a process is interrupted
     * @throws IOException          if reading worker output fails
     */
    private static long collectResults(int p, List<Process> processes,
            List<BufferedReader> readers) throws InterruptedException, IOException {

        long total = 0L;

        for (int i = 0; i < p; i++) {
            Process proc = processes.get(i);
            BufferedReader br = readers.get(i);

            String line = br.readLine();

            int exitCode = proc.waitFor();
            if (exitCode != 0) {
                System.err.println("Error: worker process " + i + " failed");
                System.exit(EXIT_ERROR);
            }

            if (line == null) {
                System.err.println("Error: worker process " + i + " produced no output");
                System.exit(EXIT_ERROR);
            }

            try {
                total += Long.parseLong(line.trim());
            } catch (NumberFormatException e) {
                System.err.println("Error: worker process " + i + " produced invalid output");
                System.exit(EXIT_ERROR);
            }
        }

        return total;
    }

    /**
     * Closes all buffered readers, ignoring any exceptions.
     *
     * @param readers the list of readers to close
     */
    private static void closeReaders(List<BufferedReader> readers) {
        for (BufferedReader br : readers) {
            if (br != null) {
                try { br.close(); } catch (IOException ignored) { }
            }
        }
    }

    /**
     * Destroys all worker processes to prevent stray child processes.
     *
     * @param processes the list of processes to destroy
     */
    private static void destroyProcesses(List<Process> processes) {
        for (Process proc : processes) {
            if (proc != null) proc.destroy();
        }
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
        System.err.println("Usage: java com.tryright.ProcessTriangles <filename> <numProcesses>");
        System.err.println("Parameter(s): <filename> <numProcesses>");
    }
}
