package simpledb.plan;

import java.util.*;

import simpledb.materialize.DistinctPlan;
import simpledb.materialize.GroupByPlan;
import simpledb.tx.Transaction;
import simpledb.metadata.*;
import simpledb.parse.*;

/**
 * A small improvement on the basic query planner.
 * @author Edward Sciore
 */
public class BetterQueryPlanner implements QueryPlanner {
   private MetadataMgr mdm;
   
   public BetterQueryPlanner(MetadataMgr mdm) {
      this.mdm = mdm;
   }
   
   /**
    * Creates a query plan as follows. It first takes
    * the product of all tables and views; it then selects on the predicate;
    * it groups the records if the query has a group by clause
    * or aggregation functions; it projects on the field list;
    * and finally it removes duplicates for a distinct query.
    */
   public Plan createPlan(QueryData data, Transaction tx) {
      //Step 1: Create a plan for each mentioned table or view.
     List<Plan> plans = new ArrayList<Plan>();
      for (String tblname : data.tables()) {
         String viewdef = mdm.getViewDef(tblname, tx);
         if (viewdef != null) { // Recursively plan the view.
            Parser parser = new Parser(viewdef);
            QueryData viewdata = parser.query();
            plans.add(createPlan(viewdata, tx));
         }
         else
            plans.add(new TablePlan(tx, tblname, mdm));
      }
      
      //Step 2: Create the product of all table plans
      Plan p = plans.remove(0);
      for (Plan nextplan : plans) {
         // Try both orderings and choose the one having lowest cost
         Plan choice1 = new ProductPlan(nextplan, p);
         Plan choice2 = new ProductPlan(p, nextplan);
         if (choice1.blocksAccessed() < choice2.blocksAccessed())
            p = choice1;
         else
            p = choice2;
      }            
      
      //Step 3: Add a selection plan for the predicate
      p = new SelectPlan(p, data.pred());

      if (!data.aggfns().isEmpty() || !data.groupfields().isEmpty())
         p = new GroupByPlan(tx, p, data.groupfields(), data.aggfns());
      
      //Step 4: Project on the field names
      p = new ProjectPlan(p, data.fields());
      if (data.isDistinct()) {
          p = new DistinctPlan(tx, p);
      }
      return p;
   }
}
