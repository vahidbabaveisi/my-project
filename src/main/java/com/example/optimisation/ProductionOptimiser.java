package com.example.optimisation;

import org.ojalgo.optimisation.Expression;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.Optimisation;
import org.ojalgo.optimisation.Variable;

public class ProductionOptimiser {

    public record Solution(
            Optimisation.State state,
            double x,
            double y,
            double objectiveValue) {
    }

    public static Solution solve(boolean requireIntegers) {

        ExpressionsBasedModel model =
                new ExpressionsBasedModel();

 
 
        Variable x = model.newVariable("x")
                .lower(0)
                .weight(40);

        Variable y = model.newVariable("y")
                .lower(0)
                .weight(30);

        if (requireIntegers) {
            x.integer(true);
            y.integer(true);
        }

        Expression capacity1 =
                model.newExpression("Capacity_1")
                        .upper(41);

        capacity1.set(x, 2);
        capacity1.set(y, 1);

        Expression capacity2 =
                model.newExpression("Capacity_2")
                        .upper(50);

        capacity2.set(x, 1);
        capacity2.set(y, 2);

        Optimisation.Result result =
                model.maximise();


        if (!result.getState().isFeasible()) {
            return new Solution(
                    result.getState(),
                    Double.NaN,
                    Double.NaN,
                    Double.NaN);
        }

        return new Solution(
                result.getState(),
                result.doubleValue(0),
                result.doubleValue(1),
                result.getValue());
    }

    private static void printSolution(
            String title,
            Solution solution) {

        System.out.println(title);
        System.out.println("State: " + solution.state());

        if (!solution.state().isFeasible()) {
            System.out.println(
                    "No feasible solution is available.");
            return;
        }

        System.out.printf("x = %.4f%n", solution.x());
        System.out.printf("y = %.4f%n", solution.y());

        System.out.printf(
                "Objective = %.4f%n",
                solution.objectiveValue());

        double activity1 =
                2 * solution.x() + solution.y();

        double activity2 =
                solution.x() + 2 * solution.y();

        System.out.printf(
                "Constraint 1 activity = %.4f / 41%n",
                activity1);

        System.out.printf(
                "Constraint 2 activity = %.4f / 50%n",
                activity2);
    }

    public static void main(String[] args) {

        Solution lpSolution = solve(false);
        printSolution(
                "Continuous LP solution",
                lpSolution);

        System.out.println();

        Solution milpSolution = solve(true);
        printSolution(
                "Integer MILP solution",
                milpSolution);
    }
}