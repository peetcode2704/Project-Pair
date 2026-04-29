#!/usr/bin/env bash

################################################################################
# Test Script for Right Triangle Counting Programs
# Author: Peter Hoang
# Course: CSC 4180
# Date: February 5, 2026
#
# Tests Programs 3 according to the test plan
################################################################################

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
if [[ -d "$SCRIPT_DIR/com" ]]; then
    ROOT="$SCRIPT_DIR"
else
    ROOT="$SCRIPT_DIR/Peter_Hoang_Program0"
fi
TEST_DIR="$ROOT/test"

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Counters
TESTS_PASSED=0
TESTS_FAILED=0
TESTS_TOTAL=0

# Process/thread counts (capped at 8 cores)
PROCESS_COUNTS=(1 2 4 8)
THREAD_COUNTS=(1 2 4 8)

################################################################################
# Helper Functions
################################################################################

print_header() {
    echo ""
    echo "================================================================================"
    echo -e "${BLUE}$1${NC}"
    echo "================================================================================"
}

print_test() {
    echo ""
    echo -e "${YELLOW}Test $1: $2${NC}"
    echo "--------------------------------------------------------------------------------"
}

print_pass() {
    echo -e "${GREEN}✓ PASS${NC}: $1"
    ((TESTS_PASSED++))
    ((TESTS_TOTAL++))
}

print_fail() {
    echo -e "${RED}✗ FAIL${NC}: $1"
    ((TESTS_FAILED++))
    ((TESTS_TOTAL++))
}

print_info() {
    echo -e "  $1"
}

# Run a Java program from the project root.
# Usage: run_test <ClassName> [args...]
run_test() {
    local program="$1"
    shift
    local output exit_code
    output=$(cd "$ROOT" && java com.tryright."$program" "$@" 2>&1)
    exit_code=$?
    echo "$output"
    return $exit_code
}

# Assert exit 0 and trimmed stdout matches $expected.
# Usage: test_output <label> <ClassName> <expected> [args...]
test_output() {
    local test_name="$1"
    local program="$2"
    local expected="$3"
    shift 3
    local args=("$@")

    local output exit_code
    output=$(run_test "$program" "${args[@]}")
    exit_code=$?

    if [[ $exit_code -eq 0 ]]; then
        output=$(echo "$output" | xargs)
        if [[ "$output" == "$expected" ]]; then
            print_pass "$test_name: $program ${args[*]} → $output"
            return 0
        else
            print_fail "$test_name: $program ${args[*]} → Expected: $expected, Got: $output"
            return 1
        fi
    else
        print_fail "$test_name: $program ${args[*]} → Program exited with code $exit_code"
        print_info "Output: $output"
        return 1
    fi
}

# Assert exit 0, stdout matches $expected, AND wall time is under $max_seconds.
# Uses bash $SECONDS builtin — works on macOS and Linux.
# Usage: test_output_timed <label> <ClassName> <expected> <max_seconds> [args...]
test_output_timed() {
    local test_name="$1"
    local program="$2"
    local expected="$3"
    local max_seconds="$4"
    shift 4
    local args=("$@")

    local output exit_code start_time elapsed

    start_time=$SECONDS
    output=$(cd "$ROOT" && java com.tryright."$program" "${args[@]}" 2>&1)
    exit_code=$?
    elapsed=$(( SECONDS - start_time ))

    print_info "Time: ${elapsed}s  (limit: ${max_seconds}s)"

    if [[ $exit_code -ne 0 ]]; then
        print_fail "$test_name: $program ${args[*]} → Program exited with code $exit_code"
        print_info "Output: $output"
        return 1
    fi

    output=$(echo "$output" | xargs)

    local ok=true
    if [[ "$output" != "$expected" ]]; then
        print_fail "$test_name: $program ${args[*]} → Expected: $expected, Got: $output"
        ok=false
    fi
    if [[ $elapsed -gt $max_seconds ]]; then
        print_fail "$test_name: $program ${args[*]} → Exceeded time limit (${elapsed}s > ${max_seconds}s)"
        ok=false
    fi

    if $ok; then
        print_pass "$test_name: $program ${args[*]} → $output  [${elapsed}s]"
        return 0
    fi
    return 1
}

