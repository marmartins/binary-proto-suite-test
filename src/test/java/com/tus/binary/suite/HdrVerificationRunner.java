package com.tus.binary.suite;

import com.tus.binary.suite.controller.MarketDataController;

import java.io.IOException;

public class HdrVerificationRunner {
    public static void main(String[] args) throws IOException {
        System.out.println("Starting HDR Verification...");

        MarketDataController controller = new MarketDataController();

        // Warmup / Load Generation
        int iterations = 10000;
        System.out.println("Running " + iterations + " iterations...");

        for (int i = 0; i < iterations; i++) {
            controller.testSbe();
        }

        System.out.println("Generating Report...");
        String result = controller.generateHdrReport();
        System.out.println(result);

        System.out.println("Done.");
    }
}
