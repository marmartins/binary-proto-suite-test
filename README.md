# Binary Protocol Suite - Performance Comparison

This project compares the performance of **Simple Binary Encoding (SBE)** and **Others** for serializing market data messages.

## Overview

The project implements serializers for the protocols and provides three ways to measure performance:
1. **JMH Benchmarks** - Microbenchmark framework for precise latency measurements
2. **HDR Histogram** - Latency percentile tracking for HTTP endpoint calls
3. **REST API** - Interactive testing via HTTP endpoints

## Prerequisites

- **Java 25** (JDK 25)
- **Maven 3.9+**
- **Windows/Linux** (tested environment)

## Building the Project

```bash
mvn clean compile
```

This command will:
- Generate SBE classes from `src/main/resources/sbe-schema.xml`
- Compile all source files

---

## Phase 1: JMH Benchmarks

JMH provides the most accurate microbenchmark measurements with statistical analysis.

### Running JMH Benchmarks

**Step 1: Build the benchmark JAR**
```bash
mvn clean package -Dmaven.test.skip=true
```
While running the above command, ensure there are no compilation errors.
YOu can see the error message:
```shell
# [ERROR] Unknown lifecycle phase ".test.skip=true". You must specify a valid lifecycle phase or a goal in the format <plugin-prefix>:<goal> or <plugin-group-id>:<plugin-artifact-id>[:<plugin-version>]:<goal>. Available lifecycle phases are: pre-clean, clean, post-clean, validate, initialize, generate-sources, process-sources, generate-resources, process-resources, compile, process-classes, generate-test-sources, process-test-sources, generate-test-resources, process-test-resources, test-compile, process-test-classes, test, prepare-package, package, pre-integration-test, integration-test, post-integration-test, verify, install, deploy, pre-site, site, post-site, site-deploy. -> [Help 1]

# It's because of PowerShell that interprets - as PowerShell params

# Use (--%) to scape PowerShell params
mvn clean package --% -Dmaven.test.skip=true
```


**Step 2: Execute benchmarks**
```bash
# In line command (Windows)
# Make sure to no run in PowerShell as it may misinterpret the args
java --add-opens java.base/sun.nio.ch=ALL-UNNAMED ^
     --add-opens java.base/java.nio=ALL-UNNAMED ^
     --add-opens java.base/java.util=ALL-UNNAMED ^
     --add-exports java.base/jdk.internal.misc=ALL-UNNAMED ^
     --add-opens java.base/jdk.internal.misc=ALL-UNNAMED ^
     --enable-native-access=ALL-UNNAMED ^
     -cp target/benchmarks.jar com.tus.binary.suite.benchmark.BenchmarkRunner
```

**Note:** JVM arguments are required for Agrona (SBE dependency) to access internal APIs.

### JMH Results

Results are saved as `yyyyMMDD_HHmmss_jmh_result.md` in the project root.

**Example Output:**
```
| Benchmark              | Mode        | Score (ns/op) | Error   | Unit  | GC Alloc Rate |
|------------------------|-------------|---------------|---------|-------|---------------|
| sbeSerialize           | AverageTime | 73.62         | ± 1.88  | ns/op | 320.00 B/op   |
| sbeDeserialize         | AverageTime | 110.91        | ± 6.53  | ns/op | 704.00 B/op   |
```

**Interpretation:**
- **Score**: Lower is better (average time per operation in nanoseconds)
- **Error**: Confidence interval (±) at 99.9%
- **GC Alloc Rate**: Memory allocated per operation (lower = less GC pressure)

**Key Findings:**
- SBE is ~2.8x faster for serialization (73.6ns vs 211.3ns)
- SBE is ~2.5x faster for deserialization (110.9ns vs 273.4ns)
- SBE allocates ~3x less memory for serialization (320 B/op vs 952 B/op)
- SBE allocates ~2x less memory for deserialization (704 B/op vs 1424 B/op)

---

## Phase 2: HDR Histogram (HTTP Latency Tracking)

HDR Histogram tracks latency percentiles for real-world HTTP endpoint calls.

### Running HDR Verification

**Option A: Using Maven (with required JVM args)**
```bash
#$env:MAVEN_OPTS="--add-opens java.base/sun.nio.ch=ALL-UNNAMED --add-opens java.base/java.nio=ALL-UNNAMED --add-opens java.base/java.util=ALL-UNNAMED --add-exports java.base/jdk.internal.misc=ALL-UNNAMED --add-opens java.base/jdk.internal.misc=ALL-UNNAMED --enable-native-access=ALL-UNNAMED"
#mvn test-compile exec:java -Dexec.mainClass=com.tus.binary.suite.HdrVerificationRunner -Dexec.classpathScope=test

$env:MAVEN_OPTS="--add-opens java.base/sun.nio.ch=ALL-UNNAMED --add-opens java.base/java.nio=ALL-UNNAMED --add-opens java.base/java.util=ALL-UNNAMED --add-exports java.base/jdk.internal.misc=ALL-UNNAMED --add-opens java.base/jdk.internal.misc=ALL-UNNAMED --enable-native-access=ALL-UNNAMED"; mvn test-compile exec:java "-Dexec.mainClass=com.tus.binary.suite.HdrVerificationRunner" "-Dexec.classpathScope=test"
```

