package simpledb.opt;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import simpledb.materialize.MergeJoinPlan;
import simpledb.materialize.HashJoinPlan;
import simpledb.tx.Transaction;
import simpledb.record.*;
import simpledb.query.*;
import simpledb.metadata.*;
import simpledb.index.planner.*;
import simpledb.multibuffer.MultibufferProductPlan;
import simpledb.plan.*;

/**
 * This class contains methods for planning a single table.
 * @author Edward Sciore
 */
class TablePlanner {
   private TablePlan myplan;
   private Predicate mypred;
   private Schema myschema;
   private Map<String,IndexInfo> indexes;
   private Transaction tx;
   
   /**
    * Creates a new table planner.
    * The specified predicate applies to the entire query.
    * The table planner is responsible for determining
    * which portion of the predicate is useful to the table,
    * and when indexes are useful.
    * @param tblname the name of the table
    * @param mypred the query predicate
    * @param tx the calling transaction
    */
   public TablePlanner(String tblname, Predicate mypred, Transaction tx, MetadataMgr mdm) {
      this.mypred  = mypred;
      this.tx  = tx;
      myplan   = new TablePlan(tx, tblname, mdm);
      myschema = myplan.schema();
      indexes  = mdm.getIndexInfo(tblname, tx);
   }
   
   /**
    * Constructs a select plan for the table.
    * The plan will use an indexselect, if possible.
    * @return a select plan for the table.
    */
   public Plan makeSelectPlan() {
      Plan p = makeIndexSelect();
      if (p == null)
         p = myplan;
      return addSelectPred(p);
   }
   
   /**
    * Constructs a join plan of the specified plan
    * and the table. A candidate plan is created for every
    * enabled join method (index join, sort-merge join,
    * hash join and nested loops join), and the candidate with
    * the lowest number of accessed blocks is chosen.
    * If none of the enabled join methods is applicable,
    * a nested loops join is used.
    * The method returns null if there is no join predicate.
    * @param current the specified plan
    * @return a join plan of the plan and this table
    */
   public Plan makeJoinPlan(Plan current) {
      Schema currsch = current.schema();
      Predicate joinpred = mypred.joinSubPred(myschema, currsch);
      if (joinpred == null)
         return null;

      Plan p1 = HeuristicQueryPlanner.indexJoinEnabled ? makeIndexJoin(current, currsch) : null;
      Plan p2 = HeuristicQueryPlanner.mergeJoinEnabled ? makeSortMergeJoin(current, currsch) : null;
      Plan p3 = HeuristicQueryPlanner.hashJoinEnabled ? makeHashJoin(current, currsch) : null;
      Plan p4 = HeuristicQueryPlanner.nestedLoopJoinEnabled ? makeNestedLoopJoinPlan(current, joinpred) : null;

      Plan bestplan = null;
      for (Plan p : Arrays.asList(p1, p2, p3, p4)) {
         if (bestplan == null || (p != null && p.blocksAccessed() < bestplan.blocksAccessed())) {
            bestplan = p;
         }
      }

      if (bestplan == null)
         bestplan = makeNestedLoopJoinPlan(current, joinpred);

      return bestplan;
   }
   
   /**
    * Constructs a product plan of the specified plan and
    * this table.
    * @param current the specified plan
    * @return a product plan of the specified plan and this table
    */
   public Plan makeProductPlan(Plan current) {
      Plan p = addSelectPred(myplan);
      return new MultibufferProductPlan(tx, current, p);
   }

   /**
    * Constructs a nested loops join plan of the specified plan
    * and this table. The selection predicate of the table
    * is applied before the join.
    * @param current the specified plan
    * @param predicate the join predicate
    * @return a nested loops join plan of the plan and this table
    */
   public Plan makeNestedLoopJoinPlan(Plan current, Predicate predicate) {
      Plan p = addSelectPred(myplan);
      return new NestedLoopJoinPlan(current, p, predicate);
   }
   
   private Plan makeIndexSelect() {
      for (String fldname : indexes.keySet()) {
         Constant val = mypred.equatesWithConstant(fldname);
         if (val != null) {
            IndexInfo ii = indexes.get(fldname);
            return new IndexSelectPlan(myplan, ii, val);
         }
      }
      return null;
   }
   
   private Plan makeIndexJoin(Plan current, Schema currsch) {
      for (String fldname : indexes.keySet()) {
         String outerfield = mypred.equatesWithField(fldname);
         if (outerfield != null && currsch.hasField(outerfield)) {
            IndexInfo ii = indexes.get(fldname);
            Plan p = new IndexJoinPlan(current, myplan, ii, outerfield);
            p = addSelectPred(p);
            return addJoinPred(p, currsch);
         }
      }
      return null;
   }

   /**
    * Constructs a sort-merge join plan of the specified plan
    * and this table, if a field of the table is equated
    * with a field of the plan.
    * @param current the specified plan
    * @param currsch the schema of the plan
    * @return a sort-merge join plan, or null if no such join is possible
    */
   private Plan makeSortMergeJoin(Plan current, Schema currsch) {
      for (String fldname : myschema.fields()) {
         String outerfield = mypred.equatesWithField(fldname);
         if (outerfield != null && currsch.hasField(outerfield)) {
            Plan p = addSelectPred(myplan);
            p = new MergeJoinPlan(tx, current, p, outerfield, fldname);
            return addJoinPred(p, currsch);
         }
      }
      return null;
   }

   /**
    * Constructs a hash join plan of the specified plan
    * and this table, if a field of the table is equated
    * with a field of the plan.
    * @param current the specified plan
    * @param currsch the schema of the plan
    * @return a hash join plan, or null if no such join is possible
    */
   private Plan makeHashJoin(Plan current, Schema currsch) {
      for (String fldname : myschema.fields()) {
         String outerfield = mypred.equatesWithField(fldname);
         if (outerfield != null && currsch.hasField(outerfield)) {
            Plan p = addSelectPred(myplan);
            p = new HashJoinPlan(current, p, outerfield, fldname);
            return addJoinPred(p, currsch);
         }
      }
      return null;
   }
   
   private Plan makeProductJoin(Plan current, Schema currsch) {
      Plan p = makeProductPlan(current);
      return addJoinPred(p, currsch);
   }
   
   private Plan addSelectPred(Plan p) {
      Predicate selectpred = mypred.selectSubPred(myschema);
      if (selectpred != null)
         return new SelectPlan(p, selectpred);
      else
         return p;
   }
   
   private Plan addJoinPred(Plan p, Schema currsch) {
      Predicate joinpred = mypred.joinSubPred(currsch, myschema);
      if (joinpred != null)
         return new SelectPlan(p, joinpred);
      else
         return p;
   }
}
