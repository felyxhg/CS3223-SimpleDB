package simpledb.plan;

import simpledb.query.Scan;
import simpledb.record.*;

/**
 * The interface implemented by each query plan.
 * There is a Plan class for each relational algebra operator.
 * @author Edward Sciore
 *
 */
public interface Plan {
   
   /**
    * Opens a scan corresponding to this plan.
    * The scan will be positioned before its first record.
    * @return a scan
    */
   public Scan   open();
   
   /**
    * Returns an estimate of the number of block accesses
    * that will occur when the scan is read to completion.
    * @return the estimated number of block accesses
    */
   public int    blocksAccessed();
   
   /**
    * Returns an estimate of the number of records
    * in the query's output table.
    * @return the estimated number of output records
    */
   public int    recordsOutput();
   
   /**
    * Returns an estimate of the number of distinct values
    * for the specified field in the query's output table.
    * @param fldname the name of a field
    * @return the estimated number of distinct field values in the output
    */
   public int    distinctValues(String fldname);
   
   /**
    * Returns the schema of the query.
    * @return the query's schema
    */
   public Schema schema();

   /**
    * Returns the query plan of the subtree rooted at this node.
    * The string contains one line for this node, followed by
    * the lines of its child plans with one more level of indentation.
    * @param indentation the indentation depth of this node in the query plan
    * @return the query plan of the subtree
    */
   public String subtreeString(int indentation);

   /**
    * Returns one line of the query plan string,
    * consisting of the indentation, the description
    * of this node and its estimated costs.
    * @param indentation indentation depth of the line
    * @param description the operator name and its details
    * @return the formatted line
    */
   default String planLine(int indentation, String description) {
      return "  ".repeat(indentation) + description
              + " (blocks=" + blocksAccessed() + ", records=" + recordsOutput() + ")\n";
   }
}
