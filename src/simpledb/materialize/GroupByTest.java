package simpledb.materialize;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

import simpledb.query.Scan;
import simpledb.server.SimpleDB;
import simpledb.tx.Transaction;

public class GroupByTest {
	public static void main(String[] args) throws Exception {
		Path tempRoot = Files.createTempDirectory("simpledb-groupby-");
		Path dbDir = tempRoot.resolve("db");
		Transaction tx = null;
		try {
			SimpleDB db = new SimpleDB(dbDir.toString());
			tx = db.newTx();
			db.planner().executeUpdate("create table t1(g int, v int)", tx);
			db.planner().executeUpdate("insert into t1(g,v) values(1,10)", tx);
			db.planner().executeUpdate("insert into t1(g,v) values(1,20)", tx);
			db.planner().executeUpdate("insert into t1(g,v) values(1,21)", tx);
			db.planner().executeUpdate("insert into t1(g,v) values(2,5)", tx);

			Scan scan = db.planner().createQueryPlan(
					"select g, sum(v), avg(v), min(v), max(v), count(v) from t1 group by g", tx).open();
			try {
				System.out.println("Grouped aggregates:");
				check(scan.next(), "missing first group");
				printAndCheckGroup(scan, 1, 51, 17, 10, 21, 3);

				check(scan.next(), "missing second group");
				printAndCheckGroup(scan, 2, 5, 5, 5, 5, 1);
				check(!scan.next(), "unexpected extra group");
			} finally {
				scan.close();
			}

			Scan totals = db.planner().createQueryPlan(
					"select sum(v), avg(v), min(v), max(v), count(v) from t1", tx).open();
			try {
				System.out.println("Ungrouped aggregates:");
				check(totals.next(), "missing ungrouped aggregate row");
				int sum = totals.getInt("sumofv");
				int avg = totals.getInt("avgofv");
				int min = totals.getInt("minofv");
				int max = totals.getInt("maxofv");
				int count = totals.getInt("countofv");
				check(sum == 56, "wrong ungrouped sum");
				check(avg == 14, "wrong ungrouped average");
				check(min == 5, "wrong ungrouped minimum");
				check(max == 21, "wrong ungrouped maximum");
				check(count == 4, "wrong ungrouped count");
				System.out.printf("sumofv=%d, avgofv=%d, minofv=%d, maxofv=%d, countofv=%d%n",
						sum, avg, min, max, count);
				check(!totals.next(), "ungrouped aggregate returned multiple rows");
			} finally {
				totals.close();
			}

			tx.commit();
			tx = null;
			System.out.println("GroupByTest passed");
		} finally {
			if (tx != null)
				tx.rollback();
			try (Stream<Path> paths = Files.walk(tempRoot)) {
				paths.sorted(Comparator.reverseOrder()).forEach(path -> {
					try {
						Files.delete(path);
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				});
			}
		}
	}

	private static void check(boolean condition, String message) {
		if (!condition)
			throw new AssertionError(message);
	}

	private static void printAndCheckGroup(Scan scan, int expectedGroup, int expectedSum, int expectedAvg,
			int expectedMin, int expectedMax, int expectedCount) {
		int group = scan.getInt("g");
		int sum = scan.getInt("sumofv");
		int avg = scan.getInt("avgofv");
		int min = scan.getInt("minofv");
		int max = scan.getInt("maxofv");
		int count = scan.getInt("countofv");
		check(group == expectedGroup, "unexpected group " + group);
		check(sum == expectedSum, "wrong sum for group " + group);
		check(avg == expectedAvg, "wrong average for group " + group);
		check(min == expectedMin, "wrong minimum for group " + group);
		check(max == expectedMax, "wrong maximum for group " + group);
		check(count == expectedCount, "wrong count for group " + group);
		System.out.printf("g=%d, sumofv=%d, avgofv=%d, minofv=%d, maxofv=%d, countofv=%d%n",
				group, sum, avg, min, max, count);
	}
}
