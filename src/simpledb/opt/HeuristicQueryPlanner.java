package simpledb.opt;

import java.util.*;

import simpledb.materialize.DistinctPlan;
import simpledb.materialize.GroupByPlan;
import simpledb.materialize.SortPlan;
import simpledb.tx.Transaction;
import simpledb.metadata.MetadataMgr;
import simpledb.parse.QueryData;
import simpledb.plan.*;

/**
 * A query planner that optimizes using a heuristic-based algorithm.
 * @author Edward Sciore
 */
public class HeuristicQueryPlanner implements QueryPlanner {
   private Collection<TablePlanner> tableplanners = new ArrayList<>();
   private MetadataMgr mdm;
   /**
    * Flags that restrict the join methods supported by the optimizer.
    * They are set by the joins command of SimpleIJ for the experiments.
    */
   public static boolean indexJoinEnabled = true;
   public static boolean mergeJoinEnabled = true;
   public static boolean hashJoinEnabled = true;
   public static boolean nestedLoopJoinEnabled = true;
   
   public HeuristicQueryPlanner(MetadataMgr mdm) {
      this.mdm = mdm;
   }
   
   /**
    * Creates an optimized left-deep query plan using the following
    * heuristics.
    * H1. Choose the smallest table (considering selection predicates)
    * to be first in the join order.
    * H2. Add the table to the join order which
    * results in the smallest output.
    * If the query has a group by clause or aggregation functions,
    * a group by node is added above the joins. The plan then
    * projects on the output fields. A distinct query adds a
    * distinct node, which also sorts the output by the order by
    * clause. Otherwise a sort node is added if the query has
    * an order by clause.
    */
   public Plan createPlan(QueryData data, Transaction tx) {
      tableplanners.clear();
      
      // Step 1:  Create a TablePlanner object for each mentioned table
      for (String tblname : data.tables()) {
         TablePlanner tp = new TablePlanner(tblname, data.pred(), tx, mdm);
         tableplanners.add(tp);
      }
      
      // Step 2:  Choose the lowest-size plan to begin the join order
      Plan currentplan = getLowestSelectPlan();
      
      // Step 3:  Repeatedly add a plan to the join order
      while (!tableplanners.isEmpty()) {
         Plan p = getLowestJoinPlan(currentplan);
         if (p != null)
            currentplan = p;
         else  // no applicable join
            currentplan = getLowestProductPlan(currentplan);
      }
      
      if (!data.aggfns().isEmpty() || !data.groupfields().isEmpty())
         currentplan = new GroupByPlan(tx, currentplan, data.groupfields(), data.aggfns());

      // Step 4.  Project on the field names and return
      Plan p = new ProjectPlan(currentplan, data.fields());
      if (data.isDistinct()) {
         p = new DistinctPlan(tx, p, data.order());
      } else if (!data.order().getPairs().isEmpty()) {
         p = new SortPlan(tx, p, data.order());
      }

      return p;
   }
   
   private Plan getLowestSelectPlan() {
      TablePlanner besttp = null;
      Plan bestplan = null;
      for (TablePlanner tp : tableplanners) {
         Plan plan = tp.makeSelectPlan();
         if (bestplan == null || plan.recordsOutput() < bestplan.recordsOutput()) {
            besttp = tp;
            bestplan = plan;
         }
      }
      tableplanners.remove(besttp);
      return bestplan;
   }
   
   private Plan getLowestJoinPlan(Plan current) {
      TablePlanner besttp = null;
      Plan bestplan = null;
      for (TablePlanner tp : tableplanners) {
         Plan plan = tp.makeJoinPlan(current);
         if (plan != null && (bestplan == null || plan.recordsOutput() < bestplan.recordsOutput())) {
            besttp = tp;
            bestplan = plan;
         }
      }
      if (bestplan != null)
         tableplanners.remove(besttp);
      return bestplan;
   }
   
   private Plan getLowestProductPlan(Plan current) {
      TablePlanner besttp = null;
      Plan bestplan = null;
      for (TablePlanner tp : tableplanners) {
         Plan plan = tp.makeProductPlan(current);
         if (bestplan == null || plan.recordsOutput() < bestplan.recordsOutput()) {
            besttp = tp;
            bestplan = plan;
         }
      }
      tableplanners.remove(besttp);
      return bestplan;
   }

   public void setPlanner(Planner p) {
      // for use in planning views, which
      // for simplicity this code doesn't do.
   }
}
