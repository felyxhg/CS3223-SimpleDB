package simpledb.materialize;

import simpledb.query.Constant;
import simpledb.query.Scan;

import java.util.List;

/**
 * The Scan class for the <i>distinct</i> operator.
 * @author Edward Sciore
 */
public class DistinctScan implements Scan {
   private Scan s;
   private List<String> fields;
   private GroupValue groupval;
   private boolean moregroups;

   /**
    * Create a distinct scan, given a sorted table scan.
    * @param s the sorted scan
    * @param fields the fields that are compared to find duplicates
    */
   public DistinctScan(Scan s, List<String> fields) {
      this.s = s;
      this.fields = fields;
      beforeFirst();
   }
   
   /**
    * Position the scan before the first distinct record.
    * Internally, the underlying scan is always
    * positioned at the first record of a group of
    * duplicates, which means that this method moves to the
    * first underlying record.
    * @see Scan#beforeFirst()
    */
   public void beforeFirst() {
      s.beforeFirst();
      moregroups = s.next();
   }
   
   /**
    * Move to the next distinct record.
    * The values of all fields at the current record
    * are saved.
    * The method repeatedly reads underlying records until
    * it encounters a record having different values,
    * which skips all duplicates of the saved record.
    * @see Scan#next()
    */
   public boolean next() {
      if (!moregroups)
         return false;
      groupval = new GroupValue(s, fields);
      while (moregroups = s.next()) {
         GroupValue gv = new GroupValue(s, fields);
         if (!groupval.equals(gv))
            break;
      }
      return true;
   }
   
   /**
    * Close the scan by closing the underlying scan.
    * @see Scan#close()
    */
   public void close() {
      s.close();
   }
   
   /**
    * Get the Constant value of the specified field.
    * The value is obtained from the saved values
    * of the current distinct record, since the
    * underlying scan is already positioned
    * at the next one.
    * @see Scan#getVal(String)
    */
   public Constant getVal(String fldname) {
      if (fields.contains(fldname))
         return groupval.getVal(fldname);
      throw new RuntimeException("field " + fldname + " not found.");
   }
   
   /**
    * Get the integer value of the specified field.
    * The value is obtained from the saved values
    * of the current distinct record.
    * @see Scan#getVal(String)
    */
   public int getInt(String fldname) {
      return getVal(fldname).asInt();
   }
   
   /**
    * Get the string value of the specified field.
    * The value is obtained from the saved values
    * of the current distinct record.
    * @see Scan#getVal(String)
    */
   public String getString(String fldname) {
      return getVal(fldname).asString();
   }
   
   /** Return true if the specified field is one of
    * the fields of the distinct scan.
    * @see Scan#hasField(String)
    */
   public boolean hasField(String fldname) {
      return fields.contains(fldname);
   }
}

