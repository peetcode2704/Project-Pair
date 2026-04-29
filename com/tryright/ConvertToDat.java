package com.tryright;

// Explicit imports preferred over java.io.* to make dependencies clear at a glance.
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Converts text point files to binary .dat format.
 *
 * <p>Usage: {@code java com.tryright.ConvertToDat <input.txt>}</p>
 * <p>Output: {@code <input.dat>} in same directory</p>
 */
public class ConvertToDat {
    private ConvertToDat() { }
    /**
     * Entry point for the converter.
     *
     * @param args command-line arguments: input filename
     */
    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java com.tryright.ConvertToDat <input.txt>");
            System.exit(1);
        }

        String txtFile = args[0];
        String datFile = txtFile.replace(".txt", ".dat");

        System.out.println("Converting: " + txtFile);
        System.out.println("Output: " + datFile);

        File file = new File(txtFile);
        if (!file.exists()) {
            System.err.println("Error: File not found: " + txtFile);
            System.exit(1);
        }

        List<Integer> coords = new ArrayList<>();

        try (Scanner sc = new Scanner(file)) {
            if (!sc.hasNextInt()) {
                System.err.println("Error: Invalid format - no count");
                System.exit(1);
            }

            int n = sc.nextInt();
            System.out.println("Expected points: " + n);

            int count = 0;
            while (sc.hasNextInt() && count < n) {
                int x = sc.nextInt();
                if (!sc.hasNextInt()) {
                    System.err.println("Error: Missing Y coordinate");
                    System.exit(1);
                }
                int y = sc.nextInt();
                coords.add(x);
                coords.add(y);
                count++;
            }

            System.out.println("Read " + count + " points");

        } catch (IOException e) {
            System.err.println("Error reading: " + e.getMessage());
            System.exit(1);
        }

        // Write binary
        try (DataOutputStream out = new DataOutputStream(
                new BufferedOutputStream(new FileOutputStream(datFile)))) {

            for (int val : coords) {
                out.writeInt(val);  // Java writes big-endian by default
            }

            System.out.println("SUCCESS - wrote " + datFile);
            System.out.println("File size: " + (coords.size() * 4) + " bytes");

        } catch (IOException e) {
            System.err.println("Error writing: " + e.getMessage());
            System.exit(1);
        }
    }
}