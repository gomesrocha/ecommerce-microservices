package br.com.ecommerce.mlbuilder.report;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class TrainingReportWriter {

    public void writeAll(List<TrainingReport> reports, Path reportsDir) {
        try {
            Files.createDirectories(reportsDir);

            for (TrainingReport report : reports) {
                writeMarkdownReport(report, reportsDir);
            }

            writeSummaryJson(reports, reportsDir.resolve("training-summary.json"));

            System.out.println();
            System.out.println("Relatórios de treino gerados em: " + reportsDir.toAbsolutePath());

        } catch (IOException exception) {
            throw new IllegalStateException("Falha ao gerar relatórios de treino", exception);
        }
    }

    private void writeMarkdownReport(TrainingReport report, Path reportsDir) throws IOException {
        Path output = reportsDir.resolve(report.modelVersion() + "-report.md");

        StringBuilder markdown = new StringBuilder();

        markdown.append("# Relatório de Treinamento - ")
                .append(report.modelVersion())
                .append("\n\n");

        markdown.append("## Identificação\n\n");
        markdown.append("| Campo | Valor |\n");
        markdown.append("|---|---|\n");
        markdown.append("| Modelo | ").append(report.modelName()).append(" |\n");
        markdown.append("| Versão | ").append(report.modelVersion()).append(" |\n");
        markdown.append("| Domínio | ").append(report.domain()).append(" |\n");
        markdown.append("| Algoritmo | ").append(report.algorithm()).append(" |\n");
        markdown.append("| Framework | ").append(report.framework()).append(" |\n");
        markdown.append("| Status | ").append(report.status()).append(" |\n");
        markdown.append("| Treinado em | ").append(report.trainedAt()).append(" |\n");
        markdown.append("| Dataset | ").append(report.datasetName()).append(" |\n");
        markdown.append("| Arquivo de entrada | `").append(report.inputPath()).append("` |\n");
        markdown.append("| Modelo gerado | `").append(report.outputPath()).append("` |\n\n");

        markdown.append("## Dados\n\n");
        markdown.append("| Métrica | Valor |\n");
        markdown.append("|---|---:|\n");
        markdown.append("| Registros de treino | ").append(report.trainSize()).append(" |\n");
        markdown.append("| Registros de teste | ").append(report.testSize()).append(" |\n");
        markdown.append("| Total de registros | ").append(report.totalRecords()).append(" |\n\n");

        markdown.append("## Features\n\n");
        for (String feature : report.features()) {
            markdown.append("- `").append(feature).append("`\n");
        }

        markdown.append("\nTarget: `").append(report.target()).append("`\n\n");

        markdown.append("## Métricas\n\n");
        markdown.append("| Métrica | Valor |\n");
        markdown.append("|---|---:|\n");

        for (Map.Entry<String, Object> metric : report.metrics().entrySet()) {
            markdown.append("| ")
                    .append(metric.getKey())
                    .append(" | ")
                    .append(metric.getValue())
                    .append(" |\n");
        }

        markdown.append("\n## Observações\n\n");
        markdown.append(report.notes() == null ? "" : report.notes()).append("\n");

        Files.writeString(output, markdown.toString());
    }

    private void writeSummaryJson(List<TrainingReport> reports, Path output) throws IOException {
        StringBuilder json = new StringBuilder();

        json.append("{\n");
        json.append("  \"generatedAt\": \"").append(java.time.Instant.now()).append("\",\n");
        json.append("  \"models\": [\n");

        for (int i = 0; i < reports.size(); i++) {
            TrainingReport report = reports.get(i);

            json.append("    {\n");
            json.append("      \"modelName\": \"").append(escape(report.modelName())).append("\",\n");
            json.append("      \"modelVersion\": \"").append(escape(report.modelVersion())).append("\",\n");
            json.append("      \"domain\": \"").append(escape(report.domain())).append("\",\n");
            json.append("      \"algorithm\": \"").append(escape(report.algorithm())).append("\",\n");
            json.append("      \"framework\": \"").append(escape(report.framework())).append("\",\n");
            json.append("      \"datasetName\": \"").append(escape(report.datasetName())).append("\",\n");
            json.append("      \"inputPath\": \"").append(escape(report.inputPath())).append("\",\n");
            json.append("      \"outputPath\": \"").append(escape(report.outputPath())).append("\",\n");
            json.append("      \"status\": \"").append(escape(report.status())).append("\",\n");
            json.append("      \"trainedAt\": \"").append(report.trainedAt()).append("\",\n");
            json.append("      \"trainSize\": ").append(report.trainSize()).append(",\n");
            json.append("      \"testSize\": ").append(report.testSize()).append(",\n");
            json.append("      \"totalRecords\": ").append(report.totalRecords()).append(",\n");

            json.append("      \"features\": [");
            for (int j = 0; j < report.features().size(); j++) {
                if (j > 0) {
                    json.append(", ");
                }
                json.append("\"").append(escape(report.features().get(j))).append("\"");
            }
            json.append("],\n");

            json.append("      \"target\": \"").append(escape(report.target())).append("\",\n");

            json.append("      \"metrics\": {\n");

            int metricIndex = 0;
            for (Map.Entry<String, Object> metric : report.metrics().entrySet()) {
                json.append("        \"")
                        .append(escape(metric.getKey()))
                        .append("\": ");

                Object value = metric.getValue();

                if (value instanceof Number || value instanceof Boolean) {
                    json.append(value);
                } else {
                    json.append("\"").append(escape(String.valueOf(value))).append("\"");
                }

                metricIndex++;

                if (metricIndex < report.metrics().size()) {
                    json.append(",");
                }

                json.append("\n");
            }

            json.append("      },\n");
            json.append("      \"notes\": \"").append(escape(report.notes())).append("\"\n");
            json.append("    }");

            if (i < reports.size() - 1) {
                json.append(",");
            }

            json.append("\n");
        }

        json.append("  ]\n");
        json.append("}\n");

        Files.writeString(output, json.toString());
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}