package com.example.optimisation;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class App {

    public static void main(String[] args) {

        Path inputPath;

        if (args.length > 0) {
            inputPath = Path.of(args[0]);
        } else {
            inputPath =
                    Path.of(
                            "src",
                            "main",
                            "resources",
                            "products.csv");
        }

        try {
            List<Product> products =
                    CsvProductReader.read(inputPath);

            System.out.println("Production plan:");

            for (Product product : products) {
                System.out.printf(
                        "%s: quantity = %.2f, "
                                + "unit profit = %.2f%n",
                        product.name(),
                        product.quantity(),
                        product.unitProfit());
            }

            double objective =
                    ObjectiveCalculator.calculateProfit(
                            products);

            System.out.printf(
                    "%nObjective value: %.2f%n",
                    objective);

        } catch (IOException exception) {
            System.err.println(
                    "Could not read input file: "
                            + exception.getMessage());

        } catch (IllegalArgumentException exception) {
            System.err.println(
                    "Invalid model data: "
                            + exception.getMessage());
        }
    }
}