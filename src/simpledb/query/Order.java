package simpledb.query;

import simpledb.plan.Plan;
import simpledb.record.Schema;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * An order is a list of order pairs,
 * corresponding to the order by clause of a query.
 * The records are sorted by the first pair, then by the second pair, and so on.
 */
public class Order {
   private List<OrderPair> pairs = new ArrayList<OrderPair>();

   /**
    * Create an empty order, corresponding to no order.
    */
   public Order() {}

   /**
    * Create an order containing a single pair.
    * @param p the pair
    */
   public Order(OrderPair p) {
      pairs.add(p);
   }

   /**
    * Create an order that sorts the specified field
    * in ascending order.
    * @param fldname the name of the field
    */
   public Order(String fldname) {
      pairs.add(new OrderPair(fldname));
   }

   /**
    * Create an order that sorts the specified fields
    * in ascending order, in the order of the list.
    * @param fldnames the names of the fields
    */
   public Order(List<String> fldnames) {
      pairs = new ArrayList<>(fldnames.stream().map(OrderPair::new).toList());
   }

   /**
    * Modifies the order to be the conjunction of
    * itself and the specified order.
    * @param order the other order
    */
   public void conjoinWith(Order order) {
      pairs.addAll(order.pairs);
   }

   /**
    * Return true if the specified field is
    * one of the fields of the order.
    * @param fldname the name of the field
    * @return true if the field is in the order
    */
   public boolean hasField(String fldname) {
      for (OrderPair pair : pairs)
         if (pair.getFldname().equals(fldname))
            return true;
      return false;
   }

   /**
    * Returns the pairs of the order.
    * @return the list of order pairs
    */
   public List<OrderPair> getPairs() {
      return pairs;
   }

   /**
    * Returns the pairs of the order separated by commas,
    * for example "gradyear desc, sname asc".
    * @return the string representation of the order
    */
   @Override
   public String toString() {
      return pairs.stream().map(OrderPair::toString).collect(Collectors.joining(", "));
   }
}
