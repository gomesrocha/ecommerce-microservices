package br.com.ecommerce.ml;

public record OlistRouteBaselineRow(
        String originState,
        String destinationState,
        Long samples,
        Integer minDays,
        Integer estimatedDays,
        Integer maxDays,
        Double avgDays,
        String source,
        String modelVersion
) {

    public static OlistRouteBaselineRow fromCsvLine(String line) {
        String[] columns = line.split(",", -1);

        if (columns.length < 9) {
            throw new IllegalArgumentException("Linha CSV inválida para baseline Olist: " + line);
        }

        return new OlistRouteBaselineRow(
                columns[0].trim().toUpperCase(),
                columns[1].trim().toUpperCase(),
                Long.valueOf(columns[2].trim()),
                Integer.valueOf(columns[3].trim()),
                Integer.valueOf(columns[4].trim()),
                Integer.valueOf(columns[5].trim()),
                Double.valueOf(columns[6].trim()),
                columns[7].trim(),
                columns[8].trim()
        );
    }
}