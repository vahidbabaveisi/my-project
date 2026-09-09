package com.example.optimisation;

import org.ojalgo.optimisation.Expression;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.Optimisation;
import org.ojalgo.optimisation.Variable;

public class facilityLocation {

   private final double[] fixedCost;
   private final double[] capacity;
   private final double[] demand;
   private final double[][] transportCost;

   public facilityLocation() {
       fixedCost = new double[] {100, 120};
       capacity = new double[] {55, 45};
       demand = new double[] {30, 40, 20};
       transportCost = new double[][] {
               {2, 4, 5},
               {3, 1, 6}
       };
   }

   private ExpressionsBasedModel buildModel(Variable[] open, Variable[][] shipment) {
       ExpressionsBasedModel model = new ExpressionsBasedModel();

       for (int i = 0; i < open.length; i++) {
           open[i] = model.newVariable("open_" + i)
                   .binary()
                   .weight(fixedCost[i]);
       }

       for (int i = 0; i < capacity.length; i++) {
           for (int j = 0; j < demand.length; j++) {
               shipment[i][j] = model.newVariable("ship_" + i + "_" + j)
                       .lower(0)
                       .weight(transportCost[i][j]);
           }
       }

       for (int i = 0; i < capacity.length; i++) {
           Expression facilityCapacity = model.newExpression("capacity_" + i)
                   .upper(0);
           facilityCapacity.set(open[i], -capacity[i]);
           for (int j = 0; j < demand.length; j++) {
               facilityCapacity.set(shipment[i][j], 1);
           }
       }

       for (int j = 0; j < demand.length; j++) {
           Expression demandBalance = model.newExpression("demand_" + j)
                   .lower(demand[j])
                   .upper(demand[j]);
           for (int i = 0; i < capacity.length; i++) {
               demandBalance.set(shipment[i][j], 1);
           }
       }

       return model;
   }

   public ExpressionsBasedModel model() {
       Variable[] open = new Variable[fixedCost.length];
       Variable[][] shipment = new Variable[capacity.length][demand.length];
       return buildModel(open, shipment);
   }

   public void reportResults() {
       Variable[] open = new Variable[fixedCost.length];
       Variable[][] shipment = new Variable[capacity.length][demand.length];
       ExpressionsBasedModel model = buildModel(open, shipment);
       Optimisation.Result result = model.maximise();

       System.out.println("Facility location results");
       System.out.println("State: " + result.getState());

       if (!result.getState().isFeasible()) {
           System.out.println("No feasible solution found.");
           return;
       }

       System.out.printf("Objective value = %.2f%n", result.getValue());

       for (int i = 0; i < open.length; i++) {
           int variableIndex = model.indexOf(open[i]);
           System.out.printf("Facility %d open = %.0f%n", i, result.doubleValue(variableIndex));
       }

       for (int i = 0; i < capacity.length; i++) {
           for (int j = 0; j < demand.length; j++) {
               int variableIndex = model.indexOf(shipment[i][j]);
               System.out.printf(
                       "Ship from facility %d to customer %d = %.2f%n",
                       i,
                       j,
                       result.doubleValue(variableIndex));
           }
       }
   }

   public static void main(String[] args) {
       new facilityLocation().reportResults();
   }
}