**Option B: Run Spring Boot App and Test Manually**
```bash
mvn spring-boot:run
```

Then generate load and report:
1. Call endpoints to generate traffic:
   - `GET http://localhost:8080/api/test/sbe`
2. Generate report:
   - `GET http://localhost:8080/api/test/hdr/report`

### HDR Results

Results are saved as:
- `yyyyMMDD_HHmmss_hdr_result.hgrm` (text file with percentile statistics)
- `yyyyMMDD_HHmmss_hdr_result.png` (latency distribution plot)

**Example Text Output:**
```
Protobuf Serialize:
  Count: 10000
  Min:   180 ns
  Mean:  215.45 ns
  Max:   1250 ns
  P50:   210 ns
  P90:   230 ns
  P99:   280 ns
  P99.9: 450 ns

SBE Serialize:
  Count: 10000
  Min:   65 ns
  Mean:  74.12 ns
  Max:   890 ns
  P50:   72 ns
  P90:   82 ns
  P99:   110 ns
  P99.9: 185 ns
```

**Interpretation:**
- **Count**: Total number of operations measured
- **Min/Max**: Fastest/slowest operation
- **Mean**: Average latency
- **P50 (Median)**: 50% of operations completed faster than this
- **P90**: 90% of operations completed faster than this
- **P99**: 99% of operations completed faster than this (tail latency)
- **P99.9**: 99.9% of operations completed faster than this (extreme tail)

**Key Findings:**
- Lower percentiles = better performance
- Smaller gap between P50 and P99.9 = more consistent latency
- SBE shows better performance across all percentiles

---

## Phase 3: REST API Testing

### Start the Application

```bash
# Spring Boot app needs the same JVM arguments that we used for the HDR verification
#$env:MAVEN_OPTS="--add-opens java.base/sun.nio.ch=ALL-UNNAMED --add-opens java.base/java.nio=ALL-UNNAMED --add-opens java.base/java.util=ALL-UNNAMED --add-exports java.base/jdk.internal.misc=ALL-UNNAMED --add-opens java.base/jdk.internal.misc=ALL-UNNAMED --enable-native-access=ALL-UNNAMED"; java -jar target/binary-proto-suite-test-0.0.1-SNAPSHOT.jar

# After fix the pom.xml to include the required JVM args when running spring-boot:run
mvn spring-boot:run
```

### Available Endpoints

**1. Test Protobuf Serialization**
```bash
curl http://localhost:8080/api/test/sbe
```

**3. Generate HDR Report**
```bash
curl http://localhost:8080/api/test/hdr/report
```

Response:
```json
{
  "protocol": "SBE",
  "size": 89,
  "match": true,
  "original": "MarketDataPayload[...]",
  "decoded": "MarketDataPayload[...]"
}
```

---

## Project Structure

```
binary-proto-suite/
├── src/main/
│   ├── java/com/tus/binary/suite/
│   │   ├── benchmark/          # JMH benchmark classes
│   │   ├── controller/         # REST controllers
│   │   ├── dto/                # Data Transfer Objects
│   │   └── service/            # Serializers
│   └── resources/              # SBE schema
├── src/test/                   # Test classes (HDR verification)
├── target/
│   ├── benchmarks.jar          # Executable JMH JAR
│   └── generated-sources/      # Auto-generated classes
├── yyyyMMDD_HHmmss_jmh_result.md       # JMH results
├── yyyyMMDD_HHmmss_hdr_result.hgrm     # HDR text results
├── yyyyMMDD_HHmmss_hdr_result.png      # HDR plot
└── README.md
```

---

## Performance Summary

Based on benchmark results:

| Metric                  | SBE      | Winner |
|-------------------------|----------|--------|
| Serialization Speed     | 73.6 ns  | **SBE (2.8x faster)** |
| Deserialization Speed   | 110.9 ns | **SBE (2.5x faster)** |
| Serialization Memory    | 320 B/op | **SBE (3x less)** |
| Deserialization Memory  | 704 B/op | **SBE (2x less)** |

**Conclusion:**
SBE is the clear winner for low-latency, high-throughput scenarios where:
- Consistent sub-microsecond latency is critical
- Minimal GC pressure is required
- Message format can be pre-defined and fixed

Protobuf is better suited for:
- Cross-platform/cross-language systems
- Schema evolution requirements
- Developer productivity over raw performance

---

## Troubleshooting

### JVM Arguments Required
If you see `IllegalAccessError` related to Agrona/Unsafe, ensure JVM arguments are set:
```
--add-opens java.base/sun.nio.ch=ALL-UNNAMED
--add-opens java.base/java.nio=ALL-UNNAMED
--add-exports java.base/jdk.internal.misc=ALL-UNNAMED
--enable-native-access=ALL-UNNAMED
```

### Maven Build Issues
```bash
# Force clean and rebuild
mvn clean compile -U

# Skip tests during build
mvn clean package -Dmaven.test.skip=true
```

---

## License

This is a performance testing suite for educational purposes.
