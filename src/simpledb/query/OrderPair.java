package simpledb.query;

import simpledb.record.Schema;

public class OrderPair {
    private String fldname;
    private boolean desc;

    public OrderPair(String fldname) {
        this(fldname, false);
    }

    public OrderPair(String fldname, boolean desc) {
        this.fldname = fldname;
        this.desc = desc;
    }

    public boolean appliesTo(Schema sch) {
        return sch.hasField(fldname);
    }

    public String getFldname() {
        return fldname;
    }

    public boolean isDesc() {
        return desc;
    }
}
