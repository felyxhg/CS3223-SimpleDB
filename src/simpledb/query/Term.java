package simpledb.query;

import simpledb.plan.Plan;
import simpledb.record.*;

import java.util.List;

/**
 * A term is a comparison between two expressions.
 * @author Edward Sciore
 *
 */
public class Term {
   /**
    * The comparison operators supported in a term.
    * Each operator stores the strings by which it
    * can be written in an SQL statement.
    */
   public enum Operator {
      EQUAL(List.of("=")),
      NOT_EQUAL(List.of("!=", "<>")),
      LESS_THAN(List.of("<")),
      LESS_THAN_OR_EQUAL(List.of("<=")),
      GREATER_THAN(List.of(">")),
      GREATER_THAN_OR_EQUAL(List.of(">="));

      private List<String> operatorStrings;

      /**
       * Create an operator that can be written
       * as any of the specified strings.
       * @param operatorStrings the SQL strings of the operator
       */
      private Operator(List<String> operatorStrings) {
         this.operatorStrings = operatorStrings;
      }

      /**
       * Returns the strings by which the operator
       * can be written in an SQL statement.
       * @return the list of operator strings
       */
      public List<String> getOperatorStrings() {
         return operatorStrings;
      }
   }

   private Expression lhs, rhs;
   private Operator operator;
   
   /**
    * Create a new term that compares two expressions
    * for equality.
    * @param lhs  the LHS expression
    * @param rhs  the RHS expression
    */
   public Term(Expression lhs, Expression rhs) {
      this.lhs = lhs;
      this.rhs = rhs;
      this.operator = Operator.EQUAL;
   }

   /**
    * Create a new term that compares two expressions.
    * @param lhs  the LHS expression
    * @param rhs  the RHS expression
    * @param operator the operator used for the comparison
    */
   public Term(Expression lhs, Expression rhs, Operator operator) {
      this.lhs = lhs;
      this.rhs = rhs;
      this.operator = operator;
   }

   /**
    * Return true if the values of the term's expressions
    * satisfy the term's comparison operator,
    * with respect to the specified scan.
    * @param s the scan
    * @return true if the comparison holds for the current record of the scan
    */
   public boolean isSatisfied(Scan s) {
      Constant lhsval = lhs.evaluate(s);
      Constant rhsval = rhs.evaluate(s);

      return switch (operator) {
          case EQUAL -> lhsval.equals(rhsval);
          case NOT_EQUAL -> !lhsval.equals(rhsval);
          case LESS_THAN -> lhsval.compareTo(rhsval) < 0;
          case LESS_THAN_OR_EQUAL -> lhsval.compareTo(rhsval) <= 0;
          case GREATER_THAN -> lhsval.compareTo(rhsval) > 0;
          case GREATER_THAN_OR_EQUAL -> lhsval.compareTo(rhsval) >= 0;
      };
   }
   
   /**
    * Calculate the extent to which selecting on the term reduces 
    * the number of records output by a query.
    * For example if the reduction factor is 2, then the
    * term cuts the size of the output in half.
    * @param p the query's plan
    * @return the integer reduction factor.
    */
   public int reductionFactor(Plan p) {
      String lhsName, rhsName;
      if (lhs.isFieldName() && rhs.isFieldName()) {
         lhsName = lhs.asFieldName();
         rhsName = rhs.asFieldName();
         return Math.max(p.distinctValues(lhsName),
                         p.distinctValues(rhsName));
      }
      if (lhs.isFieldName()) {
         lhsName = lhs.asFieldName();
         return p.distinctValues(lhsName);
      }
      if (rhs.isFieldName()) {
         rhsName = rhs.asFieldName();
         return p.distinctValues(rhsName);
      }
      // otherwise, the term equates constants
      if (lhs.asConstant().equals(rhs.asConstant()))
         return 1;
      else
         return Integer.MAX_VALUE;
   }
   
   /**
    * Determine if this term is of the form "F=c"
    * where F is the specified field and c is some constant.
    * If so, the method returns that constant.
    * If not, the method returns null.
    * @param fldname the name of the field
    * @return either the constant or null
    */
   public Constant equatesWithConstant(String fldname) {
      if (operator == Operator.EQUAL && lhs.isFieldName() &&
          lhs.asFieldName().equals(fldname) &&
          !rhs.isFieldName())
         return rhs.asConstant();
      else if (operator == Operator.EQUAL && rhs.isFieldName() &&
               rhs.asFieldName().equals(fldname) &&
               !lhs.isFieldName())
         return lhs.asConstant();
      else
         return null;
   }
   
   /**
    * Determine if this term is of the form "F1=F2"
    * where F1 is the specified field and F2 is another field.
    * If so, the method returns the name of that field.
    * If not, the method returns null.
    * @param fldname the name of the field
    * @return either the name of the other field, or null
    */
   public String equatesWithField(String fldname) {
      if (operator == Operator.EQUAL && lhs.isFieldName() &&
          lhs.asFieldName().equals(fldname) &&
          rhs.isFieldName())
         return rhs.asFieldName();
      else if (operator == Operator.EQUAL && rhs.isFieldName() &&
               rhs.asFieldName().equals(fldname) &&
               lhs.isFieldName())
         return lhs.asFieldName();
      else
         return null;
   }
   
   /**
    * Return true if both of the term's expressions
    * apply to the specified schema.
    * @param sch the schema
    * @return true if both expressions apply to the schema
    */
   public boolean appliesTo(Schema sch) {
      return lhs.appliesTo(sch) && rhs.appliesTo(sch);
   }
   
   public String toString() {
      return lhs.toString() + operator.operatorStrings.getFirst() + rhs.toString();
   }
}
