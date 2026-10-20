package simpledb.materialize;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import simpledb.query.Constant;
import simpledb.query.Scan;
import simpledb.record.Schema;

/**
 * The Scan class for the <i>hashjoin</i> operator.
 */
public class HashJoinScan implements Scan {
	private Map<Constant, List<Map<String, Constant>>> hashTable;
	private Scan probe;
	private String probeField;
	private Schema buildSchema, probeSchema;
	private boolean isBuiltLeft;
	private Iterator<Map<String, Constant>> matches;
	private Map<String, Constant> currentBuildRow;

	/**
	 * Create a hashjoin scan using the hash table built from one input and the
	 * other input as the probe scan.
	 *
	 * @param hashTable   the hash table of the build input
	 * @param probe       the scan of the probe input
	 * @param probeField  the join field of the probe input
	 * @param buildSchema the schema of the build input
	 * @param probeSchema the schema of the probe input
	 * @param buildIsLeft true if the build input is the left input of the join
	 */
	public HashJoinScan(Map<Constant, List<Map<String, Constant>>> hashTable, Scan probe, String probeField,
			Schema buildSchema, Schema probeSchema, boolean buildIsLeft) {
		this.hashTable = hashTable;
		this.probe = probe;
		this.probeField = probeField;
		this.buildSchema = buildSchema;
		this.probeSchema = probeSchema;
		this.isBuiltLeft = buildIsLeft;
		beforeFirst();
	}

	/**
	 * Closes the probe scan. The build scan was closed after its rows were copied
	 * into the hash table.
	 * 
	 * @see simpledb.query.Scan#close()
	 */
	public void close() {
		probe.close();
	}

	/**
	 * Resets the probe scan and clears the current match. The hash table is
	 * retained and does not need to be rebuilt.
	 * 
	 * @see simpledb.query.Scan#beforeFirst()
	 */
	public void beforeFirst() {
		probe.beforeFirst();
		matches = Collections.emptyIterator();
		currentBuildRow = null;
	}

	/**
	 * Move to the next matching pair of records. For each probe record, look up its
	 * join value in the hash table. If matching build records exist, return one
	 * joined pair at a time. Probe records with no matches are skipped. Return
	 * false when the probe scan has no more records.
	 * 
	 * @see simpledb.query.Scan#next()
	 */
	public boolean next() {
		while (true) {
			if (matches.hasNext()) {
				currentBuildRow = matches.next();
				return true;
			}
			currentBuildRow = null;
			if (!probe.next())
				return false;
			List<Map<String, Constant>> rows = hashTable.get(probe.getVal(probeField));
			matches = rows == null ? Collections.emptyIterator() : rows.iterator();
		}
	}

	/**
	 * Return the integer value of the specified field. The value is obtained from
	 * whichever scan contains the field.
	 * 
	 * @see simpledb.query.Scan#getInt(java.lang.String)
	 */
	public int getInt(String fldname) {
		return getVal(fldname).asInt();
	}

	/**
	 * Return the string value of the specified field. The value is obtained from
	 * whichever scan contains the field.
	 * 
	 * @see simpledb.query.Scan#getString(java.lang.String)
	 */
	public String getString(String fldname) {
		return getVal(fldname).asString();
	}

	/**
	 * Return the value of the specified field. The value is obtained from whichever
	 * scan contains the field.
	 * 
	 * @see simpledb.query.Scan#getVal(java.lang.String)
	 */
	public Constant getVal(String fldname) {
		if (isBuiltLeft) {
			if (buildSchema.hasField(fldname))
				return currentBuildRow.get(fldname);
		} else if (probeSchema.hasField(fldname))
			return probe.getVal(fldname);
		return isBuiltLeft ? probe.getVal(fldname) : currentBuildRow.get(fldname);
	}

	/**
	 * Return true if the specified field is in either of the underlying scans.
	 * 
	 * @see simpledb.query.Scan#hasField(java.lang.String)
	 */
	public boolean hasField(String fldname) {
		return buildSchema.hasField(fldname) || probeSchema.hasField(fldname);
	}
}
