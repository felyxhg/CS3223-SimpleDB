package simpledb.plan;

import simpledb.query.NestedLoopJoinScan;
import simpledb.query.Predicate;
import simpledb.query.Scan;
import simpledb.record.Schema;

/**
 * The Plan class for the <i>nested loops join</i> operator.
 */
public class NestedLoopJoinPlan implements Plan {
   private Plan p1, p2;
   private Predicate predicate;
   private Schema schema = new Schema();

   /**
    * Creates a new nested loops join node in the query tree,
    * having the two specified subqueries and the join predicate.
    * @param p1 the left-hand (outer) subquery
    * @param p2 the right-hand (inner) subquery
    * @param predicate the join predicate
    */
   public NestedLoopJoinPlan(Plan p1, Plan p2, Predicate predicate) {
      this.p1 = p1;
      this.p2 = p2;
      this.predicate = predicate;
      schema.addAll(p1.schema());
      schema.addAll(p2.schema());
   }
   
   /**
    * Creates a nested loops join scan for this query.
    * @see Plan#open()
    */
   public Scan open() {
      Scan s1 = p1.open();
      Scan s2 = p2.open();
      return new NestedLoopJoinScan(s1, s2, predicate);
   }
   
   /**
    * Estimates the number of block accesses in the join.
    * The inner query is read once for every record of the outer query.
    * The formula is:
    * <pre> B(join(p1,p2)) = B(p1) + R(p1)*B(p2) </pre>
    * @see Plan#blocksAccessed()
    */
   public int blocksAccessed() {
      return p1.blocksAccessed() + (p1.recordsOutput() * p2.blocksAccessed());
   }
   
   /**
    * Estimates the number of output records in the join,
    * which is the size of the product reduced by the join predicate.
    * The formula is:
    * <pre> R(join(p1,p2)) = R(p1)*R(p2) / reductionFactor(predicate) </pre>
    * @see Plan#recordsOutput()
    */
   public int recordsOutput() {
      return (p1.recordsOutput() * p2.recordsOutput()) / predicate.reductionFactor(this);
   }
   
   /**
    * Estimates the distinct number of field values in the join.
    * Since the join does not increase or decrease field values,
    * the estimate is the same as in the appropriate underlying query.
    * @see Plan#distinctValues(String)
    */
   public int distinctValues(String fldname) {
      if (p1.schema().hasField(fldname))
         return p1.distinctValues(fldname);
      else
         return p2.distinctValues(fldname);
   }
   
   /**
    * Returns the schema of the join,
    * which is the union of the schemas of the underlying queries.
    * @see Plan#schema()
    */
   public Schema schema() {
      return schema;
   }

   @Override
   public String subtreeString(int indentation) {
      return planLine(indentation, "NestedLoopJoin [" + predicate + "]")
              + p1.subtreeString(indentation + 1)
              + p2.subtreeString(indentation + 1);
   }
}
