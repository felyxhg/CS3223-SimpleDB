package simpledb.parse;

import java.util.*;

import simpledb.materialize.AggregationFn;
import simpledb.query.*;

/**
 * Data for the SQL <i>select</i> statement.
 * @author Edward Sciore
 */
public class QueryData {
   private List<String> fields;
   private Collection<String> tables;
   private Predicate pred;
   private Order order;
   private List<String> groupfields;
   private List<AggregationFn> aggfns;
   private List<String> selectexpressions;
   
   /**
    * Saves the field and table list and predicate and order.
    */
   public QueryData(List<String> fields, Collection<String> tables, Predicate pred, Order order) {
      this(fields, tables, pred, order, Collections.emptyList(), Collections.emptyList(), fields);
   }

   public QueryData(List<String> fields, Collection<String> tables, Predicate pred, Order order,
         List<String> groupfields, List<AggregationFn> aggfns, List<String> selectexpressions) {
      this.fields = fields;
      this.tables = tables;
      this.pred = pred;
      this.order = order;
      this.groupfields = groupfields;
      this.aggfns = aggfns;
      this.selectexpressions = selectexpressions;
   }
   
   /**
    * Returns the fields mentioned in the select clause.
    * @return a list of field names
    */
   public List<String> fields() {
      return fields;
   }
   
   /**
    * Returns the tables mentioned in the from clause.
    * @return a collection of table names
    */
   public Collection<String> tables() {
      return tables;
   }
   
   /**
    * Returns the predicate that describes which
    * records should be in the output table.
    * @return the query predicate
    */
   public Predicate pred() {
      return pred;
   }

   /**
    * Returns the order that describes how the results
    * should be ordered.
    * @return the query order
    */
   public Order order() {
      return order;
   }

   public List<String> groupfields() {
      return groupfields;
   }

   public List<AggregationFn> aggfns() {
      return aggfns;
   }
   
   public String toString() {
      String result = "select ";
      for (String expression : selectexpressions)
         result += expression + ", ";
      result = result.substring(0, result.length()-2); //remove final comma
      result += " from ";
      for (String tblname : tables)
         result += tblname + ", ";
      result = result.substring(0, result.length()-2); //remove final comma
      String predstring = pred.toString();
      if (!predstring.equals(""))
         result += " where " + predstring;
      if (!groupfields.isEmpty()) {
         result += " group by ";
         for (String fldname : groupfields)
            result += fldname + ", ";
         result = result.substring(0, result.length()-2);
      }
      if (!order.getPairs().isEmpty()) {
         result += " order by ";
         for (OrderPair p : order.getPairs())
            result += p.getFldname() + (p.isDesc() ? " desc, " : " asc, ");
         result = result.substring(0, result.length()-2);
      }
      return result;
   }
}
