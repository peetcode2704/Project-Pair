# Right Triangle Counter

A high-performance Java application that counts the number of right triangles that can be formed from a set of 2D points. Supports multi-process and multi-thread parallelism for scalable computation.

## Overview

Given a file of 2D integer coordinates, this program determines how many unique right triangles can be formed using any three points. A triangle is considered "right" when the dot product of two edge vectors sharing a vertex equals zero.

## Architecture

The project uses a **PointStore abstraction** to decouple point storage format from triangle-counting logic:

```
PointStore (interface)
├── TextPointStore   — reads text-encoded point files
└── BinPointStore    — reads binary-encoded files via memory-mapped I/O
```

### Execution Modes

| Class | Mode | Description |
|-------|------|-------------|
| `Triangles` | Single-process | Counts all right triangles sequentially |
| `ProcessTriangles` | Multi-process | Distributes pivot ranges across child processes |
| `ThreadTriangles` | Multi-thread | Distributes pivot ranges across threads |

## File Format

### Text Format (`.txt`)
```
3
0 0
3 0
0 4
```
First line: number of points. Subsequent lines: `x y` pairs.

### Binary Format (`.dat`)
Each point is encoded as two 4-byte big-endian integers (x, then y). No header — point count is inferred from file size.

## Usage

```bash
# Compile
javac com/tryright/*.java

# Single process
java com.tryright.Triangles points.txt

# Multi-process (4 workers)
java com.tryright.ProcessTriangles points.txt 4

# Multi-thread (4 threads)
java com.tryright.ThreadTriangles points.dat 4
```

## Algorithm

For each pivot point, the algorithm:
1. Computes direction vectors to all other points
2. Reduces directions to canonical form using GCD normalization
3. Groups directions by slope using a HashMap
4. Counts perpendicular direction pairs (dot product = 0)

**Time Complexity:** O(n² log n) per pivot → O(n³ log n) total, reduced via parallelism.

## Utilities

| File | Purpose |
|------|---------|
| `ConvertToDat.java` | Converts text point files to binary `.dat` format |
| `DatFileReader.java` | Prints contents of a `.dat` file for debugging |
| `MMIOReader.java` | Reads and displays binary files using memory-mapped I/O |

## Author

**Peter Hoang** — CSC 4180: Operating Systems, Spring 2026
