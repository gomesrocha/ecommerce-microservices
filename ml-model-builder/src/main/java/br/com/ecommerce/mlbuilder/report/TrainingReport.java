package br.com.ecommerce.mlbuilder.report;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record TrainingReport(
        String modelName,
        String modelVersion,
        String domain,
        String algorithm,
        String framework,
        String datasetName,
        String inputPath,
        String outputPath,
        String status,
        Instant trainedAt,
        long trainSize,
        long testSize,
        long totalRecords,
        List<String> features,
        String target,
        Map<String, Object> metrics,
        String notes
) {
}