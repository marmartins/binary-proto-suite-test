package com.tus.binary.suite.controller;

import com.tus.binary.suite.dto.MarketDataPayload;
import com.tus.binary.suite.dto.ValidationResult;
import com.tus.binary.suite.service.ProtobufSerializer;
import com.tus.binary.suite.service.ProtocolSerializer;
import com.tus.binary.suite.service.SbeSerializer;
import org.HdrHistogram.Histogram;
import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.style.Styler;
import org.knowm.xchart.style.markers.SeriesMarkers;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/test")
public class MarketDataController {

    private final ProtocolSerializer protobufSerializer;
    private final ProtocolSerializer sbeSerializer;

    // Histograms
    private final Histogram sbeSerializeHist = new Histogram(3);
    private final Histogram sbeDeserializeHist = new Histogram(3);
    private final Histogram pbSerializeHist = new Histogram(3);
    private final Histogram pbDeserializeHist = new Histogram(3);

    public MarketDataController() {
        this.protobufSerializer = new ProtobufSerializer();
        this.sbeSerializer = new SbeSerializer();
    }

    @GetMapping("/protobuf")
    public ValidationResult testProtobuf() {
        MarketDataPayload payload = MarketDataPayload.createSample();

        try {
            long start = System.nanoTime();
            byte[] bytes = protobufSerializer.serialize(payload);
            pbSerializeHist.recordValue(System.nanoTime() - start);

            start = System.nanoTime();
            MarketDataPayload decoded = protobufSerializer.deserialize(bytes);
            pbDeserializeHist.recordValue(System.nanoTime() - start);

            boolean match = payload.equals(decoded);
            return new ValidationResult("Protobuf", bytes.length, match, payload.toString(), decoded.toString());
        } catch (IOException e) {
            return new ValidationResult("Protobuf", 0, false, e.getMessage(), "");
        }
    }

    @GetMapping("/sbe")
    public ValidationResult testSbe() {
        MarketDataPayload payload = MarketDataPayload.createSample();

        try {
            long start = System.nanoTime();
            byte[] bytes = sbeSerializer.serialize(payload);
            sbeSerializeHist.recordValue(System.nanoTime() - start);

            start = System.nanoTime();
            MarketDataPayload decoded = sbeSerializer.deserialize(bytes);
            sbeDeserializeHist.recordValue(System.nanoTime() - start);

            boolean match = payload.equals(decoded);
            return new ValidationResult("SBE", bytes.length, match, payload.toString(), decoded.toString());
        } catch (IOException e) {
            return new ValidationResult("SBE", 0, false, e.getMessage(), "");
        }
    }

    @GetMapping("/hdr/report")
    public String generateHdrReport() throws IOException {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String baseName = timestamp + "_hdr_result.hgrm";

        // 1. Generate Text Report
        try (PrintWriter writer = new PrintWriter(new FileWriter(baseName))) {
            writer.println("HDR Histogram Report - " + timestamp);
            writer.println("==================================================");
            printHistogram(writer, "Protobuf Serialize", pbSerializeHist);
            printHistogram(writer, "Protobuf Deserialize", pbDeserializeHist);
            printHistogram(writer, "SBE Serialize", sbeSerializeHist);
            printHistogram(writer, "SBE Deserialize", sbeDeserializeHist);
        }

        // 2. Generate Plot
        XYChart chart = new XYChartBuilder().width(800).height(600).title("Latency Distribution")
                .xAxisTitle("Percentile").yAxisTitle("Latency (ns)").theme(Styler.ChartTheme.Matlab).build();
        chart.getStyler().setYAxisLogarithmic(true);
        chart.getStyler().setLegendVisible(true);

        addSeriesToChart(chart, "Proto Ser", pbSerializeHist);
        addSeriesToChart(chart, "Proto Deser", pbDeserializeHist);
        addSeriesToChart(chart, "SBE Ser", sbeSerializeHist);
        addSeriesToChart(chart, "SBE Deser", sbeDeserializeHist);

        String pngFileName = baseName.replace(".hgrm", ".png");
        BitmapEncoder.saveBitmap(chart, pngFileName, BitmapEncoder.BitmapFormat.PNG);

        return "Report generated: " + baseName + " and " + pngFileName;
    }

    private void printHistogram(PrintWriter writer, String name, Histogram hist) {
        writer.printf("%s:%n", name);
        writer.printf("  Count: %d%n", hist.getTotalCount());
        writer.printf("  Min:   %d ns%n", hist.getMinValue());
        writer.printf("  Mean:  %.2f ns%n", hist.getMean());
        writer.printf("  Max:   %d ns%n", hist.getMaxValue());
        writer.printf("  P50:   %d ns%n", hist.getValueAtPercentile(50));
        writer.printf("  P90:   %d ns%n", hist.getValueAtPercentile(90));
        writer.printf("  P99:   %d ns%n", hist.getValueAtPercentile(99));
        writer.printf("  P99.9: %d ns%n", hist.getValueAtPercentile(99.9));
        writer.println("--------------------------------------------------");
    }

    private void addSeriesToChart(XYChart chart, String seriesName, Histogram hist) {
        if (hist.getTotalCount() == 0)
            return;

        List<Double> xData = new ArrayList<>();
        List<Double> yData = new ArrayList<>();

        double[] percentiles = { 0, 10, 20, 30, 40, 50, 60, 70, 80, 90, 95, 99, 99.9, 99.99 };
        for (double p : percentiles) {
            xData.add(p);
            yData.add((double) hist.getValueAtPercentile(p));
        }

        chart.addSeries(seriesName, xData, yData).setMarker(SeriesMarkers.NONE);
    }

}
