package simpledb.materialize;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import simpledb.metadata.MetadataMgr;
import simpledb.plan.Plan;
import simpledb.plan.TablePlan;
import simpledb.query.Scan;
import simpledb.server.SimpleDB;
import simpledb.tx.Transaction;

public class HashJoinTest {
	public static void main(String[] args) throws Exception {
		Path tempRoot = Files.createTempDirectory("simpledb-hashjoin-");
		Transaction tx = null;
		try {
			SimpleDB db = new SimpleDB(tempRoot.resolve("db").toString());
			tx = db.newTx();
			db.planner().executeUpdate("create table lhs(id int, lval varchar(10))", tx);
			db.planner().executeUpdate("create table rhs(id int, rval varchar(10))", tx);

			db.planner().executeUpdate("insert into lhs(id,lval) values(1,'l1a')", tx);
			db.planner().executeUpdate("insert into lhs(id,lval) values(1,'l1b')", tx);
			db.planner().executeUpdate("insert into lhs(id,lval) values(2,'l2')", tx);
			db.planner().executeUpdate("insert into lhs(id,lval) values(9,'unmatched')", tx);
			db.planner().executeUpdate("insert into rhs(id,rval) values(1,'r1a')", tx);
			db.planner().executeUpdate("insert into rhs(id,rval) values(1,'r1b')", tx);
			db.planner().executeUpdate("insert into rhs(id,rval) values(2,'r2')", tx);
			db.planner().executeUpdate("insert into rhs(id,rval) values(8,'unmatched')", tx);

			MetadataMgr md = db.mdMgr();
			Plan left = new TablePlan(tx, "lhs", md);
			Plan right = new TablePlan(tx, "rhs", md);
			Scan scan = new HashJoinPlan(left, right, "id", "id").open();
			try {
				Set<String> rows = new HashSet<>();
				while (scan.next()) {
					int id = scan.getInt("id");
					String lval = scan.getString("lval");
					String rval = scan.getString("rval");
					check(id == 1 || id == 2,
							"unmatched key appeared in result");
					String row = id + ":" + lval + ":" + rval;
					rows.add(row);
					System.out.println(row);
				}
				Set<String> expected = Set.of("1:l1a:r1a", "1:l1a:r1b", "1:l1b:r1a", "1:l1b:r1b", "2:l2:r2");
				check(rows.equals(expected), "unexpected hash join rows: " + rows);
				System.out.println("HashJoinTest passed: " + rows.size() + " matching rows");
			} finally {
				scan.close();
			}

			tx.commit();
			tx = null;
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
}