# Assert program exits non-zero (error required).
# Usage: test_error <label> <ClassName> [args...]
test_error() {
    local test_name="$1"
    local program="$2"
    shift 2
    local args=("$@")

    local output exit_code
    output=$(run_test "$program" "${args[@]}")
    exit_code=$?

    if [[ $exit_code -ne 0 ]]; then
        print_pass "$test_name: $program ${args[*]} → Correctly reported error (exit $exit_code)"
        print_info "Error message: $output"
        return 0
    else
        print_fail "$test_name: $program ${args[*]} → Should have failed but succeeded"
        print_info "Output: $output"
        return 1
    fi
}

################################################################################
# Compilation
################################################################################

print_header "COMPILING PROGRAMS"

echo "Project root : $ROOT"
echo "Compiling    : javac com/tryright/*.java"

if (cd "$ROOT" && javac com/tryright/*.java 2>&1); then
    echo -e "${GREEN}✓ Compilation successful${NC}"
else
    echo -e "${RED}✗ Compilation failed — aborting${NC}"
    exit 1
fi

################################################################################
# Benchmarking mode — bash run_tests.sh --benchmark
#
# Varies process/thread counts across 1 2 4 8 16 32 using test_long_list.dat,
# records wall-clock time for each run, and writes results to test/benchmark.csv.
# Run make_graph.py with the CSV to regenerate the performance charts.
################################################################################

if [[ "$1" == "--benchmark" ]]; then
    # Optional second argument overrides the default benchmark file.
    # Usage: bash run_tests.sh --benchmark [file]
    BENCH_FILE="${2:-test/test_long_list.dat}"
    BENCH_COUNTS=(1 2 4 8 16 32)
    BENCH_CSV="$TEST_DIR/benchmark.csv"

    print_header "BENCHMARKING MODE"
    echo "Input file : $BENCH_FILE"
    echo "Workers    : ${BENCH_COUNTS[*]}"
    echo "Output CSV : $BENCH_CSV"

    # Write CSV header
    echo "mode,workers,real_seconds" > "$BENCH_CSV"

    # Time a single run and append one row to the CSV.
    bench_run() {
        local mode="$1" program="$2" workers="$3"
        shift 3
        local start end elapsed output exit_code
        start=$(python3 -c "import time; print(time.time())")
        output=$(cd "$ROOT" && java com.tryright."$program" "$@" 2>&1)
        exit_code=$?
        end=$(python3 -c "import time; print(time.time())")
        elapsed=$(python3 -c "print(round($end - $start, 3))")

        if [[ $exit_code -ne 0 ]]; then
            echo -e "  ${RED}ERROR${NC}: $program $* failed (exit $exit_code) — $output"
            return 1
        fi

        echo -e "  workers=$workers  ${elapsed}s  →  result: $output"
        echo "$mode,$workers,$elapsed" >> "$BENCH_CSV"
    }

    echo ""
    echo -e "${YELLOW}ProcessTriangles${NC}"
    echo "--------------------------------------------------------------------------------"
    for w in "${BENCH_COUNTS[@]}"; do
        bench_run "process" "ProcessTriangles" "$w" "$BENCH_FILE" "$w"
    done

    echo ""
    echo -e "${YELLOW}ThreadTriangles${NC}"
    echo "--------------------------------------------------------------------------------"
    for w in "${BENCH_COUNTS[@]}"; do
        bench_run "thread" "ThreadTriangles" "$w" "$BENCH_FILE" "$w"
    done

    echo ""
    echo -e "${GREEN}Benchmark complete. Results written to $BENCH_CSV${NC}"
    echo "Run: python3 make_graph.py $BENCH_CSV"
    exit 0
fi

################################################################################
# Test directory and fixture setup
################################################################################

if [[ ! -d "$TEST_DIR" ]]; then
    echo -e "${RED}✗ Test directory not found: $TEST_DIR${NC}"
    exit 1
fi

# Ensure test_no_perm.txt exists then lock it down
NO_PERM_FILE="$TEST_DIR/test_no_perm.txt"
if [[ ! -f "$NO_PERM_FILE" ]]; then
    printf '3\n0 0\n3 0\n0 4\n' > "$NO_PERM_FILE"
fi
chmod 000 "$NO_PERM_FILE"

################################################################################
# Test Execution
################################################################################

print_header "RUNNING TESTS"

# ==============================================================================
# Test 1 (10 pts): Spec list — txt AND dat, expect 4
# ==============================================================================
print_test "1" "Spec list — expect 4  [10 pts]"

print_info "--- Text format (test_spec_list-1.txt) ---"
test_output "Test1-Triangles"          "Triangles"        "4" "test/test_spec_list-1.txt"
for p in "${PROCESS_COUNTS[@]}"; do
    test_output "Test1-Process-$p"     "ProcessTriangles"  "4" "test/test_spec_list-1.txt" "$p"
done
for t in "${THREAD_COUNTS[@]}"; do
    test_output "Test1-Thread-$t"      "ThreadTriangles"   "4" "test/test_spec_list-1.txt" "$t"
done

print_info "--- Binary format (test_spec_list-1.dat) ---"
test_output "Test1-Triangles-dat"      "Triangles"        "4" "test/test_spec_list-1.dat"
for p in "${PROCESS_COUNTS[@]}"; do
    test_output "Test1-Process-$p-dat" "ProcessTriangles"  "4" "test/test_spec_list-1.dat" "$p"
done
for t in "${THREAD_COUNTS[@]}"; do
    test_output "Test1-Thread-$t-dat"  "ThreadTriangles"   "4" "test/test_spec_list-1.dat" "$t"
done

# ==============================================================================
# Test 2 (20 pts): Long list — txt AND dat, expect 32909, max 2 min
# ==============================================================================
print_test "2" "Long list — expect 32909, max 2 min  [20 pts]"

print_info "--- Text format (test_long_list.txt) ---"
test_output_timed "Test2-Triangles"          "Triangles"        "32909" 120 "test/test_long_list.txt"
for p in "${PROCESS_COUNTS[@]}"; do
    test_output_timed "Test2-Process-$p"     "ProcessTriangles"  "32909" 120 "test/test_long_list.txt" "$p"
done
for t in "${THREAD_COUNTS[@]}"; do
    test_output_timed "Test2-Thread-$t"      "ThreadTriangles"   "32909" 120 "test/test_long_list.txt" "$t"
done

print_info "--- Binary format (test_long_list.dat) ---"
test_output_timed "Test2-Triangles-dat"      "Triangles"        "32909" 120 "test/test_long_list.dat"
for p in "${PROCESS_COUNTS[@]}"; do
    test_output_timed "Test2-Process-$p-dat" "ProcessTriangles"  "32909" 120 "test/test_long_list.dat" "$p"
done
for t in "${THREAD_COUNTS[@]}"; do
    test_output_timed "Test2-Thread-$t-dat"  "ThreadTriangles"   "32909" 120 "test/test_long_list.dat" "$t"
done

# ==============================================================================
# Test 3 (15 pts): Time list — txt only, expect 2161, max 30 sec
# ==============================================================================
print_test "3" "Time list — expect 2161, max 30 sec  [15 pts]"

test_output_timed "Test3-Triangles"      "Triangles"        "2161" 30 "test/test_time_list.txt"
for p in "${PROCESS_COUNTS[@]}"; do
    test_output_timed "Test3-Process-$p" "ProcessTriangles"  "2161" 30 "test/test_time_list.txt" "$p"
done
for t in "${THREAD_COUNTS[@]}"; do
    test_output_timed "Test3-Thread-$t"  "ThreadTriangles"   "2161" 30 "test/test_time_list.txt" "$t"
done

# ==============================================================================
# Test 4 (10 pts): No read permission — graceful error, non-zero exit
# ==============================================================================
print_test "4" "No read permission — graceful error  [10 pts]"

test_error "Test4-Triangles"  "Triangles"        "test/test_no_perm.txt"
test_error "Test4-Process-1"  "ProcessTriangles" "test/test_no_perm.txt" "1"
test_error "Test4-Thread-1"   "ThreadTriangles"  "test/test_no_perm.txt" "1"

# ==============================================================================
# Test 5 (10 pts): Non-existent file — graceful error, non-zero exit
# ==============================================================================
print_test "5" "Non-existent file — graceful error  [10 pts]"

print_info "--- Text (nofile.txt) ---"
test_error "Test5-Triangles"      "Triangles"        "test/nofile.txt"
test_error "Test5-Process-1"      "ProcessTriangles" "test/nofile.txt" "1"
test_error "Test5-Thread-1"       "ThreadTriangles"  "test/nofile.txt" "1"

print_info "--- Binary (nofile.dat) ---"
test_error "Test5-Triangles-dat"  "Triangles"        "test/nofile.dat"
test_error "Test5-Process-1-dat"  "ProcessTriangles" "test/nofile.dat" "1"
test_error "Test5-Thread-1-dat"   "ThreadTriangles"  "test/nofile.dat" "1"

# ==============================================================================
# Test 6 (10 pts): Truncated file — graceful error, non-zero exit
# ==============================================================================
print_test "6" "Truncated file — graceful error  [10 pts]"

test_error "Test6-Triangles"  "Triangles"        "test/test_too_short.txt"
test_error "Test6-Process-1"  "ProcessTriangles" "test/test_too_short.txt" "1"
test_error "Test6-Thread-1"   "ThreadTriangles"  "test/test_too_short.txt" "1"

# ==============================================================================
# Test 7 (5 pts): Two points — txt only, expect 0
# ==============================================================================
print_test "7" "Two points — expect 0  [5 pts]"

test_output "Test7-Triangles"      "Triangles"        "0" "test/test_two_points.txt"
for p in "${PROCESS_COUNTS[@]}"; do
    test_output "Test7-Process-$p" "ProcessTriangles"  "0" "test/test_two_points.txt" "$p"
done
for t in "${THREAD_COUNTS[@]}"; do
    test_output "Test7-Thread-$t"  "ThreadTriangles"   "0" "test/test_two_points.txt" "$t"
done

# ==============================================================================
# Test 8 (5 pts): Not a triangle — txt only, expect 0
# ==============================================================================
print_test "8" "Not a triangle — expect 0  [5 pts]"

test_output "Test8-Triangles"      "Triangles"        "0" "test/test_not_triangle.txt"
for p in "${PROCESS_COUNTS[@]}"; do
    test_output "Test8-Process-$p" "ProcessTriangles"  "0" "test/test_not_triangle.txt" "$p"
done
for t in "${THREAD_COUNTS[@]}"; do
    test_output "Test8-Thread-$t"  "ThreadTriangles"   "0" "test/test_not_triangle.txt" "$t"
done

# ==============================================================================
# Test 9 (15 pts): Giant triangle — txt AND dat, expect 12
# ==============================================================================
print_test "9" "Giant triangle — expect 12  [15 pts]"

print_info "--- Text format (test_giant_triangle.txt) ---"
test_output "Test9-Triangles"          "Triangles"        "12" "test/test_giant_triangle.txt"
for p in "${PROCESS_COUNTS[@]}"; do
    test_output "Test9-Process-$p"     "ProcessTriangles"  "12" "test/test_giant_triangle.txt" "$p"
done
for t in "${THREAD_COUNTS[@]}"; do
    test_output "Test9-Thread-$t"      "ThreadTriangles"   "12" "test/test_giant_triangle.txt" "$t"
done

print_info "--- Binary format (test_giant_triangle.dat) ---"
test_output "Test9-Triangles-dat"      "Triangles"        "12" "test/test_giant_triangle.dat"
for p in "${PROCESS_COUNTS[@]}"; do
    test_output "Test9-Process-$p-dat" "ProcessTriangles"  "12" "test/test_giant_triangle.dat" "$p"
done
for t in "${THREAD_COUNTS[@]}"; do
    test_output "Test9-Thread-$t-dat"  "ThreadTriangles"   "12" "test/test_giant_triangle.dat" "$t"
done

# ==============================================================================
# Bonus: Remaining binary-only files
# ==============================================================================
print_test "B1" "Triangle binary (triangle.dat) — expect 1"
test_output "B1-Triangles"      "Triangles"        "1" "test/triangle.dat"
for p in "${PROCESS_COUNTS[@]}"; do
    test_output "B1-Process-$p" "ProcessTriangles"  "1" "test/triangle.dat" "$p"
done
for t in "${THREAD_COUNTS[@]}"; do
    test_output "B1-Thread-$t"  "ThreadTriangles"   "1" "test/triangle.dat" "$t"
done

print_test "B2" "Collinear binary (collinear.dat) — expect 0"
test_output "B2-Triangles"      "Triangles"        "0" "test/collinear.dat"
for p in "${PROCESS_COUNTS[@]}"; do
    test_output "B2-Process-$p" "ProcessTriangles"  "0" "test/collinear.dat" "$p"
done
for t in "${THREAD_COUNTS[@]}"; do
    test_output "B2-Thread-$t"  "ThreadTriangles"   "0" "test/collinear.dat" "$t"
done

print_test "B3" "Square binary (square.dat) — expect 4"
test_output "B3-Triangles"      "Triangles"        "4" "test/square.dat"
for p in "${PROCESS_COUNTS[@]}"; do
    test_output "B3-Process-$p" "ProcessTriangles"  "4" "test/square.dat" "$p"
done
for t in "${THREAD_COUNTS[@]}"; do
    test_output "B3-Thread-$t"  "ThreadTriangles"   "4" "test/square.dat" "$t"
done

print_test "B4" "Corrupt binary (bad.dat) — graceful error"
test_error "B4-Triangles"  "Triangles"        "test/bad.dat"
test_error "B4-Process-1"  "ProcessTriangles" "test/bad.dat" "1"
test_error "B4-Thread-1"   "ThreadTriangles"  "test/bad.dat" "1"

print_test "B5" "Scaletest txt vs dat — results must match"
SCALE_EXPECTED="$(cd "$ROOT" && java com.tryright.Triangles test/scaletest.txt 2>/dev/null | xargs)"
print_info "--- Text format (scaletest.txt) — expect $SCALE_EXPECTED ---"
test_output "B5-Triangles"          "Triangles"        "$SCALE_EXPECTED" "test/scaletest.txt"
for p in "${PROCESS_COUNTS[@]}"; do
    test_output "B5-Process-$p"     "ProcessTriangles"  "$SCALE_EXPECTED" "test/scaletest.txt" "$p"
done
for t in "${THREAD_COUNTS[@]}"; do
    test_output "B5-Thread-$t"      "ThreadTriangles"   "$SCALE_EXPECTED" "test/scaletest.txt" "$t"
done

print_info "--- Binary format (scaletest.dat) — expect $SCALE_EXPECTED ---"
test_output "B5-Triangles-dat"      "Triangles"        "$SCALE_EXPECTED" "test/scaletest.dat"
for p in "${PROCESS_COUNTS[@]}"; do
    test_output "B5-Process-$p-dat" "ProcessTriangles"  "$SCALE_EXPECTED" "test/scaletest.dat" "$p"
done
for t in "${THREAD_COUNTS[@]}"; do
    test_output "B5-Thread-$t-dat"  "ThreadTriangles"   "$SCALE_EXPECTED" "test/scaletest.dat" "$t"
done

################################################################################
# Cleanup
################################################################################

chmod 644 "$NO_PERM_FILE" 2>/dev/null

################################################################################
# Summary
################################################################################

print_header "TEST SUMMARY"

echo ""
echo "Total Tests: $TESTS_TOTAL"
echo -e "${GREEN}Passed: $TESTS_PASSED${NC}"
echo -e "${RED}Failed: $TESTS_FAILED${NC}"
echo ""

if [[ $TESTS_FAILED -eq 0 ]]; then
    echo -e "${GREEN}═══════════════════════════════════════════════════════════════════════════════${NC}"
    echo -e "${GREEN}                    ALL TESTS PASSED! ✓                                        ${NC}"
    echo -e "${GREEN}═══════════════════════════════════════════════════════════════════════════════${NC}"
    exit 0
else
    echo -e "${RED}═══════════════════════════════════════════════════════════════════════════════${NC}"
    echo -e "${RED}                    SOME TESTS FAILED ✗                                         ${NC}"
    echo -e "${RED}═══════════════════════════════════════════════════════════════════════════════${NC}"
    exit 1
fi
