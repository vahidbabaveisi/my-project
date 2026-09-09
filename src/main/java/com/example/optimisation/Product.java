package com.example.optimisation;


public record Product(
        String name,
        double unitProfit,
        double quantity) {

    public Product {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Product name cannot be blank.");
        }

        if (!Double.isFinite(unitProfit)) {
            throw new IllegalArgumentException(
                    "Unit profit must be finite.");
        }

        if (!Double.isFinite(quantity)
                || quantity < 0.0) {

            throw new IllegalArgumentException(
                    "Quantity must be finite and nonnegative.");
        }
    }
}