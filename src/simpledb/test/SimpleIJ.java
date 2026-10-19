package simpledb.test;

import simpledb.jdbc.embedded.EmbeddedDriver;
import simpledb.jdbc.network.NetworkDriver;
import simpledb.plan.Plan;
import simpledb.plan.Planner;
import simpledb.query.Scan;
import simpledb.record.Schema;
import simpledb.server.SimpleDB;
import simpledb.tx.Transaction;
import simpledb.opt.HeuristicQueryPlanner;

import java.sql.*;
import java.util.Scanner;

public class SimpleIJ {
   public static void main(String[] args) {
      try {
         // analogous to the driver
         SimpleDB db = new SimpleDB("studentdb");

         // analogous to the connection
         Transaction tx  = db.newTx();
         Planner planner = db.planner();

         Scanner sc = new Scanner(System.in);
         System.out.print("\nSQL> ");
         while (sc.hasNextLine()) {
            // process one line of input
            String cmd = sc.nextLine().trim();
            if (cmd.startsWith("exit"))
               break;
            else if (cmd.startsWith("select"))
               doQuery(planner, tx, cmd);
            else if (cmd.startsWith("joins"))
               setJoinMethods(cmd);
            else if (cmd.startsWith("time"))
               doTimedQuery(planner, tx, cmd.substring(4).trim());
            else
               doUpdate(planner, tx, cmd);
            System.out.print("\nSQL> ");
         }
      }
      catch (Exception e) {
         e.printStackTrace();
      }
   }

   private static void doQuery(Planner planner, Transaction tx, String cmd) {
      Scan s = null;
      try {
         Plan plan = planner.createQueryPlan(cmd, tx);

         System.out.println("Query plan:");
         System.out.print(plan.subtreeString(0) + "\n");

         s = plan.open();

         int numcols = plan.schema().fields().size();
         int totalwidth = 0;

         // print header
         for(String fldname : plan.schema().fields()) {
            int width = displaySize(plan.schema(), fldname);
            totalwidth += width;
            String fmt = "%" + width + "s";
            System.out.format(fmt, fldname);
         }
         System.out.println();
         for(int i=0; i<totalwidth; i++)
            System.out.print("-");
         System.out.println();

         // print records
         while(s.next()) {
            for (String fldname : plan.schema().fields()) {
               int fldtype = plan.schema().type(fldname);
               String fmt = "%" + displaySize(plan.schema(), fldname);
               if (fldtype == Types.INTEGER) {
                  int ival = s.getInt(fldname);
                  System.out.format(fmt + "d", ival);
               }
               else {
                  String sval = s.getString(fldname);
                  System.out.format(fmt + "s", sval);
               }
            }
            System.out.println();
         }
      }
      catch (Exception e) {
         e.printStackTrace();
      }
      finally {
         if (s != null) {
            s.close();
         }
      }
   }

   private static void setJoinMethods(String cmd) {
      String methods = cmd.substring(5);
      boolean all = methods.contains("all");
      HeuristicQueryPlanner.indexJoinEnabled = all || methods.contains("index");
      HeuristicQueryPlanner.mergeJoinEnabled = all || methods.contains("merge");
      HeuristicQueryPlanner.hashJoinEnabled = all || methods.contains("hash");
      HeuristicQueryPlanner.nestedLoopJoinEnabled = all || methods.contains("nested");
      System.out.println("index=" + HeuristicQueryPlanner.indexJoinEnabled
              + ", merge=" + HeuristicQueryPlanner.mergeJoinEnabled
              + ", hash=" + HeuristicQueryPlanner.hashJoinEnabled
              + ", nested=" + HeuristicQueryPlanner.nestedLoopJoinEnabled);
   }

   private static void doTimedQuery(Planner planner, Transaction tx, String cmd) {
      Scan s = null;
      try {
         Plan plan = planner.createQueryPlan(cmd, tx);
         System.out.println("Query plan:");
         System.out.print(plan.subtreeString(0) + "\n");

         long start = System.nanoTime();
         s = plan.open();
         int count = 0;
         while (s.next()) {
            for (String fldname : plan.schema().fields())
               s.getVal(fldname);
            count++;
         }
         long elapsed = System.nanoTime() - start;
         System.out.println(count + " records in " + (elapsed / 1000000.0) + " ms");
      }
      catch (Exception e) {
         e.printStackTrace();
      }
      finally {
         if (s != null)
            s.close();
      }
   }

   private static void doUpdate(Planner planner, Transaction tx, String cmd) {
      try {
         int howmany = planner.executeUpdate(cmd, tx);
         System.out.println(howmany + " records processed");
      }
      catch (Exception e) {
         e.printStackTrace();
      }
   }

   private static int displaySize(Schema sch, String fldname) {
      int fldlength = (sch.type(fldname) == Types.INTEGER) ? 6 : sch.length(fldname);
      return Math.max(fldname.length(), fldlength) + 1;
   }
}