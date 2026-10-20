package simpledb.materialize;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import simpledb.plan.Plan;
import simpledb.query.Constant;
import simpledb.query.Scan;
import simpledb.record.Schema;

/**
 * The Plan class for the <i>hashjoin</i> operator.
 */
public class HashJoinPlan implements Plan {
	private Plan p1, p2;
	private String fldname1, fldname2;
	private Schema sch = new Schema();

	/**
	 * Creates a hashjoin plan for the two specified queries.
	 * 
	 * @param p1       the LHS query plan
	 * @param p2       the RHS query plan
	 * @param fldname1 the LHS join field
	 * @param fldname2 the RHS join field
	 */
	public HashJoinPlan(Plan p1, Plan p2, String fldname1, String fldname2) {
		this.p1 = p1;
		this.p2 = p2;
		this.fldname1 = fldname1;
		this.fldname2 = fldname2;
		sch.addAll(p1.schema());
		sch.addAll(p2.schema());
	}

	/**
	 * Builds a hash table from the input with fewer estimated records, then probes
	 * it with the other input. The scan preserves the logical left and right input
	 * fields.
	 * 
	 * @see simpledb.plan.Plan#open()
	 */
	public Scan open() {
		boolean buildLeft = p1.recordsOutput() <= p2.recordsOutput();
		Plan buildPlan = buildLeft ? p1 : p2;
		Plan probePlan = buildLeft ? p2 : p1;
		String buildField = buildLeft ? fldname1 : fldname2;
		String probeField = buildLeft ? fldname2 : fldname1;
		Map<Constant, List<Map<String, Constant>>> hashTable = buildHashTable(buildPlan, buildField);
		return new HashJoinScan(hashTable, probePlan.open(), probeField, buildPlan.schema(), probePlan.schema(),
				buildLeft);
	}

	/**
	 * Reads the input into a hash table keyed by its join field,
	 * copying each record. Closes the input scan before
	 * returning the table.
	 *
	 * @param plan      the input to build the hash table from
	 * @param joinField the join field of the input
	 * @return the hash table, mapping each join value to its records
	 */
	private Map<Constant, List<Map<String, Constant>>> buildHashTable(Plan plan, String joinField) {
		Map<Constant, List<Map<String, Constant>>> table = new HashMap<>();
		Scan scan = plan.open();
		try {
			while (scan.next()) {
				Map<String, Constant> row = new HashMap<>();
				for (String fldname : plan.schema().fields())
					row.put(fldname, scan.getVal(fldname));
				table.computeIfAbsent(scan.getVal(joinField), key -> new ArrayList<>()).add(row);
			}
		} finally {
			scan.close();
		}
		return table;
	}

	/**
	 * Return the number of block accesses required to hashjoin the tables.
	 * Since the hash table is kept in memory, each input is read
	 * only once, so the method returns the sum of the block
	 * accesses of the two inputs.
	 *
	 * @see simpledb.plan.Plan#blocksAccessed()
	 */
	public int blocksAccessed() {
		return p1.blocksAccessed() + p2.blocksAccessed();
	}

	/**
	 * Return the number of records in the join. Assuming uniform distribution, the
	 * formula is:
	 * 
	 * <pre>
	 *  R(join(p1,p2)) = R(p1)*R(p2)/max{V(p1,F1),V(p2,F2)}
	 * </pre>
	 * 
	 * @see simpledb.plan.Plan#recordsOutput()
	 */
	public int recordsOutput() {
		int maxvals = Math.max(p1.distinctValues(fldname1), p2.distinctValues(fldname2));
		return maxvals == 0 ? 0 : (p1.recordsOutput() * p2.recordsOutput()) / maxvals;
	}

	/**
	 * Estimate the distinct number of field values in the join. Since the join does
	 * not increase or decrease field values, the estimate is the same as in the
	 * appropriate underlying query.
	 * 
	 * @see simpledb.plan.Plan#distinctValues(java.lang.String)
	 */
	public int distinctValues(String fldname) {
		if (p1.schema().hasField(fldname))
			return p1.distinctValues(fldname);
		return p2.distinctValues(fldname);
	}

	/**
	 * Return the schema of the join, which is the union of the schemas of the
	 * underlying queries.
	 * 
	 * @see simpledb.plan.Plan#schema()
	 */
	public Schema schema() {
		return sch;
	}

	@Override
	public String subtreeString(int indentation) {
		return planLine(indentation, "HashJoin [" + fldname1 + " = " + fldname2 + "]")
				+ p1.subtreeString(indentation + 1)
				+ p2.subtreeString(indentation + 1);
	}
}
