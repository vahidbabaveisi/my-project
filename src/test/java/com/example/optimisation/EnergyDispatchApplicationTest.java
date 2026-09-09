package com.example.optimisation;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnergyDispatchApplicationTest {

    @Test
    void solvesPeakDemandScenario() {

        List<EnergyDispatchApplication.Generator>
                generators = List.of(

                new EnergyDispatchApplication.Generator(
                        "Solar", 0, 0, 40, 0),

                new EnergyDispatchApplication.Generator(
                        "Battery", 12, 8, 30, 5),

                new EnergyDispatchApplication.Generator(
                        "Grid", 20, 25, 100, 10)
        );

        var scenario =
                new EnergyDispatchApplication.Scenario(
                        "Peak",
                        60);

        var solver =
                new EnergyDispatchApplication
                        .DispatchSolver();

        var solution =
                solver.solve(generators, scenario);

        assertTrue(solution.state().isOptimal());

        assertEquals(
                172.0,
                solution.objectiveValue(),
                1e-6);

        var validation =
                new EnergyDispatchApplication
                        .SolutionValidator()
                        .validate(
                                generators,
                                solution);

        assertTrue(validation.valid());

        double totalGeneration = 0;

        for (double value : solution.generation()) {
            totalGeneration += value;
        }

        assertEquals(
                60.0,
                totalGeneration,
                1e-6);
    }
}