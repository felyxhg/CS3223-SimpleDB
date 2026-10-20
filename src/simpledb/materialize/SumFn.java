package simpledb.materialize;

import simpledb.query.Constant;
import simpledb.query.Scan;

/**
 * The <i>sum</i> aggregation function.
 */
public class SumFn implements AggregationFn {
	private String fldname;
	private int totalsum;

	/**
	 * Create a sum aggregation function for the specified field.
	 * 
	 * @param fldname the name of the aggregated field
	 */
	public SumFn(String fldname) {
		this.fldname = fldname;
	}

	/**
	 * Start a new totalsum. Since SimpleDB does not support null values, every
	 * record will be added, regardless of the field. The current totalsum is thus
	 * set to int value of first record.
	 * 
	 * @see simpledb.materialize.AggregationFn#processFirst(simpledb.query.Scan)
	 */
	public void processFirst(Scan s) {
		totalsum = s.getInt(fldname);
	}

	/**
	 * Since SimpleDB does not support null values, this method always adds the sum,
	 * regardless of the field.
	 * 
	 * @see simpledb.materialize.AggregationFn#processNext(simpledb.query.Scan)
	 */
	public void processNext(Scan s) {
		totalsum += s.getInt(fldname);
	}

	@Override
	public String inputFieldName() {
		return fldname;
	}

	/**
	 * Return the field's name, prepended by "sumof".
	 * 
	 * @see simpledb.materialize.AggregationFn#fieldName()
	 */
	public String fieldName() {
		return "sumof" + fldname;
	}

	/**
	 * Return the current sum.
	 * 
	 * @see simpledb.materialize.AggregationFn#value()
	 */
	public Constant value() {
		return new Constant(totalsum);
	}
}
