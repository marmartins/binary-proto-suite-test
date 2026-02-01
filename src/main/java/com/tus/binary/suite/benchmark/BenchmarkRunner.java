package com.tus.binary.suite.benchmark;

import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;

public class BenchmarkRunner {

    static void main(String[] args) throws RunnerException, IOException {
        Options opt = new OptionsBuilder()
                .include(SerializationBenchmark.class.getSimpleName())
                .forks(1)
                .warmupIterations(3)
                .measurementIterations(5)
                .addProfiler(GCProfiler.class)
                .build();

        Collection<RunResult> results = new Runner(opt).run();

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMDD_HHmmss"));
        String fileName = timestamp + "_jmh_result.md";

        try (PrintWriter writer = new PrintWriter(new FileWriter(fileName))) {
            writer.println("# JMH Benchmark Results - " + timestamp);
            writer.println();
            writer.println("| Benchmark | Mode | Score (ns/op) | Error | Unit | GC Alloc Rate |");
            writer.println("|---|---|---|---|---|---|");

            for (RunResult result : results) {
                String benchmarkName = result.getParams().getBenchmark();
                benchmarkName = benchmarkName.substring(benchmarkName.lastIndexOf(".") + 1);
                String mode = result.getParams().getMode().name();
                double score = result.getPrimaryResult().getScore();
                double error = result.getPrimaryResult().getStatistics().getMeanErrorAt(0.99);
                String unit = result.getPrimaryResult().getScoreUnit();

                String gcAlloc = "";
                var secondary = result.getSecondaryResults();
                if (secondary.containsKey("gc.alloc.rate.norm")) {
                    gcAlloc = String.format("%.2f B/op", secondary.get("gc.alloc.rate.norm").getScore());
                }

                writer.printf("| %s | %s | %.2f | ± %.2f | %s | %s |%n",
                        benchmarkName, mode, score, error, unit, gcAlloc);
            }

            System.out.println("Results written to " + fileName);
        }
    }
}
