package simpledb.test;

import simpledb.plan.Plan;
import simpledb.plan.Planner;
import simpledb.query.Scan;
import simpledb.server.SimpleDB;
import simpledb.tx.Transaction;

public class ChangeMajor {
   public static void main(String[] args) {
      try {
         SimpleDB db = new SimpleDB("studentdb");

         Transaction tx = db.newTx();
         Planner planner = db.planner();

         String cmd = "update STUDENT "
                 + "set MajorId=30 "
                 + "where SName = 'amy'";
         int p = planner.executeUpdate(cmd, tx);

         System.out.println("Amy is now a drama major.");
      }
      catch(Exception e) {
         e.printStackTrace();
      }
   }
}
