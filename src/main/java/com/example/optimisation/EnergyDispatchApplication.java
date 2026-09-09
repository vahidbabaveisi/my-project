package com.example.optimisation;

import org.ojalgo.optimisation.Expression;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.Optimisation;
import org.ojalgo.optimisation.Variable;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class EnergyDispatchApplication {

    private static final double TOLERANCE = 1e-6;

    public record Generator(
            String name,
            double fixedCost,
            double variableCost,
            double capacity,
            double minimumOutput) {

        public Generator {
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException(
                        "Generator name cannot be blank.");
            }

            if (capacity < 0
                    || minimumOutput < 0
                    || minimumOutput > capacity) {

                throw new IllegalArgumentException(
                        "Invalid output limits for " + name);
            }
        }
    }

    public record Scenario(
            String name,
            double demand) {

        public Scenario {
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException(
                        "Scenario name cannot be blank.");
            }

            if (demand < 0) {
                throw new IllegalArgumentException(
                        "Demand cannot be negative.");
            }
        }
    }

    public record DispatchSolution(
            Scenario scenario,
            Optimisation.State state,
            double[] operating,
            double[] generation,
            double objectiveValue) {
    }

    public record ValidationResult(
            boolean valid,
            double recomputedObjective,
            double maximumViolation) {
    }

    public static final class DataReader {

        public List<Generator> readGenerators(Path path)
                throws IOException {

            List<String> lines = Files.readAllLines(path);
            List<Generator> generators = new ArrayList<>();

            if (lines.isEmpty()
                    || !lines.get(0).trim().equals(
                    "name,fixedCost,variableCost,"
                            + "capacity,minimumOutput")) {

                throw new IllegalArgumentException(
                        "Invalid generator CSV header.");
            }

            for (int lineNumber = 1;
                    lineNumber < lines.size();
                    lineNumber++) {

                String line = lines.get(lineNumber);

                if (line.isBlank()) {
                    continue;
                }

                String[] fields = line.split(",", -1);

                if (fields.length != 5) {
                    throw new IllegalArgumentException(
                            "Expected 5 generator fields at line "
                                    + (lineNumber + 1));
                }

                try {
                    generators.add(new Generator(
                            fields[0].trim(),
                            Double.parseDouble(
                                    fields[1].trim()),
                            Double.parseDouble(
                                    fields[2].trim()),
                            Double.parseDouble(
                                    fields[3].trim()),
                            Double.parseDouble(
                                    fields[4].trim())));

                } catch (NumberFormatException exception) {
                    throw new IllegalArgumentException(
                            "Invalid generator number at line "
                                    + (lineNumber + 1),
                            exception);
                }
            }

            if (generators.isEmpty()) {
                throw new IllegalArgumentException(
                        "No generators were loaded.");
            }

            return List.copyOf(generators);
        }

        public List<Scenario> readScenarios(Path path)
                throws IOException {

            List<String> lines = Files.readAllLines(path);
            List<Scenario> scenarios = new ArrayList<>();

            if (lines.isEmpty()
                    || !lines.get(0).trim().equals(
                    "name,demand")) {

                throw new IllegalArgumentException(
                        "Invalid scenario CSV header.");
            }

            for (int lineNumber = 1;
                    lineNumber < lines.size();
                    lineNumber++) {

                String line = lines.get(lineNumber);

                if (line.isBlank()) {
                    continue;
                }

                String[] fields = line.split(",", -1);

                if (fields.length != 2) {
                    throw new IllegalArgumentException(
                            "Expected 2 scenario fields at line "
                                    + (lineNumber + 1));
                }

                try {
                    scenarios.add(new Scenario(
                            fields[0].trim(),
                            Double.parseDouble(
                                    fields[1].trim())));

                } catch (NumberFormatException exception) {
                    throw new IllegalArgumentException(
                            "Invalid scenario number at line "
                                    + (lineNumber + 1),
                            exception);
                }
            }

            if (scenarios.isEmpty()) {
                throw new IllegalArgumentException(
                        "No scenarios were loaded.");
            }

            return List.copyOf(scenarios);
        }
    }

    public static final class DispatchSolver {

        public DispatchSolution solve(
                List<Generator> generators,
                Scenario scenario) {

            ExpressionsBasedModel model =
                    new ExpressionsBasedModel();

            int generatorCount = generators.size();

            Variable[] operating =
                    new Variable[generatorCount];

            Variable[] generation =
                    new Variable[generatorCount];

            // Binary operating variables are added first.
            for (int i = 0; i < generatorCount; i++) {

                Generator generator = generators.get(i);

                operating[i] =
                        model.newVariable(
                                        "operating_"
                                                + generator.name())
                                .binary()
                                .weight(
                                        generator.fixedCost());
            }

            // Continuous generation variables are added next.
            for (int i = 0; i < generatorCount; i++) {

                Generator generator = generators.get(i);

                generation[i] =
                        model.newVariable(
                                        "generation_"
                                                + generator.name())
                                .lower(0)
                                .weight(
                                        generator.variableCost());
            }

            // Demand balance.
            Expression demandBalance =
                    model.newExpression(
                                    "demand_"
                                            + scenario.name())
                            .level(scenario.demand());

            for (Variable variable : generation) {
                demandBalance.set(variable, 1);
            }

            // Generator operating limits.
            for (int i = 0; i < generatorCount; i++) {

                Generator generator = generators.get(i);

                // p_i - capacity_i * y_i <= 0
                Expression maximumOutput =
                        model.newExpression(
                                        "maximum_"
                                                + generator.name())
                                .upper(0);

                maximumOutput.set(generation[i], 1);
                maximumOutput.set(
                        operating[i],
                        -generator.capacity());

                // p_i - minimum_i * y_i >= 0
                Expression minimumOutput =
                        model.newExpression(
                                        "minimum_"
                                                + generator.name())
                                .lower(0);

                minimumOutput.set(generation[i], 1);
                minimumOutput.set(
                        operating[i],
                        -generator.minimumOutput());
            }

            Optimisation.Result result =
                    model.minimise();

            if (!result.getState().isFeasible()) {
                return new DispatchSolution(
                        scenario,
                        result.getState(),
                        new double[generatorCount],
                        new double[generatorCount],
                        Double.NaN);
            }

            double[] operatingValues =
                    new double[generatorCount];

            double[] generationValues =
                    new double[generatorCount];

            for (int i = 0; i < generatorCount; i++) {

                operatingValues[i] =
                        result.doubleValue(i);

                generationValues[i] =
                        result.doubleValue(
                                generatorCount + i);
            }

            return new DispatchSolution(
                    scenario,
                    result.getState(),
                    operatingValues,
                    generationValues,
                    result.getValue());
        }
    }

    public static final class SolutionValidator {

        public ValidationResult validate(
                List<Generator> generators,
                DispatchSolution solution) {

            if (!solution.state().isFeasible()) {
                return new ValidationResult(
                        false,
                        Double.NaN,
                        Double.POSITIVE_INFINITY);
            }

            double totalGeneration = 0.0;
            double objective = 0.0;
            double maximumViolation = 0.0;

            for (int i = 0; i < generators.size(); i++) {

                Generator generator = generators.get(i);
                double y = solution.operating()[i];
                double p = solution.generation()[i];

                double integralityViolation =
                        Math.min(
                                Math.abs(y),
                                Math.abs(y - 1));

                double nonnegativityViolation =
                        Math.max(0, -p);

                double capacityViolation =
                        Math.max(
                                0,
                                p - generator.capacity() * y);

                double minimumViolation =
                        Math.max(
                                0,
                                generator.minimumOutput() * y
                                        - p);

                maximumViolation = Math.max(
                        maximumViolation,
                        integralityViolation);

                maximumViolation = Math.max(
                        maximumViolation,
                        nonnegativityViolation);

                maximumViolation = Math.max(
                        maximumViolation,
                        capacityViolation);

                maximumViolation = Math.max(
                        maximumViolation,
                        minimumViolation);

                totalGeneration += p;

                objective +=
                        generator.fixedCost() * y
                                + generator.variableCost() * p;
            }

            double demandViolation =
                    Math.abs(
                            totalGeneration
                                    - solution.scenario().demand());

            double objectiveDifference =
                    Math.abs(
                            objective
                                    - solution.objectiveValue());

            maximumViolation = Math.max(
                    maximumViolation,
                    demandViolation);

            maximumViolation = Math.max(
                    maximumViolation,
                    objectiveDifference);

            return new ValidationResult(
                    maximumViolation <= TOLERANCE,
                    objective,
                    maximumViolation);
        }
    }

    public static final class ResultWriter {

        public void write(
                Path outputPath,
                List<Generator> generators,
                List<DispatchSolution> solutions)
                throws IOException {

            Path parent = outputPath.toAbsolutePath().getParent();

            if (parent != null) {
                Files.createDirectories(parent);
            }

            try (BufferedWriter writer =
                         Files.newBufferedWriter(outputPath)) {

                writer.write(
                        "scenario,state,objective,"
                                + "generator,operating,generation");

                writer.newLine();

                for (DispatchSolution solution : solutions) {

                    if (!solution.state().isFeasible()) {
                        writer.write(String.format(
                                Locale.ROOT,
                                "%s,%s,,,,",
                                solution.scenario().name(),
                                solution.state()));

                        writer.newLine();
                        continue;
                    }

                    for (int i = 0;
                            i < generators.size();
                            i++) {

                        writer.write(String.format(
                                Locale.ROOT,
                                "%s,%s,%.4f,%s,%.4f,%.4f",
                                solution.scenario().name(),
                                solution.state(),
                                solution.objectiveValue(),
                                generators.get(i).name(),
                                solution.operating()[i],
                                solution.generation()[i]));

                        writer.newLine();
                    }
                }
            }
        }
    }

    private static void printSolution(
            List<Generator> generators,
            DispatchSolution solution,
            ValidationResult validation) {

        System.out.printf(
                "%nScenario: %s, demand = %.2f%n",
                solution.scenario().name(),
                solution.scenario().demand());

        System.out.println(
                "State: " + solution.state());

        if (!solution.state().isFeasible()) {
            System.out.println(
                    "No feasible dispatch found.");
            return;
        }

        for (int i = 0; i < generators.size(); i++) {
            System.out.printf(
                    "%-10s operating = %.0f, generation = %.2f%n",
                    generators.get(i).name(),
                    solution.operating()[i],
                    solution.generation()[i]);
        }

        System.out.printf(
                "Objective: %.2f%n",
                solution.objectiveValue());

        System.out.printf(
                "Maximum violation: %.8f%n",
                validation.maximumViolation());

        System.out.println(
                "Validated: " + validation.valid());
    }

    public static void main(String[] args) {

        Path generatorPath =
                args.length >= 1
                        ? Path.of(args[0])
                        : Path.of(
                                "src",
                                "main",
                                "resources",
                                "generators.csv");

        Path scenarioPath =
                args.length >= 2
                        ? Path.of(args[1])
                        : Path.of(
                                "src",
                                "main",
                                "resources",
                                "scenarios.csv");

        Path outputPath =
                args.length >= 3
                        ? Path.of(args[2])
                        : Path.of(
                                "target",
                                "dispatch-solutions.csv");

        try {
            DataReader reader = new DataReader();

            List<Generator> generators =
                    reader.readGenerators(generatorPath);

            List<Scenario> scenarios =
                    reader.readScenarios(scenarioPath);

            DispatchSolver solver =
                    new DispatchSolver();

            SolutionValidator validator =
                    new SolutionValidator();

            List<DispatchSolution> solutions =
                    new ArrayList<>();

            for (Scenario scenario : scenarios) {

                DispatchSolution solution =
                        solver.solve(generators, scenario);

                ValidationResult validation =
                        validator.validate(
                                generators,
                                solution);

                printSolution(
                        generators,
                        solution,
                        validation);

                solutions.add(solution);
            }

            new ResultWriter().write(
                    outputPath,
                    generators,
                    solutions);

            System.out.println(
                    "\nResults written to: "
                            + outputPath.toAbsolutePath());

        } catch (IOException exception) {
            System.err.println(
                    "File error: "
                            + exception.getMessage());

        } catch (IllegalArgumentException exception) {
            System.err.println(
                    "Invalid model data: "
                            + exception.getMessage());
        }
    }



}