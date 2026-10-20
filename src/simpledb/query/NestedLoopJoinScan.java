package simpledb.query;

/**
 * The scan class corresponding to the <i>nested loops join</i>
 * operator.
 */
public class NestedLoopJoinScan implements Scan {
   private Scan s1, s2;
   private Predicate predicate;

   /**
    * Create a nested loops join scan having the two underlying scans
    * and the join predicate.
    * @param s1 the LHS (outer) scan
    * @param s2 the RHS (inner) scan
    * @param predicate the join predicate
    */
   public NestedLoopJoinScan(Scan s1, Scan s2, Predicate predicate) {
      this.s1 = s1;
      this.s2 = s2;
      this.predicate = predicate;
      beforeFirst();
   }

   /**
    * Position the scan before its first record.
    * In particular, the LHS scan is positioned at 
    * its first record, and the RHS scan
    * is positioned before its first record.
    * @see Scan#beforeFirst()
    */
   public void beforeFirst() {
      s1.beforeFirst();
      s1.next();
      s2.beforeFirst();
   }

   /**
    * Move the scan to the next pair of records
    * that satisfies the join predicate.
    * Pairs that do not satisfy the predicate are skipped.
    * If there are no more pairs, the method returns false.
    * @see Scan#next()
    */
   public boolean next() {
      while (nextPair()) {
         if (predicate.isSatisfied(this)) {
            return true;
         }
      }

      return false;
   }

   /**
    * Move to the next pair of records, as in a product.
    * The method moves to the next RHS record, if possible.
    * Otherwise, it moves to the next LHS record and the
    * first RHS record.
    * @return false if there are no more pairs
    */
   private boolean nextPair() {
      if (s2.next())
         return true;
      else {
         s2.beforeFirst();
         return s2.next() && s1.next();
      }
   }

   /** 
    * Return the integer value of the specified field.
    * The value is obtained from whichever scan
    * contains the field.
    * @see Scan#getInt(String)
    */
   public int getInt(String fldname) {
      if (s1.hasField(fldname))
         return s1.getInt(fldname);
      else
         return s2.getInt(fldname);
   }

   /** 
    * Returns the string value of the specified field.
    * The value is obtained from whichever scan
    * contains the field.
    * @see Scan#getString(String)
    */
   public String getString(String fldname) {
      if (s1.hasField(fldname))
         return s1.getString(fldname);
      else
         return s2.getString(fldname);
   }

   /** 
    * Return the value of the specified field.
    * The value is obtained from whichever scan
    * contains the field.
    * @see Scan#getVal(String)
    */
   public Constant getVal(String fldname) {
      if (s1.hasField(fldname))
         return s1.getVal(fldname);
      else
         return s2.getVal(fldname);
   }

   /**
    * Returns true if the specified field is in
    * either of the underlying scans.
    * @see Scan#hasField(String)
    */
   public boolean hasField(String fldname) {
      return s1.hasField(fldname) || s2.hasField(fldname);
   }

   /**
    * Close both underlying scans.
    * @see Scan#close()
    */
   public void close() {
      s1.close();
      s2.close();
   }
}
