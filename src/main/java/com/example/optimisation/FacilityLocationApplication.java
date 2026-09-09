package com.example.optimisation;

import org.ojalgo.optimisation.Optimisation;
import org.ojalgo.optimisation.Variable;
import org.ojalgo.optimisation.Expression;
import org.ojalgo.optimisation.ExpressionsBasedModel;

public class FacilityLocationApplication {

    private static final double TOLERANCE = 1e-6;

    public record FacilityLocationData(
            String[] facilityNames,
            String[] customerNames,
            double[] fixedCosts,
            double[][] transportationCosts,
            double[] capacity,
            double[] customerDemands) {

        public FacilityLocationData {
            if (facilityNames.length != fixedCosts.length) {
                throw new IllegalArgumentException(
                        "Number of facility names must match number of fixed costs.");
            }
            if (customerNames.length != transportationCosts[0].length) {
                throw new IllegalArgumentException(
                        "Number of customer names must match number of transportation cost columns.");
            }
            if (customerDemands.length != customerNames.length) {
                throw new IllegalArgumentException(
                        "Number of customer demands must match number of customer names.");
            }

            for (int i = 0; i < facilityNames.length; i++) {
                if (facilityNames[i] == null || facilityNames[i].isBlank() || facilityNames[i].isEmpty()) {
                    throw new IllegalArgumentException(
                            "Facility names must be non-null and non-empty.");
                }
                if (fixedCosts[i] < 0) {
                    throw new IllegalArgumentException(
                            "Fixed costs must be non-negative.");
                }
            }

            for (int j = 0; j < customerNames.length; j++) {
                if (customerNames[j] == null || customerNames[j].isBlank() || customerNames[j].isEmpty()) {
                    throw new IllegalArgumentException(
                            "Customer names must be non-null and non-empty.");
                }
                if (customerDemands[j] < 0) {
                    throw new IllegalArgumentException(
                            "Customer demands must be non-negative.");
                }
            }

        }

        public int getNumberOfFacilities() {
            return facilityNames.length;
        }

        public int getNumberOfCustomers() {
            return customerNames.length;
        }

        //
    }

    public record FacilityLocationSolution(
            Optimisation.State state,
            double[] open,
            double[][] shipment,
            double objectiveValue) {
    }

    public record ValidationResult(
            boolean feasible,
            double recomputedObjective,
            double maximumViolation,
            double objectiveDifference) {
    }

    public static final class FacilityLocationSolver {

        public FacilityLocationSolution solve(FacilityLocationData data) {

            ExpressionsBasedModel model = new ExpressionsBasedModel();

            int numFacilities = data.getNumberOfFacilities();
            int numCustomers = data.getNumberOfCustomers();

            Variable[] open = new Variable[numFacilities];
            Variable[][] shipment = new Variable[numFacilities][numCustomers];

            for (int i = 0; i < numFacilities; i++) {
                open[i] = model.addVariable("open_" + data.facilityNames()[i])
                        .binary()
                        .weight(data.fixedCosts()[i]);
            }
            for (int i = 0; i < numFacilities; i++) {
                for (int j = 0; j < numCustomers; j++) {
                    shipment[i][j] = model
                            .addVariable("shipment_" + data.facilityNames()[i] + "_" + data.customerNames()[j])
                            .lower(0)
                            .weight(data.transportationCosts()[i][j]);
                }
            }

            // Facility capacity constraints
            for (int i = 0; i < numFacilities; i++) {
                Expression constraint = model.newExpression("capacity_" + data.facilityNames()[i])
                        .upper(0);
                constraint.set(open[i], -data.capacity()[i]);

                for (int j = 0; j < numCustomers; j++) {
                    constraint.set(shipment[i][j], 1);
                }
            }

            // Customer demand equality constraints
            for (int j = 0; j < numCustomers; j++) {
                Expression constraint = model.newExpression("demand_" + data.customerNames()[j])
                        .level(data.customerDemands()[j]);
                for (int i = 0; i < numFacilities; i++) {
                    constraint.set(shipment[i][j], 1);
                }
            }

            Optimisation.Result result = model.minimise();
            if (result.getState().isFeasible()) {
                double[] openValues = new double[numFacilities];
                double[][] shipmentValues = new double[numFacilities][numCustomers];

                for (int i = 0; i < numFacilities; i++) {
                    openValues[i] = open[i].getValue().doubleValue();
                    for (int j = 0; j < numCustomers; j++) {
                        shipmentValues[i][j] = shipment[i][j].getValue().doubleValue();
                    }
                }

                return new FacilityLocationSolution(
                        result.getState(),
                        openValues,
                        shipmentValues,
                        result.getValue());
            } else {
                return new FacilityLocationSolution(
                        result.getState(),
                        null,
                        null,
                        Double.NaN);
            }
        }

    }

    public static final class SolutionValidator {

