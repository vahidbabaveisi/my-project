package com.example.optimisation;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ObjectiveCalculatorTest {

    @Test
    void calculatesExpectedObjectiveValue() {

        List<Product> products = List.of(
                new Product("A", 50.0, 20.0),
                new Product("B", 35.0, 15.0),
                new Product("C", 20.0, 10.0)
        );

        double result =
                ObjectiveCalculator.calculateProfit(
                        products);

        assertEquals(1725.0, result, 1e-6);
    }

    @Test
    void emptyListHasZeroObjectiveValue() {

        double result =
                ObjectiveCalculator.calculateProfit(
                        List.of());

        assertEquals(0.0, result, 1e-6);
    }
}