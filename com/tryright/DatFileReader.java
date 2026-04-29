package com.tryright;

// Explicit imports preferred over java.io.* to make dependencies clear at a glance.
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Reads and prints points from a binary .dat file.
 */
public class DatFileReader {
    private DatFileReader() { }
    /**
     * Entry point. Reads and displays all points from the specified .dat file.
     *
     * @param args command-line arguments: filename
     */
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java com.tryright.DatFileReader <filename>");
            return;
        }

        File file = new File(args[0]);
        if (!file.exists()) {
            System.out.println("File not found: " + file.getAbsolutePath());
            return;
        }

        try (DataInputStream dis = new DataInputStream(new FileInputStream(file))) {
            // File is raw pairs of 4-byte big-endian integers (x, y) with no header.
            // Compute number of points from file size.
            long fileSize = file.length();
            int totalPoints = (int) (fileSize / 8); // each point = 8 bytes (4 for X, 4 for Y)
            System.out.println("Reading " + totalPoints + " points:");

            int count = 0;
            // Read pairs while there are at least 8 bytes left (4 for X, 4 for Y)
            while (dis.available() >= 8) {
                int x = dis.readInt();
                int y = dis.readInt();

                System.out.printf("Point %d: (%d, %d)%n", ++count, x, y);
            }
        } catch (IOException e) {
            System.err.println("Error parsing the binary data.");
            e.printStackTrace();
        }
    }
}
