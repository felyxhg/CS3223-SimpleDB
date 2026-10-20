package simpledb.query;

import simpledb.record.Schema;

/**
 * A pair of a field name and a sort direction,
 * corresponding to one entry of an order by clause.
 */
public class OrderPair {
    private String fldname;
    private boolean desc;

    /**
     * Create a pair that sorts the specified field
     * in ascending order.
     * @param fldname the name of the field
     */
    public OrderPair(String fldname) {
        this(fldname, false);
    }

    /**
     * Create a pair that sorts the specified field
     * in the specified direction.
     * @param fldname the name of the field
     * @param desc true for descending, false for ascending order
     */
    public OrderPair(String fldname, boolean desc) {
        this.fldname = fldname;
        this.desc = desc;
    }

    /**
     * Return true if the field of the pair
     * is contained in the specified schema.
     * @param sch the schema
     * @return true if the schema contains the field
     */
    public boolean appliesTo(Schema sch) {
        return sch.hasField(fldname);
    }

    /**
     * Returns the name of the field.
     * @return the name of the field
     */
    public String getFldname() {
        return fldname;
    }

    /**
     * Returns true if the field is sorted in descending order.
     * @return true for descending, false for ascending order
     */
    public boolean isDesc() {
        return desc;
    }

    /**
     * Returns the field name followed by its sort direction,
     * for example "gradyear desc".
     * @return the string representation of the pair
     */
    @Override
    public String toString() {
        return fldname + (desc ? " desc" : " asc");
    }
}
