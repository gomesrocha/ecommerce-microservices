package br.com.ecommerce.mlops;

import java.util.List;
import java.util.Map;

public record ModelInfoResponse(
        String modelName,
        String modelVersion,
        String domain,
        String algorithm,
        String framework,
        String status,
        Map<String, Object> dataset,
        Map<String, Object> metrics,
        List<String> features,
        String target,
        List<String> usedBy,
        String modelPath,
        String trainedAt,
        String createdBy,
        String notes
) {
}