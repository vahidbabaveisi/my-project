package com.example.optimisation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ProductTest {

    @Test
    void rejectsNegativeQuantity() {

        assertThrows(
                IllegalArgumentException.class,
                () -> new Product(
                        "A",
                        50.0,
                        -1.0));
    }

    @Test
    void rejectsBlankName() {

        assertThrows(
                IllegalArgumentException.class,
                () -> new Product(
                        " ",
                        50.0,
                        10.0));
    }
}