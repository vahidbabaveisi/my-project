package com.example.optimisation;

import java.util.List;

public final class ObjectiveCalculator {

    private ObjectiveCalculator() {
    }

    public static double calculateProfit(
            List<Product> products) {

        if (products == null) {
            throw new IllegalArgumentException(
                    "Products cannot be null.");
        }

        double objectiveValue = 0.0;

        for (Product product : products) {
            objectiveValue +=
                    product.unitProfit()
                            * product.quantity();
        }

        return objectiveValue;
    }
}