        public ValidationResult validate(
                FacilityLocationData data,
                FacilityLocationSolution solution) {

            if (!solution.state().isFeasible()) {
                return new ValidationResult(
                        false,
                        Double.NaN,
                        Double.POSITIVE_INFINITY,
                        Double.POSITIVE_INFINITY);
            }

            int facilityCount = data.getNumberOfFacilities();
            int customerCount = data.getNumberOfCustomers();

            double maximumViolation = 0.0;
            double recomputedObjective = 0.0;

            for (int i = 0; i < facilityCount; i++) {

                double openValue = solution.open()[i];

                // Distance from the nearest binary value.
                double integralityViolation = Math.min(
                        Math.abs(openValue),
                        Math.abs(openValue - 1.0));

                maximumViolation = Math.max(
                        maximumViolation,
                        integralityViolation);

                recomputedObjective += data.fixedCosts()[i] * openValue;

                double totalShipment = 0.0;

                for (int j = 0; j < customerCount; j++) {

                    double quantity = solution.shipment()[i][j];

                    double nonnegativityViolation = Math.max(0.0, -quantity);

                    maximumViolation = Math.max(
                            maximumViolation,
                            nonnegativityViolation);

                    totalShipment += quantity;

                    recomputedObjective += data.transportationCosts()[i][j]
                            * quantity;
                }

                double availableCapacity = data.capacity()[i] * openValue;

                double capacityViolation = Math.max(
                        0.0,
                        totalShipment
                                - availableCapacity);

                maximumViolation = Math.max(
                        maximumViolation,
                        capacityViolation);
            }

            for (int j = 0; j < customerCount; j++) {

                double delivered = 0.0;

                for (int i = 0; i < facilityCount; i++) {

                    delivered += solution.shipment()[i][j];
                }

                double demandViolation = Math.abs(
                        delivered
                                - data.customerDemands()[j]);

                maximumViolation = Math.max(
                        maximumViolation,
                        demandViolation);
            }

            double objectiveDifference = Math.abs(
                    recomputedObjective
                            - solution.objectiveValue());

            maximumViolation = Math.max(
                    maximumViolation,
                    objectiveDifference);

            return new ValidationResult(
                    maximumViolation <= TOLERANCE,
                    recomputedObjective,
                    maximumViolation,
                    objectiveDifference);
        }
    }

    public static final class SolutionReporter {

        public void print(
                FacilityLocationData data,
                FacilityLocationSolution solution,
                ValidationResult validation) {

            System.out.println(
                    "Solver state: " + solution.state());

            if (!solution.state().isFeasible()) {
                System.out.println(
                        "No feasible solution is available.");
                return;
            }

            System.out.println("\nFacility decisions:");

            for (int i = 0; i < data.getNumberOfFacilities(); i++) {

                double usedCapacity = 0.0;

                for (int j = 0; j < data.getNumberOfCustomers(); j++) {

                    usedCapacity += solution.shipment()[i][j];
                }

                System.out.printf(
                        "%s: open = %.0f, usage = %.2f / %.2f%n",
                        data.facilityNames()[i],
                        solution.open()[i],
                        usedCapacity,
                        data.capacity()[i]);
            }

            System.out.println("\nShipments:");

            for (int i = 0; i < data.getNumberOfFacilities(); i++) {

                for (int j = 0; j < data.getNumberOfCustomers(); j++) {

                    double quantity = solution.shipment()[i][j];

                    if (quantity > TOLERANCE) {
                        System.out.printf(
                                "%s -> %s: %.2f%n",
                                data.facilityNames()[i],
                                data.customerNames()[j],
                                quantity);
                    }
                }
            }

            System.out.printf(
                    "%nSolver objective: %.2f%n",
                    solution.objectiveValue());

            System.out.printf(
                    "Recomputed objective: %.2f%n",
                    validation.recomputedObjective());

            System.out.printf(
                    "Maximum violation: %.8f%n",
                    validation.maximumViolation());

            System.out.println(
                    "Independently validated: "
                            + validation.feasible());
        }
    }

    private static FacilityLocationData createExampleData() {

        return new FacilityLocationData(
                new String[] { "F1", "F2" },
                new String[] { "C1", "C2", "C3" },
                new double[] { 100, 120 },
                new double[][] {
                        { 2, 4, 5 },
                        { 3, 1, 6 }
                },
                new double[] { 30, 40 },
                new double[] { 20, 15, 10 });
    }

    public static void main(String[] args) {

        FacilityLocationData data = createExampleData();

        FacilityLocationSolver solver = new FacilityLocationSolver();

        FacilityLocationSolution solution = solver.solve(data);

        SolutionValidator validator = new SolutionValidator();

        ValidationResult validation = validator.validate(data, solution);

        SolutionReporter reporter = new SolutionReporter();

        reporter.print(data, solution, validation);
    }

}