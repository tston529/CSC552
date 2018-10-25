package com.test;

public class Percentage extends ResultCell {

    private String id;
    private String location;
    private String type;
    private String value;
    private String description;
    
    /**
     * Constructor; only sets values.
     * @param hdId Row's unique ID number
     * @param hdName Row's county and/or state
     * @param hdType Whether the row is for the county or the state
     * @param hdYear The year from which this row's data was accurate
     * @param hdVar Contains the general topic of what the row's data is about
     * @param hdVal A numerical value associated with the row
     */
    Percentage(String id, String tp, String vl, String desc, String loc) {
        location = loc;
        description = desc;
        float fVal = Float.parseFloat(vl)*100;
        value = Float.toString(fVal) + "%";
    }

    /**
     * Returns the row's ID
     * @return the row's ID
     */
    public String getId() { return id; }

    /**
     * Returns the row's associated county/state
     * @return row's associated county/state
     */
    public String getLocation() { return location; }

    public String getType() { return type; }
    public String getValue(){ return value; }
    public String getDescription() { return description }

    /* Mutators */
    private void setId(String hdId) { id = hdId; }

    private void setLocation(String hdLocation) { location = hdLocation; }

    private void setType(String hdType) { type = hdType; }

    private void setYear(int hdYear) { year = hdYear; }

    private void setVariable(String hdVar) { variable = hdVar; }

    private void setValue(float hdVal) { value = hdVal; }
}