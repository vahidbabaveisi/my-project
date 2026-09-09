package com.example.optimisation;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class CsvProductReader {

    private CsvProductReader() {
        // Utility class: prevent object creation.
    }

    public static List<Product> read(Path path)
            throws IOException {

        List<Product> products = new ArrayList<>();

        try (BufferedReader reader =
                     Files.newBufferedReader(path)) {

            String header = reader.readLine();

            if (header == null) {
                throw new IllegalArgumentException(
                        "CSV file is empty.");
            }

            if (!header.trim().equals(
                    "name,unitProfit,quantity")) {

                throw new IllegalArgumentException(
                        "Unexpected CSV header: " + header);
            }

            String line;
            int lineNumber = 1;

            while ((line = reader.readLine()) != null) {
                lineNumber++;

                if (line.isBlank()) {
                    continue;
                }

                String[] fields = line.split(",", -1);

                if (fields.length != 3) {
                    throw new IllegalArgumentException(
                            "Expected 3 fields at line "
                                    + lineNumber);
                }

                String name = fields[0].trim();

                try {
                    double unitProfit =
                            Double.parseDouble(
                                    fields[1].trim());

                    double quantity =
                            Double.parseDouble(
                                    fields[2].trim());

                    products.add(
                            new Product(
                                    name,
                                    unitProfit,
                                    quantity));

                } catch (NumberFormatException exception) {
                    throw new IllegalArgumentException(
                            "Invalid number at line "
                                    + lineNumber
                                    + ": " + line,
                            exception);
                }
            }
        }

        if (products.isEmpty()) {
            throw new IllegalArgumentException(
                    "No product rows were found.");
        }

        return List.copyOf(products);
    }
}