package simpledb.query;

import simpledb.plan.Plan;
import simpledb.record.Schema;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A order is a list of attributes and ordering pairs
 */
public class Order {
   private List<OrderPair> pairs = new ArrayList<OrderPair>();

   /**
    * Create an empty order, corresponding to no order.
    */
   public Order() {}

   /**
    * Create a oder containing a single pair.
    * @param p the pair
    */
   public Order(OrderPair p) {
      pairs.add(p);
   }

   public Order(String fldname) {
      pairs.add(new OrderPair(fldname));
   }

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

   public List<OrderPair> getPairs() {
      return pairs;
   }

   @Override
   public String toString() {
      return pairs.stream().map(OrderPair::toString).collect(Collectors.joining(", "));
   }
}
