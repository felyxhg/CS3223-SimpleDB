package simpledb.materialize;

import simpledb.plan.Plan;
import simpledb.query.Order;
import simpledb.query.Scan;
import simpledb.record.Schema;
import simpledb.tx.Transaction;

import java.util.List;

/**
 * The Plan class for the <i>distinct</i> operator.
 */
public class DistinctPlan implements Plan {
   private Plan p;

   /**
    * Create a distinct plan for the underlying query.
    * Two records are duplicates if they have
    * the same values for all fields
    * of the underlying query.
    * @param p a plan for the underlying query
    * @param tx the calling transaction
    */
   public DistinctPlan(Transaction tx, Plan p) {
      this.p = new SortPlan(tx, p, new Order(p.schema().fields()));

   }

   /**
    * Create a distinct plan for the underlying query,
    * whose output is sorted by the specified order.
    * The records are sorted by the fields of the order first
    * and then by the remaining fields, so that duplicate
    * records are still next to each other and no
    * additional sort is needed.
    * @param p a plan for the underlying query
    * @param order the order of the output
    * @param tx the calling transaction
    */
   public DistinctPlan(Transaction tx, Plan p, Order order) {
      Order sortorder = new Order();
      sortorder.conjoinWith(order);
      for (String fldname : p.schema().fields())
         if (!order.hasField(fldname))
            sortorder.conjoinWith(new Order(fldname));
      this.p = new SortPlan(tx, p, sortorder);
   }
   
   /**
    * This method opens a sort plan for the specified plan.
    * The sort plan ensures that duplicate records
    * will be next to each other.
    * @see Plan#open()
    */
   public Scan open() {
      Scan s = p.open();
      return new DistinctScan(s, p.schema().fields());
   }
   
   /**
    * Return the number of blocks required to
    * remove the duplicates,
    * which is one pass through the sorted table.
    * @see Plan#blocksAccessed()
    */
   public int blocksAccessed() {
      return p.blocksAccessed();
   }
   
   /**
    * Return the number of distinct records.  Assuming equal distribution,
    * this is the product of the distinct values
    * for each field, but at most the number
    * of records in the underlying query.
    * @see Plan#recordsOutput()
    */
   public int recordsOutput() {
      int numdistinct = 1;
      for (String fldname : p.schema().fields())
         numdistinct *= p.distinctValues(fldname);
      return Math.min(numdistinct, p.recordsOutput());
   }
   
   /**
    * Return the number of distinct values for the
    * specified field, which is the same
    * as in the underlying query, since removing
    * duplicate records does not remove any field value.
    * @see Plan#distinctValues(String)
    */
   public int distinctValues(String fldname) {
      return p.distinctValues(fldname);
   }
   
   /**
    * Returns the schema of the output table,
    * which is the same as in the underlying query.
    * @see Plan#schema()
    */
   public Schema schema() {
      return p.schema();
   }

   @Override
   public String subtreeString(int indentation) {
      return planLine(indentation, "Distinct [" + String.join(", ", schema().fields()) + "]") + p.subtreeString(indentation + 1);
   }
}
